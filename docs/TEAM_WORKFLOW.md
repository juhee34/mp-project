# 함께 고치고 만들고 공유하는 방법 (팀 작업 안내)

두 사람이 같은 Tiny Runners 코드를 동시에 고치고, 합치고, 다른 사람에게 새 버전을 나눠 주는 방법이다.
처음 보는 사람도 순서대로 따라 할 수 있게 적었다. 막히면 "8. 문제가 생겼을 때"를 먼저 본다.

## 0. 한눈에 보기

```
  [A의 PC]                      [GitHub: juhee34/mp-project]                   [B의 PC]
  Pull ◀───────────────────────  main (항상 빌드되는 최신본)  ───────────────────▶ Pull
  브랜치에서 작업                       ▲          │                              브랜치에서 작업
  Commit → Push ──▶ 내 브랜치 ──▶ Pull Request ──┘          └──▶ Releases (APK 링크) ──▶ 친구·교수님 휴대폰
                                  (상대가 확인 후 Merge)
```

- **GitHub 저장소** = 두 사람이 함께 쓰는 코드 보관함. 소유자 `juhee34`, 두 계정 모두 쓰기 권한이 있다.
- **main 브랜치** = 언제 받아도 빌드되고 실행되는 최신본. 여기에 직접 올리는 대신 브랜치 + PR로 합친다.
- **APK / Releases** = 코드를 고칠 수 없는 "완성된 앱 파일". 다른 사람에게 나눠 줄 때만 쓴다.

## 1. 역할 (누가 어떤 파일을 고치나)

같은 파일을 동시에 고치면 충돌이 난다. 기본 담당을 나눠 두고, 상대 담당 파일을 고쳐야 하면 먼저 말한다.

| 담당 | 주로 고치는 곳 |
| --- | --- |
| **A (콘텐츠·화면)** | `app/src/main/assets/content/`(characters.json, stories.json, assets.json), `app/src/main/res/drawable-nodpi/`(그림), `characters/`(선택·프로필 화면), `docs/CHARACTER_*`, `docs/ART_DIRECTION.md` |
| **B (게임·통합)** | `game/`(러너, 안내), `auth/`·`opening/`·`progress/`·`result/`·`records/`·`settings/`·`story/`·`weather/`·`ui/`, `build.gradle.kts`, `AndroidManifest.xml` |
| **함께 합의** | `docs/DATA_CONTRACT.md`, `scripts/validate-content.cjs`, `MainActivity.kt`, `PROJECT_STATE.md`(각자 자기 기록만 추가) |

- 데이터 형식(필드 이름·구조)을 바꿀 때는 DATA_CONTRACT.md와 검사기를 **같이** 고친다.
- `PROJECT_STATE.md`는 둘 다 고치는 파일이라 충돌이 잘 난다. 맨 위 쪽 새 섹션에 자기 작업만 짧게 추가한다.

## 2. 처음 한 번 준비 (각자 PC에서)

1. **Android Studio**를 설치한다(이미 있으면 생략).
2. **저장소 받기**
   - 처음이면: 시작 화면 **Clone Repository** → URL `https://github.com/juhee34/mp-project.git` → 폴더 선택 → Clone
   - 이미 받아 둔 폴더가 있으면: 그 폴더를 Open → 위쪽 브랜치 이름 → **main** → Checkout → **Git → Pull**
   - 반드시 `settings.gradle.kts`가 있는 최상위 `mp-project` 폴더를 연다(`app`이나 `gradle` 폴더가 아니다).
3. **Gradle JDK를 17로**: Settings(Ctrl+Alt+S) → Build, Execution, Deployment → Build Tools → **Gradle** → Gradle JDK → Download JDK → **17 / Eclipse Temurin** → OK.
   기본 Java 25로는 이 프로젝트(Gradle 8.9)가 빌드되지 않는다.
4. **Firebase 설정 파일 넣기**: `app/google-services.json`은 Git에 올리지 않는다(.gitignore). 팀원에게 카톡 등으로 받아서 자기 `app` 폴더에 넣는다. 또는 Firebase 콘솔에서 프로젝트 멤버로 초대받아 직접 내려받는다(docs/FIREBASE_SETUP.md 6단계).
   - 이 파일이 없으면 로그인 대신 "설정 전 임시로 둘러보기"만 된다.
5. **File → Sync Project with Gradle Files** → 위쪽 초록 **▶ Run**으로 실행된다면 준비 끝.
6. Git 이름 확인: Settings → Version Control → Git에서 커밋할 때 자기 GitHub 계정으로 올라가는지 본다(처음 Push 때 GitHub 로그인 창이 뜨면 로그인한다).

