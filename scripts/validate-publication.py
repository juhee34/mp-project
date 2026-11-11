from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import urlsplit,unquote
import json,xml.etree.ElementTree as ET,subprocess,ast,re,hashlib
W=Path(__file__).resolve().parents[1]
pending=[p.relative_to(W).as_posix() for folder in ['docs/audio','docs/intro','docs/dialogue','docs/art/loop-review-20261111','website'] for p in (W/folder).rglob('*') if p.is_file() and '.git' not in p.parts]
changed=['docs/PROJECT_INTRO.md','app/src/main/res/values/intro_strings.xml','app/src/main/res/values/character_screen_strings.xml','scripts/build-music-review-batch.py','scripts/build-piko-audio-drafts.py','scripts/inspect-music-metadata.py']
counts={'json':0,'xml':0,'python':0,'html_refs':0}
for name in sorted(set(pending+changed)):
    p=W/name
    if p.suffix=='.json': json.loads(p.read_text(encoding='utf-8-sig'));counts['json']+=1
    elif p.suffix=='.xml': ET.parse(p);counts['xml']+=1
    elif p.suffix=='.py': ast.parse(p.read_text(encoding='utf-8-sig'));counts['python']+=1
    if p.suffix in ('.md','.txt','.json','.py','.ps1','.xml','.html','.js','.css'):
        assert not re.search(r'C:[/\\]Users[/\\]sinbe',p.read_text(encoding='utf-8-sig')),name
keys=[]
for p in (W/'app/src/main/res/values').glob('*.xml'):
    for e in ET.parse(p).getroot():
        if e.tag=='string': keys.append(e.attrib['name'])
assert len(keys)==len(set(keys)),'Duplicate string keys'
strings={e.attrib['name']:e.text for e in ET.parse(W/'app/src/main/res/values/character_screen_strings.xml').getroot() if e.tag=='string'}
expected=['오늘은 이쪽으로 가 볼까? 어, 처음 보는 꽃이다!','마침 산책 가려던 참이야. 네 자리도 비워 뒀어.','저쪽에서 처음 듣는 소리가 나. 같이 들어 볼래?','어서 와! 오늘 첫 관객은 너야. 꼬리 조심, 살짝 앉아!','나 어디 있게? …히히, 또 웃다가 들켰네.','앗, 물웅덩이다! 장화 챙겼어? 같이 첨벙하자!','친구 %1$d명이 기다리고 있어요. 마음이 가는 친구를 톡 고르고, 함께 달려 볼까요?']
assert all(s in strings.values() for s in expected),'Approved greetings/hint'
assert (W/'docs/PROJECT_INTRO.md').read_bytes()==(W/'website/dist/TINY_RUNNERS_INTRO.md').read_bytes()
assert (W/'docs/intro/intro_strings.reviewed.xml').read_bytes()==(W/'app/src/main/res/values/intro_strings.xml').read_bytes()
base=json.loads((W/'app/src/main/assets/content/talks.json').read_text(encoding='utf-8'))
review=json.loads((W/'docs/dialogue/talks.reviewed.json').read_text(encoding='utf-8'))
assert base.keys()==review.keys() and len(review['prologue'])==6
ids=['mint','cheese','popo','lumi','nunu','piko']
assert [t['characterId'] for t in review['talks']]==ids
for old,new in zip(base['talks'],review['talks']):
    assert old.keys()==new.keys()
    assert len(new['lines'])==6
    assert [l['speaker'] for l in old['lines']]==[l['speaker'] for l in new['lines']]
    assert all(l.keys()=={'speaker','text'} and l['text'].strip() for l in new['lines'])
    assert new['lines'][-1]['speaker']==new['characterId']
web=json.loads((W/'website/dist/data.js').read_text(encoding='utf-8').removeprefix('window.TINY_DATA = ').strip().removesuffix(';'))
assert [c['id'] for c in web['characters']]==ids
appstories=json.loads((W/'app/src/main/assets/content/stories.json').read_text(encoding='utf-8'))
assert len(web['stories'])==12
ready={s['id']:s for s in appstories if s['status']=='ready'}
assert {s['id'] for s in web['stories']}==ready.keys()
for s in web['stories']:
    assert all(v==ready[s['id']][k] for k,v in s.items())
class Links(HTMLParser):
    def __init__(self):super().__init__();self.refs=[];self.ids=set()
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if 'id' in a:self.ids.add(a['id'])
        for k in ['src','href']:
            if k in a:self.refs.append(a[k])
htmls=[W/'website/dist/index.html',W/'docs/art/loop-review-20261111/loop-review.html']
for page in htmls:
    parser=Links();parser.feed(page.read_text(encoding='utf-8'))
    for ref in parser.refs:
        url=urlsplit(ref)
        if url.scheme or ref.startswith('//'):continue
        if not url.path:
            assert not url.fragment or url.fragment in parser.ids,(page,ref)
        else:assert (page.parent/unquote(url.path)).exists(),(page,ref)
        counts['html_refs']+=1
for cid in ids:
    assert (W/f'website/dist/assets/{cid}.png').exists()
    assert (W/f'website/dist/assets/{cid}-home.png').exists()
for js in ['website/dist/app.js','website/dist/data.js']:
    subprocess.run(['node','--check',js],cwd=W,check=True)
subprocess.run(['node','scripts/validate-content.cjs'],cwd=W,check=True)
subprocess.run(['git','diff','--check'],cwd=W,check=True)
print('PASS publication checks',counts,'; 6 greetings+hint; reviewed talks 6x6; 12 ready web stories; intro copies; assets; JS; whitespace')
