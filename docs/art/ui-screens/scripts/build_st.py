import cv2, numpy as np, json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from lib import *

layout = json.load(open('out/layout.json'))
im = cv2.imread('src/stories.png'); H, W = im.shape[:2]
hsv = cv2.cvtColor(im, cv2.COLOR_BGR2HSV)
lum = cv2.cvtColor(im, cv2.COLOR_BGR2GRAY)
CARD_BOTTOM = 798

# ---------- card masks (flood fill from the window border) ----------
wins = [(52, 200, 452, 812), (447, 200, 842, 812), (833, 200, 1226, 812), (1215, 200, 1632, 812)]
def card_mask(win):
    x0, y0, x1, y1 = win
    c = lum[y0:y1, x0:x1]
    fill = np.where(c < 170, 0, 255).astype(np.uint8)
    h, w = fill.shape
    ff = np.zeros((h + 2, w + 2), np.uint8)
    for x in range(0, w, 3):
        for y in (0,):
            if fill[y, x] == 255: cv2.floodFill(fill, ff, (x, y), 128)
    for y in range(0, h - 20, 3):
        for x in (0, w - 1):
            if fill[y, x] == 255: cv2.floodFill(fill, ff, (x, y), 128)
    m = (fill != 128).astype(np.uint8)
    m[CARD_BOTTOM + 1 - y0:] = 0
    # keep the largest component only (the card)
    n, lab, st, _ = cv2.connectedComponentsWithStats(m, 8)
    i = 1 + np.argmax(st[1:, 4]); m = (lab == i).astype(np.uint8)
    # fill holes
    hh = m.copy(); ff2 = np.zeros((h + 2, w + 2), np.uint8); cv2.floodFill(hh, ff2, (0, 0), 1)
    m = m | (1 - hh)
    m[CARD_BOTTOM + 1 - y0:] = 0
    full = np.zeros((H, W), np.uint8); full[y0:y1, x0:x1] = m * 255
    return full
cards = [card_mask(wn) for wn in wins]

# ---------- ribbons ----------
rib_win = [(92, 200, 182, 310), (485, 205, 575, 315), (870, 205, 960, 315), (1258, 205, 1348, 315)]
ribbons = []
for (x0, y0, x1, y1) in rib_win:
    s = hsv[y0:y1, x0:x1]
    hue = s[:, :, 0].astype(int)
    m = ((s[:, :, 1] > 45) & (s[:, :, 2] > 90) & ((hue > 35) & (hue < 165))).astype(np.uint8)
    # colour of the ribbon = the dominant hue in the middle rows
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8))
    n, lab, st, _ = cv2.connectedComponentsWithStats(m, 8)
    # choose component whose bbox is ribbon-like (w 45..75, h 55..95)
    best = None
    for i in range(1, n):
        x, y, w, h, a = st[i]
        if 40 <= w <= 80 and 50 <= h <= 100 and (best is None or a > st[best][4]): best = i
    x, y, w, h, a = st[best]
    comp = (lab == best)
    cx = x + w // 2
    col = np.where(comp[:, cx - 1:cx + 2].any(1))[0]
    notch = col.max()                          # lowest coloured pixel on the centre line
    bx0, by0, bx1, by1 = x0 + x - 2, y0 + y - 2, x0 + x + w + 2, y0 + y + h + 2
    ribbons.append(dict(box=(bx0, by0, bx1, by1), notch=y0 + notch + 2))
print('ribbons', ribbons)

def ribbon_poly_mask(rb):
    x0, y0, x1, y1 = rb['box']; w, h = x1 - x0, y1 - y0
    ny = rb['notch'] - y0
    pts = [(0, 3), (3, 0), (w - 3, 0), (w, 3), (w, h), (w / 2, ny), (0, h)]
    return poly_mask(w, h, pts)

# ---------- background ----------
band = np.zeros((H, W), np.uint8)
CARD_RECTS = [(65, 231, 443, 799), (458, 231, 830, 799), (843, 231, 1214, 799), (1227, 231, 1618, 799)]
for (a0, b0, a1, b1) in CARD_RECTS: cv2.rectangle(band, (a0, b0), (a1 - 1, b1 - 1), 255, -1)
# close the narrow gaps between neighbouring cards
cv2.rectangle(band, (65, 236), (1617, 798), 255, -1)
for rb in ribbons:
    x0, y0, x1, y1 = rb['box']
    band = np.maximum(band, paste_mask(H, W, rb['box'], ribbon_poly_mask(rb)))
