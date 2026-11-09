# 공동 데이터 계약

## ID
mint, cheese, mochi, bolt, lumi, popo, nunu, bam, piko, sol — 변경 금지. 표시 이름과 ID를 분리한다.

## 캐릭터
app/src/main/assets/content/characters.json이 앱의 캐릭터 데이터다.
id/name/species/home/personality/summary/color는 필수 문자열.
portraitPath/spritePath는 실제 파일이 없으면 null. backgroundId/musicId는 캐릭터 ID와 같다.
storyIds는 stories.json에 존재하는 ID만 참조한다. assetStatus=planned는 소재 미완료다.
현재 Kotlin 모델은 시작 화면에 필요한 필드만 읽는다. 게임 구현 시 추가 필드를 명시적으로 파싱한다.

## 이야기
id, characterId, title, body, status, unlockDistance.
현재 status=outline은 서사 개요이며 완성 에피소드가 아니다. 첫 소개 unlockDistance=0.

## 배경과 음악
assets.json의 background/music에는 id, path, status, brief가 있다.
path=null인 파일은 로드하지 않는다. planned 항목은 완료 통계에 포함하지 않는다.
스프라이트는 확장 단계 시작 전 64x64 셀을 후보로 실물 검증한다. 프레임 규격을 확정하기 전 대량 생성하지 않는다.
최종 배경은 원경/중경/전경/바닥을 분리할 수 있어야 한다. 반복 이음새를 실제로 확인한다.
음원은 60~90초 반복 기악곡을 목표로 하고 제작 출처 및 실제 파일을 기록한다.

## 상태 및 저장 제안
RunResult(runId, characterId, distance, score)와 CharacterProgress(characterId, totalDistance, bestScore, readStoryIds)를 분리한다.
runId별 중복 저장 방지. 실패 거리도 반영. 10명 동일 점프/속도/기본 충돌 판정. 미접속 관계 감소 없음.

## 화면 계약
소개 → 10명 선택 → game(characterId) → 결과 → 도감/재시작.
정의되지 않은 ID는 선택 화면으로 복귀한다. 원시 JSON과 Activity 사이의 의존을 Repository로 제한한다.
