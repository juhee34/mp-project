# 캐릭터 문구·기존 대사 정적 확인

확인일: 2026-11-12 (한국). 원격 main을 fetch한 기준: `c78a46b`.
앱을 실행하거나 사용자의 설치 APK를 확인한 결과가 아니다.

## 소개가 딱딱하게 보이는 원인
- `app/src/main/assets/content/characters.json`의 personality는 “느긋하다”, “신중하다” 같은 설정 설명형 문장이다.
- `characters/CharacterScreens.kt` 선택 화면은 `friend.personality`를 직접 표시한다. 프로필은 home, personality, summary를 표시한다.
- `data/CharacterDefinition.kt`와 `data/CharacterRepository.kt`는 id/name/species/home/personality/summary/color 중심으로 읽는다.
- JSON에 shortIntro, habits, runningReason, cherishedItem 등이 있어도 현재 소개 화면으로 연결되지 않는다.

따라서 부드러운 문구가 파일 안에 존재하는 것과 사용자 화면에 표시되는 것은 별개다. 새 전달 파일도 개발팀이 파서와 화면을 연결하기 전에는 적용되지 않는다.

## 이전 짧은 대사
| 앱 파일 | 개수 | 문서 원본과 JSON 일치 |
|---|---:|---|
| short_lines.json | 144 | 일치 |
| short_lines_expansion_01.json | 32 | 일치 |
| short_lines_expansion_02.json | 32 | 일치 |
| short_lines_expansion_03.json | 32 | 일치 |
| 합계 | 240 | 중복 ID 없음 |

위 파일은 `app/src/main/assets/content/`에 있다. 대응 원본은 `docs/dialogue/short-lines*.json`이다.
`talk/ShortLines.kt`의 FILES는 네 파일을 포함하며 고유 ID로 병합한다. 파일 오류는 로그를 남기고 해당 파일을 빈 목록으로 처리하므로 실행 환경의 파싱 성공 여부는 별도 확인 대상이다.

| 상황 | 정적 코드에서 확인한 연결 |
|---|---|
| 첫 출발 | TalkFlow START의 최초 만남에서는 긴 첫 대화 우선 |
| 이후 출발 | select + start |
| 다시 달리기 | retry |
| 부딪힘 | stumble; ResultActivity에서도 별도 추출 |
| 마을 이야기 목록 | StoryListActivity가 select 사용 |
| 마을 이야기 | 인사와 home; 작성된 이야기에서 story_unread/story_reread 선택 |
| clear | 현재 Kotlin 호출에서 찾지 못함. 미사용 후보이며 임의의 완주 기능 추가 금지 |

각 이벤트는 30개(6명 × 5개)다. 240개가 모두 선택창 소개에 표시되는 구조가 아니다. LineDeck은 친구/이벤트별 순서를 섞고 소진 전 중복 및 연속 반복을 줄인다. 같은 이벤트가 서로 다른 화면에서 따로 추출될 수도 있다.

## 판단과 한계
“이전 대사가 Git에 전혀 없다”는 상태는 아니다. 파일·로더·일부 표시 호출은 확인했다. 다만 최신 APK 배포 여부, 실제 표시 결과, 첫 만남 상태, 파싱 실패 여부는 확인하지 않았으므로 사용자가 안 보았다는 원인을 확정하지 않는다.

## 새 문구의 이야기 근거
characters.json의 생활 습관과 고향 설정, village_stories.json의 6명 intro와 episode_1을 참고했다.
- 라일: 지도의 접힌 쪽 — 고사리·리본 바위·물소리·피코와 길 확인.
- 치즈: 뚜껑을 열어 두는 오후 — 햇빛 통·손수건 매듭·누누에게 표현하는 배려.
- 포포: 여름을 놓아 둘 자리 — 작은 소리·발끝·집을 그리워하면서도 듣고 싶은 마음.
- 루미: 박수보다 먼저 움직인 것 — 솔방울 마이크·반응을 기다리는 마음·포포의 발끝.
- 누누: 의자 옆의 빈칸 — 숨바꼭질·민트 리본·발자국·자기 자리.
- 피코: 두 번째 모퉁이 — 종이배·빨라지는 설명·기다릴 장소와 서로 다른 구경 속도.

새 소개는 사건의 시작만 보여 주고 결말·교훈을 미리 풀어 쓰지 않는다. 포포를 수다스럽게, 누누를 공포 캐릭터로 바꾸거나 전원에게 같은 유아체를 적용하지 않는다.
