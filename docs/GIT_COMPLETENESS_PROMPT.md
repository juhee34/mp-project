# 코덱스 전달용 — 전체 제작물 Git 업로드 누락 점검

아래 프롬프트를 현재 mp-project 작업 환경의 코덱스에 전달한다.

```text
mp-project에서 지금까지 ChatGPT와 함께 만든 모든 프로젝트 제작물이 Git 원격에 빠짐없이 반영됐는지 감사해줘. 최근 대사나 소개 화면만 확인하지 말고, 초기 기획·연구·아트부터 이야기·도감·음악 관련 자료·웹사이트까지 전체 범위를 확인해야 한다.

프로젝트 경로:
<project-root>

이번 요청은 우선 점검과 증거 있는 보고다. 임의로 전체 파일을 stage/commit/push하거나 main에 병합하지 마. 누락분을 찾으면 업로드할 정확한 파일과 변경 구간을 정리하고, 바로 이어서 실행할 수 있는 게시 계획을 작성해줘. 기존에 별도로 승인되어 진행 중인 업로드와 충돌하지 않게 해줘.

1. 지침과 현재 상태
- AGENTS.md, CLAUDE.md, README.md, PROJECT_STATE.md, docs/START_HERE.md, docs/DATA_CONTRACT.md와 관련 HANDOFF/REVIEW 문서를 읽어.
- 현재 브랜치·HEAD·원격 URL·upstream·staged/unstaged/untracked 상태를 확인해. 파일 생성과 앱 연결, 로컬 커밋과 원격 게시를 각각 구분해.
- 다른 작업자가 변경 중인 파일은 최초/최종 상태와 해시를 비교해. 검사 중 바뀐 파일을 임의로 덮어쓰거나 확정 판정하지 마.

2. 실제 제작물 목록 만들기
아래는 최소 점검 범위다. 목록에 없는 파일도 문서·manifest·스크립트·JSON 참조로 발견해 포함해.

A. 기획·연구·캐릭터
- 프로젝트 기획서, 개발 가이드, 역할별 프롬프트, 캐릭터 가이드/소개/바이블, 데이터 계약
- research/의 205개 이야기 연구 기록과 비교 자료
- 캐릭터 6명 데이터 및 참조 ID, 관련 검증 스크립트

B. 스프라이트·배경·장애물
- docs/art/source, SPRITE_HANDOFF, frame-manifest, 검수 자료
- app의 sprite_<id>_* PNG: 6명 각 필수 idle2/run4/jump2/fail1, 추가 프레임은 실제 파일 기준 확인
- 승인된 배경 원본, app/assets/backgrounds와 manifest, BACKGROUND_HANDOFF
- background-connectors 원본/부분 보정본, 접합 검수 문서·비교판·manifest
- obstacle-concepts 3쌍 시트, 최종 obstacles/png 18개, source/manifest/export.csv/검수판/OBSTACLE_HANDOFF
- loopReady=false 또는 integrated=false인 자료는 게시됐더라도 기능 완료로 바꾸지 마.

C. 이야기·도감
- docs/STORY_MANUSCRIPT_01.md, 02.md, 03.md, STORY_HANDOFF.md
- stories.json 24개: 배경 6편+첫 에피소드 6편 ready, 후속 개요 12편 outline
- characters.json storyIds, build-story-content.py, 관련 콘텐츠 검증기
- stories/Story.kt, StoryStore.kt, StoryActivity.kt와 단위 테스트
- AndroidManifest, story_strings.xml, CharacterScreens 도감 진입 변경
- 후속 본문은 제작 보류이므로 개요만 있는 것을 누락이라고 판정하지 마.

D. 짧은 대사
- docs/dialogue/SHORT_LINES.md, short-lines.json, HANDOFF.md
- 6명×8상황×3개=144개, 앱 미연결 상태를 보존

E. 앱 소개·발표 소개서
- CharacterScreens.showIntro 변경
- character_screen_strings.xml 소개 문구, intro_strings.xml
- docs/PROJECT_INTRO.md, INTRO_SCREEN_HANDOFF.md

F. 음악 관련 자료
- docs/audio/의 REVIEW·metadata·측정 결과·피코 시험본·6곡 수정 자료·묶음 ZIP 등 실제 존재하는 파일
- 관련 inspect/build 스크립트와 원본 참조
- 다른 대화에서 작업 중인 원본·시험본·최종본을 구분하고 최신 담당 문서로 판정
- 원본 음악이 다운로드 폴더/ChatGPT 첨부에만 있고 저장소에서 참조한다면 ‘외부에만 존재/미확보’로 표시. 없는 파일을 생성하거나 최종 승인됐다고 추정하지 마.
- 대용량 WAV/MP4/ZIP은 자동 업로드하지 말고 크기·중복·배포 필요성·Git LFS 또는 별도 릴리스 보관 여부를 권고해. .gitignore에 있다는 이유만으로 보존할 필요 없는 파일이라고 단정하지 마.

G. 이번 별도 웹사이트
- website/dist/index.html, styles.css, app.js, data.js
- website/dist/assets의 6개 배경 발췌와 6개 스프라이트
- website/dist/TINY_RUNNERS_INTRO.md
- website/build-content.py, build-content.ps1, README.md
- website/.openai/hosting.json: 사이트 식별자이며 비밀 토큰을 포함하지 않아야 함
- 사이트의 별도 배포 저장소에 게시한 것과 mp-project GitHub에 반영한 것은 별개다. website/가 본 프로젝트 원격에도 존재하는지 확인해.
- 로컬 임시 배포 checkout, 인증 토큰, 배포 tar, preview 서버 로그는 mp-project 업로드 대상이 아니다.

H. 통합 기록
- PROJECT_STATE, DATA_CONTRACT, 각 납품/검수 문서, 빌드·내보내기·검증 스크립트
- 루트 README의 오래된 기능 미구현 설명과 최신 코드의 차이도 별도 표시하되 이번 감사 중 임의로 대규모 수정하지 마.

3. Git 증거 확인
- git status가 깨끗하거나 push 성공 로그가 있다는 이유만으로 ‘전부 업로드’라고 판정하지 마.
- git ls-files, git check-ignore -v, git diff, git log, git ls-tree, git cat-file 등을 사용해 각 파일의 추적·커밋 상태를 확인해.
- 원격 정보를 새로 fetch한 뒤 현재 upstream과 원격 main을 읽기 전용으로 비교해. fetch가 실패하면 원격 최신 상태 미확인이라고 명시하고 캐시만으로 단정하지 마.
- 주요 텍스트는 현재 작업본과 원격 blob 내용을 비교하고, 아트/음원은 Git blob ID 또는 SHA-256을 대조해. 파일명만 존재하는 낡은 버전은 ‘최신본 미반영’이다.
- Git LFS 사용 시 포인터만 게시되고 실제 객체가 빠지지 않았는지도 확인해. submodule·중첩 저장소 경계 때문에 본 프로젝트에 파일이 포함되지 않는 경우도 구별해.
- 현재 작업 브랜치 원격 반영과 main 반영은 별도 열로 표시해. main에 없더라도 작업 브랜치에 정확히 게시된 것을 미업로드라고 혼동하지 마. main 미병합 상태는 따로 기록해.
- 같은 내용을 경로 변경으로 이미 게시했다면 실제 대체 경로와 근거를 제시하고 중복 업로드를 권하지 마.

4. 필요한 검사
- node scripts/validate-content.cjs
- 존재하는 스프라이트/배경/장애물 검증 스크립트
- 대사 144개 구성·ID·중복·원고/JSON 일치
- 웹사이트 JS 구문, 참조 자산 존재, 12개 ready 본문만 노출되는지, 발표 소개서 복사본 최신성
- git diff --check
- 앱 코드 변경이 누락되었거나 기존 빌드 증거가 현재 HEAD에 해당하지 않을 때 Android 빌드·관련 테스트 수행
- 감사 목적으로 생성 스크립트를 실행해 작업본을 덮어쓰지 마. 재생성 비교가 필요하면 임시 사본에서 수행해.

5. 보존·업로드 범위
- 다른 팀원의 staged 변경을 해제하거나 reset/clean/강제 push하지 마.
- git add . 사용 금지. 자격증명·개인 계정 데이터·서명키·local.properties·캐시·.tools/.kotlin·빌드 산출물은 일반 소스 커밋에서 제외해.
- 원본과 검수판을 불필요하다고 임의 삭제하지 마. 사용자 제작 자료가 Git에 부적합하면 대체 보관 위치와 재현 가능 여부를 기록해.
- 웹사이트 공개 범위, 저장소 공개 설정, 음악 승인 상태, 후속 이야기 제작 상태를 변경하지 마.

6. 결과물
docs/GIT_UPLOAD_AUDIT.md를 작성해줘. 다음 열을 가진 표를 포함해:
분류 / 로컬 경로 / 실제 제작 상태 / 로컬 Git 상태 / 원격 작업 브랜치 최신본 일치 / main 반영 여부 / 근거 커밋 또는 해시 / 누락·불일치 / 다음 조치.

최종 답변에는 아래를 명확하게 정리해:
- 최신 원격까지 확인한 범위와 확인 시각
- 게시 완료·로컬만 존재·로컬 커밋만 됨·최신본 미반영·의도적 제외·외부 자료 미확보·검사 중 변경 항목의 수
- 아직 Git에 올려야 할 정확한 파일/변경 구간
- 이번에 자동 업로드하면 안 되는 자료와 이유
- 누락분만 안전하게 게시하는 다음 실행 계획

확인할 수 없는 자료가 하나라도 있으면 ‘빠진 것 없이 모두 업로드 완료’라고 단정하지 마. 확인된 사실과 미확인 범위를 분리해서 보고해줘.
```
