"""Build reversible review WAVs from the verified Puddle Jumpers PCM decode.

Usage: python scripts/build-piko-audio-drafts.py INPUT_PCM_WAV ORIGINAL_MP3
Requires 44.1 kHz stereo signed 16-bit PCM. Outputs are unapproved drafts.
"""
import array
import hashlib
import json
import math
from pathlib import Path
import sys
import wave
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs' / 'audio' / 'piko-drafts'

def rms(samples):
    return math.sqrt(sum(x*x for x in samples) / len(samples)) / 32768

def db(value):
    return 20 * math.log10(max(value, 1e-12))

def save(name, samples, rate, channels):
    peak = max(abs(x) for x in samples)
    gain = min(1.0, 32767 * 10**(-1.5/20) / peak)
    output = array.array('h', (round(x * gain) for x in samples))
    path = OUT / name
    with wave.open(str(path), 'wb') as stream:
        stream.setparams((channels, 2, rate, 0, 'NONE', 'not compressed'))
        stream.writeframes(output.tobytes())
    with wave.open(str(path), 'rb') as stream:
        checked = array.array('h', stream.readframes(stream.getnframes()))
        assert stream.getnchannels() == channels
        assert stream.getframerate() == rate
        assert stream.getsampwidth() == 2
        assert checked == output
    assert max(abs(x) for x in output) < 32767
    result = dict(file=name, durationSeconds=len(output)/channels/rate,
                  samplePeakDbfs=db(max(abs(x) for x in output)/32768),
                  rmsDbfs=db(rms(output)), gainDb=db(gain),
                  fullScaleSamples=sum(x >= 32767 or x <= -32768 for x in output),
                  boundaryStepDbfs=db(max(abs(output[-channels+c]-output[c])
                                         for c in range(channels))/32768),
                  first250msRmsDbfs=db(rms(output[:round(rate*.25)*channels])),
                  last250msRmsDbfs=db(rms(output[-round(rate*.25)*channels:])),
                  sha256=hashlib.sha256(path.read_bytes()).hexdigest())
    return result

def main():
    source, original = map(Path, sys.argv[1:3])
    expected = '73afda21507f15bcd1da33bb6f48b3ec60be1ac9be2a484645b41a44e8836751'
    assert hashlib.sha256(original.read_bytes()).hexdigest() == expected, 'Different source MP3'
    with wave.open(str(source), 'rb') as stream:
        rate, channels = stream.getframerate(), stream.getnchannels()
        assert (rate, channels, stream.getsampwidth()) == (44100, 2, 2)
        pcm = array.array('h', stream.readframes(stream.getnframes()))
    OUT.mkdir(parents=True, exist_ok=True)
    def segment(start, end):
        return list(pcm[round(start*rate)*channels:round(end*rate)*channels])

    # Preserve the quiet musical introduction and release; remove exact zero padding.
    first = next(i for i, x in enumerate(pcm) if x) // channels
    last = max(i for i, x in enumerate(pcm) if x) // channels
    clean = list(pcm[first*channels:(last+1)*channels])
    fade = round(rate*.008)
    for i in range(fade):
        g = .5-.5*math.cos(math.pi*i/(fade-1))
        for c in range(channels):
            clean[i*channels+c] *= g
            clean[(len(clean)//channels-1-i)*channels+c] *= g
    clean_result = save('Puddle_Jumpers_trimmed_draft.wav', clean, rate, channels)

    # Main-body candidate, not a beat- or harmony-approved edit.
    start, end, overlap = 6.1, 34.9, .12
    raw = segment(start, end)
    cross = round(rate*overlap)
    frames = len(raw)//channels
    mixed=[]
    for i in range(cross):
        weight = .5-.5*math.cos(math.pi*i/(cross-1))
        for c in range(channels):
            tail = raw[(frames-cross+i)*channels+c]
            head = raw[i*channels+c]
            mixed.append((1-weight)*tail+weight*head)
    loop = raw[cross*channels:(frames-cross)*channels] + mixed
    loop_result = save('Puddle_Jumpers_loop_draft.wav', loop, rate, channels)
    report = dict(status='technical_drafts_not_listening_approved',sourceFile=original.name,
                  sourceSha256=expected,sourcePcmDurationSeconds=len(pcm)/channels/rate,
                  sourcePcmSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                  trimmedBoundsSeconds=[first/rate,(last+1)/rate],
                  loopSourceBoundsSeconds=[start,end],crossfadeSeconds=overlap,
                  loopMethod='Raised-cosine tail/head overlap, rotated to continuous sample boundary',
                  samplePeakCeilingDbfs=-1.5,listeningPerformed=False,
                  beatAlignmentVerified=False,harmonyVerified=False,
                  warning='Sample peaks are not true peaks; original saturation cannot be restored by attenuation.',
                  outputs=[clean_result,loop_result])
    (OUT/'measurements.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    (OUT/'README.txt').write_text(
        'Puddle Jumpers / 피코 추정 음악 — 수정 시험본\n\n'
        '원본 MP3는 변경하지 않았습니다. 실제 청음 및 게임 적용 전입니다.\n'
        '1. trimmed_draft.wav: 앞뒤의 정확한 무음 약 3.45초 제거, 음악의 조용한 도입부 보존. 약 34.53초. 반복용 확정본이 아닙니다.\n'
        '2. loop_draft.wav: 원본 디코딩 시각 6.10~34.90초에서 120ms 겹침 연결. 약 28.68초. 박자·화성 연결은 청음으로 확인해야 합니다.\n'
        '두 파일 모두 44.1kHz/16bit/stereo PCM WAV. 샘플 피크 상한 -1.5dBFS. LUFS/true peak 기준 정규화는 아닙니다.\n'
        'MP3 재압축으로 추가 손실이나 패딩을 만들지 않도록 WAV로 내보냈습니다. 압축 원본의 손실을 복구한 것은 아닙니다.\n'
        'loop_draft를 3회 이상 반복해서 박자가 겹치거나 건너뛰는지, 화성이 어색한지, 게임 효과음을 가리는지 확인해주세요.\n',encoding='utf-8')
    archive=ROOT/'docs'/'audio'/'Puddle_Jumpers_edit_drafts.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as zip_file:
        for path in sorted(OUT.iterdir()):
            if path.is_file(): zip_file.write(path,path.name)
    with zipfile.ZipFile(archive) as zip_file:
        assert zip_file.testzip() is None
    print(json.dumps(report,indent=2))

if __name__ == '__main__':
    main()
