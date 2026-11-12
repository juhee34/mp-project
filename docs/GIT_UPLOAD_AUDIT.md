# 전체 제작물 Git 업로드 감사

기준: 2026-11-10, 한국 시간. 이 문서는 게시 누락 감사 결과이며 업로드 완료 선언이 아니다. 이번 감사에서는 stage/commit/push/merge를 실행하지 않았다. 기존 사용자 작업과 staged 상태를 보존했다.

## 1. 결론과 검사 시점

- 최초 파일 스냅샷: 22:32:18 KST. 마지막 원격 fetch로 `main=bdffa289c44d792113e8edbbadb45bf1e93679ee`까지 확보했다. 파일 내용/확보한 원격 트리 최종 비교: 22:48:02 KST. 원격은 계속 바뀔 수 있으므로 이 커밋까지의 감사다.
- 작업 브랜치/upstream: `codex/character-screens` / `origin/codex/character-screens`. 로컬 HEAD와 원격은 모두 `ba5a2ec254d905cbad9c3ace0510bb224c57c73a`로 같다.
- 저장소: https://github.com/juhee34/mp-project.git . 처음과 마지막 staged 변경 0개. 기존 unstaged 파일 3개와 미추적 제작물은 그대로 보존했다.
- **전체 279개 제작·소스 파일 중 같은 경로의 최신 내용이 작업 브랜치와 일치하는 파일 224개, 로컬만 존재하는 경로 52개, 기존 경로의 최신 수정이 미게시인 파일 3개. 로컬 커밋만 된 변경은 0개.**
- `main`은 같은 경로/내용 200개, 내용 차이 27개, 경로 없음 52개다. 27개를 모두 낡은 파일로 판정하지 않는다. 대부분 B의 후속 구현이며 아래에서 별도로 설명한다.
- 감사 중 `main`이 `89ef1ad` → `f315c37` → `bdffa28`로 갱신됐다. **A 도감은 f315c37에서 실제 병합됐다.** 최종 기준에서 이야기 24개 JSON, 원고, A의 `stories/` 패키지/테스트는 원격 main과 같다. 최초의 “도감 main 미반영” 판정은 더 이상 유효하지 않다.
- `website/` 21개 경로, 음악 관련 27개 경로, 앱 소개의 신규 3개 파일은 로컬에만 있다. 감사 요청서 1개도 미게시다. 여기에 소개 코드/문자열/공유 상태 문서의 최신 수정 3개가 더해진다.

집계는 파일 경로 단위이며 디렉터리·후속편의 미제작 본문·채팅 첨부 개수는 섞지 않았다. 아래 외부 자료와 캐시 제외 항목은 279개 밖의 별도 집계다. 이 감사 보고서 자체도 새 로컬 파일로서 279개 집계에서 제외했고 아직 Git에 게시하지 않았다.

| 판정 | 수 | 범위/해석 |
|---|---:|---|
| 작업 브랜치 게시 완료 | 224개 | 원격 blob과 로컬 Git 정규화 blob 동일 |
| 로컬만 존재하는 경로 | 52개 | 이 중 사이트 스프라이트 복사본 6개는 앱 경로에 동일 바이트가 이미 게시됨. 완전히 새로운 아트 누락으로 세지 않음 |
| 로컬 커밋만 됨 | 0개 | HEAD와 upstream 동일 |
| 최신본 미반영 | 3개 | PROJECT_STATE, CharacterScreens, character_screen_strings |
| 검사 중 기존 제작물 내용 변경 | 0개 | 279개 최초/최종 SHA-256 동일, 기존 경로 추가/삭제 없음 |
| 의도적 제외 | .kotlin 7개 파일 + 7개 경로/유형군 | 캐시/개인 환경 내부 파일 수는 집계하지 않음. 아래 범위 명시 |
| 원본 음악 외부에만 존재 | 12개 | Downloads MP4 6개 + MP3 6개, 기록 SHA-256과 모두 일치, 저장소에는 없음 |
| 이름이 알려졌으나 미확보 | 2개 | Puddle_Jumpers(1).mp3, Tiny_Runners_Android_Guide.html: 프로젝트와 Downloads에서 확보하지 못함. 다른 저장 위치/첨부 동일성 미확인 |
| 정확한 개수·파일명이 없는 외부 참조 | 2묶음 | 초기 콘셉트/첨부 시안들, 교수 안내 PDF. 전수 확보·게시 여부 미확인 |

## 2. 조사 방법과 증거 범위

AGENTS, CLAUDE, README, PROJECT_STATE, START_HERE, DATA_CONTRACT, GIT_COMPLETENESS_PROMPT 및 아트/이야기/대사/소개/음악/사이트 인계·검수 문서를 대조했다. `git status`, `diff --cached`, `ls-files`, `check-ignore`, `log`, `ls-tree`, `cat-file`과 실제 파일 바이트를 사용했다. 텍스트는 `.gitattributes`를 적용하는 `git hash-object --stdin-paths`로 줄바꿈 차이를 정규화하고, 최초/최종 보존 확인과 음원/manifest는 SHA-256으로 비교했다.

정상 fetch 성공 후 네 원격 브랜치의 실제 트리를 확보했다. 동일 내용의 대체 경로도 검색했다. `main`의 최초 목록 재조회는 연결 오류였으나 이후 정상 권한의 원격 조회/fetch로 갱신했다. 캐시만으로 최신이라고 단정하지 않았다.

- `origin/codex/character-screens`: `ba5a2ec254d905cbad9c3ace0510bb224c57c73a`
- `origin/docs/team-workflow`: `fef0ca7e6187e3b4d64b34d73848c661e97b858b`
- `origin/feature/jump-skill-story`: `eb7df1b0b869ab60cd4217861cf4f91ae3381f26`
- `origin/main`: `bdffa289c44d792113e8edbbadb45bf1e93679ee`

검사한 원격 트리에 gitlink/submodule 0개, LFS 포인터 0개. 제작물 디렉터리(app/src, docs, research, scripts, website, sources, .openai)에 중첩 `.git` 없음. 따라서 이번 파일 누락은 LFS 객체나 중첩 저장소 문제로 설명되지 않는다. `.tools` 내부 임시 checkout은 제작 소스 범위 밖이며 인증정보를 출력/복사하지 않았다. `sources/previous-chat/`와 루트 `.openai/`는 현재 비어 있으며 Git은 빈 디렉터리를 저장하지 않는다.

## 3. 제작 상태와 main 통합 상태

- **기획·연구·캐릭터:** 연구 JSON 6개에 이야기 연구 205건과 경쟁작 10건, 역할 프롬프트 20개, 6명 ID/캐릭터 설정과 관련 기획서가 게시돼 있다. ID `mint/cheese/popo/lumi/nunu/piko` 유지. 옛 채팅 시안과 교수 PDF까지 저장소에 있다고 주장하지 않는다.
- **스프라이트:** 승인 원본 3장, 프레임 manifest/검수판/내보내기, 앱 PNG 72개 모두 작업 브랜치와 main에 동일하게 게시됐다. 각 캐릭터 필수 9장(idle2/run4/jump2/fail1) + greet/land/home 각 1장. 선택 인사/귀가를 완성 2프레임 애니메이션으로 보고하지 않는다. 출처 커밋 55d9180, 게임 연결 d165e41.
- **배경:** 승인 원본 3장과 앱 복사본, 6개 영역 좌표/바닥선/검수 도구 게시 완료(1429fe0). 단일 파노라마 끝 clamp이며 `loopReady=false`, `layered=false` 유지.
- **연결 배경:** 부분 보정 PNG 3장/manifest/문서/HTML/접합 비교판 모두 게시 완료(5c254b6). `loopReady=false`; 비교판은 보정 전, 최신 좌표는 재검증 전이다. 파일 업로드와 무봉제 완성은 별개다.
- **장애물:** 콘셉트 3쌍 시트, 최종 18 PNG, source/manifest/export.csv/검수판/인계/검사기 모두 게시 완료(f0d8bb0). A 납품 manifest의 `integrated=false`는 그대로다. **최신 main에서는 B가 b41f61d로 실제 앱에 18개를 복사하고 ObstacleArt에 연결했다.** docs PNG와 앱 복사본의 Git blob 동일성을 확인했다. 이를 “아트 납품 미완료”로 오해하면 안 된다. 7844ddb에는 B의 라일/치즈 기기 검수 기록이 있으나 나머지 4명 전체 검수는 아니다. 이 감사가 직접 플레이한 결과도 아니다.
- **이야기·도감:** ba5a2ec 게시, f315c37 main 병합. A `stories.json` 24개(ready12/outline12)와 A `stories/` 패키지는 동일. B의 기존 `story/` 기능은 `village_stories.json`으로 분리됐다. 두 기능의 저장소/화면 계약을 덮어쓰지 않는다. 후속편 12개의 개요는 의도된 제작 상태이므로 본문 누락으로 처리하지 않는다. B 기록상 도감 진입은 확인했으나 STORY_HANDOFF의 상세 기기 검수는 여전히 남아 있다.
- **144개 대사:** b439067으로 작업 브랜치/main 모두 동일 게시. `content_ready_for_review`, `integrated=false`. 최신 main의 별도 `talks.json`(세계 이야기 6쪽+각 6줄 출발 대화)은 B의 새 자료다. 이 구현을 A의 144개 팩 연결 완료로 세지 않는다.
- **앱 소개·발표:** 현재 로컬 showIntro 개편/소개 리소스/PROJECT_INTRO/INTRO_SCREEN_HANDOFF는 미게시. main은 세계 프롤로그→선택→출발 전 대화를 추가했으므로, 발표서의 기존 흐름·착지 후 점프 설명을 최신 main의 2단 점프/대화 흐름과 다시 대조해야 한다. 자동 문안 수정은 하지 않았다.
- **음악:** 전부 기술 검토용 자료와 시험본이다. 캐릭터 매핑/음악적 루프/청음 승인/게임 연결 미완료. 메타데이터 확인과 파형 수치 검사를 음악 완성으로 표시하지 않는다.

