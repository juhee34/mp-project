# Tiny Runners

학교 과제로 만드는 Android 2D 픽셀 러너. **처음 플레이 가능한 시연부터 캐릭터 10명 전원 선택**을 지원한다.

## 지금 무엇이 들어 있나요
- Android Studio에서 열 프로젝트 골격: Kotlin, XML host, 소개/10명 선택/프로필 코드
- 캐릭터 10명 설정과 앱용 JSON, 이야기 개요, 배경·음악 제작 명세
- [처음 시작하기](docs/START_HERE.md)와 [Claude 첫 프롬프트](docs/FIRST_CLAUDE_PROMPT.txt)
- [두 사람용 단계별 프롬프트 20개](docs/PROMPTS.md)
- [소개서와 전체 개발 가이드](docs/DEVELOPMENT_GUIDE.html): 다운로드하여 브라우저에서 열기
- [데이터 계약](docs/DATA_CONTRACT.md), [현재 진행 상태](PROJECT_STATE.md)
- research/ 안의 이야기 연구 205편과 비교 게임 10개

## 중요한 현재 상태
**완성된 게임이 아닌 개발 시작 프로젝트다.** 러닝 엔진, 정식 배경 10종, 실제 음악 10곡은 아직 없다.
10명 프로필은 색상 표식으로 표시한다. 생성했던 4종 콘셉트 이미지는 이 저장소에 실제 이미지 파일로 포함되지 않았으므로 아트 작업 시 사용자가 승인한 시안을 함께 제공해야 한다.
Android 빌드/실행은 현재 환경에서 검증하지 못했다. 유효한 SDK가 없고 외부 다운로드가 실패했다. Wrapper JAR도 아직 없으므로 먼저 [환경 준비](docs/START_HERE.md)를 따른다.

## 가장 빠른 시작
1. 이 폴더를 Android Studio에서 Open한다.
2. JDK 17과 SDK 35를 준비한다.
3. PowerShell에서 scripts/bootstrap-gradle.ps1을 실행하여 Gradle Wrapper를 준비한다.
4. Gradle Sync와 app 실행을 확인한다.
5. 프로젝트 Terminal에서 claude를 실행하고 docs/FIRST_CLAUDE_PROMPT.txt를 붙인다.

## 개발 순서
공동 규격 → 10명 데이터/골격 → 10명 선택 및 러닝 → 캐릭터 애니메이션 → 고향 배경 10종 → 음악 10곡 → 이야기/기록 → 소개/안내 → 테스트 → 과제 제출.

## 내용 검사
Node.js가 있으면 node scripts/validate-content.cjs를 실행한다. 이것은 데이터 검증이며 Android 빌드 검증을 대신하지 않는다.

## 역할
A(ChatGPT/Codex): 콘텐츠·이미지·소개/선택/도감·교차 검수.
B(Claude Code): 프로젝트·게임·오디오·저장·통합·실기기 테스트.

## 조사 자료
원문 또는 실질 시놉시스 검토이며 205편 전편 완독/시청은 아니다. 기존 작품 원문이나 이미지·음원은 재배포하지 않고 출처와 짧은 연구 메모만 포함한다. 캐릭터와 이야기는 아직 제안 초안이다.

## 저장소

https://github.com/juhee34/mp-project

기존 저장소 소개: mp-progect
