# Firebase 설정 (로그인 · 기록 저장)

과제 요건 "회원가입·로그인(BaaS 인증)"과 "원격 저장(BaaS)"을 Firebase Authentication과 Cloud Firestore로 충족한다.
무료 Spark 요금제 안에서만 개발한다. **결제 수단을 등록하거나 Blaze로 올리지 않는다.**

## 1. 프로젝트 만들기 (팀에서 한 명)
1. https://console.firebase.google.com 에서 프로젝트를 만든다. Google Analytics는 꺼도 된다.
2. Android 앱 추가: 패키지 이름 `com.tinyrunners.school`. 앱 닉네임은 자유. SHA-1은 이메일 로그인에는 필요 없다.
3. `google-services.json`을 내려받아 `app/google-services.json`에 둔다.
   - 이 파일은 `.gitignore`에 있어 커밋되지 않는다. 팀원은 Firebase 콘솔의 프로젝트 설정 → 내 앱에서 같은 파일을 받는다. 프로젝트 멤버로 초대하면 된다.
4. Build → Authentication → 시작하기 → 로그인 방법에서 **이메일/비밀번호**를 사용 설정한다.
5. Build → Firestore Database → 데이터베이스 만들기. 위치는 `asia-northeast3 (서울)`을 권장한다. 프로덕션 모드로 만든 뒤 아래 규칙을 넣는다.

## 2. Firestore 보안 규칙
본인 기록만 읽고 쓸 수 있게 한다.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

## 3. 앱에서 확인
- `app/google-services.json`이 있으면 Gradle이 `com.google.gms.google-services` 플러그인을 적용한다. 없으면 플러그인 없이 빌드된다.
- Android Studio에서 Sync 후 실행 → 로그인 화면의 "설정 파일이 없어요" 안내가 사라지고 로그인 버튼이 켜져야 한다.
- 회원가입 → 한 판 달리기 → 결과 화면에 "계정(Firebase)에 저장된 기록이에요."가 보이면 원격 저장이 동작한 것이다.
- Firebase 콘솔 Firestore에서 `users/{uid}/runs/{runId}`, `users/{uid}/progress/{characterId}` 문서가 생겼는지 확인한다.

## 저장 구조
| 경로 | 필드 | 설명 |
| --- | --- | --- |
| users/{uid}/runs/{runId} | characterId, meters, score, cleared, savedAt | 한 판 기록. runId 문서가 이미 있으면 다시 저장하지 않는다. |
| users/{uid}/progress/{characterId} | characterId, runs, totalMeters, bestMeters, bestScore | 친구별 누적. 같은 트랜잭션에서 갱신한다. |

## 설정 전 임시 모드
`google-services.json`이 없을 때만 로그인 화면에 "설정 전 임시로 둘러보기"가 보인다. 이 모드는 기록을 기기(SharedPreferences)에만 저장하는 개발용 경로다. 발표 시연은 실제 로그인으로 한다.
