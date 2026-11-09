const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'..');
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
const chars=read('app/src/main/assets/content/characters.json'),stories=read('app/src/main/assets/content/stories.json'),assets=read('app/src/main/assets/content/assets.json');
const expected=["mint", "cheese", "popo", "lumi", "nunu", "piko"];
assert.deepEqual(chars.map(c=>c.id),expected);assert.equal(new Set(stories.map(s=>s.id)).size,6);
for(const c of chars){assert(c.name&&c.home&&c.personality);assert(/^#[0-9A-F]{6}$/i.test(c.color));for(const id of c.storyIds)assert(stories.some(s=>s.id===id&&s.characterId===c.id));assert(assets.some(a=>a.characterId===c.id));}
for(const a of assets)for(const kind of ['background','music']){assert.equal(a[kind].status,'planned');assert.equal(a[kind].path,null);assert(a[kind].brief);}
const research=['andersen','grimm','aesop','modern','world'].flatMap(n=>read('research/'+n+'.json'));assert.equal(research.length,205);assert.equal(new Set(research.map(r=>r.id)).size,205);
for(const r of research)assert(r.source_url.startsWith('http')&&r.verified_content);
const p=fs.readFileSync(path.join(root,'docs/PROMPTS.md'),'utf8');assert.equal((p.match(/^## \d+단계 [AB] /gm)||[]).length,20);
console.log('PASS: 6 characters; story/asset references; planned assets honest; 205 research records; 20 role prompts.');

assert.deepEqual(read('docs/CHARACTER_BIBLE.json').map(c=>c.id),expected);
assert.deepEqual(assets.map(a=>a.characterId),expected);
assert(stories.every(s=>expected.includes(s.characterId)));
