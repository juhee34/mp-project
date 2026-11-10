# Six-character sprite handoff

72 PNGs are installed in app/src/main/res/drawable-nodpi/.
Original approved sheets: docs/art/source/ (three sheets).
Review: docs/art/review/preview.html, contact-sheet.png, animation.gif, validation.json.
Exporter: scripts/art/export_sprites.py (Python, Pillow, NumPy, SciPy).
Independent Windows check: powershell -NoProfile -File scripts/validate-sprite-pixels.ps1.

Each character: idle 2, run 4, jump 2, fail 1 (required); greet, land, home 1 each (optional stills).
48x48 RGBA; binary alpha; <=16 colors per character; bottom y45; rows 46-47 empty.
Lail retains internal ID mint. Designs are approved: do not regenerate or redesign.
Selection/profile now display idle_0 without bitmap filtering. JSON marks sprite_ready.
Gameplay animation integration, collision alignment and device visual checks remain pending.
Jump artwork stays on the baseline; game physics controls vertical movement.
Review animations are pose previews, not proof of runtime behavior. Optional greet/home are not complete two-frame animations.