band = cv2.dilate((band > 20).astype(np.uint8) * 255, np.ones((11, 11), np.uint8))
band[CARD_BOTTOM + 1:] = 0
# top widgets
top = np.zeros((H, W), np.uint8)
BACK, COUNTER, BAG = (47, 41, 371, 112), (1233, 41, 1526, 112), (1562, 34, 1637, 120)
for b, r in [(BACK, 35), (COUNTER, 35), (BAG, 18)]:
    top = np.maximum(top, paste_mask(H, W, b, rr_mask(b[2] - b[0], b[3] - b[1], r)))
top = cv2.dilate((top > 20).astype(np.uint8) * 255, np.ones((9, 9), np.uint8))

bg = rowfill(im, top, std=1.2)
bg = feather(im, bg, top, 5)
# cream sky behind the cards, wooden bench rows sampled from the gaps
PAPER = (215, 239, 253)
filled = flatfill(bg, band, PAPER, std=1.6)
BENCH_TOP = 764
gaps = [449, 836, 1221]
nz = noise(H, W, 2.0)
for y in range(BENCH_TOP, CARD_BOTTOM + 1):
    samp = np.concatenate([im[y, g - 2:g + 3] for g in gaps]).astype(np.float32)
    c = np.median(samp, 0)
    row = band[y] > 0
    xs = np.where(row)[0]
    xs = xs[xs > 52]
    filled[y, xs] = np.clip(c + nz[y, xs], 0, 255)
bg = feather(bg, filled, band, 7)
cv2.imwrite('out/backgrounds/bg_story_collection.png', bg)

# ---------- parts ----------
def save(name, crop, alpha):
    cv2.imwrite(f'out/parts/{name}.png', rgba(crop, alpha))

def pill(name, box, r=None, inner=None, keep=None):
    x0, y0, x1, y1 = box; w, h = x1 - x0, y1 - y0
    crop = im[y0:y1, x0:x1].copy()
    if inner:
        crop, _ = remove_text2(crop, inner, keep=keep)
    save(name, crop, rr_mask(w, h, r if r is not None else h / 2))

BTN_P, BTN_O, BTN_D = (108, 696, 413, 761), (501, 696, 799, 761), (1269, 696, 1591, 761)
PILL = (531, 634, 774, 678)
def inner_for(b, ins=0.55, vy=10):
    w, h = b[2] - b[0], b[3] - b[1]
    return (int(h * ins), vy, w - int(h * ins), h - vy)
pill('ui_story_btn_primary', BTN_P, inner=inner_for(BTN_P))
pill('ui_story_btn_outline', BTN_O, inner=inner_for(BTN_O))
pill('ui_story_btn_disabled', BTN_D, inner=inner_for(BTN_D, 0.3))
pill('ui_story_count_pill', PILL, inner=inner_for(PILL, 0.4, 6))
# back button: keep the arrow (left 90 px), remove the words
pill('ui_story_back_button', BACK, inner=(66, 12, BACK[2] - BACK[0] - 30, BACK[3] - BACK[1] - 12))
pill('ui_story_counter_pill', COUNTER, inner=inner_for(COUNTER, 0.45, 12))
pill('ui_story_bag_button', BAG, r=18)

# thumbnails
ILL = {'world': (102, 351, 416, 635), 'mint': (495, 349, 803, 618), 'cheese': (880, 351, 1187, 618), 'popo': (1265, 352, 1590, 618)}
for k, (x0, y0, x1, y1) in ILL.items():
    save(f'story_thumb_{k}', im[y0:y1, x0:x1].copy(), rr_mask(x1 - x0, y1 - y0, 14))

# ribbons
for k, rb in zip(['world', 'mint', 'cheese', 'popo'], ribbons):
    x0, y0, x1, y1 = rb['box']
    save(f'ui_story_ribbon_{k}', im[y0:y1, x0:x1].copy(), ribbon_poly_mask(rb))

# empty card frame from card #2 (mint)
cm = cards[1].copy()
cm[:, :456] = 0; cm[:, 833:] = 0
rb = ribbons[1]; rx0, ry0, rx1, ry1 = rb['box']
# top edge under the ribbon: interpolate between columns just outside it
def top_of(x):
    ys = np.where(cm[:, x] > 0)[0]; return ys.min()
