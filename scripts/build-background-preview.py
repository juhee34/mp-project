"""Build an offline review viewport; never modifies approved image pixels."""
import base64
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
ids = ['mint', 'cheese', 'popo', 'lumi', 'nunu', 'piko']
sprites = {key: {
    frame: 'data:image/png;base64,' + base64.b64encode(
        (root / 'app/src/main/res/drawable-nodpi' / f'sprite_{key}_{frame}.png').read_bytes()
    ).decode()
    for frame in ['idle_0', 'run_0', 'run_1', 'run_2', 'run_3']
} for key in ids}
template = (root / 'scripts/background-preview.template.html').read_text(encoding='utf-8')
out = root / 'docs/art/review/background-preview.html'
out.parent.mkdir(parents=True, exist_ok=True)
manifest = json.loads((root / 'docs/art/background-manifest.json').read_text(encoding='utf-8'))
backgrounds = ['data:image/png;base64,' + base64.b64encode((root / 'docs/art/background-source' / name).read_bytes()).decode() for name in ['mint-cheese.png','popo-lumi.png','nunu-piko.png']]
out.write_text(template.replace('__SPRITES__', json.dumps(sprites)).replace('__BACKGROUNDS__', json.dumps(backgrounds)).replace('__MANIFEST__', json.dumps(manifest)), encoding='utf-8')
print(out)
