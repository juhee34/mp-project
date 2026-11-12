const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'..');
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
const chars=read('app/src/main/assets/content/characters.json'),stories=read('app/src/main/assets/content/stories.json'),assets=read('app/src/main/assets/content/assets.json');
const expected=["mint", "cheese", "popo", "lumi", "nunu", "piko"];
assert.deepEqual(chars.map(c=>c.id),expected);
assert.equal(stories.length,24);assert.equal(new Set(stories.map(s=>s.id)).size,24);
for(const id of expected){
  const own=stories.filter(s=>s.characterId===id);
  assert.deepEqual(own.map(s=>s.id),[`${id}_intro`,`${id}_episode_1`,`${id}_episode_2`,`${id}_episode_3`]);
  assert.deepEqual(own.map(s=>s.status),['ready','ready','ready','outline']); // episode 2 written (A, STORY_EXPANSION, 2026-11-11)
  assert.deepEqual(chars.find(c=>c.id===id).storyIds,own.map(s=>s.id));
}
for(const s of stories){
  assert(s.title&&s.body&&s.summary);
  assert(Number.isInteger(s.unlockDistance)&&s.unlockDistance>=0);
  if(s.status==='ready'){
    assert(s.pages.length>0);
    for(const p of s.pages){assert(p.title&&p.blocks.length>0);for(const b of p.blocks){
      assert(b.text.trim());assert(b.speaker===null||chars.some(c=>c.name===b.speaker));
      assert(!/\*\*|^####/m.test(b.text),'Unconverted markdown');
    }}
    const reconstructed=s.pages.flatMap(p=>p.blocks).map(b=>(b.speaker?b.speaker+': ':'')+b.text).join('\n\n');
    assert.equal(reconstructed,s.body,'Full text and paginated text diverged');
  }else assert.deepEqual(s.pages,[],'Outlines must not be readable as finished stories');
}
for(const c of chars){assert(c.name&&c.home&&c.personality);assert(/^#[0-9A-F]{6}$/i.test(c.color));for(const id of c.storyIds)assert(stories.some(s=>s.id===id&&s.characterId===c.id));assert(assets.some(a=>a.characterId===c.id));}
for(const a of assets)for(const kind of ['background','music']) {
 const v=a[kind];assert(v.brief);if(v.status==='planned'){assert.equal(v.path,null);continue;}
 assert.equal(kind,'background');assert(['panorama_ready','running_loop_ready'].includes(v.status));assert.equal(v.layered,false);
 const m=read('app/src/main/assets/'+v.manifest),e=m.characters.find(c=>c.id===a.characterId);assert(e);assert.equal('backgrounds/'+e.source,v.path);
 const base=path.join(root,'app/src/main/assets/backgrounds'),b=fs.readFileSync(path.join(base,e.source));assert.equal(b.toString('ascii',1,4),'PNG');
 if(v.status==='running_loop_ready'){
  assert.equal(v.loopReady,true);assert.equal(e.loopReady,true);assert.equal(m.sequence,'intro_once_then_running_loop');assert.equal(e.floorY,256);assert.deepEqual(e.rect,[0,0,b.readUInt32BE(16),280]);
  const l=fs.readFileSync(path.join(base,e.loopSource));assert.equal(l.toString('ascii',1,4),'PNG');assert.equal(l.readUInt32BE(20),280);assert(l.readUInt32BE(16)>0);assert.notEqual(e.source,e.loopSource);
  const sha=x=>require('crypto').createHash('sha256').update(x).digest('hex');assert.equal(sha(b),e.sha256);assert.equal(sha(l),e.loopSha256);
 }else assert.equal(v.loopReady,false);
}

const research=['andersen','grimm','aesop','modern','world'].flatMap(n=>read('research/'+n+'.json'));assert.equal(research.length,205);assert.equal(new Set(research.map(r=>r.id)).size,205);
for(const r of research)assert(r.source_url.startsWith('http')&&r.verified_content);
const p=fs.readFileSync(path.join(root,'docs/PROMPTS.md'),'utf8');assert.equal((p.match(/^## \d+단계 [AB] /gm)||[]).length,20);
// Village stories (B, content/village_stories.json; DATA_CONTRACT "이야기 대사")
const village=read("app/src/main/assets/content/village_stories.json");
assert.equal(new Set(village.map(s=>s.id)).size,village.length);assert(village.every(s=>expected.includes(s.characterId)));
// Story dialogue (DATA_CONTRACT "이야기 대사"): speakers, at most 2 choices with replies, at most 1 run, unlock refs.
const SPEAKERS=new Set([...expected,'player','narrator']);
const checkLine=(l,where)=>{assert(SPEAKERS.has(l.speaker),`${where}: unknown speaker "${l.speaker}"`);assert(typeof l.text==='string'&&l.text.trim(),`${where}: empty text`);};
let scripted=0;
for(const s of village){
  if(s.unlock?.afterStory!=null){const prev=village.find(x=>x.id===s.unlock.afterStory);assert(prev&&prev.characterId===s.characterId,`${s.id}: afterStory must be an earlier story of the same friend`);}
  if(!s.lines)continue;
  scripted++;
  assert(Array.isArray(s.lines)&&s.lines.length>0,`${s.id}: lines must be a non-empty array`);
  let choices=0,runs=0;
  s.lines.forEach((l,i)=>{
    const where=`${s.id} line ${i}`;
    if(l.run===true){runs++;return;}
    if(l.choice){choices++;assert(Array.isArray(l.choice)&&l.choice.length>=2&&l.choice.length<=3,`${where}: a choice needs 2-3 options`);
      for(const o of l.choice){assert(o.text&&o.text.trim(),`${where}: option text`);(o.reply||[]).forEach((r,k)=>checkLine(r,`${where} reply ${k}`));}return;}
    checkLine(l,where);
  });
  assert(choices<=2,`${s.id}: at most 2 choices per story`);
  assert(runs<=1,`${s.id}: at most 1 run per story`);
}

// Talks before a run (content/talks.json): prologue pages, one talk per friend, known speakers, friend speaks last.
const talks=read("app/src/main/assets/content/talks.json");
assert(Array.isArray(talks.prologue)&&talks.prologue.length>0&&talks.prologue.every(p=>typeof p==="string"&&p.trim()),"talks: prologue pages");
assert.deepEqual(talks.talks.map(t=>t.characterId),expected,"talks: one per friend in order");
for(const t of talks.talks){assert(t.lines.length>0,`${t.characterId}: empty talk`);
  t.lines.forEach((l,i)=>{assert([t.characterId,"player","narrator"].includes(l.speaker),`${t.characterId} talk line ${i}: speaker`);assert(l.text&&l.text.trim());});
  assert.equal(t.lines.at(-1).speaker,t.characterId,`${t.characterId}: the friend should speak last`);}

// Short dialogue pack (A, docs/dialogue): the app copy must match the document copy, 6 friends x 8 events x 3.
const norm=t=>t.split(String.fromCharCode(13)).join("");
const packDoc=norm(fs.readFileSync(path.join(root,"docs/dialogue/short-lines.json"),"utf8"));
const packApp=norm(fs.readFileSync(path.join(root,"app/src/main/assets/content/short_lines.json"),"utf8"));
assert.equal(packApp,packDoc,"app/src/main/assets/content/short_lines.json must be a copy of docs/dialogue/short-lines.json");
const pack=JSON.parse(packApp);
for(const id of expected)for(const ev of ["select","start","retry","stumble"])assert.equal(pack.lines.filter(l=>l.characterId===id&&l.event===ev).length,3,`short lines: ${id}/${ev} needs 3`);
// Approved additions (A, 2026-11-11): app copies match docs, 2 more per friend and event, ids and texts unique overall.
const extra=['01','02','03'].map(n=>{
  const doc=norm(fs.readFileSync(path.join(root,`docs/dialogue/short-lines-expansion-${n}.json`),'utf8'));
  const app=norm(fs.readFileSync(path.join(root,`app/src/main/assets/content/short_lines_expansion_${n}.json`),'utf8'));
  assert.equal(app,doc,`short_lines_expansion_${n}.json must be a copy of docs/dialogue/short-lines-expansion-${n}.json`);
  return JSON.parse(app).lines;}).flat();
const allLines=[...pack.lines,...extra];
assert.equal(new Set(allLines.map(l=>l.id)).size,allLines.length,'short lines: duplicate id');
assert.equal(new Set(allLines.map(l=>l.text)).size,allLines.length,'short lines: duplicate text');
for(const id of expected)for(const ev of pack.events.map(e=>e.id))assert.equal(allLines.filter(l=>l.characterId===id&&l.event===ev).length,5,`short lines: ${id}/${ev} needs 5 with additions`);

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

// Obstacle props (docs/art/OBSTACLE_HANDOFF.md): 3 per friend, 48x80 transparent PNG, wired in game/ObstacleArt.kt.
const OBSTACLE_PROPS={mint:['fern_log','ribbon_rock','map_crate'],cheese:['plant_pot','cushions','sun_tin'],popo:['snow_rock','shell_basket','harbor_crate'],lumi:['moss_stump','pinecone_basket','music_box'],nunu:['books','return_crate','letter_basket'],piko:['paper_crate','rope_spool','toolbox']};
let obstacleCount=0;
for(const id of expected)for(const prop of OBSTACLE_PROPS[id]){
  const f=path.join(root,SPRITE_DIR,`obstacle_${id}_${prop}.png`);
  assert(fs.existsSync(f),`missing obstacle prop obstacle_${id}_${prop}.png in ${SPRITE_DIR}`);
  const b=fs.readFileSync(f);
  assert(b.readUInt32BE(16)===48&&b.readUInt32BE(20)===80,`obstacle_${id}_${prop}.png must be 48x80`);
  assert(b[25]===6||b[25]===3,`obstacle_${id}_${prop}.png needs transparency`);
  obstacleCount++;
}

// A's friendly character copy (docs/character-copy/): the app copy is the same file, one entry per friend by id.
const copyDoc=fs.readFileSync(path.join(root,'docs/character-copy/friendly-copy.ko.json'),'utf8').replace(/\r\n/g,'\n');
const copyApp=fs.readFileSync(path.join(root,'app/src/main/assets/content/character_copy.json'),'utf8').replace(/\r\n/g,'\n');
assert.equal(copyApp,copyDoc,'character_copy.json must be a copy of docs/character-copy/friendly-copy.ko.json');
const copy=JSON.parse(copyApp);
assert.deepEqual([...copy.characters.map(c=>c.id)].sort(),[...expected].sort(),'character copy: one entry per friend');
for(const c of copy.characters){
  for(const k of ['tagline','selectionIntro','personality','cherishedItem','runningReason','storyTeaser']) assert(typeof c[k]==='string'&&c[k].trim(),c.id+': copy '+k);
  for(const k of ['traits','habits','greetings']) assert(Array.isArray(c[k])&&c[k].length>0,c.id+': copy '+k);
}
// The optional greetings are registered as their own select lines (scripts/build-greeting-lines.cjs).
const greet=read('app/src/main/assets/content/short_lines_greetings.json');
assert.deepEqual(greet.lines.map(l=>l.text),copy.characters.flatMap(c=>c.greetings),'short_lines_greetings.json is out of date: run node scripts/build-greeting-lines.cjs');
assert(greet.lines.every(l=>l.event==='select'&&expected.includes(l.characterId)));
assert.equal(new Set([...allLines.map(l=>l.id),...greet.lines.map(l=>l.id)]).size,allLines.length+greet.lines.length,'greeting ids must not clash with the 240 short lines');

// A's approved hazard and item art (docs/art/approved/): app copies are byte-identical to the extracted files.
let hazardCount=0;
for(const id of expected){
  for(const kind of ['air','spikes','platform']){
    const a=fs.readFileSync(path.join(root,'docs/art/approved/extracted',kind+'_'+id+'.png'));
    const b=fs.readFileSync(path.join(root,'app/src/main/res/drawable-nodpi','hazard_'+kind+'_'+id+'.png'));
    assert(a.equals(b),'hazard_'+kind+'_'+id+'.png differs from the approved extract'); hazardCount++;
  }
  if(id==='mint') continue; // the fern leaf is still pending separation (HANDOFF.md)
  const a=fs.readFileSync(path.join(root,'docs/art/approved/extracted','bag_'+id+'.png'));
  const b=fs.readFileSync(path.join(root,'app/src/main/res/drawable-nodpi','item_'+id+'.png'));
  assert(a.equals(b),'item_'+id+'.png differs from the approved extract');
}

console.log('PASS: 6 characters; story/asset references; asset readiness honest; 205 research records; 20 role prompts.');
console.log(`Sprites (required frames): ${spriteSummary.join(', ')}`);
console.log(`Talks: prologue ${talks.prologue.length} pages, ${talks.talks.length} friends`);
console.log(`Short lines in app: ${pack.lines.length}`);
console.log(`Obstacle props in app: ${obstacleCount}/18`);
console.log(`Short lines: ${allLines.length} (base 144 + additions ${extra.length})`);
console.log(`Character copy: ${copy.characters.length} friends, ${greet.lines.length} greeting lines; approved hazard art ${hazardCount}/18, item art 5/6 (mint pending)`);
console.log(`Village stories with dialogue: ${scripted}/${village.length} (others play from their outline)`);

assert.deepEqual(read('docs/CHARACTER_BIBLE.json').map(c=>c.id),expected);
assert.deepEqual(assets.map(a=>a.characterId),expected);
assert(stories.every(s=>expected.includes(s.characterId)));

// The runtime manifest must stay within the unchanged approved source images.
const bgManifest=read('app/src/main/assets/backgrounds/manifest.json');
assert.equal(new Set(bgManifest.characters.map(c=>c.id)).size,6);
for(const c of bgManifest.characters){
 const bytes=fs.readFileSync(path.join(root,'app/src/main/assets/backgrounds',c.source));
 const w=bytes.readUInt32BE(16),h=bytes.readUInt32BE(20),[x,y,rw,rh]=c.rect;
 assert(x>=0&&y>=0&&rw>0&&rh>0&&x+rw<=w&&y+rh<=h);
 assert(c.floorY>0&&c.floorY<rh);
 assert.equal(require('crypto').createHash('sha256').update(bytes).digest('hex'),c.sha256);
}
console.log('PASS: 6 background regions, floor anchors and approved source hashes.');