### 이름은 있지만 최신 상황을 전달하지 못하는 문서

README.md/CLAUDE.md/START_HERE.md는 로컬과 원격이 같지만 “러너/배경/실제 그림 없음”, “Wrapper 없음”, “빌드 미검증”, “달리기 예정” 등 옛 상태가 남아 있다. 최신 제작물의 Git 누락이 아니라 문서 갱신 누락이다. PROJECT_STATE의 옛 단계 표와 상단 “이야기 1차 원고”도 최신 24편 도감과 혼동을 준다. 후속 갱신은 담당자가 최신 기록으로 정리하되 이번 감사에서는 원문을 고치지 않았다.

OBSTACLE_HANDOFF의 B 연결 예정 문구와 integrated=false도 A 납품 당시 상태다. 최신 main의 B 연결/검수 기록을 별도 후속 절로 안내하는 것이 안전하다. 연결 배경의 미완성 상태는 그대로 보존해야 한다.

## 4. 사이트 배포와 GitHub 게시를 분리한 결과

- Sites의 get_site / get_deployment_status / get_site_version을 읽기 전용으로 확인했다. 사이트 active, 배포 succeeded, 버전 1.
- URL: https://tiny-runners-friends-20261110.auroraduck1.chatgpt.site
- 사이트 ID: `appgprj_6af3152718e48191a65b4ac23c1789f8`; 배포 ID: `appgdep_6af318305888819199221f7ee6b9b453`.
- 서비스가 반환한 소스 커밋: `341c70cd36c36ebb0e64828540f1cb93384e20fd`. 배포 아카이브: 18파일, 10,690,560바이트, SHA-256 `f3ef01920fe5decb059d17d75f14ddfeb9a3a546e06fca234724ef1e189c126d`.
- 접근 설정은 소유자 1명, 외부 방문자 0명이며 공개 범위를 변경하지 않았다. 팀원에게 URL만 전달하면 바로 열 수 있다고 보장하지 않는다.
- **mp-project 원격 브랜치에는 website/ 경로가 전혀 없다.** 사이트 배포 커밋은 mp-project 커밋이 아니다.
- 서비스 성공/소스 provenance는 확인했지만 배포 아카이브를 내려받아 로컬 21파일과 전수 비교하지 않았고 로그인 이후 운영 화면도 이번에 재검수하지 않았다. “현재 로컬 전체와 배포본이 바이트까지 동일”이라고 단정하지 않는다.
- hosting.json은 project_id와 static.directory만 있으며 토큰 필드가 없다. 배포 임시 checkout/인증정보/tar/log는 게시 대상이 아니다.
- **main의 .gitignore에 `dist/`가 있어 website/dist/도 새로 추적할 때 무시된다.** 현재 A 브랜치에는 이 규칙이 없어 17개 dist 파일이 미추적으로 보인다. 향후 통합에서 루트 배포 산출물만 제외하는 `/dist/` 또는 website 예외를 좁게 합의해야 한다. 무조건 force-add하거나 .gitignore 전체를 덮어쓰지 않는다.

## 5. 이번에 수행한 검사 / 수행하지 못한 검사

| 검사 | 결과 |
|---|---|
| node scripts/validate-content.cjs | 통과: 6명, 205연구, 20프롬프트, 필수 프레임 54개, 배경6영역·바닥선·원본해시 |
| python scripts/validate-obstacle-art.py | 통과: 18 PNG 전 픽셀/그림영역/기준점/manifest/SHA-256 |
| 스프라이트 PowerShell 검사기 | 실행 정책 차단, 권한을 달리한 동일 재시도도 차단. 실행 성공으로 기록하지 않음 |
| 별도 읽기 전용 Python 스프라이트 검사 | 통과: 72개, 48×48 RGBA, 알파0/255, 캐릭터별16색 이하, 마지막 불투명 y45 |
| 연결 배경 manifest | 6항목이 참조하는 3개 PNG SHA-256 일치, loopReady=false. 접합 시각 합격 검사가 아님 |
| 대사 검사 | 6×8×3=144, ID/문구 유일, 최대25 Unicode 코드포인트≤48, Markdown/JSON 일치, 읽음 조건 전부 빈 배열, 미연결 상태 통과 |
| 이야기 재생성 | 임시 사본에서 build-story-content.py 실행, stories/characters JSON 의미상 일치. 사용자 작업본을 재생성하지 않음 |
| 웹 JS | node --check app.js 및 data.js 통과 |
| 웹 데이터/참조 | 6명·ready12편 내용/페이지가 앱과 정확히 동일, outline 없음. HTML 로컬 참조/12자산 존재, 스프라이트6개 앱 원본과 바이트 동일, 소개서 사본과 원고 SHA-256 동일 |
| 웹 배경 크롭 픽셀 전수 대조 | Pillow 미설치로 미실행. 자산 존재/참조 검사는 수행. 새 시각 검수는 안 함 |
| 음원 시험 파일 | WAV14개 측정 JSON의 SHA-256 일치, ZIP2개 CRC 통과(4/14항목), ZIP 내부 파일은 로컬 전달물과 모두 동일 |
| git diff --check | 통과. CRLF 변환 안내는 있었으나 whitespace 오류 없음 |
| 현재 로컬 Android assembleDebug/testDebugUnitTest/lintDebug | JDK17/SDK35로 129파일의 격리 사본을 준비해 시도했으나 Could not connect to the Gradle daemon으로 실패. 정상 권한 재시도는 사용자 승인 거절로 실행되지 않음. 이번 빌드/단위테스트/Lint 통과 아님 |
| 과거 빌드 증거 | ba5a2ec 게시용 사본의 48테스트·Lint24경고 기록은 보존. 현재 소개 개편까지 검증했다는 증거로 사용하지 않음. main의 67/70테스트는 B의 커밋 기록이며 이번 직접 실행 결과가 아님 |
| 실제 기기/UI·청음 | 이번 감사에서 수행하지 않음. 기존 사용자 기록 변경 없음 |

검사 보조 파일·원시 인벤토리·빌드 실패 로그는 무시되는 .tools 아래에만 저장했다. import로 잠시 생긴 scripts/__pycache__의 검사 캐시는 감사가 생성한 정확한 파일만 제거했다. 기존 제작물의 최초/최종 해시는 모두 같다.

## 6. 아직 게시할 정확한 범위와 안전한 계획

다음은 실행 계획이며 이번에 실행하지 않았다. 각 묶음 직전에 git status/staged/원격 HEAD를 다시 확인한다. 관련 없는 staged가 있다면 그 상태를 해제하지 않고 별도 index 또는 격리 checkout으로 묶음을 분리한다. 명시 파일/구간만 stage하고 cached diff를 검토한 뒤 정상 push한다. 원격이 앞서면 중단 후 조정하고 강제 push하지 않는다. main 병합은 별도 담당 작업이다.

1. **소개 묶음:** CharacterScreens.kt의 showIntro 변경만, character_screen_strings.xml의 characters_intro_body 변경만, intro_strings.xml, docs/PROJECT_INTRO.md, docs/INTRO_SCREEN_HANDOFF.md. PROJECT_STATE의 ‘2026-11-10 A 앱 소개 화면 및 학교 발표 소개서’ 절만 분리한다. 새로 확인한 main 흐름과 문구 대조 및 실제 빌드 성공 후 게시한다. 이미 게시된 도감 진입 코드를 다시 덮어쓰지 않는다.
2. **음악 문서/스크립트 묶음:** 아래 파일 표의 음악 텍스트·JSON 8개 + 관련 scripts 3개(총11개)를 먼저 검토한다. progress.json은 재개용 중간 기록이므로 measurements.json과 중복/보존 필요성을 확인해 제외 여부를 명시한다. PROJECT_STATE의 음악 메타데이터 착수/피코 시험본/6곡 전체 수정 시험본 3개 절만 묶는다. WAV/ZIP과 원본 음원은 이 텍스트 커밋에 자동 포함하지 않는다.
3. **음악 보관 결정:** WAV14개+ZIP2개 총174,205,063바이트(약166.1MiB). 6곡 ZIP 72,847,972바이트와 피코 ZIP 10,462,167바이트는 개별 WAV/문서를 중복 포함한다. 반복 후보의 청음/매핑을 확정한 뒤 변경 이력을 관리할 WAV는 Git LFS, 전달용 ZIP은 별도 릴리스/승인된 보관소 중 한 방식을 선택한다. 같은 WAV와 ZIP을 일반 Git에 모두 넣는 것은 권하지 않는다. LFS를 택하면 포인터와 실제 객체 fetch 검증까지 해야 한다. 외부 원본도 해시를 유지해 보존 위치를 결정하고 원본을 삭제하지 않는다.
4. **웹사이트 묶음:** 아래 21개 경로(빌더2·README·hosting·dist17)와 PROJECT_STATE의 ‘2026-11-10 별도 소개 웹사이트 제작·배포’ 절을 검토한다. data/소개서 복사본은 현재 최신이므로 임의 재생성할 필요 없다. 앱 sprite6개는 이미 다른 경로에 게시된 같은 자산이다. 새 아트 납품으로 재업로드하지 말고 기존 소스를 재사용한다. 현재 정적 사이트의 독립 실행에 필요한 dist 복사본을 동봉할지 생성 단계로 공급할지를 정한 뒤 파일 목록을 확정한다. main의 dist 무시 규칙을 함께 조율한다. 사이트 재배포는 Git 게시와 별도이며 이번 계획은 재배포 권한 변경을 포함하지 않는다.
5. **감사/기록 묶음:** docs/GIT_COMPLETENESS_PROMPT.md와 이 GIT_UPLOAD_AUDIT.md를 검토해 별도 게시한다. PROJECT_STATE 전체를 stage하지 않는다. 맨 위의 오래된 ‘이야기 1차 원고’ 재삽입과 이미 게시된 대사 절의 위치 이동을 새 제작물처럼 중복 게시하지 않는다. README/CLAUDE/START_HERE 최신화는 좁은 후속 문서 작업으로 분리한다.

