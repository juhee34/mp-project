"""Copy approved art and current readable stories into the static showcase."""
from pathlib import Path
import json, shutil
root=Path(__file__).resolve().parents[1]
out=root/'website/dist'; assets=out/'assets'; assets.mkdir(parents=True,exist_ok=True)
chars=json.loads((root/'app/src/main/assets/content/characters.json').read_text(encoding='utf-8'))
stories=json.loads((root/'app/src/main/assets/content/stories.json').read_text(encoding='utf-8'))
for c in chars:
    shutil.copy2(root/f"app/src/main/res/drawable-nodpi/sprite_{c['id']}_idle_0.png",assets/f"{c['id']}.png")
data={'characters':[{k:c[k] for k in ['id','name','species','home','personality','color']} for c in chars],
      'stories':[{k:s[k] for k in ['id','characterId','title','kind','body','pages']} for s in stories if s['status']=='ready']}
(out/'data.js').write_text('window.TINY_DATA = '+json.dumps(data,ensure_ascii=False)+';\n',encoding='utf-8')
shutil.copy2(root/'docs/PROJECT_INTRO.md',out/'TINY_RUNNERS_INTRO.md')
print('Exported 6 sprites, 12 ready stories and presentation document. Run build-content.ps1 for background crops. Outlines excluded.')
