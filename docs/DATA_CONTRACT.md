# 공동 데이터 계약

## ID
mint, cheese, popo, lumi, nunu, piko — 변경 금지. 표시 이름과 ID를 분리한다.

## 캐릭터
app/src/main/assets/content/characters.json이 앱의 캐릭터 데이터다.
id/name/species/home/personality/summary/color는 필수 문자열.
portraitPath/spritePath는 실제 파일이 없으면 null. backgroundId/musicId는 캐릭터 ID와 같다.
storyIds는 stories.json에 존재하는 ID만 참조한다. assetStatus=planned는 소재 미완료다.
현재 Kotlin 모델은 시작 화면에 필요한 필드만 읽는다. 게임 구현 시 추가 필드를 명시적으로 파싱한다.

- 친구 소개 문구(2026-11-12): assets/content/character_copy.json은 docs/character-copy/friendly-copy.ko.json 복사본. id로 읽어 CharacterDefinition.copy(CharacterCopy: tagline, selectionIntro, personality, traits, habits, cherishedItem, runningReason, storyTeaser, greetings). 기존 personality·summary는 바꾸지 않으며 copy가 없으면 화면이 그것을 쓴다. voiceNote·sourceStoryIds는 읽지 않는다. greetings는 scripts/build-greeting-lines.cjs가 short_lines_greetings.json(<id>_select_greeting_0n, event select)으로 따로 만든다.
- 승인 그림(2026-11-12): res/drawable-nodpi/hazard_{air,spikes,platform}_<id>.png와 item_<id>.png는 docs/art/approved/extracted/ 원본 그대로(bag_mint.png는 B가 pending 원본에서 풀밭만 걷어 낸 분리본). 그림 외곽은 충돌 판정이 아니다(판정은 엔진 수치).

## 이야기
2026-11-10 도감 구현 기준. `stories.json`은 6명 × 4편 = 24개다.

- 기존 `<characterId>_intro` 유지. 추가 ID는 `<characterId>_episode_1`, `_episode_2`, `_episode_3`.
- `id`, `characterId`, `title`, `body`, `status`, `unlockDistance` 유지. `kind`(background/episode), `order`(0~3), `summary`, `afterReading`, `pages` 추가.
- 소개와 첫 에피소드는 `status=ready`(12개). 2·3편은 `status=outline`(12개)이며 `body`는 제작용 개요다. 도감에서는 개요 본문을 표시하지 않는다.
- ready의 `pages`는 `{title, blocks:[{speaker:null 또는 캐릭터 표시 이름, text}]}` 배열이다. `body`는 페이지 블록을 순서대로 합친 전체 본문이다. outline은 `pages=[]`.
- 소개는 `unlockDistance=0`, 첫 에피소드는 해당 캐릭터의 `totalMeters >= 100`으로 열린다. 한 판 최고 거리와 혼동하지 않는다. 실패한 판의 누적 거리도 유효하다.
- 잠긴 첫 에피소드에도 사용자가 선택하는 ‘거리와 관계없이 감상하기’를 제공한다. 이 선택은 해당 이야기만 영구 개방하며 달리기 기록을 변경하지 않는다.
- outline은 거리·읽음 기록보다 우선하여 항상 준비 중이다. 거리 조건으로 개요를 열지 않는다.
- 생성: `python scripts/build-story-content.py`. 검증: `node scripts/validate-content.cjs`. 원문은 `docs/STORY_MANUSCRIPT_01~03.md`이며 JSON만 직접 수정하면 재생성 시 덮어써진다.

