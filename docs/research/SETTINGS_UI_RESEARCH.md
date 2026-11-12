# 설정 화면 조사 — 2026-11-12 (B)

사용자 요청: 인스타·핀터레스트·네이버·구글 등에서 게임 설정 화면 구성을 100여 개 살펴보고, 그 구성과 디자인을 Tiny Runners 설정 화면에 적용한다. 소리 조절 기능을 더한다.

## 조사 범위 (실제로 한 것)

- 웹 검색 11회. 결과 목록에서 링크 약 180개(중복 포함)의 제목과 요약을 읽었다. 그중 내용을 확인해 근거로 쓴 자료는 아래 30여 개다.
- 인스타그램·네이버 블로그·핀터레스트 핀 상세는 로그인 벽 때문에 열지 못했다. 핀터레스트는 보드와 키워드 페이지 수준만 확인했다. 그래서 "100개 화면을 직접 보았다"고 하지 않는다. 사례 목록과 공통점은 검색 결과와 위키·가이드·디자인 가이드라인 문서를 근거로 정리했다.
- 게임 화면 캡처 데이터베이스(Game UI Database, 5만 5천여 장)는 존재와 분류 방식만 확인했다.

## 많이 보인 구성

| 항목 | 자주 보인 형태 | 출처 예 |
|---|---|---|
| 소리 | 전체·배경음·효과음을 따로 조절하는 슬라이더, 각 값 표시, 끄기 스위치 | Stardew Valley(음악 75%·효과음 100%·환경음 75%·발소리 90%), Stumble Guys(SFX·Music), Killer Instinct, AccelByte/Unity 튜토리얼, Xbox·Game Accessibility Guidelines |
| 진동 | 켜기/끄기 스위치 | Alto's Adventure(Haptics), 8bit 오디오 설정 컴포넌트, 드래곤 플라이트2 |
| 대사·글자 | 글자 크기, 대사 속도, 건너뛰기 | IGDA 접근성 Top 10, Unity 접근성 기초, BBC 모바일 지침 |
| 화면·움직임 | 화면 흔들림 끄기, 움직임 줄이기 | Xbox Accessibility Guidelines, Roblox Reduce Motion |
| 안내 | 튜토리얼 다시 보기/건너뛰기 | 여러 모바일 게임 도움말 |
| 계정·정보 | 계정, 버전, 로그아웃(다른 색, 맨 아래) | Apple HIG Settings, 여러 게임 FAQ |
| 배치 | 왼쪽 분류 탭 + 오른쪽 내용, 카드로 묶기, 바꾸면 바로 적용 | Stardew(탭 + 스크롤), 요즘IT 설정 UI 글(카드 묶음), Roblox 튜토리얼 |
| 분위기 | 아늑한 게임은 "적을수록 좋다", 같은 색·버튼 모양 유지 | Kitfox "Designing for Coziness", SDLC Corp cozy UI 글, itch.io cozy UI 키트들 |
| 접근성 | 색만으로 상태를 구분하지 않기, 대비 4.5:1, 기본 글자 크게 | Game Accessibility Guidelines, Roblox 접근성 문서 |

## Tiny Runners에 적용한 것

- 상점과 같은 크림 천·꿀색 나무 테두리·초록 버튼(상점 승인 시안과 한 벌). 왼쪽 분류 4개: 소리 / 대화·안내 / 날씨 / 계정·정보. 오른쪽 카드 안에서 스크롤.
- 소리: 전체 음량, 배경 음악(켜기 + 음량), 효과음(켜기 + 음량 + 들어보기), 진동(넘어질 때·아이템 주울 때). 값은 %로 보이고 움직이는 동안 바로 반영된다. 기본값은 기존 소리와 같게(음악 70%, 효과음 80%).
- 대화·안내: 대사 나오는 속도 느리게/보통/빠르게와 미리보기 문장, 처음 안내 다시 보기.
- 날씨: 켜기와 도시 7개를 버튼으로, 지금 날씨 미리보기, 출처 표기.
- 계정·정보: 닉네임·이메일(또는 임시 모드), 버전, 시연용 결제 안내, 로그아웃(붉은 테두리, 맨 아래).
- 넣지 않은 것: 글자 크기(앱 전체 글자 구조가 고정 크기라 이번 범위 밖), 화면 흔들림(게임에 흔들림이 없음), 언어(한국어 전용). 없는 기능을 설정에 만들지 않았다.

## 출처

- https://stardewvalleywiki.com/Options
- https://steamcommunity.com/app/413150/discussions/0/4351116701415490546
- https://stumbleguys.helpshift.com/hc/faq/74-what-are-the-available-settings-in-mobile
- https://docs.accelbyte.io/gaming-services/tutorials/byte-wars/unity/learning-modules/storage/module-cloud-save/unity-module-cloud-save-prepared-options-menu-overview/
- https://learn.unity.com/pathway/game-development/unit/user-interfaces/tutorial/add-toggles-and-sliders-1
- https://21st.dev/@theorcdev/components/8bit-audio-settings.md
- https://gameaccessibilityguidelines.com/basic/
- https://gameaccessibilityguidelines.com/?p=2063
- https://devdocs.xbox.com/build/game-principles/accessibility/xbox-accessibility-guidelines.md
- https://unity.com/blog/video-game-accessibility-basics
- https://igda-gasig.org/?p=42
- https://create.roblox.com/docs/production/publishing/accessibility.md
- https://create.roblox.com/docs/ko-kr/tutorials/use-case-tutorials/ui/interactive-ui
- https://create.roblox.com/docs/ko-kr/tutorials/curriculums/user-interface-design/choose-an-art-style
- https://bbcstreaming.pages.dev/accessibility/forproducts/guides/mobile/adjustability
- https://developer.apple.com/design/human-interface-guidelines/settings.md
- https://yozm.wishket.com/magazine/detail/313/
- https://wikidocs.net/357359
- https://www.gamemeca.com/en/mv.php?gid=1768765
- https://apps.apple.com/az/app/id950812012
- https://techpp.com/2018/02/28/alto-odyssey-zen-mode/
- https://www.androidcentral.com/animal-crossing-pocket-camp-beginners-guide
- https://medium.com/kitfox-games/designing-for-coziness-d33d2519a59e
- https://sdlccorp.com/post/the-art-of-designing-intuitive-user-interfaces-in-cozy-games/
- https://dinodeveloper.itch.io/cozy-cute-ui-pack
- https://ldcmleo.itch.io/pixel-ui-interface-cozy-style
- https://softgrid.itch.io/tiny-pixel-menu-panels-pack
- https://www.pinterest.com/ideas/cute-game-ui-design/959603082064/
- https://www.pinterest.com/pin/pink-video-game-screen-with-play-settings-quilt--203717583136417557/
- https://alternativeto.net/news/2024/8/game-ui-database-2-0-launches-with-over-55-500-screens-and-enhanced-tools-for-ui-designers/
- https://gdevelop.io/blog/borrowing-without-copying-ui
- https://help.netflix.com/en/node/578041874633316
- https://mytimeatportia.fandom.com/wiki/Game_options
