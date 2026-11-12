"""Update local handoff documents and static website for approved episode two."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
def update(path, replacements):
    p=root/path
    s=p.read_text(encoding='utf-8')
    for old,new in replacements:
        assert old in s, (path,old)
        s=s.replace(old,new)
    p.write_text(s,encoding='utf-8')
update('website/dist/app.js',[
    ("function read(kind){const s=data.stories.find(x=>x.characterId===selected&&x.kind===kind);", "function read(kind,order=1){const s=data.stories.find(x=>x.characterId===selected&&x.kind===kind&&(kind!=='episode'||x.id===selected+'_episode_'+order));"),
    ("$('read-first').onclick=()=>read('episode');", "$('read-second').onclick=()=>read('episode',2);$('read-first').onclick=()=>read('episode');"),
    ("$('story-tease').textContent=extras[id][0];", "$('story-tease').textContent=extras[id][0];$('second-title').textContent=data.stories.find(s=>s.id===id+'_episode_2').title;")])
update('website/dist/index.html',[
    ('<p id="music-note"', '<div class="next-story"><p class="eyebrow">새로 만나는 두 번째 이야기</p><h4 id="second-title"></h4><button id="read-second" class="button dark">두 번째 이야기 읽기 ↗</button><p class="reading-note">이 웹 파일에서 먼저 읽을 수 있어요. Android 앱 연결은 준비 중이에요.</p></div><p id="music-note"'),
    ('여기서는 완성된 소개와 첫 이야기를 읽어 볼 수 있어요.', '여기서는 친구마다 배경 이야기·첫 이야기·두 번째 이야기, 총 18편을 읽을 수 있어요. 세 번째 이야기 6편은 아직 개요 단계예요.'),
    ('상황별 짧은 대사 144개의 앱 연결', '상황별 짧은 대사 240개의 앱 연결'),
    ('후속 이야기 12편의 본문 제작', '확정된 두 번째 이야기 6편의 앱 연결 · 세 번째 이야기 6편의 본문 제작'),
    ('후속 이야기는 개요 상태예요.', '두 번째 이야기 6편은 원고가 확정되어 이 웹 파일에서 감상할 수 있어요. 세 번째 이야기 6편은 개요 상태예요.'),
    ('준비 중인 144개 상황 대사', '원고가 준비된 240개 상황 대사')])
update('docs/PROJECT_INTRO.md',[
    ('후속편은 준비 중으로 표시한다.', '현재 앱의 후속편은 준비 중으로 표시한다. 확정한 두 번째 이야기 6편은 소개 웹 파일에서 먼저 읽을 수 있으며 앱 연결은 별도다.'),
    ('| 후속 이야기 | 12편 개요, 본문 제작 보류 | 제목과 준비 중 안내. 완성된 24편의 이야기라고 소개하지 않음 |', '| 후속 이야기 | 두 번째 이야기 6편 본문 확정·소개 웹 파일 반영, 세 번째 이야기 6편 개요 | 읽을 수 있는 웹 콘텐츠 총 18편. 앱은 기존 12편이며 확장 6편 연결 대기 |'),
    ('144개', '240개')])
intro=root/'docs/PROJECT_INTRO.md'
with intro.open('a',encoding='utf-8') as f:
    f.write('\n\n## 확정 후속 이야기 반영 (2026-11-11)\n\n배경 6편 + 첫 이야기 6편 + 두 번째 이야기 6편 = 완성 원고 18편. 세 번째 이야기 6편은 개요다. 짧은 대사는 기존 144개 + 추가 96개 = 240개로, 6명 × 8상황 × 대안 5개다. 웹 읽기는 앱의 읽음·해금 기록과 연동되지 않는다. 파일 수정과 실제 호스팅 사이트 게시를 구분한다.\n\n| 친구 | 두 번째 이야기 |\n| --- | --- |\n| 라일 | 비어 있는 의자 표시 |\n| 치즈 | 방석은 두 번 옮겨졌다 |\n| 포포 | 내가 좋아한 조용한 소리 |\n| 루미 | 웃다가 놓친 마지막 말 |\n| 누누 | 웃기 전에 들킨 날 |\n| 피코 | 이름이 다른 선착장 |\n')
guide=root/'docs/DEVELOPMENT_GUIDE.html'
s=guide.read_text(encoding='utf-8')
notice='''<section id="story-update-20261111"><h2>2026년 11월 11일 · 확정 이야기 업데이트</h2><p><strong>이 안내가 아래 초기 기획의 이야기 제작 상태보다 우선합니다.</strong> 배경 이야기 6편, 첫 에피소드 6편에 두 번째 에피소드 6편을 추가해 완성 원고는 총 18편입니다. 세 번째 에피소드 6편은 개요 단계입니다.</p><ul><li>라일 — 비어 있는 의자 표시</li><li>치즈 — 방석은 두 번 옮겨졌다</li><li>포포 — 내가 좋아한 조용한 소리</li><li>루미 — 웃다가 놓친 마지막 말</li><li>누누 — 웃기 전에 들킨 날</li><li>피코 — 이름이 다른 선착장</li></ul><p>상황별 짧은 대사는 기존 144개에 96개를 추가한 총 240개(6명 × 8상황 × 대안 5개)입니다. 대안은 연속 대화가 아니며 한 번에 하나씩 쓰는 후보입니다.</p><p>소개 웹 파일에서는 18편을 읽을 수 있습니다. Android 앱에는 기존 12편이 연결되어 있으며, 확장 6편과 추가 대사의 연결은 별도입니다. 긴 글을 읽지 않아도 게임을 계속할 수 있습니다. 이번 파일 수정은 Git 업로드나 호스팅 사이트 게시 완료를 의미하지 않습니다.</p></section>'''
assert '<main>' in s
s=s.replace('<main>','<main>'+notice,1)
guide.write_text(s,encoding='utf-8')
with (root/'website/dist/styles.css').open('a',encoding='utf-8') as f:
    f.write('\n.next-story{margin-top:24px;padding-top:20px;border-top:1px solid var(--line)}.next-story h4{margin:10px 0 16px}.story-actions{flex-wrap:wrap}\n')
print('Updated local website UI, introduction and guide.')