## 이야기 대사 (2026-11-10, IDEA_GUIDE 권장안 확정)
- 2026-11-12: 마을 첫 방문(<id>_intro)도 STORY_MANUSCRIPT_0n.md의 배경 이야기로 생성한다(build-story-content.cjs). 손으로 쓴 소개 대화는 더 이상 보존하지 않는다.
- **2026-11-10 분리:** 대사로 보는 "마을 이야기"(B, `story/`)는 `content/village_stories.json`을 쓴다. `stories.json`은 A의 이야기 도감(`stories/`) 전용이다. 두 기능은 메뉴 이름(마을 이야기 / 이야기 도감)과 저장 위치가 다르며 서로의 파일을 읽지 않는다. 검사기는 두 파일을 각각 검사한다.
- `village_stories.json`의 이야기에 선택 필드 `lines`, `unlock`을 더한다. `body`는 대사가 없을 때 보여 줄 개요다.
- `lines`의 한 줄: `{speaker, text}` / 선택지 `{choice:[{text, reply:[{speaker,text}]}]}`(2~3개, 이야기당 최대 2번, 바로 다음 대사만 달라짐) / 달리기 `{run:true}`(이야기당 최대 1번, 결과와 상관없이 이어짐).
- `speaker`: 캐릭터 ID, `player`(회원가입 닉네임, 없으면 "나그네"), `narrator`(이름 없이 표시).
- `unlock.afterStory`: 같은 친구의 앞 이야기 ID. 그 이야기를 보면 열린다. 없거나 null이면 처음부터 열린다.
- `lines`가 없는 이야기는 `body`의 문장을 해설 줄로 바꾸고 마지막 문장 앞에서 달린다(대사 작성 전 임시 재생).
- 현재 `mint_intro`에만 B가 쓴 샘플 대사(status=draft)가 있다. A가 고쳐 쓴다.
- 본 이야기: CharacterProgress.readStoryIds. 끝까지 보거나 건너뛰면 기록한다. Firestore는 progress 문서에 arrayUnion으로 더한다.

## 2단 점프
- 공중에서 1회, 속도 700(추가 높이 약 102). 6명 같다. 스킬은 2026-11-10에 범위에서 뺐다(IDEA_GUIDE 참고).

## 캐릭터 크기와 장애물 (2026-11-10)
- 그림 1도트 = 1.75 월드 단위. 48×48 칸 = 84단위(playerSize), 몸 높이 약 56. 화면에서는 정수 배로 반올림해 그린다(1080p 가로 폰 5배).
- 충돌 영역: 너비 52(좌우 16씩 안쪽), 발에서 높이 50. 6명 같다.
- 장애물 그림 연결(2026-11-10): BOX는 너비 44~64, 높이 = 너비×0.6~1.0(30~64)로 바꿨다. A의 소품 그림(높이/너비 0.5~1.23)과 비율이 맞아 거의 늘어나지 않는다. 각 BOX는 그 친구의 소품 3종 중 비율이 가장 가까운 것으로, TOWER는 가장 상자 같은 소품(비율 0.9에 가까운 것)을 쌓아 그린다. FLOAT는 그린 나뭇가지 그대로. 그림 선택은 화면 쪽(game/ObstacleArt.kt)에서만 하며 물리는 6명 같다.
- (이전 기록) 장애물(RunnerEngine.ObstacleKind): BOX 높이 40~80·너비 30~56(점프 1번), TOWER 높이 150~170·너비 30~40(1단 점프 최고점 140보다 높아 2단 점프 필요), FLOAT 바닥 80~120·두께 28·너비 60~100(아래로 지나가면 안전, 뛰어들면 충돌).
- 출발 후 8초는 BOX만, 이후 TOWER 25%·FLOAT 20%·BOX 55%. 탑 앞뒤 간격은 2단 점프 체공 시간 전체를 넣어 계산한다.
- 2026-11-11 갱신: 처음 100m는 BOX만(8초 기준 폐지), 이후 TOWER 25%·FLOAT 20%·SPIKES 15%·BOX 40%. FLOAT 두께 28→18(위아래 가시 모양, 가시도 충돌 범위). SPIKES: 바닥 가시 높이 26, 폭 = 속도×2단 점프 체공시간+30(점프로 못 넘음), 위에 한쪽 통과 발판(윗면 85~105, 두께 14, 가시 양끝보다 50씩 김). 발판 위에서는 floorY=발판 높이로 서 있고 끝을 지나면 떨어진다. 6명 동일.

