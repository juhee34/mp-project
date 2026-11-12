# 납품 상태

집은 첫 출발에 한 번만 표시하고, 이후에는 집 없는 달리는 길만 반복합니다. 재시작하면 집으로 돌아옵니다.

- 6명 × 출발/반복 배경 12장, 게임 연결 코드, 검수용 preview.html 포함.
- assembleDebug, testDebugUnitTest, lintDebug 통과. 단위 테스트 77개 실패/오류 0. Lint 오류 0, 경고 44.
- 최종 APK에 12장 및 manifest/assets.json이 납품 파일과 동일하게 포함된 것을 바이트 비교로 확인.
- 실제 Android 기기 화면 검수는 미완료. 에뮬레이터 연결을 확보하지 못했습니다. 정적 원화·반복 비교판 검수와 코드 검증만 완료했습니다.
- 커밋 c95614f, 브랜치 codex/running-background-loops. GitHub push는 인증 실행 실패로 미완료이며 main에 반영되지 않았습니다.

## 업로드 담당 코덱스
기존 mp-project의 다른 작업을 보존하고, 제공된 running-background-loops.bundle에서 커밋을 가져와 검토하세요. 현재 main과 변경을 비교하고 충돌을 해결한 뒤 검증하여 codex/running-background-loops 브랜치로 push하세요. 강제 push는 하지 마세요. 원격 업로드 성공 및 원격 커밋을 확인한 뒤 알려 주세요. main 통합은 팀의 현재 진행 변경과 함께 검토하세요.

ZIP에는 앱에 덮어쓸 파일뿐 아니라 원본과 기술 내보내기 스크립트도 포함합니다. 전체를 무조건 덮어쓰지 말고 저장소 변경으로 비교하여 반영하세요. APK는 별도 테스트 서명으로 빌드했으므로 기존 설치본과 서명이 다를 수 있습니다. 기존 앱의 데이터 삭제를 요구하는 설치를 임의 진행하지 마세요.
