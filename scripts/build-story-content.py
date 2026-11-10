"""Export reviewed in-repo manuscripts to the app's story catalogue."""
from pathlib import Path
import json, re

ROOT=Path(__file__).resolve().parents[1]
IDS=['mint','cheese','popo','lumi','nunu','piko']
NAMES=['라일','치즈','포포','루미','누누','피코']
records=[]

def blocks(text):
    out=[]
    for p in re.split(r'\n\s*\n',text.strip()):
        p=p.strip()
        if not p: continue
        m=re.fullmatch(r'\*\*([^:*]+):\*\*\s*(.*)',p,re.S)
        if m:
            line=m[2].strip()
            if line.startswith('“') and line.endswith('”'): line=line[1:-1]
            out.append({'speaker':m[1],'text':line})
        else: out.append({'speaker':None,'text':p})
    return out

def paginate(title,text):
    out=[]; current=[]; length=0
    for block in blocks(text):
        size=len(block['text'])
        if current and (length+size>270 or len(current)>=4):
            out.append({'title':title,'blocks':current});current=[];length=0
        current.append(block);length+=size
    if current: out.append({'title':title,'blocks':current})
    return out

for index,(cid,name) in enumerate(zip(IDS,NAMES)):
    file=ROOT/f'docs/STORY_MANUSCRIPT_0{index//2+1}.md'
    source=file.read_text(encoding='utf-8')
    section=re.search(r'^## '+name+r':.*?(?=^## |\Z)',source,re.M|re.S).group()
    background=re.search(r'### 배경 이야기\s*\n(.*?)\n### 첫 에피소드',section,re.S)[1].strip()
    ep=re.search(r'### 첫 에피소드 — (.*?)\n(.*?)\n### 도감용',section,re.S)
    summary=re.search(r'- 목록 요약: (.*)',section)[1]
    after=re.search(r'- 읽은 뒤 문구: (.*)',section)[1]
    pages=[]
    for scene in re.finditer(r'^#### (.*?)\n(.*?)(?=^#### |\Z)',ep[2],re.M|re.S):
        pages.extend(paginate(scene[1],scene[2]))
    def entry(suffix,title,body,status,kind,order,ps,distance,summary,after=''):
        return dict(id=cid+suffix,characterId=cid,title=title,body=body,status=status,kind=kind,order=order,
                    unlockDistance=distance,summary=summary,afterReading=after,pages=ps)
    records.append(entry('_intro',name+'의 집과 작은 바람',background,'ready','background',0,
                         paginate('집과 작은 바람',background),0,background.split('\n\n')[0]))
    epbody='\n\n'.join((b['speaker']+': ' if b['speaker'] else '')+b['text'] for p in pages for b in p['blocks'])
    records.append(entry('_episode_1',ep[1].strip(),epbody,'ready','episode',1,pages,100,summary,after))
    outlines=list(re.finditer(r'\*\*([23])편 — (.*?)\*\*\s*\n(.*?)(?=\n대사 방향|\n\*\*[23]편|\n## |\Z)',section,re.S))
    assert len(outlines)==2,(cid,len(outlines))
    for m in outlines:
        body=m[3].strip()
        records.append(entry('_episode_'+m[1],m[2],body,'outline','episode',int(m[1]),[],0,'다음 이야기를 준비하고 있어요.'))

target=ROOT/'app/src/main/assets/content/stories.json'
target.write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
cp=target.with_name('characters.json'); chars=json.loads(cp.read_text(encoding='utf-8'))
for c in chars: c['storyIds']=[s['id'] for s in records if s['characterId']==c['id']]
cp.write_text(json.dumps(chars,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for cid in IDS:
    e=next(s for s in records if s['id']==cid+'_episode_1')
    print(cid, 'first episode pages:',len(e['pages']),'dialogue turns:',sum(b['speaker'] is not None for p in e['pages'] for b in p['blocks']))
