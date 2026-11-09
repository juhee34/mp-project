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
- 내용 검증: 생성 후 실행 로그를 확인할 것.
- Gradle 빌드와 Android 실행: 미검증. 현재 작업 환경에 유효한 Android SDK가 없고 외부 다운로드가 실패했다.
- Gradle Wrapper JAR와 표준 실행 스크립트: 아직 없음. scripts/bootstrap-gradle.ps1로 정상 인터넷 환경에서 생성해야 한다.
- 설치된 Android Studio의 JBR은 Java 25로 확인됐다. 고정 Gradle 8.9 프로젝트에는 Gradle JDK 17을 별도로 선택한다.
- GitHub 원격 업로드: 아직 미완료. 인증 상태 확인 후 README 상태와 함께 갱신한다.

## 미완료
러닝 엔진/충돌/점수/저장/음악 재생/정식 아트/배경 6종/테마 6곡/완성 에피소드.
현재 화면은 개발 시작용이며 플레이 가능한 게임이 아니다.

## 다음 작업
docs/START_HERE.md에 따라 JDK 17과 SDK 35를 준비하고 wrapper 생성, Gradle sync, 첫 빌드를 확인한다. 그 다음 모든 6명을 유지한 러닝 기능을 구현한다.

## 2026-11-09 구성 변경
- 사용자 확정: 라일·치즈·포포·루미·누누·피코 6명. 첫 시연부터 전원 선택 및 플레이 목표.
- 캐릭터·서사·배경/음악 명세·설정집을 6명으로 정리하고 제외 캐릭터와의 관계를 수정했다.
- docs/PROJECT_PLAN.md에 A 역할 기획, 화면 흐름, 데이터 규격 제안, 미확정 과제 조건을 기록했다.
- 게임 기능·정식 소재는 이 변경으로 완성되지 않는다. Android 실행 검증은 별도 필요하다.

## A 역할 콘텐츠 상세화
- 6명 생활 습관·애착 소품·달리는 이유·말투·짧은 소개·감정/갈등을 characters.json과 설정집에 기록했다.
- 기존 id와 모든 참조를 유지하고 새 콘텐츠 필드를 문서화했다. 배경·음악·이미지는 planned/null이다.
- 게임 코드 변경 없음. Android 빌드·플레이 검증은 이번 콘텐츠 작업 범위 밖이다.
- 6개 고유 ID, 필수 콘텐츠, 이야기 소유자, 배경/음악 ID, planned/null, 기존 205건 연구 검사 통과.
## 캐릭터 화면 인수인계

- 화면 담당 파일은 `app/src/main/java/com/tinyrunners/school/characters/`의 `CharacterScreens.kt`와 `CharacterPortrait.kt`, 그리고 `app/src/main/res/values/character_screen_strings.xml`이다. 화면은 Repository의 목록을 그대로 표시한다. 프로필에서 이름·사는 곳·성격·이야기 개요를 확인할 수 있고, 목록으로 돌아가면 이전 위치와 선택 표시를 복원한다.
- `MainActivity`가 화면 호스트와 뒤로가기·상태 복원을 연결한다. 팀원은 `MainActivity.onStartRun(characterId: String)`의 준비 안내를 게임 진입 코드로 교체한다. 별도 게임 Activity를 시작하면 게임에서 뒤로 갈 때 기존 프로필을 그대로 쓸 수 있다. 콜백에는 표시 이름이나 목록 위치 대신 데이터 계약의 캐릭터 ID가 전달된다.
- 임시 초상은 캐릭터의 기존 색상을 사용한다. 정식 초상이 준비되면 Repository에서 데이터 계약의 `portraitPath`를 읽고 `CharacterScreens.portraitArtwork`에서 불러온다. 파일이 없는 캐릭터에는 `CharacterPortrait`를 쓰고 임시 안내를 자산 상태에 맞춰 갱신한다.

## A 선택 화면 연결 구현
- 카드에 이름·성격·고향·선택 상태, 목록 아래 시작/프로필 버튼. 임시 초상 표시와 ScrollView 유지.
- MainActivity가 GameActivity로 characterId를 Intent extra로 전달한다. 수신 ID 검증 후 B 연결용 화면을 표시하며 실제 러닝 엔진은 아직 없다.
- 6명 데이터 순서와 XML 검사 통과. Gradle daemon/ADB 연결 오류로 컴파일·실제 6명 탭·작은 화면 검증은 미실시.
- B 연결 규격과 기기 검증 절차: docs/A_SCREEN_HANDOFF.md.

## 최신 디자인 변경
- 민트의 표시 이름을 라일로, 몸 색상을 라벤더로 변경. 호환성을 위해 내부 ID mint 유지.
- 루미 빨간 망토, 6명 7가지 동작·감정 명세: docs/ART_DIRECTION.md.
- 생성 선택보드는 콘셉트이며 정확한 픽셀 격자·프레임·발 기준선 검증 전이다. 최신 이미지 원본 파일은 저장소에 아직 포함되지 않았다.