tl, tr = top_of(rx0 - 4), top_of(rx1 + 4)
for x in range(rx0 - 3, rx1 + 4):
    t = int(round(tl + (tr - tl) * (x - rx0 + 3) / (rx1 - rx0 + 6)))
    cm[:t, x] = 0
ys, xs = np.where(cm > 0)
CX0, CY0, CX1, CY1 = xs.min(), ys.min(), xs.max() + 1, ys.max() + 1
content = np.zeros((H, W), np.uint8)
# title text (dark green) in the header rows
hdr = im[280:340, CX0:CX1].astype(int)
g = ((hdr[:, :, 1] > hdr[:, :, 2] + 15) & (hdr[:, :, 1] < 140)).astype(np.uint8)
yy, xx = np.where(g > 0)
cv2.rectangle(content, (CX0 + xx.min() - 8, 280 + yy.min() - 6), (CX0 + xx.max() + 8, 280 + yy.max() + 6), 255, -1)
for b in [ILL['mint'], PILL, BTN_O]:
    cv2.rectangle(content, (b[0] - 4, b[1] - 4), (b[2] + 4, b[3] + 4), 255, -1)
content = np.maximum(content, cv2.dilate(paste_mask(H, W, rb['box'], ribbon_poly_mask(rb)), np.ones((7, 7), np.uint8)))
content[cm == 0] = 0
card = rowfill(im, content, valid=cm > 0, std=1.1)
alpha = cv2.GaussianBlur(cm.astype(np.float32), (3, 3), 0).clip(0, 255).astype(np.uint8)
save('ui_story_card_frame', card[CY0:CY1, CX0:CX1], alpha[CY0:CY1, CX0:CX1])
print('card frame', (CX0, CY0, CX1, CY1))

# ---------- layout ----------
def cbox(m):
    ys, xs = np.where(m > 0); return [int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1)]
items = [
    {'id': 'btn_back', 'part': 'ui_story_back_button', 'rect': list(BACK), 'text': '마을 이야기로', 'text_rect': [128, 52, 350, 101], 'text_color': '#1E5A2A', 'text_size_px': 34},
    {'id': 'seen_total', 'part': 'ui_story_counter_pill', 'rect': list(COUNTER), 'text': '본 이야기 8 / 31', 'text_color': '#1E5A2A', 'text_size_px': 31},
    {'id': 'btn_bag', 'part': 'ui_story_bag_button', 'rect': list(BAG)},
]
names = ['world', 'mint', 'cheese', 'popo']
titles = ['세계 이야기', '라일의 이야기', '치즈의 이야기', '포포의 이야기']
pills = [None, PILL, (916, 634, 1158, 678), (1302, 634, 1564, 678)]
btns = [(BTN_P, 'ui_story_btn_primary', '다시 보기', '#FFF8E7'), (BTN_O, 'ui_story_btn_outline', '이야기 보기', '#1E5A2A'),
        ((885, 696, 1183, 761), 'ui_story_btn_outline', '이야기 보기', '#1E5A2A'), (BTN_D, 'ui_story_btn_disabled', '아직 열리지 않았어요', '#9A958C')]
counts = [None, '본 이야기 5 / 5', '본 이야기 1 / 5', '본 이야기 0 / 5']
for i, k in enumerate(names):
    c = {'id': f'card_{k}', 'part': 'ui_story_card_frame', 'rect': list(CARD_RECTS[i]),
         'children': [
             {'id': 'ribbon', 'part': f'ui_story_ribbon_{k}', 'rect': [int(v) for v in ribbons[i]['box']]},
             {'id': 'title', 'kind': 'text', 'text': titles[i], 'text_color': '#1E5A2A', 'text_size_px': 40},
             {'id': 'thumb', 'part': f'story_thumb_{k}', 'rect': list(ILL[k])},
             {'id': 'button', 'part': btns[i][1], 'rect': list(btns[i][0]), 'text': btns[i][2], 'text_color': btns[i][3], 'text_size_px': 34}]}
    if pills[i]:
        c['children'].insert(3, {'id': 'count', 'part': 'ui_story_count_pill', 'rect': list(pills[i]), 'text': counts[i], 'text_color': '#3D3550', 'text_size_px': 27})
    items.append(c)
layout['story_collection'] = {'reference_size': [W, H], 'background': 'bg_story_collection.png', 'items': items}
json.dump(layout, open('out/layout.json', 'w'), ensure_ascii=False, indent=1)
print('ok')
