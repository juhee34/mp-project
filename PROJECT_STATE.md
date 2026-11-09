# 현재 상태

기준일: 2026-11-09

## 준비 완료
- Kotlin Android 프로젝트 골격과 리소스
- 6명 캐릭터 선택 및 프로필 연결 코드
- 6명 캐릭터/서사 개요/배경 및 음악 명세 JSON
- 소개서, 단계별 프롬프트 20개, 역할 분담
- 이야기 연구 205편과 유사 게임 10개 출처
- 내용 검증 도구, Windows 개발 환경 준비 절차

## 검증 상태
- `node scripts/validate-content.cjs` 통과. 6명 ID와 이야기·배경·음악 참조를 확인했다.
- 화면 검증용 로컬 도구로 JDK 17.0.14, 공식 체크섬을 확인한 Gradle 8.9와 Android SDK Platform 35, Build-Tools 34.0.0을 준비했다. 빌드에는 `.tools`의 Gradle 배포본을 사용했다.
- Debug APK 빌드와 린트가 성공했다. 린트 오류 0건, 경고 7건. 단위 테스트 소스는 없다.
- Gradle Wrapper JAR와 표준 실행 스크립트는 별도 준비가 필요하다. 기기 검증 결과는 [화면 인수인계](docs/A_SCREEN_HANDOFF.md)에 기록한다.
- GitHub 원격 업로드: 아직 미완료. 인증 상태 확인 후 README 상태와 함께 갱신한다.

## 미완료
러닝 엔진/충돌/점수/저장/음악 재생/정식 아트/배경 6종/테마 6곡/완성 에피소드.
현재 화면은 개발 시작용이며 플레이 가능한 게임이 아니다.

## 다음 작업
팀원은 `GameActivity`의 연결용 화면을 공통 러너로 교체하고 6명 각각으로 실제 플레이를 검증한다. 새 개발 환경에서는 [시작 안내](docs/START_HERE.md)에 따라 Gradle Wrapper를 준비한다.

## 2026-11-09 구성 변경
- 사용자 확정: 민트·치즈·포포·루미·누누·피코 6명. 첫 시연부터 전원 선택 및 플레이 목표.
- 캐릭터·서사·배경/음악 명세·설정집을 6명으로 정리하고 제외 캐릭터와의 관계를 수정했다.
- docs/PROJECT_PLAN.md에 A 역할 기획, 화면 흐름, 데이터 규격 제안, 미확정 과제 조건을 기록했다.
- 정식 소재 제작과 실제 게임 구현은 후속 단계다.

## 캐릭터 화면 인수인계

- 화면 담당 파일은 `app/src/main/java/com/tinyrunners/school/characters/`의 `CharacterScreens.kt`와 `CharacterPortrait.kt`, 그리고 `app/src/main/res/values/character_screen_strings.xml`이다. 화면은 Repository의 목록을 그대로 표시한다. 프로필에서 이름·사는 곳·성격·이야기 개요를 확인할 수 있고, 목록으로 돌아가면 이전 위치와 선택 표시를 복원한다.
- `MainActivity`가 화면 호스트와 뒤로가기·상태 복원을 연결한다. `MainActivity.onStartRun(characterId: String)`가 `GameActivity`에 ID를 전달한다. 팀원은 수신부의 연결용 화면을 러너로 교체한다.
- 임시 초상은 캐릭터의 기존 색상을 사용한다. 정식 초상이 준비되면 Repository에서 데이터 계약의 `portraitPath`를 읽고 `CharacterScreens.portraitArtwork`에서 불러온다. 파일이 없는 캐릭터에는 `CharacterPortrait`를 쓰고 임시 안내를 자산 상태에 맞춰 갱신한다.

## A 선택 화면 연결 구현
- 카드에 이름·성격·고향·선택 상태, 목록 아래 시작/프로필 버튼. 임시 초상 표시와 ScrollView 유지.
- MainActivity가 GameActivity로 characterId를 Intent extra로 전달한다. 수신 ID 검증 후 B 연결용 화면을 표시하며 실제 러닝 엔진은 아직 없다.
- B 연결 규격과 기기 검증 절차: docs/A_SCREEN_HANDOFF.md.
