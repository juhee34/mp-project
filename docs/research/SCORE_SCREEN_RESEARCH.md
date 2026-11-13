# 게임 점수창(결과 화면) 조사 — 2026-11-13 (B)

요청: 인스타·핀터레스트·네이버·구글 등에서 게임 점수창 구성을 조사해 Tiny Runners 분위기(따뜻한 도트, 그림책·종이·벤치)에 맞게 결과 화면을 다시 짜고, 친구 화면의 "자세히 보기"도 같은 구성으로.

## 조사 범위
웹 검색 4회(영문 결과 화면 일반, 한국어 결과창 UI, 핀터레스트/도트 승리 화면, 러너 게임오버·최고 기록). 핀터레스트·인스타 핀 상세는 로그인 때문에 열지 못해 검색 결과 요약과 공개 문서를 근거로 했다.

## 많이 보인 구성
- **맨 위에 결과를 크게**, 가운데에 성적 숫자, 그 아래 보상, 마지막에 다음 행동(다시 하기·다음) 버튼.
- **숫자는 세어 올라가며(count-up)** 보여 주고, 기준을 넘으면 별·도장 같은 표시가 하나씩 튀어나옴.
- **최고 기록과 비교**: 새 기록이면 "New best"를 따로 크게, 아니면 지금 점수와 최고 기록을 함께.
- **보상은 아이콘과 함께 작게 묶어** 한눈에(코인·아이템).
- 실패했어도 **중립·긍정적인 말투**(개인 최고, 참여 보상 강조).
- 정보는 너무 작거나 구석에 두지 말 것, 색만으로 구분하지 말고 아이콘·모양도 함께.
- 도트 승리 화면 예시: 리본/배너 제목, 칸이 나뉜 성적 패널, 아이템 목록, 점선 구분선.

## 적용 (ui/ScoreBoard)
- 친구의 길 그림 위에 **그림책 종이 판**(크림 바탕, 꿀색 테두리, 아래 갈색 턱) + 위에 **초록 리본 제목**("오늘의 달리기" / "친구 소개").
- 친구 **둥근 얼굴(친구 색 바탕, 꿀색 테두리)** + 이름 + 말풍선 한마디.
- **숫자 칸은 바느질 점선 테두리**, 숫자가 세어 올라감. 새 기록이면 거리 칸에 기울어진 **"새 기록!" 도장**이 찍히며 나타남.
- **최고 기록까지 막대**(초록으로 차오름, 끝에 작은 깃발).
- **받은 것**: 코인·아이템·장애물 작은 알약.
- 오른쪽 버튼은 이야기 모음 시안의 초록/테두리 버튼 부품.
- "자세히 보기"는 같은 판에 최고 기록·함께 달린 거리·횟수 칸, 성격 알약, A의 소개 글을 점선 상자 하나에.

## 출처
- https://bugnet.io/blog/how-to-design-a-results-or-victory-screen
- https://defold.com/tutorials/level-complete
- https://mail.infosecinstitute.com/league-defeat-screen-1165466103.html
- https://wikidocs.net/366955
- https://wikidocs.net/343709
- https://create.roblox.com/docs/ko-kr/tutorials/curriculums/user-interface-design/choose-an-art-style
- https://21st.dev/@theorcdev/components/8bit-victory-screen.md
- https://www.8bitcn.com/docs/blocks/gaming/victory-screen
- https://coddy.tech/learn/courses/unity_game_sky_lanes/best_score
- https://awesome.ecosyste.ms/projects/github.com%2Fbreadscorner%2Funity-endless-runner
