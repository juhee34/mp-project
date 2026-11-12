import cv2, numpy as np, json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from lib import *

os.makedirs('out/backgrounds', exist_ok=True); os.makedirs('out/parts', exist_ok=True)
layout = json.load(open('out/layout.json')) if os.path.exists('out/layout.json') else {}

def part(img, box, r, name, inner=None, T=55, keep=None):
    x0, y0, x1, y1 = box
    crop = img[y0:y1, x0:x1].copy()
    if inner is not None:
        crop, _ = remove_text2(crop, inner, keep=keep)
    a = rr_mask(x1 - x0, y1 - y0, r)
    cv2.imwrite(f'out/parts/{name}.png', rgba(crop, a))
    return a

# ---------------- main menu ----------------
im = cv2.imread('src/menu.png'); H, W = im.shape[:2]
P = (605, 335, 1064, 424)                # primary pill (x0,y0,x1,y1)
S = [(635, 442, 1037, 522), (637, 537, 1035, 616), (637, 629, 1035, 710), (636, 724, 1035, 805)]
labels = ['스토리 진행하기', '게임하기', '상점', '튜토리얼', '설정']
mask = np.zeros((H, W), np.uint8)
for b in [P] + S:
    x0, y0, x1, y1 = b; h = y1 - y0
    mask = np.maximum(mask, paste_mask(H, W, (x0, y0, x1, y1), rr_mask(x1 - x0, y1 - y0, h / 2)))
mask = cv2.dilate((mask > 20).astype(np.uint8) * 255, np.ones((11, 11), np.uint8))
# one soft cream glow column behind the whole button stack (the mockup's halo)
col = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (41, 41)))
filled = flatfill(im, np.ones((H, W), np.uint8), (210, 238, 253))   # BGR of halo cream (253,238,210)
a = cv2.GaussianBlur((col > 0).astype(np.float32), (0, 0), 9)
a = np.maximum(a, (mask > 0).astype(np.float32))[..., None]
bg = np.clip(im * (1 - a) + filled * a, 0, 255).astype(np.uint8)
cv2.imwrite('out/backgrounds/bg_main_menu.png', bg)

h = P[3] - P[1]
part(im, P, h / 2, 'ui_menu_btn_primary', inner=(int(h * 0.55), 14, P[2] - P[0] - int(h * 0.55), h - 14))
b = S[0]; h = b[3] - b[1]
part(im, b, h / 2, 'ui_menu_btn_secondary', inner=(int(h * 0.55), 12, b[2] - b[0] - int(h * 0.55), h - 12))
layout['main_menu'] = {
    'reference_size': [W, H], 'background': 'bg_main_menu.png',
    'items': [{'id': f'menu_{i}', 'part': 'ui_menu_btn_primary' if i == 0 else 'ui_menu_btn_secondary',
               'rect': [int(v) for v in b], 'text': t,
               'text_color': '#FFF8E7' if i == 0 else '#1E5A2A',
               'text_size_px': 44 if i == 0 else 42}
              for i, (b, t) in enumerate(zip([P] + S, labels))]}

# ---------------- login ----------------
im = cv2.imread('src/login.png'); H, W = im.shape[:2]
inE, inP = (582, 389, 1089, 458), (582, 519, 1089, 589)
bL, bS = (583, 632, 1090, 712), (580, 731, 1091, 808)
lE, lP = (584, 346, 668, 380), (584, 476, 700, 510)
mask = np.zeros((H, W), np.uint8)
for b, r in [(inE, 24), (inP, 24), (bL, 40), (bS, 38)]:
    x0, y0, x1, y1 = b
    mask = np.maximum(mask, paste_mask(H, W, b, rr_mask(x1 - x0, y1 - y0, r)))
for b in [lE, lP]:
    cv2.rectangle(mask, b[:2], (b[2], b[3]), 255, -1)
mask = cv2.dilate((mask > 20).astype(np.uint8) * 255, np.ones((9, 9), np.uint8))
filled = rowfill(im, mask, std=1.3)
bg = feather(im, filled, mask, 5)
cv2.imwrite('out/backgrounds/bg_login.png', bg)

part(im, inE, 24, 'ui_login_input')
h = bL[3] - bL[1]
part(im, bL, h / 2, 'ui_login_btn_primary', inner=(int(h * .55), 12, bL[2] - bL[0] - int(h * .55), h - 12))
h = bS[3] - bS[1]
part(im, bS, h / 2, 'ui_login_btn_secondary', inner=(int(h * .55), 12, bS[2] - bS[0] - int(h * .55), h - 12))
layout['login'] = {
    'reference_size': [W, H], 'background': 'bg_login.png',
    'items': [
        {'id': 'label_email', 'kind': 'text', 'rect': list(lE), 'text': '이메일', 'text_color': '#1E5A2A', 'text_size_px': 30},
        {'id': 'input_email', 'part': 'ui_login_input', 'rect': list(inE), 'hint': '이메일', 'text_color': '#3A3A2A', 'text_size_px': 26, 'padding_px': [26, 0, 26, 0]},
        {'id': 'label_password', 'kind': 'text', 'rect': list(lP), 'text': '비밀번호', 'text_color': '#1E5A2A', 'text_size_px': 30},
        {'id': 'input_password', 'part': 'ui_login_input', 'rect': list(inP), 'hint': '비밀번호', 'text_color': '#3A3A2A', 'text_size_px': 26, 'padding_px': [26, 0, 26, 0]},
        {'id': 'btn_login', 'part': 'ui_login_btn_primary', 'rect': list(bL), 'text': '로그인', 'text_color': '#FFF8E7', 'text_size_px': 38},
        {'id': 'btn_signup', 'part': 'ui_login_btn_secondary', 'rect': list(bS), 'text': '회원가입', 'text_color': '#1E5A2A', 'text_size_px': 38},
    ]}
json.dump(layout, open('out/layout.json', 'w'), ensure_ascii=False, indent=1)
print('ok')