### 자동 게시에서 제외할 경로/유형 7군

`.tools/`, `.kotlin/`, `.gradle/`, `**/build/`, 개인 환경(.idea/local.properties/google-services.json), 자격증명·서명키(.env/keystore 등), 임시 배포 checkout/tar/서버 로그. 기존 .kotlin 로그 7개는 현재 A 브랜치에서 무시되지 않아 untracked로 보이지만 커밋 대상이 아니다. 본 감사가 개인 설정 파일을 게시 대상에 넣지 않았다는 뜻이며 해당 유형이 전부 실제로 존재한다는 뜻은 아니다.

.tools/audio-review/piko-decoded.wav는 예외적으로 재현에 필요한 원본 PCM 중간물이다. 파일을 확인했고 기록 SHA-256과 일치한다. .tools 전체는 올리지 않되 보존 가치가 없다고 삭제해서도 안 된다. 원본 MP3와 디코더 준비/보관 방식을 음악 담당자가 함께 결정한다.

## 7. 미게시 파일별 정확한 목록

아래 55개는 경로별 52개 신규+3개 수정이다. 이 보고서 자체는 별도 1개 신규 파일이다. 크기는 현재 파일의 바이트 크기다.

| 로컬 경로 | 바이트 | 판정 | 게시 묶음/주의 |
|---|---:|---|---|
| `PROJECT_STATE.md` | 56,016 | 최신본 미반영 | 음악3절/소개1절/사이트1절만 분리; 옛 이야기/대사 이동 제외 |
| `app/src/main/java/com/tinyrunners/school/characters/CharacterScreens.kt` | 14,263 | 최신본 미반영 | 소개 묶음; main 흐름 대조·빌드 필요 |
| `app/src/main/res/values/character_screen_strings.xml` | 1,914 | 최신본 미반영 | 소개 묶음; main 흐름 대조·빌드 필요 |
| `app/src/main/res/values/intro_strings.xml` | 3,066 | 로컬만 존재 | 소개 묶음; main 흐름 대조·빌드 필요 |
| `docs/GIT_COMPLETENESS_PROMPT.md` | 8,564 | 로컬만 존재 | 감사 문서 묶음 |
| `docs/INTRO_SCREEN_HANDOFF.md` | 3,928 | 로컬만 존재 | 소개 묶음; main 흐름 대조·빌드 필요 |
| `docs/PROJECT_INTRO.md` | 11,151 | 로컬만 존재 | 소개 묶음; main 흐름 대조·빌드 필요 |
| `docs/audio/Puddle_Jumpers_edit_drafts.zip` | 10,462,167 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/REVIEW.md` | 7,912 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `docs/audio/Tiny_Runners_6_music_edits.zip` | 72,847,972 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/README.md` | 1,938 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `docs/audio/all-music-drafts/loops/cheese_loop_draft.wav` | 5,009,804 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/loops/lumi_loop_draft.wav` | 4,833,404 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/loops/mint_loop_draft.wav` | 5,362,604 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/loops/nunu_loop_draft.wav` | 5,539,004 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/loops/piko_loop_draft.wav` | 4,762,844 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/loops/popo_loop_draft.wav` | 3,552,740 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/measurements.json` | 13,019 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `docs/audio/all-music-drafts/progress.json` | 6,008 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `docs/audio/all-music-drafts/trimmed/cheese_trimmed.wav` | 15,303,096 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/trimmed/lumi_trimmed.wav` | 6,893,744 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/trimmed/mint_trimmed.wav` | 10,734,596 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/trimmed/nunu_trimmed.wav` | 6,351,016 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/trimmed/piko_trimmed.wav` | 6,091,940 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/all-music-drafts/trimmed/popo_trimmed.wav` | 5,308,996 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/metadata.json` | 3,985 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `docs/audio/piko-drafts/Puddle_Jumpers_loop_draft.wav` | 5,059,196 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/piko-drafts/Puddle_Jumpers_trimmed_draft.wav` | 6,091,940 | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| `docs/audio/piko-drafts/README.txt` | 915 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `docs/audio/piko-drafts/measurements.json` | 1,798 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `docs/audio/piko-mp3-measurements.json` | 2,121 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `scripts/build-music-review-batch.py` | 15,067 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `scripts/build-piko-audio-drafts.py` | 6,226 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `scripts/inspect-music-metadata.py` | 2,919 | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| `website/.openai/hosting.json` | 89 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/README.md` | 2,697 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/build-content.ps1` | 1,023 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/build-content.py` | 1,301 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/TINY_RUNNERS_INTRO.md` | 11,151 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/app.js` | 3,938 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/assets/cheese-home.png` | 1,550,938 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/assets/cheese.png` | 809 | 로컬만 존재 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| `website/dist/assets/lumi-home.png` | 1,968,392 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/assets/lumi.png` | 1,084 | 로컬만 존재 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| `website/dist/assets/mint-home.png` | 1,469,510 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/assets/mint.png` | 866 | 로컬만 존재 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| `website/dist/assets/nunu-home.png` | 1,719,491 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/assets/nunu.png` | 1,017 | 로컬만 존재 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| `website/dist/assets/piko-home.png` | 2,000,387 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/assets/piko.png` | 973 | 로컬만 존재 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| `website/dist/assets/popo-home.png` | 1,849,289 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/assets/popo.png` | 801 | 로컬만 존재 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| `website/dist/data.js` | 77,459 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/index.html` | 7,927 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| `website/dist/styles.css` | 9,271 | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |

## 8. 전체 파일별 대조표

근거의 L은 로컬 Git blob ID다. 작업 브랜치/main에 ‘동일’이면 해당 원격 blob도 이 ID와 같다. 차이는 해당 main blob을 M으로 함께 적었다. 기준 원격 커밋은 1~2절의 고정 SHA다. 텍스트의 줄바꿈 정규화와 실제 바이너리 바이트 일치를 구별해 비교했다.

