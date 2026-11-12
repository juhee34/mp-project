"""Copy approved art and current readable stories into the static showcase."""
from pathlib import Path
import json, shutil, re
root=Path(__file__).resolve().parents[1]
out=root/'website/dist'; assets=out/'assets'; assets.mkdir(parents=True,exist_ok=True)
chars=json.loads((root/'app/src/main/assets/content/characters.json').read_text(encoding='utf-8'))
stories=json.loads((root/'app/src/main/assets/content/stories.json').read_text(encoding='utf-8'))
manifest=json.loads((root/'app/src/main/assets/backgrounds/manifest.json').read_text(encoding='utf-8'))
expansions=[]
for number in range(1,4):
    source=(root/f'docs/STORY_EXPANSION_0{number}.md').read_text(encoding='utf-8')
    for match in re.finditer(r'^## (.*?) — (.*?)\n(.*?)(?=^## |\Z)', source, re.M|re.S):
        name,title,section=match.groups()
        sid=re.search(r'^대상 ID: (\w+)',section,re.M)[1]
        pages=[]
        for scene in re.finditer(r'^### (.*?)\n(.*?)(?=^### |\Z)',section,re.M|re.S):
            blocks=[]
            for paragraph in re.split(r'\n\s*\n',scene[2].strip()):
                speech=re.fullmatch(r'\*\*([^:*]+):\*\*\s*“(.*)”',paragraph,re.S)
                blocks.append({'speaker':speech[1] if speech else None,'text':speech[2] if speech else paragraph.strip()})
            pages.append({'title':scene[1],'blocks':blocks})
        assert len(pages)==4, sid
        expansions.append({'id':sid,'characterId':sid.removesuffix('_episode_2'),'title':title,
            'kind':'episode','status':'ready','publicationStatus':'web_only_app_pending',
            'body':'\n\n'.join(b['text'] for p in pages for b in p['blocks']),'pages':pages})
assert len(expansions)==6
stories=[s for s in stories if s['id'] not in {x['id'] for x in expansions}]+expansions
for c in chars:
    area=next(x for x in manifest['characters'] if x['id']==c['id'])
    shutil.copy2(root/f"app/src/main/res/drawable-nodpi/sprite_{c['id']}_idle_0.png",assets/f"{c['id']}.png")
data={'characters':[{k:c[k] for k in ['id','name','species','home','personality','color']} for c in chars],
      'stories':[{k:s[k] for k in ['id','characterId','title','kind','body','pages']} for s in stories if s['status']=='ready']}
(out/'data.js').write_text('window.TINY_DATA = '+json.dumps(data,ensure_ascii=False)+';\n',encoding='utf-8')
shutil.copy2(root/'docs/PROJECT_INTRO.md',out/'TINY_RUNNERS_INTRO.md')
print(f'Exported 6 sprites, {len(data["stories"])} readable stories (6 web-only expansions) and presentation document. App assets unchanged. Outlines excluded.')
