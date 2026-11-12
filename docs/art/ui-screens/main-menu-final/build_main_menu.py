"""Final main menu (A approved 2026-11-12): background without buttons + text-free button parts."""
import cv2, numpy as np, sys, os, json
sys.path.insert(0, '../ui')
from lib import rr_mask, paste_mask, rowfill, feather, remove_text2, noise, rgba
im = cv2.imread('src.png'); H, W = im.shape[:2]
P = (610, 317, 1061, 401)
S = [(619, 410, 1053, 488), (618, 497, 1053, 575), (619, 584, 1053, 663), (619, 672, 1053, 750)]
labels = ['스토리 진행하기', '게임하기', '상점', '튜토리얼', '설정']
mask = np.zeros((H, W), np.uint8)
for b in [P] + S:
    x0, y0, x1, y1 = b
    mask = np.maximum(mask, paste_mask(H, W, b, rr_mask(x1 - x0, y1 - y0, (y1 - y0) / 2)))
mask = cv2.dilate((mask > 10).astype(np.uint8) * 255, np.ones((9, 9), np.uint8))
filled = rowfill(im, mask, std=1.2)
bg = feather(im, filled, mask, 5)
os.makedirs('out', exist_ok=True)
cv2.imwrite('out/bg_main_menu_final.png', bg)
# wide 2091: extend with each row's outer-edge colour (the brown desk)
TW = 2091; pl = (TW - W) // 2; pr = TW - W - pl
lc = np.median(bg[:, 0:6].astype(np.float32), 1); rc = np.median(bg[:, W - 6:W].astype(np.float32), 1)
wide = np.zeros((H, TW, 3), np.float32)
wide[:, :pl] = lc[:, None, :]; wide[:, pl + W:] = rc[:, None, :]
wide += noise(H, TW, 1.2); wide[:, pl:pl + W] = bg
cv2.imwrite('out/bg_main_menu_wide.png', np.clip(wide, 0, 255).astype(np.uint8))
cv2.imwrite('out/bg_main_menu_wide.jpg', np.clip(wide, 0, 255).astype(np.uint8), [cv2.IMWRITE_JPEG_QUALITY, 92])
def part(box, name):
    x0, y0, x1, y1 = box; h = y1 - y0
    crop = im[y0:y1, x0:x1].copy()
    crop, _ = remove_text2(crop, (int(h * .55), 10, x1 - x0 - int(h * .55), h - 10))
    cv2.imwrite(f'out/{name}.png', rgba(crop, rr_mask(x1 - x0, h, h / 2)))
part(P, 'ui_menu_btn_primary'); part(S[0], 'ui_menu_btn_secondary')
layout = {'reference_size': [1672, 941], 'background': 'bg_main_menu_wide (centre 1672 px)', 'items': [
    {'id': f'menu_{i}', 'part': 'ui_menu_btn_primary' if i == 0 else 'ui_menu_btn_secondary', 'rect': list(b), 'text': t,
     'text_color': '#FFF8E7' if i == 0 else '#1E5A2A', 'text_size_px': 44 if i == 0 else 42}
    for i, (b, t) in enumerate(zip([P] + S, labels))]}
json.dump(layout, open('out/layout_main_menu.json', 'w'), ensure_ascii=False, indent=1)
# recomposed preview
from PIL import Image, ImageDraw, ImageFont
import subprocess
F = subprocess.run(['fc-match', '-f', '%{file}', 'Noto Sans CJK KR:style=Bold'], capture_output=True, text=True).stdout
c = Image.open('out/bg_main_menu_final.png').convert('RGBA'); d = ImageDraw.Draw(c)
for it in layout['items']:
    p = Image.open(f"out/{it['part']}.png").convert('RGBA'); x0, y0, x1, y1 = it['rect']
    p = p.resize((x1 - x0, y1 - y0), Image.LANCZOS); c.alpha_composite(p, (x0, y0))
    d.text(((x0 + x1) / 2, (y0 + y1) / 2), it['text'], font=ImageFont.truetype(F, it['text_size_px']), fill=it['text_color'], anchor='mm')
c = c.convert('RGB'); c.save('out/preview_recomposed.png')
o = Image.open('src.png').convert('RGB')
both = Image.new('RGB', (W, H * 2 + 10), 'white'); both.paste(o, (0, 0)); both.paste(c, (0, H + 10))
both.resize((W // 2, (H * 2 + 10) // 2)).save('out/preview_compare.png')
print('diff', np.abs(np.asarray(o, float) - np.asarray(c, float)).mean())
