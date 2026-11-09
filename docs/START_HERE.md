# Claude Code와 Android Studio에서 시작하기

## 1 프로젝트 열기
GitHub 업로드가 완료되면 저장소의 Code 버튼에서 HTTPS 주소를 복사한다.
Android Studio의 Get from VCS로 복제하거나 터미널에서 git clone 뒤 그 폴더를 연다.
ZIP을 받은 경우 압축 해제한 mp-project 폴더를 Open한다. app 폴더가 아니라 settings.gradle.kts가 있는 최상위 폴더다.

## 2 개발 환경
학교에서 지정한 버전이 있으면 우선한다. 이 시작 프로젝트는 재현 가능한 기존 조합으로 AGP 8.7.3 / Gradle 8.9 / Kotlin 2.0.21 / JDK 17 / compileSdk 35를 고정했다. 최신 버전이라는 뜻은 아니다.
Android Studio Settings → Build, Execution, Deployment → Build Tools → Gradle에서 Gradle JDK를 17로 선택한다. 다운로드 기능이 있으면 JDK 17을 설치한다. Java 25를 이 Gradle 조합에 그대로 사용하지 않는다.
SDK Manager에서 Android SDK Platform 35, SDK Build-Tools 34.0.0, Platform-Tools를 설치한다. SDK 경로는 자신의 실제 경로를 사용한다. 존재하지 않는 ANDROID_SDK_ROOT가 설정되어 있으면 바로잡는다.
Android Studio가 local.properties에 sdk.dir를 설정하게 하고 이 파일은 Git에 올리지 않는다.

## 3 Wrapper 준비
현재 환경에서 네트워크 다운로드가 실패해 Wrapper JAR/표준 gradlew 파일은 포함되지 않았다. 인터넷 연결이 되는 PC에서 프로젝트 Terminal을 PowerShell로 열고 실행한다.

    powershell -File scripts/bootstrap-gradle.ps1

스크립트는 JAVA_HOME의 JDK 17을 요구한다. JAVA_HOME이 설정되지 않았으면 본인 PC의 JDK 17 경로로 설정한 뒤 실행한다.
스크립트가 Gradle 8.9 ZIP을 다운로드하고 공식 SHA-256과 비교한 후 .tools 안에서 압축을 풀고 표준 Wrapper를 생성한다.
다운로드 권한/프록시 문제가 있으면 우회하지 말고 정상 네트워크에서 재시도한다. 정책상 스크립트 실행이 막히면 학교/기기 정책에 맞는 방법을 사용한다.
Wrapper를 만든 뒤 gradlew, gradlew.bat, gradle/wrapper/gradle-wrapper.jar, properties를 저장소에 커밋하면 팀원은 이 준비 과정을 반복하지 않아도 된다.

## 4 빌드와 실행
Sync Project with Gradle Files를 실행한다. 첫 동기화에는 인터넷이 필요하다.
Device Manager에서 API 26 이상 에뮬레이터를 준비하거나 USB 디버깅이 켜진 실제 기기를 연결한다.
Run에서 app을 실행한다. 현재 예상 화면은 소개 → 친구 10명 → 프로필이다. 달리기 버튼은 개발 예정 안내를 표시한다.

    .\gradlew.bat :app:assembleDebug

빌드가 성공하면 app/build/outputs/apk/debug/app-debug.apk가 생성된다. 코드가 있다는 것과 빌드가 성공했다는 것은 다르므로 실행 결과를 기록한다.

## 5 Claude Code 시작
Claude Code는 공식 quickstart에 따라 설치한다. 이미 있다면 설치를 반복하지 않는다.

    claude --version
    claude

반드시 프로젝트 최상위 폴더에서 실행한다. 초기 로그인은 본인이 수행한다. 채팅에 비밀번호나 토큰을 붙이지 않는다.
docs/FIRST_CLAUDE_PROMPT.txt를 복사해 첫 요청으로 사용한다. 이후 docs/PROMPTS.md의 자기 담당 단계 프롬프트를 사용한다.
Android Studio 플러그인을 쓰면 Claude Code CLI와 JetBrains 연동을 모두 확인한다. 일반 Claude 채팅에 파일을 첨부하는 방식과 로컬 프로젝트를 직접 편집·실행하는 방식은 다르다.

## 6 개발 진행
처음에는 환경/빌드 확인 후 공통 러너를 구현한다. 처음부터 10개 캐릭터 ID를 지원하고 소재가 없는 친구는 임시 그림을 쓴다.
그 다음 아트 → 배경 → 음악 → 이야기/저장 → 소개/안내 → 통합 검수 순서로 진행한다.
2명씩 소재를 완성해도 선택 가능한 친구 수는 10명으로 유지한다.
한 번에 앱을 통째로 다시 만들라는 지시보다 현재 단계/담당 파일/완료 기준을 주는 방식으로 작업한다.

## 7 두 사람의 협업
각자 브랜치에서 작업하고 기능 하나씩 커밋한다. 콘텐츠와 선택/도감 화면은 A, 게임/오디오/저장/설정은 B가 맡는다.
공동 모델 변경과 MainActivity 분리는 함께 합의한다. 브랜치를 통합한 뒤 다시 빌드한다.
막힌 오류는 정확한 메시지와 재현 순서를 공유하고 다른 AI가 코드를 검토하게 한다.

## 8 과제 제출
수업 요구를 우선한다. 기본 후보는 프로젝트 원본, debug APK, 실행 README, 소개서, 시연 대본이다.
실제 구현 기능, AI 활용과 본인 검토, 팀원별 기여를 사실대로 설명한다. 앱스토어 등록은 하지 않는다.

## 공식 참고
- Android 게임 개발: https://developer.android.com/games/guides/basics
- AGP 8.7 호환성: https://developer.android.com/build/releases/agp-8-7-0-release-notes
- Claude Code 시작: https://code.claude.com/docs/en/quickstart
- Android Studio 연동: https://code.claude.com/docs/en/jetbrains
