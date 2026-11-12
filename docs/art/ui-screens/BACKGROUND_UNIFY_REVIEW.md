# 배경 통일 검수 — 마을 이야기 · 이야기 모음 · 캐릭터 선택 · 내 기록

기준: `origin/main` a89d3d1 (2026-11-12). A 요청: 이야기 모음 시안의 배경으로 네 화면을 통일.

## 결론

네 화면은 지금 모두 같은 그림 배경(`ui/Scenery.kt`의 `SceneryView`: 크림 하늘, 움직이는 픽셀 구름, 언덕 띠)을 코드로 그리고 있습니다.
이 PR은 그 자리에 **이야기 모음 시안에서 제목만 지운 공통 배경** `bg_common_paper`를 까는 `PaperBackdropView`를 넣습니다.
화면 내용·흐름·저장 코드는 건드리지 않았습니다.

| 화면 | 코드 위치 | 지금 배경 | 이 PR 이후 |
|---|---|---|---|
| 마을 이야기 | `story/StoryListActivity.kt` | `SceneryView` | `PaperBackdropView` |
| 이야기 모음 | `story/StoryCollectionActivity.kt` | `SceneryView` | `PaperBackdropView` |
| 캐릭터 선택 (게임하기) | `MainActivity` + `res/layout/activity_main.xml` → `characters/CharacterScreens.kt` | `SceneryView` (XML) | `PaperBackdropView` (XML) |
| 내 기록 | `records/RecordsActivity.kt` → `ui/Page.kt` | `Page.show()` 안의 `SceneryView` | `Page.show(paper = true)` |

그대로 두는 화면: 오프닝, 허브(메인 메뉴), 광고 제안, 설정·이야기 읽기 등 나머지 `Page.show()` 화면, 로그인/회원가입 팝업.

## 함께 바뀌는 것 (확인 필요)

- `activity_main.xml`은 캐릭터 **소개·선택·프로필** 세 화면이 같이 쓰므로, 선택 화면만이 아니라 세 화면 모두 새 배경이 됩니다(셋 다 A 화면).
- 구름 애니메이션이 없어집니다. 대신 화면마다 15fps로 다시 그리던 부담이 사라집니다.

## 그림 검수

- 그림: `res/drawable-nodpi/bg_common_paper.webp` (2091×941, 76 KB). 가운데 1672px가 시안, 양옆은 20:9 화면용으로 종이색을 이어 붙였습니다.
- 가운데(카드·글자가 놓이는 곳)는 무늬 없는 종이색 `#FDEFD7`입니다. 원래 카드가 있던 자리라 양옆 나무는 가장자리에서 부드럽게 흐려지도록 보정했습니다.
- 아래쪽 약 19%(시안 기준 y 764 이하)는 벤치와 풀밭입니다.
- 글자 대비 (WCAG 기준 4.5 이상 권장):

| 글자색 (`Page`) | 종이색 위 | 벤치 위 |
|---|---:|---:|
| INK `#26342B` | 11.5 | 4.5 |
| LEAF `#305341` | 7.6 | 3.0 |
| MUTED `#4F6054` | 5.9 | **2.3** |

  종이 위는 모두 충분합니다. **벤치 위에 맨 글자(특히 MUTED·LEAF)가 오면 읽기 어렵습니다.** 카드처럼 자기 바탕이 있는 요소는 괜찮습니다.

## B가 기기에서 확인할 것

1. `.\gradlew.bat :app:assembleDebug` 빌드 (이 PR은 빌드·기기 실행을 하지 못했습니다).
2. 네 화면 + 소개·프로필에서 배경이 화면을 꽉 채우는지, 16:9 / 20:9 두 비율에서 확인.
3. 화면 아래쪽 글자(마을 이야기의 상태 글자·페이지 점, 내 기록 맨 아래 카드 등)가 벤치 위에서 읽히는지. 읽기 어려우면 아래 여백을 늘리거나 카드 안으로 넣기.
4. 스크롤해도 배경은 고정인지 (`PaperBackdropView`는 스크롤 뷰 뒤에 있음).

## 이 PR에 들어 있는 것

- 코드: `ui/PaperBackdrop.kt`(새 파일), 위 표의 4곳 한 줄씩 교체, `Page.show()`에 `paper` 인자 추가(기본값 false라 다른 화면 영향 없음).
- 그림: `res/drawable-nodpi/bg_common_paper.webp`.
- 자료: `docs/art/ui-screens/` — 메인 메뉴·로그인·이야기 모음 시안의 배경/부품 분리본 전체(README, layout.json, 9-patch, shape XML, 비교 미리보기)와 이 검수 문서.