## 배경과 음악
assets.json의 background/music에는 id, path, status, brief가 있다.
path=null인 파일은 로드하지 않는다. planned 항목은 완료 통계에 포함하지 않는다.
스프라이트 규격은 docs/SPRITE_SPEC.md로 확정했다(2026-11-10): 48×48 셀, 발 기준선 y45, 충돌 영역 x9~38, 필수 idle2·run4·jump2·fail1, `res/drawable-nodpi/sprite_<id>_<동작>_<번호>.png`. 필수 프레임이 다 있으면 spritePath="sprite_<id>", assetStatus="sprite_ready".
최종 배경은 원경/중경/전경/바닥을 분리할 수 있어야 한다. 반복 이음새를 실제로 확인한다.
음원은 60~90초 반복 기악곡을 목표로 하고 제작 출처 및 실제 파일을 기록한다.

## 상태 및 저장 제안
RunResult(runId, characterId, distance, score)와 CharacterProgress(characterId, totalDistance, bestScore, readStoryIds)를 분리한다.
runId별 중복 저장 방지. 실패 거리도 반영. 6명 동일 점프/속도/기본 충돌 판정. 미접속 관계 감소 없음.

## 저장 구현 (B, 2026-11-09)
- 코드: `progress/Progress.kt`(RunResult, CharacterProgress, ProgressRules, ProgressBook), `progress/ProgressStore.kt`.
- RunResult(runId, characterId, meters, score, cleared). runId는 GameActivity가 판마다 UUID로 만든다.
- CharacterProgress(characterId, runs, totalMeters, bestMeters, bestScore). 실패한 판도 runs·totalMeters에 더한다. 감소·만료 없음.
- 같은 runId는 한 번만 반영한다. 로그인 사용자는 Firestore(`users/{uid}/runs/{runId}`, `users/{uid}/progress/{characterId}`) 트랜잭션, 임시 모드는 기기 SharedPreferences.
- A의 도감은 `ProgressStore.forCurrentUser(context).loadAll { }`로 거리만 읽는다. B의 진행 저장 스키마는 변경하지 않는다. 읽음 표시는 아래 별도 로컬 저장 계약을 따른다.
- 짧은 대사 추가분(B, 2026-11-11): assets/content/short_lines_expansion_0n.json은 docs/dialogue/short-lines-expansion-0n.json의 그대로 복사본. 앱은 short_lines.json과 합쳐 id 기준으로 한 번씩 읽는다. 마을 이야기 첫 인사는 story_unread(처음)/story_reread(다시), 개요 이야기는 select.
- 마을 이야기(B, 2026-11-11): village_stories.json = 친구마다 <id>_intro, <id>_episode_1~3(unlock.afterStory로 차례). 1·2편 lines는 scripts/build-story-content.cjs가 원고에서 만든다(손으로 고치지 말고 원고를 고친 뒤 다시 실행). speaker는 6명 id 누구나, player, narrator. 앱은 모두 대화 장면(TalkActivity)으로 보여 주며 run 단계는 대화에서 건너뛴다. stories.json 2편은 status ready.
- 코인 충전·물품 구매(B, 2026-11-12): shop/state에 bought(Int, 시연용 코인 충전 누계) 추가, 잔액 = 총거리÷25 + found + bought − spent. 물품 구매는 spent += 10과 items/bag의 have +1을 한 트랜잭션에서. 광고 보상은 found에 더한다.
- 보너스 코인(B, 2026-11-11): users/{uid}/shop/state에 found(Int, 길에서 주운 코인 누계) 추가. 잔액 = 총거리÷25 + found − spent. 임시 모드는 shop_guest의 found.
- 친구 물품(B, 2026-11-11): 로그인 사용자는 `users/{uid}/items/bag` {have: {친구id: Int}, given: [storyId]}, 임시 모드는 기기 SharedPreferences(items_guest). 물품 정의는 quest/Quest.kt(친구당 1종, 이야기마다 3개 필요). 모르는 친구 id와 음수는 읽을 때 버린다. 기존 보안 규칙(users/{uid}/**)으로 충분하다. 친구 물품은 ShopRules.ITEM_PRICE(2026-11-12부터 25코인)로 산다.
- 상점 음악(B, 2026-11-11): shop/state에 music(고른 음악 id, 기본 music_main) 추가. 음악 아이템 music_<친구id>는 res/raw/music_<친구id>.(mp3|m4a|ogg|mp4)가 있어야 팔린다. 메인 음악 res/raw/main_theme.mp4.
- 꾸미기 상점(B, 2026-11-11): 코인 = 모든 친구 totalMeters 합 ÷ 25(버림, ShopRules.METERS_PER_COIN) − 쓴 코인. 2026-11-11 10m→25m로 낮춤. 로그인 사용자는 `users/{uid}/shop/state` {owned: [itemId], equipped: itemId, spent: Int}, 구매는 진행 문서 6개와 같은 트랜잭션에서 잔액 확인. 임시 모드는 기기 SharedPreferences(shop_guest). 아이템은 달릴 때 뒤에 그려지는 모양(TrailStyle)뿐이며 물리·충돌과 무관하다. 기존 보안 규칙(users/{uid}/**)으로 충분하다. 친구 물품은 ShopRules.ITEM_PRICE(2026-11-12부터 25코인)로 산다.
- 상세 설정·보안 규칙: docs/FIREBASE_SETUP.md.

## 화면 계약
(로그인/회원가입) → 시작 화면(HubActivity: 스토리 진행하기 → 세계 이야기 → 마을 주민 대화 / 게임하기 → 6명 선택 → 친구와 대화(TalkActivity) → game(characterId) / 튜토리얼 하기 → 게임 안내) → 결과(runId 저장) → 다시 달리기 / 내 기록 / 친구 선택. 소개·선택 화면 위 메뉴에서 내 기록·설정.
정의되지 않은 ID는 선택 화면으로 복귀한다. 원시 JSON과 Activity 사이의 의존을 Repository로 제한한다.

### 출발 전 대화 (B, 2026-11-10)
- 2026-11-11 갱신: 대화는 매번 나온다. 그 친구와 첫 달리기만 talks.json의 긴 대화, 이후는 short_lines.json(A의 docs/dialogue/short-lines.json 복사본)의 select+start. 한 번 더 달리기는 retry, 부딪힌 뒤에는 내레이션 + stumble 후 결과 화면. 앱 복사본은 원본과 같아야 한다(검사기).
- `content/talks.json`: `prologue`(문자열 배열, 한 번 누를 때 한 쪽), `talks`(6명 순서, `{characterId, lines:[{speaker, text}]}`). speaker는 해당 캐릭터 ID, `player`(로그인 닉네임, 없으면 "나"), `narrator`(이름표 없음). 마지막 대사는 그 친구가 한다.
- 세계 이야기는 계정별 첫 로그인 한 번, 친구 대화는 계정·친구별 첫 달리기 한 번만(TalkProgress, 기기 저장).
- 대화 배경은 그 친구의 달리기 배경 첫 장면(거리 0)을 멈춘 채 쓴다. 대화가 끝나거나 건너뛰기를 누르면 게임이 시작된다. 다시 달리기·이야기 달리기·도감의 바로 달리기는 대화를 거치지 않는다.
- 검사: validate-content의 Talks 항목, TalkTest.

### 도감 화면·읽음 저장 (A, 2026-11-10)
- 소개·친구 선택·프로필 → `StoryActivity`. `StoryParser`가 JSON을 모델로 변환한다. 선택/프로필 진입은 현재 캐릭터를 전달한다.
- 도감 표시: 아직 읽지 않음 / 읽은 이야기·다시 읽기 / 아직 열리지 않음 / 후속편 준비 중. 6명 탭과 4개 카드 제공.
- `StoryStore`의 계정별 기기 SharedPreferences(`story_reading_v1_user_<uid>` 또는 guest)에 읽은 ID, 열린 ID, 페이지 번호를 저장한다. Firestore와 다른 기기 동기화는 하지 않는다. UI에도 이 제한을 알린다.
- 중간 종료는 읽던 페이지만 저장한다. 마지막 페이지의 ‘읽음으로 저장’을 선택해야 읽음이 된다. ‘다시 읽기’는 처음부터 시작하며 기존 읽음 표시는 유지한다.
- 이미 열린 이야기는 거리 조회 실패로 다시 잠기지 않는다. 조회 실패 시 안내하고 수동 감상 진입은 계속 제공한다. 읽음 저장 실패 시 성공으로 표시하지 않는다.
- ‘나중에 읽기’, ‘바로 달리기’를 고정 하단에 배치한다. 본문 스크롤·완독·도감 방문은 게임 진행 조건이 아니다. 미완성 개요는 감상 버튼이 없다.
- 기기 삭제/앱 데이터 초기화 시 읽음·열림·책갈피는 사라진다. 계정 간 상태를 공유하지 않는다. 향후 서버 동기화는 B와 별도 합의한다.

## 기획 합의
상세 필드 제안은 PROJECT_PLAN.md를 따른다. 기존 JSON 키 id는 유지하며 화면 전달값 characterId와 같은 식별값이다. 전체 키 변경은 B와 합의 후 진행한다.

## A 역할 콘텐츠 필드 보강
기존 id 및 자산/이야기 참조는 유지한다. characterId는 화면 전달 시 같은 id를 뜻한다. 새 필드는 콘텐츠 전용이며 이번 작업에서 Kotlin 모델이나 화면 코드는 변경하지 않는다. B가 읽어 사용할 시점을 합의한다.

| 필드 | 형태 | 의미 |
| --- | --- | --- |
| habits | 문자열 배열 | 생활 습관 |
| cherishedItem | 문자열 | 애착 소품 |
| runningReason | 문자열 | 달리는 구체적인 이유 |
| speechStyle | 문자열 | 말투 원칙 |
| sampleLines | 문자열 배열 | 짧은 대사 예시 |
| shortIntro | 문자열 | 선택·프로필용 짧은 소개 |
| design | 문자열 | 따뜻한 픽셀 디자인 명세 |
| centralConflict | 문자열 | 캐릭터 고유 갈등 |
| emotionalTheme | 문자열 | 감정 중심 |
| contentStatus | 문자열 | 현재 draft: 콘텐츠 초안 |

소재 상태 assetStatus=planned는 콘텐츠 상태 draft와 별개다. background/music의 실제 제작 상태는 assets.json에서 관리한다.

## 이름 변경 호환성
표시 이름 민트 → 라일(LAIL), 몸 색상은 라벤더로 변경한다. 기존 저장·이야기·게임 연결을 보존하기 위해 내부 ID mint와 mint_intro, backgroundId/musicId는 유지한다. 루미의 망토는 빨간색이다.


## 배경 파노라마 연결 (2026-11-10)
- background.status=panorama_ready는 승인된 단일 파노라마의 앱 연결 상태다. 원근 레이어 및 무한 반복 완성을 뜻하지 않는다.
- background.path는 assets 기준 원본 PNG 경로, manifest는 backgrounds/manifest.json이다. characterId로 해당 rect/floorY를 찾는다.
- rect=[x,y,width,height], floorY는 표시 영역 상단에서 충돌 바닥까지의 이미지 픽셀 거리다. 원본은 그대로 보존한다.
- loopReady=false, layered=false. 거리 기반 0.25배 카메라는 끝에서 clamp한다. 엔진 거리/점프/충돌/점수는 바꾸지 않는다.
- 바닥 위 그림은 비율을 유지하며 화면을 덮는다. 바닥 아래 띠만 기존 엔진 바닥 두께에 맞춰 세로 표시한다.


## 2026-11-11 배경 출발·반복 분리
- 사용자 확정: 한 판에서 집은 최초 한 번. 출발 연결 이후 집 없는 달리는 길만 반복한다. 재시작(distance=0)할 때만 집이 다시 나온다.
- assets.json background.status=running_loop_ready, path는 출발 PNG, loopReady=true, layered=false. panorama_ready 구형 자산은 이전 계약을 유지한다.
- backgrounds/manifest.json sequence=intro_once_then_running_loop. source/rect/floorY는 출발 이미지, loopSource는 독립 반복 이미지 전체다. 두 PNG 높이 280, 바닥 기준 y256 동일. sha256/loopSha256는 각 파일 해시다.
- RunningBackgroundMotion은 출발 이미지를 한 번 배치하고 이후 loopSource만 배치한다. 파일 누락 시 기존 집 반복으로 대체하지 않는다. loopSource 필드 없는 구형 자산은 원본 끝에서 멈춘다.
- 루프의 원경 지붕·작은 창문은 마을 풍경이다. 캐릭터의 큰 출발 집·무대·선착장 집은 반복 이미지에 넣지 않는다.
- 아트 내보내기/경계 검수와 실제 Android 기기 검수는 구분한다.

## 홈 화면 위젯 대사 (2026-11-15, B)
- 위젯(widget/FriendWidget.kt)은 기존 짧은 대사 묶음(content/short_lines*.json)에서 `event`가 `widget_clear`, `widget_cloudy`, `widget_rain`, `widget_snow`, `widget_hot`인 줄을 그 날씨에 씀. 형식은 다른 짧은 대사와 같음(`id`, `characterId`, `event`, `text`, `requiresReadStoryIds`). 안개는 cloudy, 폭풍은 rain, 28°C 이상은 hot.
- A가 아직 넣지 않았으므로 지금은 친구의 `home` 대사(없으면 `select`)를 씀. 새 이벤트를 `events` 목록에 올리면 validate-content가 친구마다 5줄을 요구하므로, 목록에 올리지 않고 줄만 추가하거나 5줄씩 채운다. 위젯 한 줄은 15자 안팎(2줄까지 보임).
- 오늘의 친구: 마지막으로 함께 달린 친구, 달린 적이 없으면 날마다 돌아가며(TodayFriend.choose). A·B가 다른 규칙으로 정하면 이 함수만 바꾼다.

## 친구의 날씨 안부 알림 대사 (2026-11-16, B)
- 알림(notice/FriendNotice.kt)은 짧은 대사 묶음에서 `event`가 `notice_rain`, `notice_snow`, `notice_hot`, `notice_cold`인 줄을 그 날씨에 씀(형식은 다른 짧은 대사와 같음). 아직 없으면 친구의 `home` 대사.
- 특별한 날: 비(폭풍 포함)·눈·30°C 이상·-5°C 이하(NoticeRules). 재촉하거나 놓치면 손해라는 말은 쓰지 않는다. validate-content의 events 목록 규칙은 위젯 대사와 같다.

## 계절 꾸미기와 엽서 (2026-11-16, B — 보완 가이드 ⑤)
- 계절: 봄 3–5월, 여름 6–8월, 가을 9–10월, 겨울 11–2월(Season.of). 꾸미기(ShopItem)에 `season`(null = 일 년 내내). 상점은 그 계절 것과 이미 산 것만 보여 줌(ShopCatalog.onShelf). 산 꾸미기는 계절과 상관없이 계속 씀. 능력치 영향 없음.
- 지금 계절 꾸미기: 겨울 `snowflakes`(눈송이 자국)·`yarn`(털실 방울), 봄 `clover`, 여름 `fireflies`, 가을 `maple`. 모양은 TrailPainter의 픽셀 도형(임시, A 그림 오면 교체).
- 진행 기록 `CharacterProgress.specialWeatherRuns`(새 필드, 옛 저장은 0): 실제 특별한 날씨(비·폭풍·눈·30°C 이상·-5°C 이하, 설정에서 직접 고른 하늘은 제외)에 함께 달린 횟수. Firestore runs 문서에 `specialWeather`도 남김.
- 엽서: `app/src/main/assets/content/postcards.json` — `postcards[]`: `id`(친구id_번호), `characterId`, `title`, `back`(뒷면 친구 말), `draft`(B 임시 문구면 true), `condition`{`type`: `meters`(함께 달린 거리 ≥ value) | `stories`(그 친구 마을 이야기 모두, value 0) | `weather`(특별한 날씨 달리기 ≥ value), `value`}. 친구마다 2장 이상. validate-content가 검사.
- 받은 엽서는 저장하지 않고 진행 기록에서 계산(PostcardRules.earnedIds) — 기록이 있으면 기기를 바꿔도 같음. 새 엽서 안내를 이미 보여 준 id만 기기에 둠(PostcardSeen).
- 엽서첩 위치: 가방 화면 위쪽 "엽서첩" 버튼(B 기본값, A와 다르게 정하면 버튼만 옮김).


## 달리며 흐르는 이야기와 보물 전설 엔딩 (2026-11-16, B)
- `app/src/main/assets/content/run_stories.json`: 키 = 마을 이야기 id(stories.json, 24개 전부) — 읽음 기록(readStoryIds)을 그대로 씀. `default`는 대본 없는 이야기용({name}, {title} 채움).
  - `lines[]`: `at`(달린 m), `who`(friend = 머리 위 말풍선 | narrator = 위쪽 자막), `text`.
  - `fork`: `at`(갈림길이 생기는 m, 모든 줄 뒤), `question`, `up`(점프해 높은 길), `down`(그대로 아래 길), `correct`(그 화의 옳은 길 up|down).
  - `endings`: 보통 화는 `up`/`down`(틀린 길이면 "단서를 놓친 것 같다" 자막이 덧붙음), 마지막 화(`finale: true`, episode_3)는 `happy`/`hidden`/`bad`.
- 엔딩 규칙(EndingRule): 앞 화들과 마지막 화의 길이 모두 옳으면 낮 = 해피, 밤 = 히든(실제 밤 또는 설정에서 고른 밤). 하나라도 틀리면 배드. 고른 길과 본 엔딩은 계정별로 기기에 저장(RunRoutes, SharedPreferences "routes_<uid>").
- 고른 길에 따라 달리는 음악 색이 바뀜(Music.setRoute: 같은 곡에 이퀄라이저 — 위 = 밝게, 아래 = 멀고 부드럽게). 곡이 악기별로 나뉜 파일이 아니라 악기 구성 자체를 바꾸지는 못함.
- 날씨 난이도(Difficulty.of): 맑은 낮 쉬움(속도 ×0.88, 여유 +0.2초) … 밤의 비·뇌우 아주 어려움(×1.18, −0.1초). 모든 친구 동일, 가장 어려워도 상자는 한 번 점프로 넘음(RunStoryTest).
- validate-content: 24개 이야기 id 모두 대본, 갈림길이 줄 뒤, 엔딩 키, 마지막 화 3엔딩 검사.


## 이야기 4막 구성과 날씨 (2026-11-16, B — 위 "달리며 흐르는 이야기"를 넓힘)
- 한 판 = 한 이야기, 4막: ① 발단 = 달리기 전 RPG 대화(`opening`, 없으면 `lines`의 앞 두 줄) ② 전개·위기 = 나머지 `lines` 말풍선/자막 + 갈림길 ③ 절정 = 갈림길 8m 뒤(늦어도 25m) 달리기를 멈추고 RPG 대화(보통 화: 그 이야기의 `endings.up/down` 또는 `climax.up/down`, 마지막 화: 공용 `finaleClimax.right/wrong`) ④ 결말 = 조금 빠른 마지막 질주 `goal`m(기본 45, 이야기별 `goal` 가능) → 엔딩 일러스트 + RPG 대화(보통 화: 공용 `closing.up/down` 또는 이야기 `closing`, 마지막 화: `endings.happy/hidden/bad`). 결말 대화까지 보면 읽음 기록, 중간에 넘어지면 다시.
- 날씨: 공용 `legendWeather.<하늘>`(해설, 발단 끝에 붙음)과 `weather.<친구id>.<하늘>`(친구 말풍선, 32m). 하늘 키 = clear, cloudy(안개 포함), rain, snow, storm, night(밤이면 하늘보다 우선). 날씨를 끄면 안 붙음. 실제 날씨를 최대 3초 기다림.
- {name}, {title}은 공용 대사에서 채워짐. validate-content가 6하늘 × 6친구 날씨 대사, finaleClimax, closing, goal을 검사.
