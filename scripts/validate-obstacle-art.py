"""Validate exported PNG bytes with stdlib only; no image edits. Python 3."""
import csv, hashlib, json, re, struct, sys, zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'docs/art/obstacles'
IDS = ['mint', 'cheese', 'popo', 'lumi', 'nunu', 'piko']

def decode(path):
    data = path.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', path
    at, payload = 8, bytearray()
    while at < len(data):
        n = int.from_bytes(data[at:at+4], 'big')
        typ, chunk = data[at+4:at+8], data[at+8:at+8+n]
        assert zlib.crc32(typ+chunk) & 0xffffffff == int.from_bytes(data[at+8+n:at+12+n], 'big'), 'PNG CRC'
        if typ == b'IHDR':
            w,h,depth,kind,comp,filt,inter = struct.unpack('>IIBBBBB', chunk)
            assert (depth,kind,comp,filt,inter) == (8,6,0,0,0), 'Expected noninterlaced RGBA8'
        if typ == b'IDAT': payload.extend(chunk)
        at += n+12
        if typ == b'IEND': break
    raw = zlib.decompress(payload)
    stride = w*4
    assert len(raw) == h*(stride+1)
    prev = bytearray(stride); pixels = bytearray()
    for y in range(h):
        ft = raw[y*(stride+1)]; row = bytearray(raw[y*(stride+1)+1:(y+1)*(stride+1)])
        assert ft in range(5)
        for x in range(stride):
            a = row[x-4] if x >= 4 else 0; b = prev[x]; c = prev[x-4] if x >= 4 else 0
            p = a+b-c; pa,pb,pc = abs(p-a),abs(p-b),abs(p-c)
            predictor = [0,a,b,(a+b)//2,a if pa<=pb and pa<=pc else b if pb<=pc else c][ft]
            row[x] = (row[x]+predictor)&255
        pixels.extend(row); prev = row
    return w,h,pixels

def inspect(path):
    w,h,p = decode(path)
    assert (w,h)==(48,80), path.name
    points=set()
    for y in range(h):
        for x in range(w):
            i=(y*w+x)*4; a=p[i+3]
            assert a in (0,255), 'Partial alpha: '+path.name
            if a: points.add((x,y))
            else: assert p[i:i+3] == b'\0\0\0', 'Hidden RGB'
    assert points, 'Empty PNG'
    x0=min(x for x,y in points); x1=max(x for x,y in points)
    y0=min(y for x,y in points); y1=max(y for x,y in points)
    assert x0>=2 and x1<=45 and y0>=8 and y1==77, 'Padding/baseline'
    todo=[next(iter(points))]; seen=set(todo)
    while todo:
        x,y=todo.pop()
        for dx in (-1,0,1):
            for dy in (-1,0,1):
                q=(x+dx,y+dy)
                if q in points and q not in seen: seen.add(q); todo.append(q)
    assert seen==points, 'Detached pixels'
    return [x0,y0,x1-x0+1,y1-y0+1], len(points)

def main():
    rows=list(csv.DictReader((BASE/'export.csv').open(encoding='utf-8-sig')))
    assert len(rows)==18
    sprites=[]
    for r in rows:
        name=r['id']; assert re.fullmatch(r'obstacle_[a-z_]+',name)
        p=BASE/'png'/(name+'.png')
        rect,count=inspect(p)
        assert rect == [int(r[k]) for k in ('x','y','w','h')]
        sprites.append(dict(id=name,characterId=r['characterId'],variant=int(r['variant']),file='png/'+p.name,
            androidResource=name,frame=[48,80],opaqueRect=rect,anchor=[24,78],lastOpaqueRow=77,
            sha256=hashlib.sha256(p.read_bytes()).hexdigest(),opaquePixels=count,
            source='source/'+r['source'],sourceCrop=[int(r[k]) for k in ('cropX','cropY','cropW','cropH')]))
    assert len({s['id'] for s in sprites})==18
    assert {p.name for p in (BASE/'png').glob('*.png')} == {s['id']+'.png' for s in sprites}
    for char in IDS: assert sorted(s['variant'] for s in sprites if s['characterId']==char)==[0,1,2]
    manifest=dict(schemaVersion=1,status='art_ready_for_integration',integrated=False,
        coordinates='zero-based; opaqueRect=[x,y,width,height]; anchor is pixel edge',
        alpha='binary 0/255',sampling='nearest; no mipmaps',sprites=sprites)
    target=BASE/'manifest.json'
    if '--write-manifest' in sys.argv: target.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    else: assert json.loads(target.read_text(encoding='utf-8'))==manifest, 'Manifest stale'
    print('PASS: 18 RGBA PNGs; 6 characters x 3; 48x80; binary alpha; zero hidden RGB; baseline y77; 2px margins; one connected component; manifest bounds and SHA-256.')

if __name__=='__main__': main()
