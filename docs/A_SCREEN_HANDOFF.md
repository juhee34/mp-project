# A 화면 → B 게임 연결

## 구현

소개 → 6명 선택 → 게임 진입 Activity. 선택 목록에 이름·종족·성격 첫 문장·고향을 표시한다. 카드를 누르면 선택 표시와 Android selected 상태가 갱신된다. 목록 아래 시작 버튼은 현재 선택 ID를 전달하며 프로필 버튼은 별도로 제공한다. 처음에는 라일가 선택된다.

목록·프로필은 ScrollView와 높이 WRAP_CONTENT를 사용한다. 글자 줄 수를 제한하지 않고 버튼은 최소 높이만 지정한다. 마지막 친구와 시작 버튼까지 스크롤할 수 있다. 상태 저장 시 선택 ID와 목록 위치를 보존한다. 초상은 코드로 그린 임시 픽셀 이미지임을 명시한다.

## 실제 연결 규격

- `CharacterScreens`의 `(characterId: String) -> Unit` 콜백 → `MainActivity.onStartRun` → `startActivity(GameActivity.intent(this, characterId))`.
- 수신 Activity: `com.tinyrunners.school.game.GameActivity`, 외부 실행 불가(exported=false).
- Intent extra key: `GameActivity.EXTRA_CHARACTER_ID = "characterId"`.
- 값: `mint`, `cheese`, `popo`, `lumi`, `nunu`, `piko`. 표시 이름이나 인덱스를 전달하지 않는다.
- 수신 시 Repository에서 검증한다. 누락·잘못된 ID는 finish하여 기존 선택 화면으로 돌아간다.
- Activity 재생성 시 Intent에 같은 ID가 유지된다. 게임에서 뒤로가기 또는 종료 시 선택 화면이 유지된다.

**현재 GameActivity는 ID를 실제로 수신하는 연결용 화면이다. 러닝 엔진은 아직 없어 플레이 완료로 간주하지 않는다.** B는 검증된 character를 게임 상태/렌더러 생성에 사용하고 준비 안내 화면을 실제 러너로 교체한다. 선택 콜백과 extra 키는 유지한다. backgroundId/musicId는 해당 데이터 행에서 읽고 planned/null 소재를 실제 파일처럼 로드하지 않는다.

## 순서대로 실행할 검증

| 순서 | 카드 | 시작 후 수신 ID |
| --- | --- | --- |
| 1 | 라일 | mint |
| 2 | 치즈 | cheese |
| 3 | 포포 | popo |
| 4 | 루미 | lumi |
| 5 | 누누 | nunu |
| 6 | 피코 | piko |

각 행마다 카드를 선택 → 선택됨 문구 확인 → 아래로 스크롤 → 시작 → 게임 진입 ID 확인 → 뒤로가기 → 선택 유지 확인. 마지막 피코도 목록 끝까지 스크롤하여 접근한다. 프로필에서도 시작해 같은 ID가 전달되는지 확인한다.

320dp 너비·작은 화면과 가로 화면에서 반복한다. 글꼴 배율 1.0/1.3/2.0에서 이름·성격·고향·시작 버튼이 스크롤로 모두 읽히는지 확인한다. 선택 화면과 게임 진입 화면 회전 후 선택 ID를 확인한다. 잘못된 ID 및 누락 extra는 선택 화면 복귀를 확인한다.

## 이번 검증 결과

- 6명 순서·고유 ID·이야기/자산 데이터 참조 검사 통과.
- 콜백에서 Intent extra, 수신 Repository 조회까지 정적 연결 확인. Android XML 구문 검사 통과.
- Gradle 빌드 시도: Gradle daemon 연결 실패로 컴파일 미확인.
- ADB 시도: 서버 연결 실패로 6명 실제 탭·작은 화면·회전 실행 검증 미실시. 위 절차를 기기에서 수행해야 한다.
- B의 엔진 연결 및 실제 플레이 검증은 미완료.
