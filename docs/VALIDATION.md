# 검증 기록

2026-11-09 기준.

- 콘텐츠 검사 통과: 캐릭터 6명, 고유 ID, 스토리·배경·음악 참조, 미제작 자산의 planned 표시, 조사 자료 205개, 역할별 프롬프트 20개.
- Android XML 파일 구문 검사와 PowerShell 스크립트 구문 검사 통과.
- Android 빌드·에뮬레이터 실행·게임 플레이 테스트는 미실시. 현재 환경에 유효한 Android SDK가 없고 Gradle Wrapper 다운로드도 네트워크 문제로 실패했다.
- Wrapper JAR는 포함되지 않는다. 인터넷이 연결된 개발 PC에서 JDK 17을 지정하고 scripts/bootstrap-gradle.ps1을 실행해야 한다.
- 캐릭터 그림·배경 이미지·음악 파일은 미제작이다. JSON에 기획과 제작 지시문이 들어 있다.
- 현재 화면은 소개 → 6명 선택 → 프로필이며 달리기는 아직 구현하지 않았다.

## GitHub 업로드

준비 당시 GitHub CLI의 기존 인증이 유효하지 않아 원격 저장소 생성과 업로드를 완료하지 못했다.
로그인 후 프로젝트 루트에서 다음 명령으로 새 비공개 저장소를 만들고 업로드할 수 있다.

```powershell
gh auth login
powershell -File scripts/publish-github.ps1
```

ZIP으로 받은 경우 Git 이력은 포함되지 않으므로 먼저 다음 명령을 실행한다.

```powershell
git init -b main
git add .
git commit -m "Initial Android starter"
```

기존 같은 이름의 저장소가 있다면 덮어쓰지 말고 `-RepositoryName tiny-runners-school`처럼 새 이름을 지정한다.
