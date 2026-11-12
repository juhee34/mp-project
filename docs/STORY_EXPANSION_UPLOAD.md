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
