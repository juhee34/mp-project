import cv2, numpy as np

RNG = np.random.default_rng(7)


def noise(h, w, std=1.6):
    n = RNG.normal(0, std, (h, w, 1)).astype(np.float32)
    return cv2.GaussianBlur(n, (3, 3), 0).reshape(h, w, 1)


def rr_mask(w, h, r, ss=4):
    """anti-aliased rounded-rect alpha (0..255) of size w x h"""
    W, H, R = w * ss, h * ss, int(r * ss)
    m = np.zeros((H, W), np.uint8)
    R = min(R, H // 2, W // 2)
    cv2.rectangle(m, (R, 0), (W - 1 - R, H - 1), 255, -1)
    cv2.rectangle(m, (0, R), (W - 1, H - 1 - R), 255, -1)
    for cx, cy in [(R, R), (W - 1 - R, R), (R, H - 1 - R), (W - 1 - R, H - 1 - R)]:
        cv2.circle(m, (cx, cy), R, 255, -1)
    return cv2.resize(m, (w, h), interpolation=cv2.INTER_AREA)


def poly_mask(w, h, pts, ss=4):
    m = np.zeros((h * ss, w * ss), np.uint8)
    cv2.fillPoly(m, [np.array([(int(x * ss), int(y * ss)) for x, y in pts], np.int32)], 255)
    return cv2.resize(m, (w, h), interpolation=cv2.INTER_AREA)


def paste_mask(H, W, box, small):
    x0, y0, x1, y1 = box
    m = np.zeros((H, W), np.uint8)
    m[y0:y1, x0:x1] = small
    return m


def rowfill(img, mask, valid=None, std=1.4):
    """fill masked pixels row by row by linear interpolation between the
    nearest unmasked pixels on the left/right of each masked run."""
    out = img.astype(np.float32).copy()
    H, W = mask.shape
    if valid is None:
        valid = np.ones((H, W), bool)
    nz = noise(H, W, std)
    for y in range(H):
        row = mask[y] > 0
        if not row.any():
            continue
        xs = np.where(row)[0]
        # split runs
        runs = np.split(xs, np.where(np.diff(xs) > 1)[0] + 1)
        for r in runs:
            a, b = r[0], r[-1]
            L = [x for x in range(a - 1, max(-1, a - 6), -1) if valid[y, x] and not row[x]]
            Rr = [x for x in range(b + 1, min(W, b + 6)) if valid[y, x] and not row[x]]
            lc = out[y, L].mean(0) if L else None
            rc = out[y, Rr].mean(0) if Rr else None
            if lc is None and rc is None:
                continue
            if lc is None: lc = rc
            if rc is None: rc = lc
            t = np.linspace(0, 1, b - a + 3)[1:-1].reshape(-1, 1)
            out[y, a:b + 1] = lc * (1 - t) + rc * t + nz[y, a:b + 1]
    return np.clip(out, 0, 255).astype(np.uint8)


def flatfill(img, mask, color_bgr, std=1.6):
    out = img.astype(np.float32).copy()
    H, W = mask.shape
    fill = np.zeros_like(out) + np.array(color_bgr, np.float32) + noise(H, W, std)
    out[mask > 0] = fill[mask > 0]
    return np.clip(out, 0, 255).astype(np.uint8)


def feather(orig, filled, mask, k=5):
    a = cv2.GaussianBlur((mask > 0).astype(np.float32), (k, k), 0)
    a = np.maximum(a, (cv2.erode(mask, np.ones((3, 3), np.uint8)) > 0).astype(np.float32))[..., None]
    return np.clip(orig * (1 - a) + filled * a, 0, 255).astype(np.uint8)


def remove_text(crop, inner, T=55, dil=2, keep=None):
    """inpaint glyphs inside inner=(x0,y0,x1,y1) (crop coords)."""
    x0, y0, x1, y1 = inner
    reg = crop[y0:y1, x0:x1].astype(int)
    fillc = np.median(reg.reshape(-1, 3), 0)
    d = np.sqrt(((reg - fillc) ** 2).sum(2))
    m = np.zeros(crop.shape[:2], np.uint8)
    m[y0:y1, x0:x1] = (d > T).astype(np.uint8) * 255
    if keep is not None:
        m[keep > 0] = 0
    m = cv2.dilate(m, np.ones((2 * dil + 1, 2 * dil + 1), np.uint8))
    # restrict to inner
    lim = np.zeros_like(m); lim[y0:y1, x0:x1] = 255
    m = cv2.bitwise_and(m, lim)
    return rowfill(crop, m, std=0.9), m


def rgba(crop_bgr, alpha):
    return np.dstack([crop_bgr, alpha])


def ninepatch(rgba_img, sx, sy, px, py):
    """sx,sy: (start,end) stretch ranges; px,py: content padding ranges"""
    h, w = rgba_img.shape[:2]
    out = np.zeros((h + 2, w + 2, 4), np.uint8)
    out[1:-1, 1:-1] = rgba_img
    blk = (0, 0, 0, 255)
    out[0, 1 + sx[0]:1 + sx[1]] = blk
    out[1 + sy[0]:1 + sy[1], 0] = blk
    out[-1, 1 + px[0]:1 + px[1]] = blk
    out[1 + py[0]:1 + py[1], -1] = blk
    return out


def remove_text2(crop, inner, T=40, padx=12, pady=6, keep=None, std=1.0):
    """replace the glyph bbox with a per-row colour profile sampled from the
    same rows outside the glyphs (handles glows/outlines around text)."""
    x0, y0, x1, y1 = inner
    reg = crop[y0:y1, x0:x1].astype(int)
    fillc = np.median(reg.reshape(-1, 3), 0)
    d = np.sqrt(((reg - fillc) ** 2).sum(2))
    m = (d > T).astype(np.uint8)
    if keep is not None:
        m[keep[y0:y1, x0:x1] > 0] = 0
    m = cv2.morphologyEx(m, cv2.MORPH_OPEN, np.ones((2, 2), np.uint8))
    ys, xs = np.where(m > 0)
    if len(xs) == 0:
        return crop, None
    bx0, bx1 = max(x0, x0 + xs.min() - padx), min(x1, x0 + xs.max() + padx)
    by0, by1 = max(y0, y0 + ys.min() - pady), min(y1, y0 + ys.max() + pady)
    out = crop.astype(np.float32).copy()
    h, w = crop.shape[:2]
    nz = noise(h, w, std)
    for y in range(by0, by1):
        samp = np.concatenate([out[y, x0:max(x0, bx0 - 2)], out[y, min(x1, bx1 + 2):x1]])
        c = np.median(samp, 0) if len(samp) else fillc
        out[y, bx0:bx1] = c + nz[y, bx0:bx1]
    # horizontal feather
    res = out.copy()
    f = 8
    for i in range(f):
        t = (i + 1) / (f + 1)
        for xx, src in [(bx0 + i, bx0 + i), (bx1 - 1 - i, bx1 - 1 - i)]:
            pass
    return np.clip(res, 0, 255).astype(np.uint8), (bx0, by0, bx1, by1)
