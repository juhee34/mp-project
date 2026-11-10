# 공동 데이터 계약

## ID
mint, cheese, popo, lumi, nunu, piko — 변경 금지. 표시 이름과 ID를 분리한다.

## 캐릭터
app/src/main/assets/content/characters.json이 앱의 캐릭터 데이터다.
id/name/species/home/personality/summary/color는 필수 문자열.
portraitPath/spritePath는 실제 파일이 없으면 null. backgroundId/musicId는 캐릭터 ID와 같다.
storyIds는 stories.json에 존재하는 ID만 참조한다. assetStatus=planned는 소재 미완료다.
현재 Kotlin 모델은 시작 화면에 필요한 필드만 읽는다. 게임 구현 시 추가 필드를 명시적으로 파싱한다.

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
- 상세 설정·보안 규칙: docs/FIREBASE_SETUP.md.

## 화면 계약
(로그인/회원가입) → 소개 → 6명 선택 → game(characterId) → 결과(runId 저장) → 다시 달리기 / 내 기록 / 친구 선택. 소개·선택 화면 위 메뉴에서 내 기록·설정.
정의되지 않은 ID는 선택 화면으로 복귀한다. 원시 JSON과 Activity 사이의 의존을 Repository로 제한한다.

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
