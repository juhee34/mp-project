# A팀 보류 제작물 게시 검토 — 2026-11-11

## 실제 원격 기준과 보존

- 원래 `codex/character-screens` 사본과 빈 staged 인덱스를 보존하고 최신 main에서 별도 `codex/a-content-release` 작업 사본을 만들었다. 원래 로컬 파일을 덮어쓰거나 staged 해제하지 않았다.
- 최초 main `79a5c13`, 게시 직전 새 main `892ee41f7ddfa01a0dc454ffc3eb4e288bc74333`까지 fetch하고 fast-forward했다. B팀의 가시/발판·코인·대사 변경을 그대로 보존했다. A 커밋으로 게임·공용 계정·거리·오디오·저장 코드는 수정하지 않았다.
- 배경 c95614f/e385c8c 및 fa3f8cc는 원격 브랜치에 있고 [PR #3](https://github.com/juhee34/mp-project/pull/3)이 main에 병합됐다. 과거 업로드 실패 기록은 현재 사실로 사용하지 않는다.
- 파일별 대조: `PUBLICATION_FILE_MAP_20261111.json`. 87개 보류 후보와 ignored 과거 APK를 추가 조사했다. 원본 경로·크기·SHA-256·동일 원격 경로·처리를 기록했다. 바뀐 내용은 승인 인사말 병합, 최신 기능 상태, 개인 경로 제거, 중복 이미지 경로 재사용이다.
- 이전 전수 감사 `GIT_UPLOAD_AUDIT.md`는 과거 기록으로 보존한다. 기획·205편 연구·캐릭터·스프라이트·장애물·이야기·도감·144개 대사 원본은 main의 실제 파일과 대조했으며, 더 오래된 로컬 파일을 최신 main 위에 덮어쓰지 않았다. 원래 PROJECT_STATE의 미게시 A 기록은 `LOCAL_WORK_HISTORY_20261111.md`에 별도 보존했다.

## 항목별 처리

| 항목 | 파일 | 이번 처리 | main/앱/사이트 | 남은 담당 |
| --- | --- | --- | --- | --- |
| 선택창 | CharacterScreens.kt, character_screen_strings.xml | 승인 인사 6개·선택 안내 그대로, 전용 문자열 및 접근성 설명 연결. B의 넘김 카드·시작 연결 유지 | 이번 A 브랜치 코드, main 미반영 | B: 작은 화면·큰 글자·실제 선택/시작 검수 |
| 앱 소개 | intro_strings.xml, docs/intro/, INTRO_SCREEN_HANDOFF.md | 기존 키 유지, 2단 점프·가시 막대 아래 통과·발판·정지/진행·대화 건너뛰기·튜토리얼·집/길 안내 | 코드 반영, 새 APK 빌드 성공; main 미반영 | B: 기기 화면/동선 인수 |
| 발표 소개서 | docs/PROJECT_INTRO.md, website/dist/TINY_RUNNERS_INTRO.md | 원본/다운로드 사본 바이트 일치, 요구한 6단 구성 유지 | 문서 게시; main 미반영 | A/발표자: 실제 시연 APK 확인 |
| 소개 사이트 | website/ 전체 21파일 | 기존 디자인과 ID 유지, 최신 안내·상태, 모바일 줄바꿈 보완 | Git 소스 게시, 사이트 재배포 미완료 | 게시 담당: 기존 사이트 인증 승인 후 같은 ID로 배포 |
| 검토 첫 대화 | docs/dialogue/OPENING_REVIEW.md, talks.reviewed.json | 6명×6줄·프롤로그 6장 검토본 보존 | 문서만, 앱 talks.json 교체 안 함 | 사용자/A 문안 확인, B 연결/검수 |
| 반복 배경 | docs/art/running-loops/, running-loop-source/, app/assets/backgrounds/running/; 새 역사 검수 문서 | 이미 main인 PNG/코드 중복 게시 없음. 과거 집→길→집 비교판은 최종 동작 아님을 표시 | PR #3 main 반영. B 6명 에뮬레이터 기록 있음 | A/B: 누누 작은 돌·수풀 이음매, B 다른 화면 비율 |
| 음악 | docs/audio/, 음악 스크립트 3개, 후보 ZIP 2개 | 측정·제작/연결 안내는 Git, WAV 14개는 기존 ZIP으로 보관 | 후보, 최종 승인/앱 연결 없음 | A 청음·선정·출처, B 승인 후 재생 통합 |
| 기타 | 감사/게시 프롬프트·LOCAL_WORK_HISTORY·아트 검수 | 과거 실패/완료와 현재 원격 상태 분리, 개인 경로 제거 | 문서 게시 | 아래 제외물 및 보류사항 참고 |

144개 상황 대사는 현재 B main 892ee41에서 4상황(select/start/retry/stumble, 72개)을 사용한다. clear/home/story_unread/story_reread는 미연결이다. 144개 전체 데이터가 앱에 복사됐다는 것과 전 상황이 노출된다는 것은 다르다. A의 첫 대화 수정안은 별도 문서다.

## 이번 실제 검사

- `node scripts/validate-content.cjs` 통과: 6명, 205 연구, 스프라이트, 이야기/자산 참조, 첫 대화, 앱 상황 대사 데이터, 장애물 18개, 배경 높이/바닥/해시.
- `python scripts/validate-publication.py`: JSON/XML 파싱, 문자열 키 중복, 확정 6인사/안내, 소개서·XML 검토 사본 일치, 첫 대화 구조·화자·순서, 웹 ready 이야기 12개와 본문/페이지 원본 일치, 링크/앵커/자산·JS 구문, Python AST, 개인 경로 누출 검사.
- ZIP 2개 CRC 및 내부 18항목(후보 WAV 14개+설명/측정값)의 SHA-256을 로컬 파일과 대조했다. 음원 청음이나 재생성은 하지 않았다.
- `:app:assembleDebug --offline --console=plain -Pkotlin.compiler.execution.strategy=in-process` 통과. JDK 17, SDK 35. 최신 B main 반영 후 재빌드 42초/33작업. 환경의 오래된 ANDROID_SDK_ROOT 경고가 있었지만 실제 ANDROID_HOME SDK로 성공했다. 개인 환경 설정은 커밋하지 않았다.
- 이번 단위 테스트·Lint·앱 설치/기기 검수는 수행하지 않았다. main의 B 기록(92테스트·lint·에뮬레이터)은 B의 검증이며 이번 A 통합본 검증과 구분한다. 개인 계정/앱 데이터 변경 없음.
- 웹 Chrome 1365×900 및 375×812: 한글/선택/이야기/이미지/내부 링크 확인, 6명 첫 이야기 창·피코 배경 이야기 열기/닫기, 수평 넘침·잘림·누락 이미지 없음, 콘솔 오류 없음. 마지막 B 업데이트에 따른 문안 조정 후에는 정적 재검사만 수행했다. 실제 모바일 기기/큰 글자·보조기술 검수는 미실시.
- 소개서 다운로드 버튼은 브라우저 파일 다운로드 권한 거절로 완료하지 못했다. 다른 방법으로 우회 다운로드하지 않았다. 링크 대상의 소스 파일 존재와 원본 일치는 별도 정적 검사다.
- `git diff --check` 및 각 커밋 staged 공백 검사 통과.

## 사이트 배포는 별도 미완료

주소: https://tiny-runners-friends-20261110.auroraduck1.chatgpt.site

기존 project_id `appgprj_6af3152718e48191a65b4ac23c1789f8`, 소유자 전용/ChatGPT 로그인 범위를 유지했다. 기존 사이트와 소스 v1 `341c70cd36c36ebb0e64828540f1cb93384e20fd`를 확인하고 전용 소스 사본을 열었다. 갱신된 게시 인증을 전달하는 실행 승인이 거절되어 push/package/deploy를 수행하지 못했다. GitHub 업로드가 사이트 배포 성공을 뜻하지 않는다. 기존 사이트에 수정본이 표시된다고 보고하지 않는다.

후속: 정상 승인 환경에서 이 브랜치의 website/를 기존 사이트 소스에 반영 → 검사/패키징 → 같은 ID/같은 공개 범위로 배포 → 다운로드/최종 문안 화면 확인. 새 사이트를 만들지 않는다. 인증정보는 Git·문서·파일에 기록하지 않았다.

## Git 밖에 보존하는 자료와 이유

- 음악 ZIP 2개: `docs/audio/Tiny_Runners_6_music_edits.zip`, `docs/audio/Puddle_Jumpers_edit_drafts.zip`. WAV와 ZIP을 동시에 Git에 넣지 않는다. 해시·크기·내부 목록은 `docs/audio/ARCHIVE_MANIFEST.json`. 검토용 prerelease 게시 결과는 아래 게시 실적에 기록한다.
- WAV 14개: 원래 `docs/audio/all-music-drafts/*.wav` 12개 및 `docs/audio/piko-drafts/*.wav` 2개. ZIP과 동일하므로 Git 중복 제외; 원본 로컬 유지.
- 배경 `docs/art/loop-review-20261111/final/TinyRunners-running-backgrounds.zip`, `running-background-loops.bundle`: 이미 main에 있는 작업의 복구 묶음이라 중복 업로드 제외, 로컬 유지. 파일별 해시는 FILE_MAP에 있다.
- 같은 final 폴더의 `TinyRunners-running-backgrounds.apk`: 과거 별도 서명 테스트 APK, 최신 공식 앱으로 오해할 수 있어 업로드하지 않고 로컬 보존. 기존 공식 앱 릴리스 `v1.0.0`을 교체하지 않는다. B가 검수한 최신 APK를 별도 릴리스할 일이다.
- 중복 배경 원본 3개·출발/루프 PNG 12개·preview/gallery/export-inputs 등: FILE_MAP의 same 경로에 동일 SHA-256으로 이미 main 게시. 새 검수 HTML/스크립트는 canonical 원본을 참조한다. 새 seams.png·joins.png만 추가 보존했다.
- 외부 다운로드 폴더의 제작 원본 MP3/MP4 각 6개: 프로젝트 밖 원본, 분석 JSON에 파일명/해시가 있으며 로컬 유지. 후보 ZIP에는 원본 MP3/MP4가 들어 있지 않다. 재제작과 이용 조건 확인은 제작 담당에게 인계한다.
- `.tools/`, `.kotlin/`, 빌드 APK/캐시, local.properties, Firebase 개인 설정, 인증 파일·키·비밀 파일 제외. 배포 임시 사본 및 토큰을 Git에 추가하지 않았다.

## 게시 실적

작업 브랜치 `codex/a-content-release`. 이번 변경의 main 병합은 하지 않는다. 원격 게시의 실제 완료 결과는 아래와 같다.

- GitHub push 성공: `b412a81` 선택·소개 / `2999b20` 웹사이트 / `492c1c0` 대사·아트·음악 인계 및 감사. 원격 서버의 새 브랜치 생성과 push 완료 응답 확인.
- PR: https://github.com/juhee34/mp-project/pull/4 (base main, head codex/a-content-release). main 직접 push·병합·강제 push 없음. 이번 A 변경은 PR 검토/병합 대기다.
- 음악 ZIP 2개 업로드 명령 성공(종료 0): https://github.com/juhee34/mp-project/releases/tag/a-content-review-20261111 . 검토용 prerelease이며 latest/기존 공식 APK 릴리스를 교체하지 않았다. 후보 청음·선정/앱 연결과는 별개다.
- PR/릴리스 추가 읽기 조회는 실행 승인 거절로 수행하지 못했다. push/PR 생성/릴리스 업로드 성공 응답은 확보했지만 게시 이후 원격 파일의 재다운로드 해시 대조는 하지 않았다. 승인 가능한 환경에서 팀원이 PR 파일 목록과 릴리스 자산을 확인한다.

## 작업 중 별도로 생긴 자료 — 이번 게시 제외

마지막 원래 사본 점검에서 시작 시점에는 없던 다음 4파일이 발견됐다. 이 게시 작업이 만든 파일이 아니며 사용자 검토 대기/앱 미연결로 기록된 동시 진행 작업이므로 임의로 묶어 게시하지 않았다.

- `docs/STORY_EXPANSION_01.md`
- `docs/STORY_EXPANSION_02.md`
- `docs/dialogue/short-lines-expansion-01.json`
- `docs/dialogue/short-lines-expansion-02.json`

원래 PROJECT_STATE에 라일·치즈 및 포포·루미 확장 기록이 추가됐다. 시작 시 315파일 중 314개 SHA-256은 그대로이며 PROJECT_STATE만 외부 작업으로 바뀌었다. 원래 staged 인덱스는 계속 비어 있다. 새 기록을 되돌리거나 이번 상태 파일로 덮어쓰지 않았다. 원고/추가 대사 검토·앱 반영/게시 여부는 해당 제작 담당이 정리할 후속 작업이다. 따라서 프로젝트의 모든 로컬 자료가 업로드됐다고 단정하지 않는다.