AI 도우미를 쓴다면: B는 Claude Code, A는 Codex/ChatGPT를 쓴다. 어떤 도구를 쓰든 아래 순서(Pull → 브랜치 → 커밋 → PR)는 같다. AI에게 작업을 맡길 때는 "main에서 Pull하고 새 브랜치에서 작업한 뒤 PR로 올려줘"처럼 말한다.

## 3. 매번 작업하는 순서 (가장 중요)

### ① 시작 전: 최신본 받기
- 위쪽 브랜치를 **main**으로 바꾸고 **Git → Pull**.
- 상대가 올린 내용이 내려온다. 이걸 건너뛰면 오래된 코드 위에 작업하게 되어 충돌이 커진다.

### ② 내 작업용 브랜치 만들기
- 위쪽 브랜치 이름 → **New Branch** → 이름을 정한다.
- 이름 규칙: `feature/하는일` (기능), `fix/고칠것` (버그), `content/내용` (대사·데이터), `art/그림`
  - 예: `content/six-stories`, `feature/background-art`, `fix/login-popup-scroll`
- 한 브랜치에는 한 가지 일만 담는다.

### ③ 고치고 확인하기
- 고친 뒤 **▶ Run**으로 실제로 실행해 본다.
- 데이터(JSON)를 고쳤다면 Terminal에서 `node scripts/validate-content.cjs` → `PASS`가 나와야 한다.
- 코드를 고쳤다면 Terminal에서 `.\gradlew.bat :app:testDebugUnitTest` → `BUILD SUCCESSFUL`이 나와야 한다.

### ④ 커밋하기 (저장 기록 남기기)
- **Git → Commit**(Ctrl+K) → 바꾼 파일에 체크 → 메시지를 적는다 → **Commit**.
- 메시지는 "무엇을 왜"를 한 줄로: 예) `포포·루미 첫 이야기 대사 추가`, `로그인 팝업 스크롤 안 되던 문제 수정`
- 기능 하나가 끝날 때마다 작게 자주 커밋한다.
- `google-services.json`, `local.properties`, `.apk` 파일이 목록에 보이면 체크하지 않는다(보통 자동으로 빠진다).

### ⑤ 올리기 (Push)
- **Git → Push**(Ctrl+Shift+K) → Push. 처음이면 GitHub 로그인 창이 뜬다.

### ⑥ Pull Request(PR) 만들기
1. GitHub 저장소 페이지에 들어가면 위쪽에 노란 띠로 **"Compare & pull request"** 버튼이 뜬다. 누른다.
   (안 뜨면 **Pull requests** 탭 → **New pull request** → base: `main`, compare: 내 브랜치)
2. 제목: 무엇을 했는지. 설명: 바꾼 것, 확인한 방법(Run/검사 결과), 상대가 봐 줬으면 하는 것.
3. **Create pull request**.
4. 카톡으로 "PR 올렸어, 봐 줘"라고 알린다.

### ⑦ 상대가 확인하고 합치기 (Merge)
- 상대는 PR의 **Files changed** 탭에서 바뀐 내용을 본다. 궁금한 줄에 댓글을 남길 수 있다.
- 직접 실행해 보고 싶으면: Android Studio에서 그 브랜치로 Checkout → Pull → ▶ Run.
- 괜찮으면 PR 화면 아래 **Merge pull request → Confirm merge**.
- 합친 뒤 **Delete branch**로 브랜치를 정리해도 된다.
- 마지막으로 **두 사람 모두 main으로 돌아와 Pull** → 다음 작업은 ①부터.

> 아주 작은 수정(오타, 문구 한 줄)은 서로 합의했다면 main에 바로 커밋·Push해도 된다. 그때도 Push 전에 꼭 Pull한다.

## 4. 동시에 일할 때 충돌을 줄이는 법

- **작업 시작 전에 카톡으로 "나 지금 OO 파일 고친다"** 한 줄 남긴다.
- **PR은 오래 묵히지 않는다.** 하루 안에 확인·합치기. 오래 갈수록 main과 멀어져 충돌이 커진다.
- 내 브랜치가 오래됐으면 중간에 main을 받아 온다: 브랜치에 있는 상태에서 **Git → Pull**이 아니라, 위쪽 브랜치 메뉴 → `main` 옆 **⋮ → Merge 'main' into '내 브랜치'**.
- JSON 파일은 한 줄만 달라도 충돌이 나기 쉽다. 대사(stories.json)는 A, 캐릭터 필드 추가는 합의 후 한 명이 한다.

