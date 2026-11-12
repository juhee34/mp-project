# 캐릭터 눈 정리 (2026-11-12, A)

A 요청: 모든 캐릭터의 두 눈을 같은 모양·크기로 맞춤. 원본 스프라이트는 3/4 옆모습이라 먼 쪽 눈이 1픽셀 점이나 얇은 선이었다.

- 바꾼 프레임: 50장 / 72장 (`app/src/main/res/drawable-nodpi/sprite_*.png`). 나머지는 이미 두 눈이 같거나 감은 눈이라 그대로.
- 방법: 프레임마다 두 눈 중 큰 쪽의 픽셀 모양을 작은 쪽 자리에 같은 높이로 복사. 먼 눈이 안 보이던 프레임(라일 대부분, 루미 일부, 피코 점프)은 같은 눈을 새로 그림.
  라일은 큰 눈(3×5)을 달리기 프레임의 2칸 눈으로 통일했고, 감은 눈 2장(idle_1, home_0)은 같은 ∩ 모양 두 개로 다시 그림. 치즈 착지(land_0)는 `> <`.
- 지킨 것: 48×48·투명 영역(알파) 그대로, 바뀐 픽셀은 눈 주변 얼굴 줄(y 18~31)만, 색은 캐릭터별 기존 색 안에서만(16색 규칙 유지).
- 이야기 모음 썸네일(`parts/story_thumb_*`)도 고친 스프라이트로 다시 만듦.
- 주의: `docs/art/source/*.png` 원본 시트와 `scripts/art/export_sprites.py`는 바꾸지 않았다. **시트에서 다시 내보내면 눈이 예전으로 돌아간다.** 다시 내보낼 일이 있으면 `scripts/fix_eyes2.py`, `scripts/snap_palette.py`를 이어서 돌리면 된다.
- 비교: `preview/eyes_before_after.png`
- 확인 필요(B): 빌드 후 선택 화면·달리기·이야기 카드에서 눈이 자연스러운지, 앱 아이콘(라일 idle)도 같이 바뀜.

바뀐 파일:
- `sprite_cheese_fail_0.png`
- `sprite_cheese_greet_0.png`
- `sprite_cheese_home_0.png`
- `sprite_cheese_idle_0.png`
- `sprite_cheese_idle_1.png`
- `sprite_cheese_jump_0.png`
- `sprite_cheese_land_0.png`
- `sprite_cheese_run_0.png`
- `sprite_cheese_run_1.png`
- `sprite_cheese_run_2.png`
- `sprite_cheese_run_3.png`
- `sprite_lumi_greet_0.png`
- `sprite_lumi_idle_0.png`
- `sprite_lumi_jump_0.png`
- `sprite_lumi_jump_1.png`
- `sprite_lumi_run_0.png`
- `sprite_lumi_run_2.png`
- `sprite_lumi_run_3.png`
- `sprite_mint_fail_0.png`
- `sprite_mint_greet_0.png`
- `sprite_mint_home_0.png`
- `sprite_mint_idle_0.png`
- `sprite_mint_idle_1.png`
- `sprite_mint_jump_0.png`
- `sprite_mint_jump_1.png`
- `sprite_mint_land_0.png`
- `sprite_mint_run_0.png`
- `sprite_mint_run_1.png`
- `sprite_mint_run_2.png`
- `sprite_mint_run_3.png`
- `sprite_nunu_fail_0.png`
- `sprite_nunu_greet_0.png`
- `sprite_nunu_idle_1.png`
- `sprite_nunu_jump_0.png`
- `sprite_nunu_jump_1.png`
- `sprite_nunu_run_1.png`
- `sprite_nunu_run_3.png`
- `sprite_piko_greet_0.png`
- `sprite_piko_idle_0.png`
- `sprite_piko_idle_1.png`
- `sprite_piko_jump_1.png`
- `sprite_piko_run_0.png`
- `sprite_piko_run_1.png`
- `sprite_piko_run_2.png`
- `sprite_piko_run_3.png`
- `sprite_popo_fail_0.png`
- `sprite_popo_greet_0.png`
- `sprite_popo_idle_0.png`
- `sprite_popo_idle_1.png`
- `sprite_popo_run_1.png`
