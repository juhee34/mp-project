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
Approved assets and portrait integration were committed and pushed as 55d9180.
Gameplay integration is implemented and tested. The user explicitly requested including it on codex/character-screens after confirming stage 4 B ownership.
Jump artwork stays on the baseline; game physics controls vertical movement.
Review animations are pose previews, not proof of runtime behavior. Optional greet/home are not complete two-frame animations.

## Gameplay integration handoff (stage 4 B)

- CharacterSprites.kt owns the cached nine-frame sets for all six IDs; portraits and RunnerView share them. No per-frame bitmap decoding.
- RunnerAnimation.kt selects idle every 0.5 seconds, four run frames by traveled distance, jump_0 for positive vertical velocity, jump_1 otherwise while airborne, and fail_0 on OVER. Paused simulation preserves the current pose.
- RunnerView.kt draws at integer scale with bitmap filtering disabled, x24 centered on the existing player and the lower edge of row45 aligned to the physics foot position. No extra jump displacement or running bob is applied.
- RunnerEngine, character IDs, approved PNGs, audio and persistence are unchanged. GameActivity and ResultActivity retain the existing characterId contract.
- Verified: content and pixel validators, debug APK, 30 unit tests, lint (zero errors; 25 warnings, including 18 deliberately unused optional stills).
- API 37 Medium_Phone: all six selected through normal UI, jump/run/fail screenshots checked, result and retry retain the same character, back returns to the selected card. Test screenshots/logs are local under ignored .tools/sprite-runtime/ and .tools/sprite-runtime.log.
- Still unverified: physical devices, other screen sizes/font scales, sustained frame-time profiling, and every individual animation frame captured on-device. Ascent/descent frame selection and all four run states are covered by unit tests.
- Ownership: artwork and portraits belong to A; runtime integration belongs to B (docs/PROMPTS.md stage 4). Coordinate with B's separate feature/jump-skill-story changes before combining gameplay work.
