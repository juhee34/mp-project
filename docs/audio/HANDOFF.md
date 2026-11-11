# 음악 후보 자료 인계 — 2026-11-11

음악 확정본은 없다. 이 자료는 기술 분석·반복 구간 편집 후보이며 `integrated=false`다. 제목에 loop가 있어도 청음 승인이나 실제 Android 반복 재생 검수를 뜻하지 않는다. 앱의 음악 planned/null 및 오디오 코드는 변경하지 않았다.

- `REVIEW.md`, `metadata.json`, `piko-mp3-measurements.json`: 원본 분석과 한계.
- `all-music-drafts/`: 6명 × loop/tail-fix WAV 후보 12개에 대한 측정값·설명. progress.json은 중간 진행 기록이며 최종 품질 판정이 아니다.
- `piko-drafts/`: 먼저 만든 피코 후보 2개와 측정값·설명. 6명 묶음과 별도 버전이므로 둘 다 보존.
- 실제 WAV는 Git에 중복 추가하지 않고 기존 ZIP 2개를 GitHub 검토용 prerelease `a-content-review-20261111`의 자산으로 보관한다. 업로드 실적은 `../PENDING_DELIVERABLES_20261111.md`를 확인한다. ZIP 내부 WAV 14개와 로컬 원본을 SHA-256으로 대조했다. 파일별 해시와 크기는 `ARCHIVE_MANIFEST.json`.
- 기존 원본 MP3 6개·MP4 6개는 사용자 다운로드 폴더에 보존되어 있다. 이 게시물에 원본 파일까지 포함되지는 않으며 파일명·측정값·해시는 분석 JSON에 있다. 재편집할 B팀은 제작 담당에게 해당 원본과 제작 이용 조건을 받아야 한다. 로컬 PCM 임시 디코딩 파일은 캐시로 제외한다.

## 제작 및 연결

`scripts/inspect-music-metadata.py`, `scripts/build-music-review-batch.py`, `scripts/build-piko-audio-drafts.py`를 보존한다. 입력은 명령행 인자로 본인의 원본 경로를 전달하며 개인 경로를 저장하지 않는다. Windows ACM 기반 스크립트는 Windows가 필요하다. 이번 게시에서는 구문만 검사했으며 음원을 다시 생성하거나 청음하지 않았다.

A: 캐릭터별 분위기·악기·원본 출처와 이용 조건 확인, 후보 청음 및 최종 선택. B: 최종 승인 후 형식/자산 경로 결정, 음량·루프·일시정지·재개·설정 및 실제 기기 검수. 후보를 임의로 최종곡으로 연결하지 않는다.
