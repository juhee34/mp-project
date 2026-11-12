"""Windows-only MP3 decoding and reversible technical music edits.

python scripts/build-music-review-batch.py SOURCE_DIRECTORY
No third-party packages. Outputs are listening-review candidates, not approved loops.
"""
import array
import ctypes as C
from ctypes import wintypes as W
import hashlib
import heapq
import json
import math
from pathlib import Path
import statistics
import struct
import sys
import wave
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/audio/all-music-drafts'
TRACKS = [('mint','라일','Little_Wooden_Steps'),('cheese','치즈','Soft_Paws_on_Window_Sills'),
          ('popo','포포','Summer_by_the_Harbor'),('lumi','루미','Strolling_Through_Amber'),
          ('nunu','누누','A_Seat_for_the_Unseen'),('piko','피코','Puddle_Jumpers')]

class Fmt(C.Structure):
    _pack_ = 1
    _fields_ = [('tag',W.WORD),('channels',W.WORD),('rate',W.DWORD),('avg',W.DWORD),
                ('align',W.WORD),('bits',W.WORD),('extra',W.WORD)]
class MP3(C.Structure):
    _pack_ = 1
    _fields_ = [('fmt',Fmt),('id',W.WORD),('flags',W.DWORD),('block',W.WORD),
                ('frames',W.WORD),('delay',W.WORD)]
class Header(C.Structure):
    _fields_ = [('cbStruct',W.DWORD),('fdwStatus',W.DWORD),('dwUser',C.c_size_t),
                ('pbSrc',C.c_void_p),('cbSrcLength',W.DWORD),('cbSrcLengthUsed',W.DWORD),
                ('dwSrcUser',C.c_size_t),('pbDst',C.c_void_p),('cbDstLength',W.DWORD),
                ('cbDstLengthUsed',W.DWORD),('dwDstUser',C.c_size_t),
                ('dwReservedDriver',C.c_size_t*10)]

def check(code, operation):
    if code: raise RuntimeError(f'{operation}: Windows ACM error {code}')