| 분류 | 로컬 경로 | 실제 제작 상태 | 로컬 Git 상태 | 원격 작업 브랜치 최신본 일치 | main 반영 여부 | 근거 커밋 또는 해시 | 누락·불일치 | 다음 조치 |
|---|---|---|---|---|---|---|---|---|
| 144개 대사 | `docs/dialogue/HANDOFF.md` | 144개; integrated=false | 추적·HEAD 동일 | 동일 | 동일 | L `668618659391941e637d9dd04ffb14ced30322cd` | 없음 | 유지 |
| 144개 대사 | `docs/dialogue/SHORT_LINES.md` | 144개; integrated=false | 추적·HEAD 동일 | 동일 | 동일 | L `5478e4c3550dcf2755d93687726c520edcec65d6` | 없음 | 유지 |
| 144개 대사 | `docs/dialogue/short-lines.json` | 144개; integrated=false | 추적·HEAD 동일 | 동일 | 동일 | L `4bc4aab4736d3424d3c5098c3ae74465b780b02d` | 없음 | 유지 |
| 기획·연구·통합 문서 | `AGENTS.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `91d4a888b7a703dea97ff8bfc8e42c2e7c9d43b7` | 없음 | 유지 |
| 기획·연구·통합 문서 | `CLAUDE.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `57f6112b5c110cde31e48cfe7b9dc4ddc3f12ead` | 없음 | 유지 |
| 기획·연구·통합 문서 | `PROJECT_STATE.md` | 기획/연구/기록; 일부 옛 안내 | 수정 | 내용 다름 | 내용 다름 | L `ac13b6be76971be378c8e635433fb591c987bd53`; M `df4f734bb7567d74e8c263fe5dd6b10b28484e93` | 최신본 미반영 | 음악3절/소개1절/사이트1절만 분리; 옛 이야기/대사 이동 제외 |
| 기획·연구·통합 문서 | `README.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `0a586702c6400f09a31529e39efe6e00ea5178e2` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/ART_DIRECTION.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `5b287e0409164a2c78661ee1a08414ffafe5c59e` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/A_SCREEN_HANDOFF.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `cd454b48549abf18aaf2fe1ef8be927caf229020` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/CHARACTER_BIBLE.json` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `6ce1dcc21401235f850ae573b6a207bace15784f` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/CHARACTER_PROFILES.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `355cb9999e5ae71f0d744a9654d45a85e466014f` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/DATA_CONTRACT.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `1ed50a44a512b4bc29cba05c9212cea974a091c6`; M `48122adbaa1415e3c278f73bb229824170382a64` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 기획·연구·통합 문서 | `docs/DEVELOPMENT_GUIDE.html` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `c74c200ed4dcee57537e70829fb1793be2c982dd` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/FIREBASE_SETUP.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `13bd23f3f10016cfbbc143fd7993116af67bb250` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/FIRST_CLAUDE_PROMPT.txt` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `ae7eb346af11b3751cf85a0ac0ec93444201357f` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/GIT_COMPLETENESS_PROMPT.md` | 기획/연구/기록; 일부 옛 안내 | 미추적 | 없음 | 없음 | L `4a36ba2aaa1b642c90f75113807646a5a5515dcd` | 로컬만 존재 | 감사 문서 묶음 |
| 기획·연구·통합 문서 | `docs/PROJECT_PLAN.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `5533144d3b761958a54b300a9416aec0dc6abc14` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/PROMPTS.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `f0c7fe6ed297bc08be48140e5ddede43f9b57f5d` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/START_HERE.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `c88de922d2ec6fdfc96152e60eb4016738a7a044` | 없음 | 유지 |
| 기획·연구·통합 문서 | `docs/VALIDATION.md` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `850b11186ebcc5708e8de305d200d8659ff9c642` | 없음 | 유지 |
| 기획·연구·통합 문서 | `research/aesop.json` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `66b8c363dd58fbeec35a348f423806cb062a255a` | 없음 | 유지 |
| 기획·연구·통합 문서 | `research/andersen.json` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `1e879cca1f8066b1a1af67707d1f258db866b93a` | 없음 | 유지 |
| 기획·연구·통합 문서 | `research/competitors.json` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `5b0ee44bb0509aa8dc4d58c4539abc0cb0c09a19` | 없음 | 유지 |
| 기획·연구·통합 문서 | `research/grimm.json` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `f0178bd1b90e36afdbc140efe3a5b51655992b3a` | 없음 | 유지 |
| 기획·연구·통합 문서 | `research/modern.json` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `5e926808350bbe4953611e98c68525b3df439c90` | 없음 | 유지 |
| 기획·연구·통합 문서 | `research/world.json` | 기획/연구/기록; 일부 옛 안내 | 추적·HEAD 동일 | 동일 | 동일 | L `6ddf665b10ae89695c45d2052756225e5fb00c34` | 없음 | 유지 |
| 배경 | `app/src/main/assets/backgrounds/manifest.json` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `eedea03f3f85f1a7a645dcb61a88ed5d39481e30` | 없음 | 유지 |
| 배경 | `app/src/main/assets/backgrounds/mint-cheese.png` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `8edfe5bf2fecea31fdf5438d3a232c19165a5a27` | 없음 | 유지 |
| 배경 | `app/src/main/assets/backgrounds/nunu-piko.png` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `57f621f4a97bc42d0fb49eb7892c73f57b128b06` | 없음 | 유지 |
| 배경 | `app/src/main/assets/backgrounds/popo-lumi.png` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `6aa6d5c036b1e0e9313727609b1ee8f7b8ae73b0` | 없음 | 유지 |
| 배경 | `app/src/main/java/com/tinyrunners/school/game/BackgroundMotion.kt` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `d00e41844c4a6ee598612538b9d13e0d315a5958` | 없음 | 유지 |
| 배경 | `app/src/main/java/com/tinyrunners/school/game/StoryBackground.kt` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `2fe2ec5607113a8668d8855868de710aa8ce63f0` | 없음 | 유지 |
| 배경 | `app/src/test/java/com/tinyrunners/school/game/BackgroundMotionTest.kt` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `e76260ef54efc6acc856d58f79266f5b26cae433` | 없음 | 유지 |
| 배경 | `docs/art/BACKGROUND_HANDOFF.md` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `5377beb97ad6210d460ca79222c926d4a8eab206` | 없음 | 유지 |
| 배경 | `docs/art/background-manifest.json` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `eedea03f3f85f1a7a645dcb61a88ed5d39481e30` | 없음 | 유지 |
| 배경 | `docs/art/background-source/mint-cheese.png` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `8edfe5bf2fecea31fdf5438d3a232c19165a5a27` | 없음 | 유지 |
| 배경 | `docs/art/background-source/nunu-piko.png` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `57f621f4a97bc42d0fb49eb7892c73f57b128b06` | 없음 | 유지 |
| 배경 | `docs/art/background-source/popo-lumi.png` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `6aa6d5c036b1e0e9313727609b1ee8f7b8ae73b0` | 없음 | 유지 |
| 배경 | `docs/art/review/background-preview.html` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `03803e8da2775b29c35e5aec2c5a378067502c2f` | 없음 | 유지 |
| 배경 | `scripts/background-preview.template.html` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `c35c8e0d5e16db347ac9ea9e9099a987a21c9449` | 없음 | 유지 |
| 배경 | `scripts/build-background-preview.py` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `e65b232a75864334810ab5ee5c5313125630c42e` | 없음 | 유지 |
| 배경 | `scripts/publish-backgrounds.ps1` | 단일 파노라마; 무한 반복 미완료 | 추적·HEAD 동일 | 동일 | 동일 | L `54d0213b23651345caf62d437410b06d7edc122b` | 없음 | 유지 |
| 스프라이트 | `app/src/main/java/com/tinyrunners/school/characters/CharacterSprites.kt` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `3db9a9d02ce8cab5cb456563363254afd1ba3097` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_fail_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `6843ba816ac754bdb63425f406489b3530654cd0` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_greet_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `194fc054b824b104b7206b76743953aa43c66504` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_home_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `ef164df44464576aa6193ef9faaee9aa04952e14` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_idle_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `9e93881bebc15255b354b7a811bfbfba2d28252a` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_idle_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `8ca17b7cba12a75fe37ad74c29d5cc68842d4216` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_jump_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `615a9a794793446eebc48227351ef90301c384f8` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_jump_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `a39d72f30a9c9a9d0ca3b8a1e515e3a884a5d13a` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_land_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `fe091d688f55ded881879305527fa999b79d2320` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_run_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `01488742ca16712bc92fc4ca61a1d29d089e102e` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_run_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `a8017799b6e370960036d9f595f7f3f168f043ab` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_run_2.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `ebbdfb1667d295fd93fc8ec644921820370204bb` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_cheese_run_3.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `d09ec0cd8262096220a9c6daf9da927eb48bccef` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_fail_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `4b82dfddfbc73267b8fc04a1bc65c1439b880e14` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_greet_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `b633091a354a58d52ab62e03e496c23507d69017` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_home_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `c46c1615795c7596ba3b3535cb4ea3705781f312` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_idle_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `ad6f0c646c00303482a8deb2de37d6a4761e18cf` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_idle_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `b8a4bd7e1c5ce965fc28e44d4eb602a8cedd4bd3` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_jump_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `febd35cfa2b70b1e7affaba755d6925d9bedf63b` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_jump_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `7382bc3f5e127c876b439f575be5ffe92c1b29be` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_land_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `06f897c38d5325969be3477d81ac86ef1dfb92ba` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_run_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `1c5b9392c551404967cf9b8fef587587a05a6099` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_run_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `1745c5dbd776913b843a3c229d15f1001eed86fc` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_run_2.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `2e6289a3ff575d87b6041702e9e4eed5053d194d` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_lumi_run_3.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `d494ecbbfe725622715628bc2f240984efa8430b` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_fail_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `ae42e73df972de9c342ab36691faf9990c62acf4` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_greet_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `d15db9b2055b245a2132fc5a321803465da33e0d` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_home_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `9cb6527c50baa3061de22cd26adb5a9d8813dd65` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_idle_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `a8b5668dac79aa58f4a2c1c70745abb706de9223` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_idle_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `a10e07cbbd6328e80bb177a50132c18ea33f1b32` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_jump_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `b57db491589ddb0a8252688f279d0c5940d6ed2c` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_jump_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `df88580ec0dfe93f41f227edad91abd922576183` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_land_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `08a71ea0601822a22ebc2c168a42810b8c48ba01` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_run_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `684a3421adcfbd9acd93da1d4919f619ef69b9b7` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_run_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `af551c68fecb345ab62d163cb61e57d47bed1e55` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_run_2.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `c25d2e5e0949b62610d2c91a334effc80280f8dd` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_mint_run_3.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `0629477b620f7d7891598766db1fc619386d9df5` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_fail_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `534b42facfb870731ad673c580e0d8310799b332` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_greet_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `9bfe8bb955b2be80cbb35f7d1220f41ecf01bf5e` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_home_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `571b25f788942ef43031118b7f1aa65501307cbc` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_idle_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `d0fbefbe1d4f610001128c72cbcbfc717c4cd94e` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_idle_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `97be916e4ae857e0710bc9a42a817d14c4e2d934` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_jump_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `046d33861ad97d087381b4a726c6e3a3bde33ae9` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_jump_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `6b4ca1a9d7ef364e1431346d1acac3ae15c466ac` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_land_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `375245b5cb45105cb027e8e7be5782a48abcb0ee` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_run_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `83b8eb8c54566d5b508183e7fa7fc10b304e49bd` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_run_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `5c893b80dbbe5ddb0214ef827c3bfe4561d5ef5a` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_run_2.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `5e201a0e2175be17ebcf71628266992bc64dffa9` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_nunu_run_3.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `d6d2617565c3bf38ec88cd0bdd4919038d7769ea` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_fail_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `f8d1813240d00f819dab706d35a54179552c3bff` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_greet_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `ba4a78b5d8a51adeff6672773ef46da59950e976` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_home_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `b9de3969dfb618e8cb3cafe519202d2201e6e004` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_idle_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `f6d329a1a407203126ad43e9b07d31bca9c319e6` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_idle_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `a0be240b567308a1fd2fef9c647ce16491605e57` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_jump_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `38b3e0315a102e494ef85074986c58157b0435f8` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_jump_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `516b165466378dfa0f8eed2b3735a60196385123` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_land_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `355d19b82deaaebe1d3ebc78fe5928dea4728d7e` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_run_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `e977179f39263e085ce149fae73475bd4fa64f38` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_run_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `1536b2300f1439e3e2528dd2384cfbd0fb5995fd` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_run_2.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `e4a7b3ed929b1def13a1300390497143cf0be61d` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_piko_run_3.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `047d33f5c30ea464da4779846f8018324a450c65` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_fail_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `ee39238aa57eeb0461f3504adeed328980f63773` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_greet_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `da603d2151352a5db7c64fbf8a2284eb2c6ba25b` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_home_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `6a0910abdd58db9f785dc40a9a3fd059cb0471dd` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_idle_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `d7b194c0336c602a6543e45e2a12b98560d0fb20` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_idle_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `2e4f2232f936e4555274e37b246b0b830f69fd5c` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_jump_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `8099175f2186989cabdabb05b0145f99d41770c0` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_jump_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `41cc255afb213e97f9698d7abd8f740cbca99eb0` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_land_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `66784fd6dff20e3b9d57a983dff6027fc9fef9cc` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_run_0.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `0a1f05a15d05bc604aa4033282ee2b6cedb158aa` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_run_1.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `aa5a75bf94956a6d6b0fcea2a1b0a796fe911fe2` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_run_2.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `a270767adf016a4f2694a7eddeb2497a142443ec` | 없음 | 유지 |
| 스프라이트 | `app/src/main/res/drawable-nodpi/sprite_popo_run_3.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `383dcc17e09fbf5f93b5006a9874d9d7191bd395` | 없음 | 유지 |
| 스프라이트 | `docs/SPRITE_SPEC.md` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `27617e5c67bf974b3b919fc1438ef99b9ed4c26d` | 없음 | 유지 |
| 스프라이트 | `docs/art/SPRITE_HANDOFF.md` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `7760313a03b774fb924628a2e5c8b8774413d7b1` | 없음 | 유지 |
| 스프라이트 | `docs/art/frame-manifest.json` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `d2dcd102a6306ffef714d331df105cddd48fe692` | 없음 | 유지 |
| 스프라이트 | `docs/art/review/animation.gif` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `8ed528b19a6381381da728df873c4a9c70d3e294` | 없음 | 유지 |
| 스프라이트 | `docs/art/review/contact-sheet.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `1d653b22bca9ca227e28a06aff89b9d1d3112b3c` | 없음 | 유지 |
| 스프라이트 | `docs/art/review/preview.html` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `517334517f9233f9ed082f6fe5cfcde2068c7787` | 없음 | 유지 |
| 스프라이트 | `docs/art/review/validation.json` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `3fbf7ac12b43e6f865860a755fb1b6d49440a535` | 없음 | 유지 |
| 스프라이트 | `docs/art/source-inspection.json` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `4af09a1510b8e5dd59cff3f9621075e7dd6944a3` | 없음 | 유지 |
| 스프라이트 | `docs/art/source/mint-cheese.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `62a5ad688e162eeb5e949ea77df5ce894dcb1654` | 없음 | 유지 |
| 스프라이트 | `docs/art/source/nunu-piko.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `d912bde8633a197655976e12a50476f858d82b9c` | 없음 | 유지 |
| 스프라이트 | `docs/art/source/popo-lumi.png` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `c5f8f971c8c438eb4104659129af6ca40766c58b` | 없음 | 유지 |
| 스프라이트 | `scripts/art/export_sprites.py` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `22875735182fe75b5e8d740a92e9e2cc95ee6311` | 없음 | 유지 |
| 스프라이트 | `scripts/validate-sprite-pixels.ps1` | 승인 자산/검수·연결 자료 | 추적·HEAD 동일 | 동일 | 동일 | L `3b879d8a6e4ad31afcc7aa315ac5b9f8ddf18416` | 없음 | 유지 |
| 앱 소개·발표·공유 화면 | `app/src/main/java/com/tinyrunners/school/characters/CharacterScreens.kt` | 소개 개편/발표 원고; 현재 빌드 미확인 | 수정 | 내용 다름 | 내용 다름 | L `09a285d1b1a049fe078a8fb014ae9ab263a97e4a`; M `e6e9c73bb9399df6228ce8c433b6eb42cfe8d5ac` | 최신본 미반영 | 소개 묶음; main 흐름 대조·빌드 필요 |
| 앱 소개·발표·공유 화면 | `app/src/main/res/values/character_screen_strings.xml` | 소개 개편/발표 원고; 현재 빌드 미확인 | 수정 | 내용 다름 | 내용 다름 | L `c14f2ad23c5b143cd8f9c70e809d2959e59e0f6d`; M `90ad27463ac3455b1b6c507a0ad07e9aaa7674ab` | 최신본 미반영 | 소개 묶음; main 흐름 대조·빌드 필요 |
| 앱 소개·발표·공유 화면 | `app/src/main/res/values/intro_strings.xml` | 소개 개편/발표 원고; 현재 빌드 미확인 | 미추적 | 없음 | 없음 | L `86691b656bfcf6c77c6e11bbe4e10bd81018bf0e` | 로컬만 존재 | 소개 묶음; main 흐름 대조·빌드 필요 |
| 앱 소개·발표·공유 화면 | `docs/INTRO_SCREEN_HANDOFF.md` | 소개 개편/발표 원고; 현재 빌드 미확인 | 미추적 | 없음 | 없음 | L `cf01afc8e6d67e376158b5c418e9f92e832c4651` | 로컬만 존재 | 소개 묶음; main 흐름 대조·빌드 필요 |
| 앱 소개·발표·공유 화면 | `docs/PROJECT_INTRO.md` | 소개 개편/발표 원고; 현재 빌드 미확인 | 미추적 | 없음 | 없음 | L `2c8022de7c66083a18f4d5d6e810004818b4d3b8` | 로컬만 존재 | 소개 묶음; main 흐름 대조·빌드 필요 |
| 앱·검증·환경 | `.gitattributes` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `505d52d1ee66f162ab3aad1ecf3b4430d7227cb3` | 없음 | 유지 |
| 앱·검증·환경 | `.gitignore` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `aea408e91d7a3f068f881267ba1a3fa54d2b6d09`; M `00f30e3a8bb2cff096b199943280806632e97f85` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/build.gradle.kts` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `5824f5bf630a17aa83af558709fa6dd27e0dbe3b`; M `68986d71ff028415612834c40468168f0f5a6fd6` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/AndroidManifest.xml` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `9a4c4679466f6ea794a79fd1108f9fe7014742a4`; M `048a2cbac297938fdc0011569e2bfdd8defb6f94` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/assets/content/assets.json` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `d5af6ec858f01ba6fcda9393ff12fbf8e9ff010c` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/main/assets/content/characters.json` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `7e40b34a8fa7d51408dc858030fb225d0371715c` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/MainActivity.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `322bc4db29e509622675f5dbd3b43d6bf3ebc41c`; M `a5fd04731a3914c28802c5e4f41555a7d79340cd` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/auth/AuthService.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `c1d2d59b28ccb54702d6990d53a9c364c66e101a` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/auth/LoginActivity.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `970c4cf4b22c99ad64b091681d98978f2aa1b58f`; M `cdb98aa667bf702c10bcd03b6e385e537c5a573b` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/auth/SignupActivity.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `cc37302843bc87de01524f3f6ef8763054de9d79`; M `5153fd8b958ebf65940d750381d0b5160162b4a8` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/characters/CharacterPortrait.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `336d1aa7f26c261c822e9556266dfe5a30496742` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/data/CharacterDefinition.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `32de2b8d0798e3617d33337963d29e657aa47e0a`; M `79bdd4b90321c5be6499fff4689fa90883f16c76` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/data/CharacterRepository.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `0fe54b4d467be89d30051e7abdafbff65ec5c4f2` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/game/GameActivity.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `2ea7999a1c00b4edcf4550a501be66e06d66319b`; M `017ed94ce6f78b43bd14568420ea2aa346664337` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/game/RunnerAnimation.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `783603ac2cdf927e4da9d55d8c65746de9bf4459` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/game/RunnerEngine.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `e4362e71578f3a73c089b29e3b09cfa211f84727`; M `358c9edc5342185f3c0b5976d4710a5456b1cc8c` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/game/RunnerView.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `207fea481c2e1aabc6c3e4e1b37d4cc97adfd8eb`; M `0722b80ef140a694bef31f02fc4853cd2d918277` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/progress/Progress.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `07b62135dea1534a2077216b2f1f56b12e8a8bfb`; M `b00f259265c34ad115e7779f08366fc0e3b229e9` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/progress/ProgressStore.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `e118f4bf71cbdf38dc31bf42e10ead3ddf9f5103`; M `d30bbbfb658a2bd8c13e0b4c0f6f3fbb58760965` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/records/RecordsActivity.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `82dfc3b352b44d4ade205bbeffd8cd806903020a` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/result/ResultActivity.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `cba92da6df8e2e020a6026122c19c5858cecb5fe`; M `fc732f94dbe6e98a9a526191d9afbef53d558cd2` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/settings/SettingsActivity.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `6bc94259c7aa9ae60276f546ec5d44f1d1fa726f`; M `bd4ef2e04446923d7181a2294de72b2d5f273b6e` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/ui/Page.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `46c2917fc99a3010ed24923bc4314e2cf21ddfac`; M `2862fd82ce2267f80b126873a04aa2b9d55f0669` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/java/com/tinyrunners/school/weather/Weather.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `ca6f62a9d2bebaa020909ab7abb634a0d8a2e70e` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/main/res/layout/activity_main.xml` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `3091efc7f9ee0fac338976d7606f050230e35aae`; M `d460005a41fc7a26cb6e4af1ea6f0f2d9194c805` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/res/values/account_strings.xml` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `619469148a1f4f542e4123579a6753b355b8c32b`; M `dad64b2a4d6d5a83a1e7502a8c37843e1a9dcd67` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/res/values/game_strings.xml` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `bd0ce1eb8b64e73d329d16e46126f8b00415d67d`; M `283033dc6912b12aaaaa4f4e20ed1c53eff62bf4` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/main/res/values/strings.xml` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `0e9194a80ffc11186056ec636283b4ec8cd66d91` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/main/res/values/styles.xml` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `a9b5e62cea0cb852637dc32617f6d8723708b7f4`; M `f7bbec4c2311eb22f1d8a1cbbb79012cbb027a52` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/test/java/com/tinyrunners/school/auth/AuthInputTest.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `3b69279c2d390a8aa83bbe59ec2f3641a6c38652` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/test/java/com/tinyrunners/school/game/RunnerAnimationTest.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `a15feb2920706689141a6af243b12ae4c3039fcc` | 없음 | 유지 |
| 앱·검증·환경 | `app/src/test/java/com/tinyrunners/school/game/RunnerEngineTest.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `f942e6239b39085c2e43fe319aab1196bfec66f6`; M `cd2803931f836162dee1d7368617121b71d8c80a` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/test/java/com/tinyrunners/school/progress/ProgressBookTest.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `c34961198971727e56f19087436d5fa5d64d849b`; M `6a5e99d68a05a1bfa011e5cb7d303b0c1a683850` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `app/src/test/java/com/tinyrunners/school/weather/WeatherTest.kt` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `a2ea207cd737415b9826ee28d72d562106c34884` | 없음 | 유지 |
| 앱·검증·환경 | `build.gradle.kts` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `895bf3b9b035b7544c231d2fa7bd443b399d21ce` | 없음 | 유지 |
| 앱·검증·환경 | `gradle.properties` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `5190ee81fb5e48e50988a3e2a500e94219982057` | 없음 | 유지 |
| 앱·검증·환경 | `gradle/wrapper/gradle-wrapper.jar` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `2c3521197d7c4586c843d1d3e9090525f1898cde` | 없음 | 유지 |
| 앱·검증·환경 | `gradle/wrapper/gradle-wrapper.properties` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `543fdee7dcc309b66c5d3841cf73038a27fbcae1` | 없음 | 유지 |
| 앱·검증·환경 | `gradlew` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `f5feea6d6b116baaca5a2642d4d9fa1f47d574a7` | 없음 | 유지 |
| 앱·검증·환경 | `gradlew.bat` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `9d21a21834d5195c278ba17baec3115b2aaab06e` | 없음 | 유지 |
| 앱·검증·환경 | `scripts/bootstrap-gradle.ps1` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `93996d4f4412c13a97c351c9d8422d6f26955835` | 없음 | 유지 |
| 앱·검증·환경 | `scripts/publish-github.ps1` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `72c356516af98fbcb3d725db5728f9a3a2af3231` | 없음 | 유지 |
| 앱·검증·환경 | `scripts/validate-content.cjs` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `640ab41ece2155fe8e363be009ff4e96e0294977`; M `25e52cc19fa44ae072ad34d1e49fc7600d7196e8` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 앱·검증·환경 | `settings.gradle.kts` | 공통 코드/검사/환경; B 후속 차이 존재 | 추적·HEAD 동일 | 동일 | 동일 | L `7918af616de7253faccc80091037ba76641d38f8` | 없음 | 유지 |
| 연결 배경 | `docs/art/CONNECTOR_REVIEW.md` | 부분 보정; loopReady=false; 비교판 구버전 | 추적·HEAD 동일 | 동일 | 동일 | L `e15deb617ed3715ae5fc0768e7a81c1b7a13eda5` | 없음 | 유지 |
| 연결 배경 | `docs/art/background-connectors/manifest.json` | 부분 보정; loopReady=false; 비교판 구버전 | 추적·HEAD 동일 | 동일 | 동일 | L `b8c8604db00b2c70735946983492d167f30f9714` | 없음 | 유지 |
| 연결 배경 | `docs/art/background-connectors/mint-cheese.png` | 부분 보정; loopReady=false; 비교판 구버전 | 추적·HEAD 동일 | 동일 | 동일 | L `f0e539b80c892822c602d1f4c5b5e9a6e402832f` | 없음 | 유지 |
| 연결 배경 | `docs/art/background-connectors/nunu-piko.png` | 부분 보정; loopReady=false; 비교판 구버전 | 추적·HEAD 동일 | 동일 | 동일 | L `9530b229a15ae84ee98ce23dda15e56b006d314e` | 없음 | 유지 |
| 연결 배경 | `docs/art/background-connectors/popo-lumi.png` | 부분 보정; loopReady=false; 비교판 구버전 | 추적·HEAD 동일 | 동일 | 동일 | L `9c53f56485e35332a5c49fe1799cb4aa1e32fd5e` | 없음 | 유지 |
| 연결 배경 | `docs/art/review/connector-review.html` | 부분 보정; loopReady=false; 비교판 구버전 | 추적·HEAD 동일 | 동일 | 동일 | L `3684cb03fa2576fcb03875666b856d2a426920f3` | 없음 | 유지 |
| 연결 배경 | `docs/art/review/connector-seams.png` | 부분 보정; loopReady=false; 비교판 구버전 | 추적·HEAD 동일 | 동일 | 동일 | L `1a8274545e9b8190fa424fb403b07dba852a3e1d` | 없음 | 유지 |
| 웹사이트 | `website/.openai/hosting.json` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `990f129b66f395a037e47f846c530131944b60e1` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/README.md` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `e31af7fea55c2cecb7e2c12164b2fd69ad067729` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/build-content.ps1` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `3cdeebeae941658aee76870dc59ace2f2657a77d` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/build-content.py` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `9eb34388c905772a503897df7e49b90ad5715dc6` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/TINY_RUNNERS_INTRO.md` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `2c8022de7c66083a18f4d5d6e810004818b4d3b8` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/app.js` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `23423e2f5ce5fd0cc2a6e9e6cae1dd9723d72a7a` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/assets/cheese-home.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `78be0cf90019e2b7690c5dd1a3000a98eecadf31` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/assets/cheese.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `9e93881bebc15255b354b7a811bfbfba2d28252a` | 로컬만 존재; 같은 sprite는 앱 경로 게시됨 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| 웹사이트 | `website/dist/assets/lumi-home.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `89714b4037101cd11f70d7b4319c42f9be846b4d` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/assets/lumi.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `ad6f0c646c00303482a8deb2de37d6a4761e18cf` | 로컬만 존재; 같은 sprite는 앱 경로 게시됨 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| 웹사이트 | `website/dist/assets/mint-home.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `c1104137588befc6dade2f208ac308ad54d0ca45` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/assets/mint.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `a8b5668dac79aa58f4a2c1c70745abb706de9223` | 로컬만 존재; 같은 sprite는 앱 경로 게시됨 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| 웹사이트 | `website/dist/assets/nunu-home.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `af39674b08d271db2c63eabfcbea06f9e185fe62` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/assets/nunu.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `d0fbefbe1d4f610001128c72cbcbfc717c4cd94e` | 로컬만 존재; 같은 sprite는 앱 경로 게시됨 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| 웹사이트 | `website/dist/assets/piko-home.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `5e6c66ec953f227053ec0856f07c8d76208d6eb0` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/assets/piko.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `f6d329a1a407203126ad43e9b07d31bca9c319e6` | 로컬만 존재; 같은 sprite는 앱 경로 게시됨 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| 웹사이트 | `website/dist/assets/popo-home.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `7695be8168d993439a16f6ab1fcda0e1f6153caa` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/assets/popo.png` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `d7b194c0336c602a6543e45e2a12b98560d0fb20` | 로컬만 존재; 같은 sprite는 앱 경로 게시됨 | 사이트 묶음; 이미 앱에 있는 동일 sprite 재사용 |
| 웹사이트 | `website/dist/data.js` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `e1683cfb5a89a24e4bc97105fb32dfbd76fd6a40` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/index.html` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `1cb9e31d817196ceb52d00aae9de5cb9c9ea37a8` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 웹사이트 | `website/dist/styles.css` | 별도 배포 성공; 앱 미연동 | 미추적 | 없음 | 없음 | L `06dcb881e356471b26a4617e043d0d36b257cc5a` | 로컬만 존재 | 사이트 묶음; dist 무시 규칙 확인 |
| 음악 | `docs/audio/Puddle_Jumpers_edit_drafts.zip` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `03078fe090bcad3943381448ab867d7be37085fc` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/REVIEW.md` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `ac01a799611a9edb423c3ffe3f95a887a7a4e990` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 음악 | `docs/audio/Tiny_Runners_6_music_edits.zip` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `3224ca1dc109fec591a456d49e1d1b77d3e35d78` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/README.md` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `b69e25dde2dc5731a125cb4fe702dc27ebc04e54` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 음악 | `docs/audio/all-music-drafts/loops/cheese_loop_draft.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `6fa6389b1303c7d1a38c08b7f1c74e3dada91202` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/loops/lumi_loop_draft.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `0006f8b1e2f9ab7155c66b9f5fcb6ca6e439fc53` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/loops/mint_loop_draft.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `159e42159bcc9aafeee13dcfe12130c7d0fc14f4` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/loops/nunu_loop_draft.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `b86f25c27e08e157665fd8312fbf1af8a3145350` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/loops/piko_loop_draft.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `d4644a7c5bf820e039761981f76a4c11c2a07517` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/loops/popo_loop_draft.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `389a55e3d5c2321632962627dda784b9692e0bfb` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/measurements.json` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `7acbc5af353eb06787cbac95545e0892cb91ec33` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 음악 | `docs/audio/all-music-drafts/progress.json` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `c0c1175ac5f606d8b37c3b890779de8815b1b815` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 음악 | `docs/audio/all-music-drafts/trimmed/cheese_trimmed.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `8add2877544a93257a7713508def2aae31513d7b` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/trimmed/lumi_trimmed.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `3fa34c8397fe060b633b1c29d6778bfe310d4119` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/trimmed/mint_trimmed.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `1f2889c32711d090ecc21fb2bed7660fc55c5472` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/trimmed/nunu_trimmed.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `a3ae5ee28c86b98066efcc96f6113eadf255f030` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/trimmed/piko_trimmed.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `36e46bf5eb4417dbb6e7fe5db7127eafb3acae7d` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/all-music-drafts/trimmed/popo_trimmed.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `06b8cf26a114ac550db0d17712b6caa41920e732` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/metadata.json` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `5627b064d0a88e121a693cfad94d4fa493d2ffe3` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 음악 | `docs/audio/piko-drafts/Puddle_Jumpers_loop_draft.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `ed08f1ce4b29cdf05a02ffa290c74ff4f6b68f6b` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/piko-drafts/Puddle_Jumpers_trimmed_draft.wav` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `51840bf00846364626299f85b5e8571d32989d27` | 로컬만 존재 | 보관 방식/청음 결정 전 자동 게시 보류 |
| 음악 | `docs/audio/piko-drafts/README.txt` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `e3574d5953a2e2a02be44f4a4505e68efcd8a523` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 음악 | `docs/audio/piko-drafts/measurements.json` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `e401fe9079303f0b5f1ab05f64fe6e9ac051112a` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 음악 | `docs/audio/piko-mp3-measurements.json` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `d2dff6e717da116b6ab85bed06eec40ca554f46f` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 음악 | `scripts/build-music-review-batch.py` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `5346cb37fed0fd701179b42583a80a9ee109ba44` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 음악 | `scripts/build-piko-audio-drafts.py` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `08fe424a22c2a95d5e5c4f0fcd9812a52a428d61` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 음악 | `scripts/inspect-music-metadata.py` | 기술 시험/청음·매핑 미승인 | 미추적 | 없음 | 없음 | L `aefab9c318946840373e82add2ce3b23e200595e` | 로컬만 존재 | 음악 문서·스크립트; progress는 중간 기록 검토 |
| 이야기·도감 | `app/src/main/assets/content/stories.json` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `ad840bdd5dc938760517463dae514549b383930b` | 없음 | 유지 |
| 이야기·도감 | `app/src/main/java/com/tinyrunners/school/stories/Story.kt` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `8a4eff2f29a43931510e9352003b6c85c7568d1d` | 없음 | 유지 |
| 이야기·도감 | `app/src/main/java/com/tinyrunners/school/stories/StoryActivity.kt` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `67047e2b33218f4f80729695ebe5c12ab8bb8f71` | 없음 | 유지 |
| 이야기·도감 | `app/src/main/java/com/tinyrunners/school/stories/StoryStore.kt` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `f0d88b148a5f074b155c79b31a028c4fc3fd71f6` | 없음 | 유지 |
| 이야기·도감 | `app/src/main/res/values/story_strings.xml` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 내용 다름 | L `cc9155a280a607b60ef1719ba5c66121d3ae8ae7`; M `91022a7b1b04ca9caf9045e8b757ba4a31914356` | main B 후속/통합 차이 | B 후속 보존; 덮어쓰기 금지 |
| 이야기·도감 | `app/src/test/java/com/tinyrunners/school/stories/StoryStoreTest.kt` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `59a74509b9c74c0b8d5a8932709b2b8fb7b073b5` | 없음 | 유지 |
| 이야기·도감 | `app/src/test/java/com/tinyrunners/school/stories/StoryTest.kt` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `efbdda342081a030fa009626336847eedfa2ed16` | 없음 | 유지 |
| 이야기·도감 | `docs/STORY_HANDOFF.md` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `90fd2533bb27565959c426e3d0755d518562db7e` | 없음 | 유지 |
| 이야기·도감 | `docs/STORY_MANUSCRIPT_01.md` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `76bb54678af2a8b28337e340da64a0fe550e9ef3` | 없음 | 유지 |
| 이야기·도감 | `docs/STORY_MANUSCRIPT_02.md` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `3a140782c6c2190953029db2842f5e8f594f00d6` | 없음 | 유지 |
| 이야기·도감 | `docs/STORY_MANUSCRIPT_03.md` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `a8f7ad6a690555514cf89d3c53089d0060c14e68` | 없음 | 유지 |
| 이야기·도감 | `scripts/build-story-content.py` | ready12/outline12; 상세 기기검수 남음 | 추적·HEAD 동일 | 동일 | 동일 | L `7b8b577d59aa1b0d0da1222a42387f1e519db75d` | 없음 | 유지 |
| 장애물 | `docs/art/OBSTACLE_HANDOFF.md` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `ea2b9b499ad9013f729e4272f332e88219142980` | 없음 | 유지 |
| 장애물 | `docs/art/UPLOAD_OBSTACLE_PROMPT.txt` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `bc9cf42cc2694e07d4f5c98a40d4357214aefb3a` | 없음 | 유지 |
| 장애물 | `docs/art/obstacle-concepts/README.md` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `c0fc93a00820e27ed616236afbef8b4c2d2ff345` | 없음 | 유지 |
| 장애물 | `docs/art/obstacle-concepts/REVIEW.md` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `734af4c81cc85c3427bc41ff541c3b1ea8e2d378` | 없음 | 유지 |
| 장애물 | `docs/art/obstacle-concepts/mint-cheese.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `d68ed435c55c408e8447a10e4e117c24528a174d` | 없음 | 유지 |
| 장애물 | `docs/art/obstacle-concepts/nunu-piko.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `a0efd5931a5e30843fe5acc9b276235231f40fc8` | 없음 | 유지 |
| 장애물 | `docs/art/obstacle-concepts/popo-lumi.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `61954d78ad1b31d1f29ebe168f2b4bc3dd13166a` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/background-review.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `bfc1dd6de0db3dfce042196597f23d0567bd5d6d` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/export.csv` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `6f0dedf756c8f92c8c887037e6d33429daada418` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/manifest.json` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `21f1d3d2c0b5125a4c6d76e2bf58f10086f56fe8` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_cheese_cushions.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `697efb1649f3b8e8102dc4fa937028f1ec34eb03` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_cheese_plant_pot.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `27783252251eddbeae26156112fe04b48a283d0e` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_cheese_sun_tin.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `029ce405eb2d12aa483e8905f83caa3d2a2c6f1a` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_lumi_moss_stump.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `1c7c458b448d070f5d1b7bff484853f857f2acb1` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_lumi_music_box.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `ecb468d72bdedd4ea30c9fc5693f69dc653deed5` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_lumi_pinecone_basket.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `f6da80d98fc4ee72c57b861341330d758e316adf` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_mint_fern_log.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `4e7b98251ca91211e5e34a307c2115b2ca9b7fca` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_mint_map_crate.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `c9efa58f63588d288db527ca784934f1b3419977` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_mint_ribbon_rock.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `a91bb9661e0033857a59d42ebb1db559ef0193d9` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_nunu_books.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `80594155695ae821416e71def52d6b549e7c9cab` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_nunu_letter_basket.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `1d9432686581a4e49ffb4f828bc79dd945914602` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_nunu_return_crate.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `ee081a63e1dfe92a10d10770aca48d652f7cee11` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_piko_paper_crate.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `d4e6283b73fa227d706151dd5c4d9b4024e00c6f` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_piko_rope_spool.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `20d1d76e83f601d4181337ee9c3c57f53bdbf67b` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_piko_toolbox.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `97377dc4399e0b5ca9f60f1c60b5e2ab1941d10a` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_popo_harbor_crate.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `353d07d4a6c5a7d7c6c53856bd2f53c6b9662cce` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_popo_shell_basket.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `abd1338246dd1f89f0334a65338452dd9be0d5de` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/png/obstacle_popo_snow_rock.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `24e72143e5f679cee15925bcf39180186b6c612b` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/review.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `8c9d0baceec9f93748106759c5cd2e987a3baec6` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/source/mint-cheese.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `4a33f1b2aeaadd6227415041d80e2892a092fac1` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/source/nunu-piko.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `9810a59616abde3099608a2abf9aba9901b579da` | 없음 | 유지 |
| 장애물 | `docs/art/obstacles/source/popo-lumi.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `61954d78ad1b31d1f29ebe168f2b4bc3dd13166a` | 없음 | 유지 |
| 장애물 | `docs/art/review/obstacle-composite-check.png` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `d275001b29e2644ee32d9b19b3c5c45280c677af` | 없음 | 유지 |
| 장애물 | `scripts/ExportObstacleArt.cs` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `398fccd5b3dc72ca7f515e0f6a0d61c8e7a5dc78` | 없음 | 유지 |
| 장애물 | `scripts/validate-obstacle-art.py` | 최종18/이전콘셉트 구분; A 납품 완료 | 추적·HEAD 동일 | 동일 | 동일 | L `5a91b7ccdfe2825e5bb6a89a97e956c407c9e00d` | 없음 | 유지 |

## 9. 외부 원본 음악 12개

다음 파일은 Downloads에서 존재와 전체 SHA-256을 확인했고 측정 문서와 모두 일치했다. 저장소 원격에 원본 음원은 없다. 이름만 같은 것으로 판정하지 않았다. 개인 절대 경로는 기록하지 않는다.

| 외부 파일명 | 바이트 | SHA-256 | 상태 |
|---|---:|---|---|
| `Little_Wooden_Steps.mp4` | 4,974,713 | `e9c2bbaae597a1b8b24166b20f0d4ddfb7e8b00a3cf30a509b0d87f23f69ff40` | Downloads에만 존재; 원격 미보관 |
| `Soft_Paws_on_Window_Sills.mp4` | 5,519,744 | `00088c555902ae3364025f4b5a04c4e262a38f385d4a06405b75bf9fc78f0700` | Downloads에만 존재; 원격 미보관 |
| `Summer_by_the_Harbor.mp4` | 2,920,477 | `b91e937d1d6d9e5db12c3b8d6ff5011a6fd847629c64a8f2d64c4a0af78cec0c` | Downloads에만 존재; 원격 미보관 |
| `Strolling_Through_Amber.mp4` | 3,631,499 | `265a65fc32e364f8879dc263cf36278e0abe9b65c788612d27925a1ee9bd0d08` | Downloads에만 존재; 원격 미보관 |
| `A_Seat_for_the_Unseen.mp4` | 3,204,655 | `39ed1bba6af840ee98486aef657b156bb18dc8e27e305e047703bb1622c17ea5` | Downloads에만 존재; 원격 미보관 |
| `Puddle_Jumpers.mp4` | 3,420,287 | `de496fbcab785452a92eb5df7ec75de1dd17fd9e614bd8d0a587a3d80eb5fcf7` | Downloads에만 존재; 원격 미보관 |
| `Little_Wooden_Steps.mp3` | 1,532,096 | `8dd9a025e33f0f8d5300fed9ed42d78357bf903235f4106f930a4ed6f8b38f0c` | Downloads에만 존재; 원격 미보관 |
| `Soft_Paws_on_Window_Sills.mp3` | 2,107,625 | `544b7e79e6f178d57e2f08fc9385965d2aa8e471406dbfb03e438878cd2cc763` | Downloads에만 존재; 원격 미보관 |
| `Summer_by_the_Harbor.mp3` | 779,769 | `145d7cbeb2f05c5329273b438e7743a4869f6bfccdd34699309ebe22271f01cc` | Downloads에만 존재; 원격 미보관 |
| `Strolling_Through_Amber.mp3` | 965,343 | `c7c9d1a0b88a3642956afe3721fa88a2284fd66bf03f377e1a1503741b263709` | Downloads에만 존재; 원격 미보관 |
| `A_Seat_for_the_Unseen.mp3` | 903,276 | `c2cd50ebc588aa6fa474a4a4da9cad99af4e02506ee583992de1229043d8a945` | Downloads에만 존재; 원격 미보관 |
| `Puddle_Jumpers.mp3` | 917,696 | `73afda21507f15bcd1da33bb6f48b3ec60be1ac9be2a484645b41a44e8836751` | Downloads에만 존재; 원격 미보관 |

Puddle_Jumpers(1).mp3 첨부와 이 원본의 동일성은 미확인이다. 초기 생성 콘셉트/첨부 시안은 최종 승인 원본 3장이 게시됐다는 사실과 구분한다. 초기 모든 변형 이미지가 보존됐다는 증거는 없고, 교수 PDF도 정확한 파일명을 확보하지 못했다. Tiny_Runners_Android_Guide.html은 연구/설정에 참조됐으나 해당 이름의 원본은 프로젝트/Downloads에 없다. 다른 디렉터리·Library·과거 채팅 전체를 전수 조사한 것은 아니다.

## 10. main에만 있는 후속 파일과 중복 경로

아래는 main의 후속 작업이며 로컬 제작물의 업로드 누락이 아니다. 새 원격 코드로 기존 로컬 작업을 자동 교체하지 않았다.

- `app/src/main/assets/content/talks.json` — `3768cce8de1cbaf02f63efcfd07fc9d83fbe11af`
- `app/src/main/assets/content/village_stories.json` — `fcbcad498ef79fc6c10fa44ebbcc6f364fe66dfb`
- `app/src/main/java/com/tinyrunners/school/game/ObstacleArt.kt` — `f25f5edc7254f53ca99653cc2f5d24532447636f`
- `app/src/main/java/com/tinyrunners/school/game/Tutorial.kt` — `e80893ddac972ec5e390f393a6eb6c2d54fe8e1f`
- `app/src/main/java/com/tinyrunners/school/opening/OpeningActivity.kt` — `f6084cce910a53daf86b04c0b07de6deb03ebe6d`
- `app/src/main/java/com/tinyrunners/school/story/Story.kt` — `544d6ee2f8ea79470b246af014c1572e1d95698c`
- `app/src/main/java/com/tinyrunners/school/story/StoryActivity.kt` — `2f7cfd1e63d81a70ed6562cb13ae1f93c779424f`
- `app/src/main/java/com/tinyrunners/school/talk/PrologueActivity.kt` — `7d69c626dab893b04cd7dc44ab913881ed027086`
- `app/src/main/java/com/tinyrunners/school/talk/Talk.kt` — `3082476ece7446582ab9ecfa3d5cc01c124eccaa`
- `app/src/main/java/com/tinyrunners/school/talk/TalkActivity.kt` — `694471fe8191799a6dd741536f5ea97746be4c3e`
- `app/src/main/java/com/tinyrunners/school/ui/Scenery.kt` — `2cfa837cb8d17f32adf6acb8d61e6e6b96b11251`
- `app/src/main/res/drawable-nodpi/obstacle_cheese_cushions.png` — `697efb1649f3b8e8102dc4fa937028f1ec34eb03`
- `app/src/main/res/drawable-nodpi/obstacle_cheese_plant_pot.png` — `27783252251eddbeae26156112fe04b48a283d0e`
- `app/src/main/res/drawable-nodpi/obstacle_cheese_sun_tin.png` — `029ce405eb2d12aa483e8905f83caa3d2a2c6f1a`
- `app/src/main/res/drawable-nodpi/obstacle_lumi_moss_stump.png` — `1c7c458b448d070f5d1b7bff484853f857f2acb1`
- `app/src/main/res/drawable-nodpi/obstacle_lumi_music_box.png` — `ecb468d72bdedd4ea30c9fc5693f69dc653deed5`
- `app/src/main/res/drawable-nodpi/obstacle_lumi_pinecone_basket.png` — `f6da80d98fc4ee72c57b861341330d758e316adf`
- `app/src/main/res/drawable-nodpi/obstacle_mint_fern_log.png` — `4e7b98251ca91211e5e34a307c2115b2ca9b7fca`
- `app/src/main/res/drawable-nodpi/obstacle_mint_map_crate.png` — `c9efa58f63588d288db527ca784934f1b3419977`
- `app/src/main/res/drawable-nodpi/obstacle_mint_ribbon_rock.png` — `a91bb9661e0033857a59d42ebb1db559ef0193d9`
- `app/src/main/res/drawable-nodpi/obstacle_nunu_books.png` — `80594155695ae821416e71def52d6b549e7c9cab`
- `app/src/main/res/drawable-nodpi/obstacle_nunu_letter_basket.png` — `1d9432686581a4e49ffb4f828bc79dd945914602`
- `app/src/main/res/drawable-nodpi/obstacle_nunu_return_crate.png` — `ee081a63e1dfe92a10d10770aca48d652f7cee11`
- `app/src/main/res/drawable-nodpi/obstacle_piko_paper_crate.png` — `d4e6283b73fa227d706151dd5c4d9b4024e00c6f`
- `app/src/main/res/drawable-nodpi/obstacle_piko_rope_spool.png` — `20d1d76e83f601d4181337ee9c3c57f53bdbf67b`
- `app/src/main/res/drawable-nodpi/obstacle_piko_toolbox.png` — `97377dc4399e0b5ca9f60f1c60b5e2ab1941d10a`
- `app/src/main/res/drawable-nodpi/obstacle_popo_harbor_crate.png` — `353d07d4a6c5a7d7c6c53856bd2f53c6b9662cce`
- `app/src/main/res/drawable-nodpi/obstacle_popo_shell_basket.png` — `abd1338246dd1f89f0334a65338452dd9be0d5de`
- `app/src/main/res/drawable-nodpi/obstacle_popo_snow_rock.png` — `24e72143e5f679cee15925bcf39180186b6c612b`
- `app/src/main/res/drawable/auth_popup_background.xml` — `fd3652a79c8a7bf9966d830b563c0cd88393f72e`
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — `57a8f03cc3036014768f2d3aaeb29623f2f120a2`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — `a8a8fa5518fcdb2db0385028800968b2667af71e`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` — `a8a8fa5518fcdb2db0385028800968b2667af71e`
- `app/src/main/res/values/ic_launcher_colors.xml` — `022770b9be503c540200a8b4ab2911de217d5f1b`
- `app/src/main/res/values/opening_strings.xml` — `02a840d90520f54760a0a49bb2d7be4e0fcfa781`
- `app/src/main/res/values/talk_strings.xml` — `207aee44ebeeef85870d54283aaa5b7b13ede6b6`
- `app/src/main/res/values/tutorial_strings.xml` — `d4dfd29e8ba2eaaafe3bd3e1d29c5552d7a8f6ad`
- `app/src/test/java/com/tinyrunners/school/game/TutorialFlowTest.kt` — `6c8ce09cc2dd1a8fef4da373c866b0cd38bf33b4`
- `app/src/test/java/com/tinyrunners/school/story/StoryTest.kt` — `e21ea1fc428827c5986e6c2c29dba8341c6081b8`
- `app/src/test/java/com/tinyrunners/school/talk/TalkTest.kt` — `85144c02b0fdb1b13d164d789a46569890831340`
- `docs/IDEA_GUIDE.md` — `c394dc0ac22d9d684f46639ea53cf04131afce76`
- `docs/INSTALL_GUIDE.md` — `f5995e4938a53f43c1fac778b2ebbdb79e28723c`
- `docs/OPENING_SPEC.md` — `225c135fa927e67678abbd39555d809f61446576`

