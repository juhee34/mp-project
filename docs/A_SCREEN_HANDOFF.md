# A 화면 → B 게임 연결

## 구현

소개 → 6명 선택 → 프로필 또는 게임 진입 화면. 선택 목록에 이름·종족·성격 첫 문장·고향을 표시한다. 카드를 누르면 선택 표시와 Android selected 상태가 갱신된다. 목록 아래에서 선택한 친구의 프로필을 열거나 달리기를 시작할 수 있다. 처음에는 라일이 선택된다.

목록·프로필은 ScrollView와 높이 WRAP_CONTENT를 사용한다. 글자 줄 수를 제한하지 않고 버튼은 최소 높이만 지정한다. 마지막 친구와 시작 버튼까지 스크롤할 수 있다. 상태 저장 시 선택 ID와 목록 위치를 보존한다. 초상은 코드로 그린 임시 픽셀 이미지임을 명시한다.

## 실제 연결 규격

- `CharacterScreens`의 `(characterId: String) -> Unit` 콜백 → `MainActivity.onStartRun` → `startActivity(GameActivity.intent(this, characterId))`.
- 수신 Activity: `com.tinyrunners.school.game.GameActivity`, 외부 실행 불가(exported=false).
- Intent extra key: `GameActivity.EXTRA_CHARACTER_ID = "characterId"`.
- 값: `mint`, `cheese`, `popo`, `lumi`, `nunu`, `piko`. 표시 이름이나 인덱스를 전달하지 않는다.
- 수신 시 Repository에서 ID를 확인한다. 누락·잘못된 ID는 Activity를 닫아 진입 전 화면으로 돌아간다.
- Activity 재생성 시 Intent에 같은 ID가 유지된다. 게임에서 뒤로 가면 진입 전 선택 화면 또는 프로필로 돌아간다.

GameActivity는 공통 러너를 실행한다. 충돌 후 ResultActivity에서 결과를 저장하고, 다시 달리기·내 기록·친구 선택으로 이동한다. 결과의 ‘친구 선택으로’는 MainActivity.selectionIntent를 통해 선택 화면을 연다. 캐릭터 ID 계약은 그대로 유지한다.

## 순서대로 실행할 검증

| 순서 | 카드 | 시작 후 수신 ID |
| --- | --- | --- |
| 1 | 라일 | mint |
| 2 | 치즈 | cheese |
| 3 | 포포 | popo |
| 4 | 루미 | lumi |
| 5 | 누누 | nunu |
| 6 | 피코 | piko |

각 행마다 카드를 선택 → 선택됨 문구 확인 → 아래로 스크롤 → 시작 → 게임 HUD 이름과 결과 캐릭터 확인 → 뒤로가기 → 선택 유지 확인. 마지막 피코도 목록 끝까지 스크롤하여 접근한다. 프로필에서도 시작해 같은 ID가 전달되는지 확인한다.

320dp 너비·작은 화면과 가로 화면에서 반복한다. 글꼴 배율 1.0/1.3/2.0에서 이름·성격·고향·시작 버튼이 스크롤로 모두 읽히는지 확인한다. 선택 화면과 게임 진입 화면 회전 후 선택 ID를 확인한다. 잘못된 ID 및 누락 extra는 선택 화면 복귀를 확인한다.

## 병합 검증

2026-11-10 origin/main 04a86de와 화면 브랜치를 통합했다. CharacterScreens의 성격 요약 한 줄과 상태·인수인계 문서 충돌을 해결했다. 게임 결과 전환 대기 중 재시작과 프로필에서 출발한 판의 ‘친구 선택으로’ 복귀를 보완했다.

실행 결과와 미검증 항목은 [현재 상태](../PROJECT_STATE.md)의 최신 병합 검증 기록을 따른다. 이전 화면 전용 APK의 테스트 소스 없음·러너 연결 예정 기록은 최신 병합본에 적용하지 않는다.