def decode(path):
    original = path.read_bytes()
    offset = 0
    if original[:3] == b'ID3':
        offset = 10 + sum((original[6+j]&127) << (7*(3-j)) for j in range(4))
        if original[3] == 4 and original[5]&16: offset += 10
    data = original[offset:]
    pos, count, rates, bitrates, channel_counts, sizes = 0, 0, set(), set(), set(), []
    while pos+4 <= len(data):
        if data[pos:pos+3] == b'TAG' and len(data)-pos == 128: break
        h = int.from_bytes(data[pos:pos+4],'big')
        if h>>21 != 0x7ff or (h>>19)&3 != 3 or (h>>17)&3 != 1:
            raise ValueError(f'Expected MPEG-1 Layer III frame at {pos}')
        br_index, sr_index = (h>>12)&15, (h>>10)&3
        if br_index in (0,15) or sr_index == 3: raise ValueError('Unsupported MP3 header')
        bitrate = [0,32,40,48,56,64,80,96,112,128,160,192,224,256,320][br_index]*1000
        rate = [44100,48000,32000][sr_index]
        channels = 1 if (h>>6)&3 == 3 else 2
        size = 144*bitrate//rate + ((h>>9)&1)
        if pos+size > len(data): raise ValueError('Truncated MP3 frame')
        rates.add(rate); bitrates.add(bitrate); channel_counts.add(channels); sizes.append(size)
        count += 1; pos += size
    if len(rates)!=1 or len(bitrates)!=1 or len(channel_counts)!=1:
        raise ValueError('This checked decoder supports constant rate/channels/bitrate only')
    data=data[:pos]; rate=rates.pop(); channels=channel_counts.pop(); bitrate=bitrates.pop()
    codec=C.WinDLL('msacm32'); handle=C.c_void_p()
    codec.acmStreamOpen.argtypes=[C.POINTER(C.c_void_p),C.c_void_p,C.c_void_p,C.c_void_p,C.c_void_p,C.c_size_t,C.c_size_t,W.DWORD]
    codec.acmStreamSize.argtypes=[C.c_void_p,W.DWORD,C.POINTER(W.DWORD),W.DWORD]
    for name in ['acmStreamPrepareHeader','acmStreamConvert','acmStreamUnprepareHeader']:
        getattr(codec,name).argtypes=[C.c_void_p,C.POINTER(Header),W.DWORD]
    codec.acmStreamClose.argtypes=[C.c_void_p,W.DWORD]
    source=MP3(Fmt(85,channels,rate,bitrate//8,1,0,12),1,2,max(sizes),1,0)
    target=Fmt(1,channels,rate,rate*channels*2,channels*2,16,0)
    check(codec.acmStreamOpen(C.byref(handle),None,C.byref(source),C.byref(target),None,0,0,4),'open')
    prepared=False
    try:
        size=W.DWORD(); check(codec.acmStreamSize(handle,len(data),C.byref(size),0),'size')
        src=C.create_string_buffer(data); dst=C.create_string_buffer(size.value)
        header=Header(); header.cbStruct=C.sizeof(header)
        header.pbSrc=C.addressof(src); header.cbSrcLength=len(data)
        header.pbDst=C.addressof(dst); header.cbDstLength=size.value
        check(codec.acmStreamPrepareHeader(handle,C.byref(header),0),'prepare'); prepared=True
        check(codec.acmStreamConvert(handle,C.byref(header),0x10|0x20),'convert')
        assert header.cbSrcLengthUsed == len(data), 'Incomplete decoding'
        pcm=array.array('h',dst.raw[:header.cbDstLengthUsed])
        assert len(pcm)//channels == count*1152, 'Decoded frame-count mismatch'
    finally:
        if prepared: codec.acmStreamUnprepareHeader(handle,C.byref(header),0)
        codec.acmStreamClose(handle,0)
    return pcm,rate,channels,dict(file=path.name,sha256=hashlib.sha256(original).hexdigest(),
        bytes=len(original),mp3Frames=count,bitrate=bitrate,sampleRate=rate,channels=channels,
        decodedSeconds=len(pcm)/channels/rate,allFrameBytesConsumed=True)

def db(x): return 20*math.log10(max(x,1e-12))
def rms(a): return math.sqrt(sum(x*x for x in a)/max(1,len(a)))/32768

def choose_loop(pcm,rate,ch,first,last):
    # Match loudness envelope, onset timing, and a simple spectral-brightness proxy.
    # This deliberately does NOT claim to identify musical bars or harmonic phrases.
    hop=round(rate*.02); feature=[]
    for i in range(0,len(pcm),hop*ch):
        values=pcm[i:i+hop*ch]
        level=db(rms(values))
        diffs=[values[j]-values[j-ch] for j in range(ch,len(values))]
        brightness=db(rms(diffs))-level
        feature.append((level,brightness))
    body=[x[0] for x in feature if x[0]>-55]
    body_level=statistics.median(body)
    min_start=max(0,math.ceil(first/rate/.02))
    max_end=min(len(feature)-13,math.floor(last/rate/.02))
    active=[i for i in range(min_start,max_end) if feature[i][0]>=body_level-5]
    if not active: raise ValueError('No active music body')
    min_length=min(1300,max(800,active[-1]-active[0]-350))
    quiet_prefix=[0]
    for level,_ in feature:
        quiet_prefix.append(quiet_prefix[-1]+(level<body_level-12))
    candidates=[]
    for s in range(min_start+13,max_end-min_length,5):
        if feature[s][0]<body_level-5: continue
        for length in range(min_length,1701,5):
            e=s+length
            if e>max_end: break
            if feature[e][0]<body_level-5: continue
            head=feature[s:s+13]; tail=feature[e:e+13]
            energy=sum(abs(a[0]-b[0]) for a,b in zip(head,tail))/13
            color=sum(abs(a[1]-b[1]) for a,b in zip(head,tail))/13
            quiet=(quiet_prefix[e]-quiet_prefix[s])/length
            score=energy+.3*color+abs(length*.02-30)*.12+quiet*10
            item=(-score,s,e,energy,color)
            if len(candidates)<160: heapq.heappush(candidates,item)
            elif score < -candidates[0][0]: heapq.heapreplace(candidates,item)
    if not candidates: raise ValueError('No sufficiently long main-body loop candidate')
    # Check the actual rendered join, rather than only the pre-crossfade envelope.
    overlap=round(.16*rate); q=round(.25*rate); best=None
    weights=[.5-.5*math.cos(math.pi*i/(overlap-1)) for i in range(overlap)]
    for neg_score,si,ei,energy,color in candidates:
        s=round(si*.02*rate); e=round(ei*.02*rate)
        head=pcm[(s+overlap)*ch:(s+overlap+q)*ch]
        tail=list(pcm[(e-(q-overlap))*ch:e*ch])
        for i,g in enumerate(weights):
            for c in range(ch): tail.append((1-g)*pcm[(e+i)*ch+c]+g*pcm[(s+i)*ch+c])
        difference=abs(db(rms(head))-db(rms(tail)))
        score=-neg_score+3*difference
        if best is None or score<best[0]: best=(score,s,e,energy,color,difference)
    _,s,e,energy,color,difference=best
    return s,e,dict(selectionScore=best[0],renderedBoundaryDifferenceDb=difference,
        envelopeDifferenceDb=energy,brightnessProxyDifferenceDb=color,
        selection='20ms envelope/brightness proxy match; no beat or harmony verification')

def write_wav(path,values,rate,ch,gain):
    data=array.array('h',(round(x*gain) for x in values))
    assert all(-32768<x<32767 for x in data)
    path.parent.mkdir(parents=True,exist_ok=True)
    with wave.open(str(path),'wb') as f:
        f.setparams((ch,2,rate,0,'NONE','not compressed')); f.writeframes(data.tobytes())
    with wave.open(str(path),'rb') as f:
        assert (f.getframerate(),f.getnchannels(),f.getsampwidth()) == (rate,ch,2)
        assert array.array('h',f.readframes(f.getnframes())) == data
    k=round(.25*rate)*ch
    result=dict(file=str(path.relative_to(OUT)).replace('\\','/'),seconds=len(data)/ch/rate,
        samplePeakDbfs=db(max(abs(x) for x in data)/32768),rmsDbfs=db(rms(data)),
        head250msDbfs=db(rms(data[:k])),tail250msDbfs=db(rms(data[-k:])),
        fullScaleSamples=0,sha256=hashlib.sha256(path.read_bytes()).hexdigest())
    result['headTailDifferenceDb']=abs(result['head250msDbfs']-result['tail250msDbfs'])
    return result

def main():
    source=Path(sys.argv[1]); OUT.mkdir(parents=True,exist_ok=True); records=[]
    only=set(sys.argv[2].split(',')) if len(sys.argv)>2 else None
    prior={}
    if only:
        prior={r['characterId']:r for r in json.loads((OUT/'progress.json').read_text(encoding='utf-8'))}
    for cid,name,stem in TRACKS:
        if only and cid not in only:
            previous=prior[cid]
            assert hashlib.sha256((source/(stem+'.mp3')).read_bytes()).hexdigest()==previous['sha256']
            for o in previous['outputs']:
                assert hashlib.sha256((OUT/o['file']).read_bytes()).hexdigest()==o['sha256']
            records.append(previous)
            continue
        print('Decoding '+stem,flush=True)
        pcm,rate,ch,info=decode(source/(stem+'.mp3'))
        first=next(i for i,x in enumerate(pcm) if x)//ch
        last=max(i for i,x in enumerate(pcm) if x)//ch+1
        clean=list(pcm[first*ch:last*ch]); fade=round(.008*rate)
        for i in range(fade):
            g=.5-.5*math.cos(math.pi*i/(fade-1))
            for c in range(ch):
                clean[i*ch+c]*=g; clean[(len(clean)//ch-1-i)*ch+c]*=g
        s,e,selection=choose_loop(pcm,rate,ch,first,last)
        overlap=round(.16*rate)
        # Tail overlaps the matching segment at s; rotate to keep the file boundary contiguous.
        raw=pcm[s*ch:(e+overlap)*ch]; frames=len(raw)//ch
        mixed=[]
        for i in range(overlap):
            g=.5-.5*math.cos(math.pi*i/(overlap-1))
            for c in range(ch):
                mixed.append((1-g)*raw[(frames-overlap+i)*ch+c]+g*raw[i*ch+c])
        loop=list(raw[overlap*ch:(frames-overlap)*ch])+mixed
        # One gain per source: no compression, no time stretching, no amplification.
        peak=max(max(abs(x) for x in clean),max(abs(x) for x in loop))
        gain=min(1,32767*10**(-2/20)/peak,10**(-18/20)/rms(loop))
        outputs=[write_wav(OUT/'loops'/(cid+'_loop_draft.wav'),loop,rate,ch,gain),
                 write_wav(OUT/'trimmed'/(cid+'_trimmed.wav'),clean,rate,ch,gain)]
        # Verify the new wrap is precisely an adjacent pair from the source head.
        expected=[round(pcm[(s+overlap-1)*ch+c]*gain) for c in range(ch)]
        actual=[round(loop[-ch+c]*gain) for c in range(ch)]
        assert actual == expected
        info.update(characterId=cid,displayName=name,mappingConfirmed=False,
            exactSilenceRemovedSeconds=(first+len(pcm)//ch-last)/rate,
            trimmedSourceBoundsSeconds=[first/rate,last/rate],
            loopSourceBoundsSeconds=[s/rate,(e+overlap)/rate],crossfadeSeconds=overlap/rate,
            gainDb=db(gain),selection=selection,outputs=outputs,
            listeningApproved=False,wrapUsesAdjacentSourceSamples=True)
        records.append(info)
        (OUT/'progress.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print(json.dumps({'id':cid,'source':info['decodedSeconds'],'removed':info['exactSilenceRemovedSeconds'],
            'loop':outputs[0]['seconds'],'bounds':info['loopSourceBoundsSeconds'],
            'edgeDifference':outputs[0]['headTailDifferenceDb'],'rms':outputs[0]['rmsDbfs']},ensure_ascii=True),flush=True)
    report=dict(status='technical_edits_ready_for_listening_review',decoder='Windows ACM; no encoder delay/padding tag trimming',
        samplePeakCeilingDbfs=-2,targetLoopRmsDbfs=-18,notLufsNormalized=True,
        tempoHarmonyAndMoodVerified=False,tracks=records)
    (OUT/'measurements.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    rows=['| 캐릭터(추정) | 원본 디코딩 | 제거한 정확한 무음 | 공백 제거본 | 반복 시험본 | 경계 250ms 음량 차이 |',
          '| --- | ---: | ---: | ---: | ---: | ---: |']
    for r in records:
        a,b=r['outputs']; rows.append(f"| {r['displayName']} | {r['decodedSeconds']:.2f}초 | {r['exactSilenceRemovedSeconds']:.2f}초 | {b['seconds']:.2f}초 | {a['seconds']:.2f}초 | {a['headTailDifferenceDb']:.2f}dB |")
    text='# Tiny Runners 음악 6곡 수정 시험본\n\n2026-11-10. 원본 MP3 보존. 게임 미적용.\n\n'+'\n'.join(rows)
    text+='\n\nloops 폴더: 곡별 파형의 음량 변화와 밝기 지표가 비슷한 구간을 골라 160ms 겹침 연결한 반복 시험본입니다.\n'
    text+='trimmed 폴더: 앞뒤 정확한 0 샘플 구간을 제거하고 8ms 가장자리를 처리했습니다. 조용한 도입부와 잔향은 보존했습니다.\n'
    text+='\n44.1kHz/16bit/stereo PCM WAV. 추가 MP3 재압축 없음. 속도·음높이·악기 변경 없음. 샘플 피크 -2dBFS 이하, 반복본 RMS 목표 -18dBFS로 감쇠만 적용했습니다. RMS는 LUFS가 아니며 true peak/체감 음량 일치 검증은 아닙니다.\n'
    text+='\n청음하지 않았으므로 박자·화성·캐릭터 분위기의 자연스러움은 확정하지 않았습니다. 반복 시험본을 3회 이상 들어 박자 겹침·화성 변화·피로감을 확인한 뒤 게임에 연결하세요. 단순 파형 이음새와 음악적 이음새는 서로 다른 검증입니다.\n'
    text+='\n캐릭터 매핑: mint=라일, cheese=치즈, popo=포포, lumi=루미, nunu=누누, piko=피코. 파일명에 따른 추정 매핑입니다. 상세 출처 해시·편집 지점·검증 값은 measurements.json에 있습니다. 피코 반복본은 이번 비교 기준으로 다시 선정한 새 후보이며 이전 28.68초 시험본은 별도로 보존했습니다.\n'
    (OUT/'README.md').write_text(text,encoding='utf-8')
    archive=ROOT/'docs/audio/Tiny_Runners_6_music_edits.zip'
    paths=[OUT/'README.md',OUT/'measurements.json']+[OUT/o['file'] for r in records for o in r['outputs']]
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for path in paths: z.write(path,path.relative_to(OUT))
    with zipfile.ZipFile(archive) as z:
        assert len(z.namelist())==14 and z.testzip() is None
    print('ARCHIVE_BYTES '+str(archive.stat().st_size),flush=True)

if __name__=='__main__': main()