사이트의 스프라이트 복사본 6개는 아래 원격 앱 경로와 동일하다. 기존 자산을 다시 제작하거나 별도 아트 묶음으로 중복 게시할 필요가 없다. 사이트 자체 배포용 상대 경로 유지 여부는 웹 묶음에서 결정한다.

- `website/dist/assets/cheese.png` ↔ `app/src/main/res/drawable-nodpi/sprite_cheese_idle_0.png` — `9e93881bebc15255b354b7a811bfbfba2d28252a`
- `website/dist/assets/lumi.png` ↔ `app/src/main/res/drawable-nodpi/sprite_lumi_idle_0.png` — `ad6f0c646c00303482a8deb2de37d6a4761e18cf`
- `website/dist/assets/mint.png` ↔ `app/src/main/res/drawable-nodpi/sprite_mint_idle_0.png` — `a8b5668dac79aa58f4a2c1c70745abb706de9223`
- `website/dist/assets/nunu.png` ↔ `app/src/main/res/drawable-nodpi/sprite_nunu_idle_0.png` — `d0fbefbe1d4f610001128c72cbcbfc717c4cd94e`
- `website/dist/assets/piko.png` ↔ `app/src/main/res/drawable-nodpi/sprite_piko_idle_0.png` — `f6d329a1a407203126ad43e9b07d31bca9c319e6`
- `website/dist/assets/popo.png` ↔ `app/src/main/res/drawable-nodpi/sprite_popo_idle_0.png` — `d7b194c0336c602a6543e45e2a12b98560d0fb20`

## 11. 남은 확인

현재 소개 변경 포함 Android 빌드/단위테스트/Lint 재실행, 실제 기기 도감 상세 인수, 음악 청음·매핑·원본/시험본 보관 결정, 사이트 운영 화면/모바일 검수와 배포 아카이브 내용 대조, 외부 참조 자료 확보가 남아 있다. 모든 제작물 업로드 완료로 보고할 수 없다. main이 계속 갱신되므로 실제 게시 직전에 다시 fetch하고 이 보고서 기준 커밋과의 차이를 확인해야 한다.

