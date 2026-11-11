# 확정 이야기 확장분 게시 인계

2026-11-11 사용자 문안 승인. 앱 미연결.

## 게시 파일

- docs/STORY_EXPANSION_01.md
- docs/STORY_EXPANSION_02.md
- docs/STORY_EXPANSION_03.md
- docs/dialogue/short-lines-expansion-01.json
- docs/dialogue/short-lines-expansion-02.json
- docs/dialogue/short-lines-expansion-03.json
- 이 문서 및 이번 작업에 해당하는 PROJECT_STATE.md 기록

6명 각각 두 번째 에피소드 본문, 추가 상황 대사 96개(기존 144개와 합쳐 240개). 사용자 승인 상태이며 원고의 과거 검토 대기 표기는 마지막 승인 기록이 대체한다. integrated=false 유지. 제3편은 개요 그대로다.

## 게시 방법

최신 origin/main 기준 작업 브랜치에서 위 파일만 반영한다. 다른 미게시 소개 화면·웹사이트·음악·배경 작업을 무조건 함께 stage하지 않는다. 기존 144개 대사 및 새 96개의 ID/문구 중복 없음, 48자 제한, 6명×8상황×5대안을 검증한다. 기존 콘텐츠 검사와 git diff --check도 실행한다. 커밋 후 원격 브랜치로 push하고 해시 일치를 확인한다. main 직접 푸시/강제 푸시하지 않는다.

권장 브랜치: codex/story-expansion-episodes-2

권장 커밋: Add approved second episodes and 96 character dialogue variants

## 앱 연결은 별도

stories.json을 단독 수정하지 않는다. 기존 생성기가 확장 원고를 읽도록 연결한 뒤 기존 episode_2 ID를 유지하며 페이지/본문/요약을 갱신해야 한다. 해금 거리와 읽음 저장 정책은 이번 게시에서 변경하지 않는다. 96개 대사는 기존 HANDOFF.md의 트리거 조건에 맞춰 개발 담당이 연결한다. 실제 미구현 clear/home는 예약 콘텐츠다. 확정된 선택창 고정 인사를 임의로 대체하지 않는다.

## 이번 게시 시도 결과

GitHub 원격 조회는 연결 reset, 별도 checkout 준비는 Git config 쓰기 및 MSYS sh 실행 권한 오류로 실패했다. 커밋과 원격 업로드는 아직 완료되지 않았다. 기존 작업 폴더를 초기화하거나 팀원 변경을 덮어쓰지 않았다.

## 후속 게시 검토 — 2026-11-11

위 실패 기록은 이전 환경의 결과다. 후속 작업에서는 최신 main `157831a`를 가져와 `codex/story-expansion-episodes-2` 전용 사본을 준비했다. 원고 3개와 대사 JSON 3개는 ZIP 원본 바이트를 유지했고, 이 문서/해당 PROJECT_STATE 기록만 추가했다.

ZIP CRC와 원본 해시, JSON 구조, 144+96=240개 ID/문구 중복 없음, 6명×8상황×5개, 48 코드포인트 제한, 읽음 조건 빈 배열 및 기존 episode_2 ID/제3편 개요 유지 검사를 통과했다. 기존 콘텐츠 검사와 커밋 공백 검사도 게시 전에 수행한다. 앱 연결/생성기/해금/저장 정책 변경 및 Android 실행 검증은 이번 게시 범위가 아니다. 원격 push 결과와 PR은 후속 게시 기록에 구분한다.

## 게시 완료 — 2026-11-11
- 콘텐츠 커밋: 70efecfe1428f0ba37ac3613c93d839b23502a2f. origin의 codex/story-expansion-episodes-2와 로컬 해시 일치 확인.
- PR: https://github.com/juhee34/mp-project/pull/5 . main 직접 push/병합 및 강제 push 없음. main 반영은 팀원 검토 대기.
- node scripts/validate-content.cjs 및 staged/전체 diff 공백 검사 통과. 추가 96개와 기존 144개 합계 240개, 6명×8상황×5개, ID/문구 중복 없음, 최대 25 코드포인트, 읽음 조건 빈 배열 확인.
- 원고 3개·JSON 3개 원문 유지. 총 8파일만 게시하며 ZIP 자체/중복 풀기 폴더/다른 A·B 작업은 포함하지 않았다. 앱 연결·빌드·테스트·기기 검수는 수행하지 않았다. 사용자 승인/integrated=false 유지.
