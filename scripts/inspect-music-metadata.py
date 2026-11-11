"""Read MP4 container metadata only; does not decode or judge the audio.

Usage: python scripts/inspect-music-metadata.py SOURCE_DIRECTORY
"""
import hashlib
import json
from pathlib import Path
import struct
import sys

TRACKS = [
    ('mint', 'Little_Wooden_Steps.mp4'),
    ('cheese', 'Soft_Paws_on_Window_Sills.mp4'),
    ('popo', 'Summer_by_the_Harbor.mp4'),
    ('lumi', 'Strolling_Through_Amber.mp4'),
    ('nunu', 'A_Seat_for_the_Unseen.mp4'),
    ('piko', 'Puddle_Jumpers.mp4'),
]

def boxes(data, start=0, end=None):
    end = len(data) if end is None else end
    while start + 8 <= end:
        size, tag = struct.unpack_from('>I4s', data, start)
        header = 8
        if size == 1:
            size = struct.unpack_from('>Q', data, start + 8)[0]
            header = 16
        elif size == 0:
            size = end - start
        if size < header or start + size > end:
            raise ValueError('Invalid MP4 box bounds')
        yield tag, start + header, start + size
        start += size

def descendants(data, start=0, end=None):
    for tag, a, b in boxes(data, start, end):
        yield tag, a, b
        if tag in [b'moov', b'trak', b'mdia', b'minf', b'stbl', b'udta']:
            yield from descendants(data, a, b)

def inspect(path, cid):
    raw = path.read_bytes()
    result = dict(file=path.name, proposedCharacterId=cid,
                  mappingConfirmed=False, bytes=len(raw),
                  sha256=hashlib.sha256(raw).hexdigest(),mediaTracks=[])
    for tag, a, b in descendants(raw):
        if tag in [b'mvhd', b'mdhd']:
            version = raw[a]
            offset = a + (20 if version == 1 else 12)
            scale = struct.unpack_from('>I', raw, offset)[0]
            duration = struct.unpack_from('>Q' if version == 1 else '>I', raw, offset + 4)[0]
            if tag == b'mvhd':
                result['containerDurationSeconds'] = duration / scale
            else:
                result['mediaTracks'].append(dict(timescale=scale,durationSeconds=duration/scale))
        elif tag == b'stsd':
            for codec, x, y in boxes(raw, a+8, b):
                if codec == b'mp4a':
                    result['audioSampleEntry'] = dict(codec='mp4a',
                        channels=struct.unpack_from('>H',raw,x+16)[0],
                        sampleRate=struct.unpack_from('>I',raw,x+24)[0]/65536)
    return result

if __name__ == '__main__':
    source=Path(sys.argv[1])
    results=[inspect(source/name,cid) for cid,name in TRACKS]
    output=Path(__file__).resolve().parents[1]/'docs'/'audio'
    output.mkdir(parents=True,exist_ok=True)
    report=dict(status='metadata_only_audio_review_blocked',
                listeningPerformed=False,decodedSignalAnalyzed=False,tracks=results)
    (output/'metadata.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))
