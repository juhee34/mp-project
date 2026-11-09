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
2026-11-09 B(Claude) 환경 확인 기준. JDK는 Eclipse Temurin 17.0.21(`JAVA_HOME`을 명령에만 지정), SDK는 Platform 35.
- 내용 검증: `node scripts/validate-content.cjs` → PASS (6 characters, 205 research records, 20 role prompts).
- Gradle Wrapper: `scripts/bootstrap-gradle.ps1`로 생성 완료(gradlew, gradlew.bat, gradle-wrapper.jar, SHA-256 고정). Windows PowerShell 5.1에서 실패하던 세 지점(java -version stderr, Invoke-WebRequest 기본 파싱, settings.gradle BOM)을 스크립트에서 수정했다.
- 명령줄 빌드: 6명 병합본 기준 `.\gradlew.bat :app:assembleDebug` 성공, app/build/outputs/apk/debug/app-debug.apk 생성. 병합 직후에는 CharacterScreens.kt 101행의 `'다.'` 문자 리터럴 때문에 컴파일이 실패해 `"다."` 문자열로 고쳤다.
- 단위 테스트: 러너 추가 후 `:app:testDebugUnitTest` 12개 통과, 실패 0 (RunnerEngineTest).
- Lint: `:app:lintDebug` 오류 0, 경고 6(앱 아이콘 없음, 사용하지 않는 리소스 2, Overdraw, 데이터 추출 규칙, 가로 고정 DiscouragedApi). 보고서: app/build/reports/lint-results-debug.html.
- 에뮬레이터 실행: 6명 병합본을 사용자가 Android Studio Run으로 실행해 6명 선택과 프로필 표시가 동작함을 확인했다(사용자 보고). 이때는 러닝 엔진이 없었다. 러너 추가 후 결과는 아래 "3단계 B 공통 러너"를 본다. 작은 화면 확인은 아직 하지 않았다.
- 설치된 Android Studio의 JBR은 Java 25로 확인됐다. 고정 Gradle 8.9 프로젝트에는 Gradle JDK 17을 별도로 선택한다. AGP 업그레이드 권유 알림은 버전 고정을 위해 따르지 않는다.
- GitHub 원격: https://github.com/juhee34/mp-project 에 팀원 커밋이 올라와 있다. 이번 B 변경(Wrapper·스크립트 수정·컴파일 수정)은 아직 커밋하지 않았다.

## 미완료
러닝 엔진/충돌/점수/저장/음악 재생/정식 아트/배경 6종/테마 6곡/완성 에피소드.
현재 화면은 개발 시작용이며 플레이 가능한 게임이 아니다.

## 과제 공식 요건 (교수 안내 PDF, 2026-11-09 확인)
- 마감: 보고서+코드 2026-11-23(월) 23:59 KLAS, 발표 11-24(화)/11-26(목) 10분+질의 5분(실제 시연, 예산 집행 계획 필수).
- 기술 최소 요건: 화면 5~8개 이상과 데이터 전달 / 회원가입·로그인(BaaS 인증) / 원격(BaaS) 또는 로컬 저장 / 외부 API 1개 이상 / 수업 개발 환경.
- 주제 틀: "1인당 100만 원으로 창업하기"(2인 팀 가상 예산 200만 원, 출시 준비+3개월 운영, 공식 가격·출처·조사일). 실제 과금 금지.
- 현재 충족: 화면 9개(로그인, 회원가입, 소개, 선택, 프로필, 게임, 결과, 내 기록, 설정), Intent로 characterId·RunResult 전달, Firebase Auth/Firestore 코드, 기기 저장, Open-Meteo 외부 API.
- 남은 요건 작업: Firebase 프로젝트 생성과 google-services.json 배치(사용자), 실제 로그인·Firestore 저장 시연 확인, 보고서·예산표·발표(A와 사용자).

## 다음 작업
1. 사용자: docs/FIREBASE_SETUP.md대로 Firebase 프로젝트를 만들고 app/google-services.json을 넣는다. 그 뒤 B가 실제 회원가입·로그인·Firestore 저장을 확인한다.
2. A/사용자: 보고서(서비스 개요·타깃·경쟁 앱, 기능·화면, 구조·BaaS·외부 API, 예산표)와 발표 준비.
3. 4단계 B: 스프라이트 규격 확정 후 CharacterPortrait 자리를 애니메이션 스프라이트로 교체한다.

