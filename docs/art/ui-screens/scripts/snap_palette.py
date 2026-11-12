"""Snap every changed pixel to the nearest colour already used by that character (keeps the 16-colour rule)."""
import numpy as np, os
from PIL import Image
pal = {}
for f in os.listdir('src'):
    cid = f.split('_')[1]
    a = np.array(Image.open('src/' + f).convert('RGBA')).reshape(-1, 4)
    pal.setdefault(cid, set()).update(map(tuple, a[a[:, 3] > 0][:, :3]))
changed = 0
for f in sorted(os.listdir('src')):
    cid = f.split('_')[1]; P = np.array(sorted(pal[cid]))
    a = np.array(Image.open('src/' + f).convert('RGBA')).astype(int)
    b = np.array(Image.open('fixed/' + f).convert('RGBA')).astype(int)
    d = np.any(a != b, axis=2)
    for y, x in zip(*np.where(d)):
        c = b[y, x, :3]; b[y, x, :3] = P[np.argmin(((P - c) ** 2).sum(1))]
    b[:, :, 3] = a[:, :, 3]
    Image.fromarray(b.astype(np.uint8), 'RGBA').save('fixed/' + f)
    if np.any(a != b): changed += 1
for cid, s in pal.items():
    used = set()
    for f in os.listdir('fixed'):
        if f.split('_')[1] == cid:
            a = np.array(Image.open('fixed/' + f).convert('RGBA')).reshape(-1, 4); used.update(map(tuple, a[a[:, 3] > 0][:, :3]))
    print(cid, 'palette', len(s), '-> used', len(used), 'subset:', used <= s)
print('frames changed:', changed)
