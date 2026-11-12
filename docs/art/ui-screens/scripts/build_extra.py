"""9-patch PNGs, Android shape XMLs, recomposed previews + diff check."""
import cv2, numpy as np, json, os, sys, subprocess
from PIL import Image, ImageDraw, ImageFont
sys.path.insert(0, os.path.dirname(__file__))
from lib import ninepatch

os.makedirs('out/ninepatch', exist_ok=True); os.makedirs('out/xml', exist_ok=True); os.makedirs('out/preview', exist_ok=True)
P = lambda n: cv2.imread(f'out/parts/{n}.png', cv2.IMREAD_UNCHANGED)

# ---------- 9-patch ----------
def np_pill(name, left_fixed=None, right_pad=None):
    p = P(name); h, w = p.shape[:2]; r = h // 2
    lx = left_fixed or r + 2
    out = ninepatch(p, (lx, w - r - 2), (r - 1, r + 1), (left_fixed or r, w - (right_pad or r)), (8, h - 8))
    cv2.imwrite(f'out/ninepatch/{name}.9.png', out)
for n in ['ui_menu_btn_primary', 'ui_menu_btn_secondary', 'ui_login_btn_primary', 'ui_login_btn_secondary',
          'ui_story_btn_primary', 'ui_story_btn_outline', 'ui_story_btn_disabled', 'ui_story_count_pill', 'ui_story_counter_pill']:
    np_pill(n)
np_pill('ui_story_back_button', left_fixed=120, right_pad=30)
p = P('ui_login_input'); h, w = p.shape[:2]
cv2.imwrite('out/ninepatch/ui_login_input.9.png', ninepatch(p, (28, w - 28), (28, h - 28), (24, w - 24), (8, h - 8)))
p = P('ui_story_card_frame'); h, w = p.shape[:2]
cv2.imwrite('out/ninepatch/ui_story_card_frame.9.png', ninepatch(p, (70, w - 50), (90, h - 70), (34, w - 34), (48, h - 34)))

# ---------- colours for XML shapes ----------
def hexc(bgr): return '#%02X%02X%02X' % (int(bgr[2]), int(bgr[1]), int(bgr[0]))
def sample(name):
    p = P(name)[:, :, :3].astype(int); h, w = p.shape[:2]
    col = p[:, int(w * 0.12)]             # a column through the straight part, away from text
    fill = np.median(p[h // 2 - 3:h // 2 + 3, int(w * 0.1):int(w * 0.15)].reshape(-1, 3), 0)
    lum = col.sum(1)
    # stroke = darkest run at the top edge
    top = col[1:h // 3]; idx = np.argmin(top.sum(1)); stroke = top[idx]
    d = np.sqrt(((col[1:h // 3] - fill) ** 2).sum(1)); sw = int((d > 40).sum())
    return fill, stroke, max(sw, 0), h
specs = {}
for n in ['ui_menu_btn_primary', 'ui_menu_btn_secondary', 'ui_login_btn_primary', 'ui_login_btn_secondary', 'ui_login_input',
          'ui_story_btn_primary', 'ui_story_btn_outline', 'ui_story_btn_disabled', 'ui_story_count_pill', 'ui_story_counter_pill', 'ui_story_back_button']:
    f, s, sw, h = sample(n)
    filled = np.sqrt(((f - s) ** 2).sum()) < 50          # solid button: stroke ~ fill
    specs[n] = dict(fill=hexc(f), stroke=hexc(s), stroke_px=sw if not filled else 2, height_px=h,
                    radius_px=24 if n == 'ui_login_input' else h // 2)
DP = 0.42   # 1 reference px ~ 0.42 dp on a 941 px tall design (≈ 395 dp landscape phone)
for n, s in specs.items():
    xml = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- {n}: fill {s['fill']}, stroke {s['stroke']} (from the approved mockup). Pressed state darkens ~8%. -->
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="{s['fill']}" />
            <stroke android:width="{max(2, round(s["stroke_px"] * DP + 0.5))}dp" android:color="{s['stroke']}" />
            <corners android:radius="{round(s['radius_px'] * DP)}dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="{s['fill']}" />
            <stroke android:width="{max(2, round(s["stroke_px"] * DP + 0.5))}dp" android:color="{s['stroke']}" />
            <corners android:radius="{round(s['radius_px'] * DP)}dp" />
        </shape>
    </item>
</selector>
'''
    # pressed colour: darken fill
    f = s['fill']; r, g, b = int(f[1:3], 16), int(f[3:5], 16), int(f[5:7], 16)
    dark = '#%02X%02X%02X' % (int(r * .92), int(g * .92), int(b * .92))
    xml = xml.replace(f'<solid android:color="{f}" />', f'<solid android:color="{dark}" />', 1)
    open(f"out/xml/{n.replace('ui_', 'shape_')}.xml", 'w').write(xml)
json.dump(specs, open('out/colors.json', 'w'), indent=1)

# ---------- recomposed previews ----------
layout = json.load(open('out/layout.json'))
FONT = subprocess.run(['fc-match', '-f', '%{file}', 'Noto Sans CJK KR:style=Bold'], capture_output=True, text=True).stdout
def paste(canvas, name, rect):
    p = Image.open(f'out/parts/{name}.png').convert('RGBA')
    x0, y0, x1, y1 = rect
    if p.size != (x1 - x0, y1 - y0): p = p.resize((x1 - x0, y1 - y0), Image.LANCZOS)
    canvas.alpha_composite(p, (x0, y0))
def text(canvas, it, rect):
    if 'text' not in it: return
    d = ImageDraw.Draw(canvas); f = ImageFont.truetype(FONT, int(it.get('text_size_px', 30)))
    tr = it.get('text_rect', rect)
    d.text(((tr[0] + tr[2]) / 2, (tr[1] + tr[3]) / 2), it['text'], font=f, fill=it['text_color'], anchor='mm')
for scr, src in [('main_menu', 'menu'), ('login', 'login'), ('story_collection', 'stories')]:
    L = layout[scr]
    c = Image.open(f"out/backgrounds/{L['background']}").convert('RGBA')
    for it in L['items']:
        if 'part' in it: paste(c, it['part'], it['rect'])
        if it.get('kind') == 'text': text(c, it, it['rect'])
        elif 'text' in it: text(c, it, it['rect'])
        for ch in it.get('children', []):
            if 'part' in ch: paste(c, ch['part'], ch['rect']); text(c, ch, ch['rect'])
            elif ch.get('kind') == 'text':
                cr = it['rect']; text(c, ch, [cr[0], 286, cr[2], 336])
    c = c.convert('RGB'); c.save(f'out/preview/{scr}_recomposed.png')
    o = Image.open(f'src/{src}.png').convert('RGB')
    both = Image.new('RGB', (o.width, o.height * 2 + 10), 'white'); both.paste(o, (0, 0)); both.paste(c, (0, o.height + 10))
    both.resize((both.width // 2, both.height // 2)).save(f'out/preview/{scr}_compare.png')
    # diff outside text areas: mean abs diff
    a = np.asarray(o, np.float32); b = np.asarray(c, np.float32)
    print(scr, 'mean abs diff', round(float(np.abs(a - b).mean()), 2))
print('ok')
