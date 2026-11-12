# 캐릭터 선택창 조사 — 2026-11-12 (B)

요청: 마을 이야기 화면과 캐릭터 선택 화면이 둘 다 같은 카드 줄이라 헷갈림. 다른 게임의 캐릭터 선택창 형태를 조사해 바꾸기.

## 조사 범위
웹 검색 4회(영문·한글·핀터레스트·모바일 게임 이름). 인스타·네이버 블로그·핀터레스트 핀 상세는 로그인 때문에 열지 못해 검색 결과 요약과 공개 문서를 근거로 했다.

## 많이 보인 형태
- **큰 미리보기 + 아래 캐릭터 목록**: 가운데(또는 한쪽)에 고른 캐릭터를 크게 보여 주고, 아래나 옆에 작은 얼굴 아이콘 목록을 둔다. 아이콘을 누르면 미리보기가 바뀐다. (Smash Bros 계열 분석, 커스터마이징 화면 설명 글: 상단 뒤로가기·제목, 가운데 미리보기, 하단 목록)
- **목록 + 상세 정보 나란히**: 목록에서 고르면 옆 패널에 이름·설명이 나온다. (Unity 공식 런타임 UI 예제)
- **목록 칸은 손가락 크기(56~72px 이상)**, 고른 칸은 테두리·크기로 표시, 미리보기와 목록이 서로 공간을 빼앗지 않게 균형. (인벤토리 UI 가이드, Smash Ultimate UX 비평)
- 러너 장르(Crossy Road 등)는 캐릭터를 옆으로 넘기는 캐러셀 + 큰 캐릭터 + 시작 버튼.

## 적용
- 왼쪽 큰 무대: 고른 친구가 자기 집 앞에 크게 서서 숨쉬는 모습(그림자, 친구 색 테두리), 양쪽 ◀ ▶와 좌우로 밀기.
- 오른쪽 정보 패널: 이름, 종·고향, 소개 한 줄, 이 친구와 최고 기록, ○○이랑 달리기, 자세히 보기·이야기 도감.
- 아래 친구 목록: 둥근 얼굴 6개(고른 친구는 크게, 초록 테두리), 이름 표시, 누르면 바로 바뀜.
- 마을 이야기·내 기록은 상점식 카드 줄을 유지해 두 화면이 서로 다르게 보이게 했다.

## 출처
- https://medium.com/super-jump/armchair-vgux-super-smash-bros-ultimate-e16bfeeea0c9
- https://oembed.org/blog/smash-4s-iconic-character-select
- https://www.abratabia.com/game-ui-design/inventory-ui.php
- https://docs.unity3d.com/kr/current/Manual/UIE-HowTo-CreateRuntimeUI.html
- https://wikidocs.net/383485
- https://dev.epicgames.com/documentation/fortnite/tmnt-custom-ui-player-info-in-unreal-editor-for-fortnite?lang=ko
- https://phaser.io/news/2016/02/crossy-road-character-tutorial
- https://justygames.itch.io/2d-character-selection-screen
- https://lr.us.psf.lt/r/indiegames/comments/1dxarnm/select_screen_mock_up
- https://docs.google.com/presentation/d/1jsvllj2sBSOSasFaSqI4e9R211R_tVyEhe_RB9Kx0zU/htmlpresent
