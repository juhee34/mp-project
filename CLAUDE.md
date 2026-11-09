# Tiny Runners 프로젝트 지침

반드시 README.md, PROJECT_STATE.md, docs/START_HERE.md, docs/DATA_CONTRACT.md를 먼저 읽는다.

## 확정 요구
- 학교 과제용 Android Studio 앱. 스토어 출시는 범위 밖이다.
- 첫 플레이 가능한 시연부터 캐릭터 10명을 모두 선택할 수 있어야 한다. 1명으로 줄이지 않는다.
- 캐릭터별 고향·성격·서사·배경·음악. 물리 성능은 동일하다.
- 기존 민트/치즈/모찌/볼트의 따뜻한 픽셀 디자인을 유지한다. 나머지 6종은 기획 후보다.
- 현실의 입시/취업/빚/질환 소재와 출석 압박을 피한다. 관계와 생활의 판타지로 위로를 표현한다.

## 현재 상태
앱 골격, 10명 데이터, 소개/선택/프로필 연결 코드를 준비했다. Android 빌드·실행은 검증되지 않았다.
러닝 엔진, 정식 캐릭터 그림, 배경 10종, 실제 음원 10곡은 미완료다. 색상 표식과 서사 개요를 완성 아트/완성 이야기로 소개하지 않는다.

## 작업 규칙
- Kotlin + XML + Canvas는 임시 과제 기준이다. 교수자 요구가 오면 우선한다.
- 화면 코드가 현재 MainActivity에 모인 것은 골격이다. 첫 빌드 이후 화면/게임/오디오/저장 책임을 분리한다.
- 처음에는 환경 확인과 빌드부터 수행한다. 다음은 10명 선택 상태를 유지한 러너 구현이다.
- docs/PROMPTS.md에 역할별 10단계 프롬프트가 있다. 요청된 단계만 진행하고 결과를 보여준다.
- A는 콘텐츠/소개/선택/도감, B는 설정/게임/오디오/저장/통합을 소유한다. 사용자가 전부 맡기면 범위를 조정할 수 있다.
- 실제 파일과 실행 로그를 근거로 보고한다. 실행하지 않은 테스트를 통과했다고 하지 않는다.
- generated image/음원 제작이 불가능하면 명세와 실제 자산을 구분한다.
- 공통 모델 변경은 docs/DATA_CONTRACT.md와 검증 도구를 함께 갱신한다.
- 비밀키, local.properties, 서명키, 개인 경로를 커밋하지 않는다.
- 종료 시 PROJECT_STATE.md에 변경/검증/미완료/다음 작업을 기록한다.

## 확인 명령
내용 검사: node scripts/validate-content.cjs
Windows 준비: powershell -File scripts/bootstrap-gradle.ps1
빌드: .\gradlew.bat :app:assembleDebug
테스트: .\gradlew.bat :app:testDebugUnitTest :app:lintDebug

GitHub 저장소 생성·공개 범위 변경·스토어 배포는 개별 사용자 지시가 있을 때만 한다.
