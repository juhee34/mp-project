# 장애물 아트 개발 인계

기준일: 2026-11-10. A팀 납품 상태: **18종 개별 PNG와 메타데이터 준비 완료(art_ready_for_integration)**. 게임 연결·기기 검증은 B팀 후속 작업이다. 기존 배경 접합 수정 완료를 뜻하지 않는다.

## 받을 파일

- `obstacles/png/`: 6명 × 3종 = 18개 투명 PNG. 이 폴더가 실제 연결 대상이다.
- `obstacles/manifest.json`: ID, 상대 경로, Android 리소스명, 실제 그림 영역, 기준점, 원본 출처, SHA-256.
- `obstacles/review.png`: 흰색/회색 위 3배 확대와 1배 원본 검수판.
- `obstacles/background-review.png`: 6개 고향 배경 위 1배/2배 비교. 게임 스크린샷이 아닌 임시 배치 자료다.
- `obstacles/source/`: 내보내기에 사용한 고해상도 시트. 앱에 포함하지 않는다.
- 저장소 루트의 `scripts/validate-obstacle-art.py`: 외부 패키지 없이 실행하는 납품 검사기.

## 파일 규격

| 항목 | 값 |
|---|---|
| 캔버스 | 48×80px, 1배 RGBA8 PNG |
| 알파 | 전 픽셀 0 또는 255, 투명 픽셀 RGB는 0 |
| 발/바닥 | 마지막 불투명 행 y77, 지면 기준은 픽셀 경계 y78 |
| 기준점 | `[24,78]`, 캔버스 좌상단 기준 |
| 여백 | 좌우 최소 2px, 아래 2행 비움 |
| 그림 영역 | `opaqueRect=[x,y,width,height]`, 오른쪽/아래는 exclusive |
| 확대 | nearest neighbor, 필터/디더링 없음 |
| 파일명 | `obstacle_<characterId>_<prop>.png` |

48×80은 이 장애물 납품용 캔버스이며 기존 **캐릭터 48×48 규격을 바꾸지 않는다**. 원본 그림 비율을 유지해 최대 폭44/높이70 안에 내보냈다. 빈 캔버스 전체를 장애물 충돌 크기로 사용하지 않는다. 그림별 높이와 여백이 다르므로 `opaqueRect`를 읽는다. 캐릭터의 16색 제한을 장애물 공통 규격으로 새로 적용하지 않았고, 장애물의 색 수는 제한하지 않았다.

## 구성

| ID | variant 0 | variant 1 | variant 2 |
|---|---|---|---|
| mint (라일) | fern_log | ribbon_rock | map_crate |
| cheese | plant_pot | cushions | sun_tin |
| popo | snow_rock | shell_basket | harbor_crate |
| lumi | moss_stump | pinecone_basket | music_box |
| nunu | books | return_crate | letter_basket |
| piko | paper_crate | rope_spool | toolbox |

## B팀 연결 방법과 경계

아래 엔진 수치와 연결 예시는 납품 작업 브랜치 기준이다. origin/main에는 후속 타워·공중 장애물 변경(d4e66ef)이 있으므로, B는 적용 대상 브랜치의 최신 구조와 추가 장애물 위치를 확인한 뒤 연결한다. 이번 아트 게시에서 main을 병합하거나 게임 코드를 수정하지 않았다.