## 과제 요건 대응: 로그인·저장·외부 API·화면 (2026-11-09, B)
- 새 화면: auth/LoginActivity, auth/SignupActivity, result/ResultActivity, records/RecordsActivity, settings/SettingsActivity. 공통 UI는 ui/Page.kt. MainActivity는 세션이 없으면 로그인으로 보낸다. activity_main.xml 위에 내 기록·설정 메뉴를 추가했다(CharacterScreens는 변경 없음).
- 로그인: Firebase Authentication 이메일/비밀번호, 닉네임은 displayName. google-services.json이 없으면 플러그인이 적용되지 않고 로그인 버튼이 꺼지며 개발용 임시 모드(기기 저장)만 열린다.
- 저장: ProgressStore. 로그인 시 Firestore 트랜잭션, 임시 모드는 SharedPreferences. runId 중복 방지, 손상된 기기 데이터는 새로 시작.
- 외부 API: Open-Meteo forecast(API 키·과금 없음, CC BY 4.0 출처 표기). 설정에서 도시 7곳 중 선택·켜기/끄기. 게임 하늘 색과 비·눈·안개 효과만 바뀌고 물리는 그대로다.
- 게임 종료 0.9초 뒤 결과 화면으로 이동하고 GameActivity는 닫힌다. 결과 화면에서 다시 달리기·내 기록·친구 선택.
- 의존성: Firebase BoM 33.5.1(auth, firestore), google-services 4.4.2, 테스트용 org.json. lint는 BoM 35.1.0이 있다고 알리지만 Kotlin 2.0.21/AGP 8.7.3 고정 조합을 위해 올리지 않았다.
- 검증: assembleDebug 성공, 단위 테스트 24개 통과(RunnerEngine 12, ProgressBook 7, Weather 3, AuthInput 2), lint 오류 0. 에뮬레이터(Claude가 adb로 조작, Firebase 미설정 상태): 로그인 화면과 미설정 안내 → 임시 모드 → 라일 한 판 → 결과 저장("새 최고 점수", 1번 28m) → 결과 화면 2회 회전 후에도 1번 유지 → 내 기록 6명 표시 → 앱 강제 종료·재실행 후 기록 유지 → 설정에서 제주 선택 시 "제주 · 맑음 12°C" 표시 → 게임 HUD "서울 · 맑음 5°C". 크래시 0.
- 미검증: 실제 Firebase 회원가입·로그인·Firestore 저장(설정 파일 없음), 비·눈 효과 실제 날씨에서의 모습, 오프라인에서 로그인 사용자의 저장 실패 안내.

## 3단계 B 공통 러너 (2026-11-09)
- 파일: game/RunnerEngine.kt(순수 Kotlin 상태·물리), game/RunnerView.kt(Canvas 그리기·터치), game/GameActivity.kt(연결용 안내 화면을 러너로 교체), res/values/game_strings.xml, app/src/test/.../RunnerEngineTest.kt. build.gradle.kts에 testImplementation junit 4.13.2 추가.
- 흐름: 준비(탭하면 출발) → 달리기(탭 점프, 오른쪽 위 일시정지) → 충돌 시 결과(0.6초 뒤 탭하면 새 판, 뒤로가기는 선택 화면). 앱이 백그라운드로 가면 자동 일시정지. GameActivity는 가로 고정.
- 6명 동일 물리: RunnerEngine은 캐릭터 입력을 받지 않는다. 캐릭터는 그림 색상과 이름 표시에만 쓰인다. ID 계약(EXTRA_CHARACTER_ID, 잘못된 ID는 finish)은 그대로다.
- 수치(월드 단위): 중력 2400, 점프 속도 820(최고 140, 체공 0.68초), 속도 260→520(초당 +6), 장애물 높이 30~70·너비 24~44, 간격 ≥ 속도×(체공+0.35초)+44. 1/120초 고정 스텝, 프레임 최대 1/20초.
- 테스트로 확인: 점프 최고점·착지, 이중 점프 불가, 최고 속도에서도 가장 큰 장애물 통과 가능, 장애물 간격 ≥ 착지 후 재점프 거리, 자동 플레이 봇이 시드 20개 모두 180초 생존, 충돌 시 종료, 재시작 지연과 상태 초기화, 일시정지, 프레임 지연 제한, 프레임률 무관, 같은 시드 같은 코스.
- 에뮬레이터(Medium_Phone, Claude가 adb로 조작): 치즈로 준비·달리기·충돌 결과·새 판·일시정지(2초간 화면 동일)·뒤로가기 후 선택 화면 복귀 확인, 앱 크래시 로그 없음. 이후 사용자가 6명 전원으로 러너 실행을 확인했다(사용자 보고, 2026-11-09). 난이도 체감 의견은 아직 받지 않았다.
- 아직 없음: 기록 저장, 결과 화면 Activity, 캐릭터별 배경·음악, 스프라이트 애니메이션, 효과음. 화면 크기 변경으로 Activity가 다시 만들어지면 진행 중인 판은 사라진다.

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