### 충돌(conflict)이 났을 때
1. Android Studio가 **Conflicts** 창을 띄운다. 파일을 더블클릭 → 왼쪽(내 것) / 오른쪽(상대 것) / 가운데(결과)가 보인다.
2. 둘 다 필요한 내용이면 양쪽 화살표(»)를 모두 눌러 가운데에 합친다. 하나만 맞으면 그쪽만 남긴다.
3. **Apply** → 실행해서 확인 → 커밋.
4. 모르겠으면 멈추고 상대와 화면 공유. 억지로 한쪽을 지우지 않는다.

## 5. 새 버전을 다른 사람에게 공유하기 (APK 링크)

코드를 고쳐도 다른 사람 휴대폰에는 **새 APK를 올려야** 반영된다.

1. **APK는 한 사람(B의 PC)만 만든다.** 같은 PC에서 만든 APK끼리만 기존 앱 위에 업데이트된다. 다른 PC에서 만들면 받는 사람이 앱을 지우고 다시 설치해야 한다(서버의 계정 기록은 그대로).
2. 만드는 사람: main을 Pull → `app/build.gradle.kts`의 `versionCode`를 1 올리고 `versionName`을 바꾼다(예: 2 → 3, `1.0.0` → `1.0.1`) → 커밋·Push.
3. Terminal: `.\gradlew.bat :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`를 `dist/TinyRunners-v1.0.1.apk`로 복사.
4. GitHub → **Releases → Draft a new release** → Tag에 `v1.0.1` 입력 후 Create new tag → 제목·설명 → 아래 점선 칸에 APK를 끌어다 놓기 → **Publish release**.
5. 링크: `https://github.com/juhee34/mp-project/releases/download/v1.0.1/TinyRunners-v1.0.1.apk`
   설치 안내 문구는 docs/INSTALL_GUIDE.md에 있다(링크의 버전만 바꿔서 보낸다).

## 6. 일정과 약속 (마감 2026-11-23 23:59)

| 시점 | 약속 |
| --- | --- |
| 매일 작업 시작 | main Pull, 오늘 고칠 파일 카톡 공유 |
| 기능 하나 끝날 때 | 커밋 → Push → PR → 상대에게 알림 |
| PR 받은 날 | 그날 안에 확인·Merge |
| 11/20 | 기능 동결. 이후엔 버그 수정만, 보고서·발표 준비 |
| 11/21~22 | 최종 APK v1.x 릴리스, 실제 휴대폰 2대 이상 설치 확인 |
| 11/23 | 보고서 + 코드(저장소 링크 또는 압축) 제출 |

- 커밋 기록은 각자 기여한 내용의 증거가 된다(과제 안내: 무임승차 확인 시 커밋 기록 사용). 자기 계정으로 커밋한다.

## 7. 하지 않을 것

- `google-services.json`, `local.properties`, 서명 키, 비밀번호를 커밋하거나 단톡방·공개 게시판에 올리지 않는다(설정 파일은 팀원에게 개인 메시지로만).
- 상대 브랜치에 말없이 Push하지 않는다.
- `git push --force`나 Android Studio의 **Force Push**를 쓰지 않는다. 상대 작업이 사라질 수 있다.
- 빌드가 안 되는 상태로 main에 합치지 않는다.
- AGP 업그레이드 알림(“Project update recommended”)을 누르지 않는다. 버전이 바뀌면 상대 PC에서 빌드가 깨진다.

## 8. 문제가 생겼을 때

| 증상 | 해결 |
| --- | --- |
| Push가 거절됨(rejected) | 상대가 먼저 올린 것. **Git → Pull** 후 다시 Push |
| 빌드 오류 "incompatible with Gradle JVM 25" | Gradle JDK를 17로(2-3번) |
| 로그인 버튼이 꺼져 있음 | `app/google-services.json` 없음(2-4번) → 넣고 Sync |
| `No matching client found for package name` | google-services.json이 다른 앱용. Firebase에서 `com.tinyrunners.school`용 파일을 다시 받기 |
| 실행 시 "Device is offline / already running" | Device Manager → 에뮬레이터 ⋮ → **Cold Boot Now** |
| 상대가 올린 게 안 보임 | main에 있는지(브랜치에만 있는지) GitHub에서 확인. 브랜치면 PR을 Merge해야 main에 들어온다 |
| 검사기 실패 | 오류 문구에 파일·줄이 나온다. 그 부분 JSON 형식을 DATA_CONTRACT.md와 비교 |

정확한 오류 문구와 "무엇을 눌렀더니 이렇게 됐다"를 캡처해서 상대에게 보내면 빨리 해결된다.
