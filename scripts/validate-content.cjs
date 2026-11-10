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
// Sprites (docs/SPRITE_SPEC.md): 48x48 PNG with alpha, sprite_<id>_<motion>_<n>.png in res/drawable-nodpi.
const SPRITE_DIR='app/src/main/res/drawable-nodpi';
const MOTIONS={idle:2,run:4,jump:2,fail:1,land:1,greet:2,home:2},REQUIRED=['idle','run','jump','fail'];
const spriteFiles=fs.existsSync(path.join(root,SPRITE_DIR))?fs.readdirSync(path.join(root,SPRITE_DIR)).filter(f=>f.startsWith('sprite_')):[];
const have={};
for(const f of spriteFiles){
  const m=f.match(/^sprite_([a-z]+)_([a-z]+)_(\d)\.png$/);
  assert(m,`${f}: name must be sprite_<id>_<motion>_<n>.png (lowercase)`);
  const [,id,motion,n]=m;
  assert(expected.includes(id),`${f}: unknown character id "${id}" (use mint for 라일)`);
  assert(motion in MOTIONS,`${f}: unknown motion "${motion}"`);
  assert(Number(n)<MOTIONS[motion],`${f}: ${motion} has frames 0..${MOTIONS[motion]-1}`);
  const b=fs.readFileSync(path.join(root,SPRITE_DIR,f));
  assert(b.length>26&&b.readUInt32BE(0)===0x89504e47,`${f}: not a PNG`);
  const w=b.readUInt32BE(16),h=b.readUInt32BE(20),colorType=b[25];
  assert(w===48&&h===48,`${f}: ${w}x${h}, frames must be exactly 48x48 (export at 1x)`);
  assert(colorType===6||colorType===3,`${f}: needs a transparent background (RGBA or indexed PNG)`);
  (have[id]??={})[motion]=(have[id][motion]||0)+1;
}
const requiredCount=REQUIRED.reduce((s,k)=>s+MOTIONS[k],0);
const spriteSummary=expected.map(id=>{const n=REQUIRED.reduce((s,k)=>s+Math.min(have[id]?.[k]||0,MOTIONS[k]),0);return `${id} ${n}/${requiredCount}`;});
for(const c of chars){
  if(c.spritePath==null){assert.notEqual(c.assetStatus,'sprite_ready',`${c.id}: sprite_ready needs spritePath`);continue;}
  assert.equal(c.spritePath,`sprite_${c.id}`,`${c.id}: spritePath must be "sprite_${c.id}"`);
  for(const k of REQUIRED)assert.equal(have[c.id]?.[k]||0,MOTIONS[k],`${c.id}: spritePath is set but ${k} frames are missing`);
}

console.log('PASS: 6 characters; story/asset references; planned assets honest; 205 research records; 20 role prompts.');
console.log(`Sprites (required frames): ${spriteSummary.join(', ')}`);

assert.deepEqual(read('docs/CHARACTER_BIBLE.json').map(c=>c.id),expected);
assert.deepEqual(assets.map(a=>a.characterId),expected);
assert(stories.every(s=>expected.includes(s.characterId)));