1. PNG 18개를 `app/src/main/res/drawable-nodpi/`로 복사한다. source·검수판은 앱에 넣지 않는다. Manifest를 assets에 배치하거나 동일 정보를 리소스 매핑으로 옮긴다.
2. Bitmap은 초기 로드해서 캐시한다. 매 프레임 decode하지 않는다. `Paint.isFilterBitmap=false`로 그린다.
3. 현재 `RunnerEngine`의 장애물은 폭24~44, 높이30~70을 서로 독립적으로 무작위 생성한다. 엔진에는 아트 variant가 없다. 아트 선택 때문에 엔진 난수 소비 순서·속도·간격·충돌·점수를 바꾸지 않는다. 안정적인 별도 아트 번호를 사용하고 프레임마다 바꾸지 않는다.
4. 기존 사각형 교체를 위한 첫 연결은 `src=opaqueRect`, `dst=[sx(o.x),sy(o.height),sx(o.x+o.width),sy(0)]`로 경계를 맞출 수 있다. Canvas Rect의 오른쪽/아래 값은 각각 x+width/y+height다. 이 방식은 그림의 원래 비율을 변경할 수 있다. 현재 임의 폭/높이의 전체 범위를 자연스럽게 보장한 아트는 아니다.
5. 비율 유지를 위해 단순히 그림만 작게 fit하면 그림 밖에서 충돌할 수 있다. 이 문제를 피하려면 B가 현재 물리와 시각 경계를 함께 검토해야 한다. 아트 작업만으로 물리 크기를 임의 변경하지 않았다. 너무 길어 보이는 조합은 비슷한 비율의 variant 선택 또는 별도 세로형 아트로 해결할 수 있다.
6. 미지원 ID/로드 실패 시 기존 장애물 그림으로 fallback한다. 여섯 캐릭터의 물리는 동일하게 유지한다.

### 원본 크기를 그대로 배치하는 경우

지면 화면좌표 G, 기준점 X, 확대율 k일 때 캔버스 좌상단은 `(X-24k,G-78k)`다. 이는 **렌더 기준점**이며 충돌 사각형 정의가 아니다. 소품별 장식과 바구니 가장자리는 불규칙하므로 실제 게임에서 충돌 디버그 박스를 켜고 확인한다.

## A가 완료한 검증

- 18파일을 독립 PNG 디코더로 전 픽셀 검사: RGBA8·크기·알파·투명 RGB·여백·바닥선·연결 영역·실제 그림 bounds·SHA-256·ID/variant 누락 모두 통과.
- 흰색/회색 위 1배·3배 및 6종 배경 위 1배·2배 시각 검수. 분리된 잡픽셀·큰 돌출 장식·극저알파 잔상 정리.
- 라일 가지를 바위 가까운 리본으로, 누누 손잡이를 낮은 바구니로 이미지 편집했다. 기술 내보내기에서 알파128 기준 이진화, 최근접 축소, 최대 8연결 영역 보존을 수행했다. 원래 콘셉트는 `obstacle-concepts/`에 유지한다.
- 원본 시트 전체를 게임 자산으로 쓰지 않는다. `obstacles/png/`만 사용한다.

검사 명령: `python scripts/validate-obstacle-art.py`

내보내기 재현: Windows PowerShell에서 `scripts/ExportObstacleArt.cs`의 `ExportObstacleArt.Run(repoRoot)`를 System.Drawing 참조와 함께 실행한다. PowerShell 7에서는 `System.Drawing.Common`, `System.Drawing.Primitives`, `System.Private.Windows.*` 및 런타임 ref 어셈블리가 필요하다. 납품 PNG를 사용하는 팀원은 재생성할 필요가 없다. 재생성 후 `python scripts/validate-obstacle-art.py --write-manifest`로 메타데이터를 갱신하고 일반 검사와 시각 검수를 다시 수행한다.

## B가 완료할 인수 조건

- 6명 각각에서 3종이 안정적으로 표시되고 지면에 붙음.
- 최소/최대 폭·높이 조합에서 그림 왜곡 및 보이지 않는 충돌 점검.
- 날씨 효과, 일시정지/재개, 재시작 시 깨짐·깜박임 없음.
- 기존 단위 테스트·assembleDebug·lintDebug와 실제 기기 확인.

이 조건들은 이번 A 작업에서 실행한 것으로 보고하지 않는다. 게임 코드·app 리소스는 이번 납품 작업에서 변경하지 않았다. Git 업로드는 사용자가 별도 Codex에 맡긴다.
