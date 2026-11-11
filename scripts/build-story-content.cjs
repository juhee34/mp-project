// Exports the reviewed manuscripts to the app (Node port of build-story-content.py, which needs a Python install).
//
//   node scripts/build-story-content.cjs
//
// 1. content/stories.json (story book): intro + episode 1 from docs/STORY_MANUSCRIPT_0n.md exactly as the Python
//    script did, episode 2 from docs/STORY_EXPANSION_0n.md (A, approved 2026-11-11), episode 3 stays an outline.
// 2. content/characters.json storyIds.
// 3. content/village_stories.json: keeps each friend's intro visit and adds episode 1-3 as village talks.
//    Written episodes become talk lines (speaker = friend id, narration = "narrator"); episode 3 is an outline.
"use strict";
const fs = require("fs");
const path = require("path");

const ROOT = path.resolve(__dirname, "..");
const IDS = ["mint", "cheese", "popo", "lumi", "nunu", "piko"];
const NAMES = ["라일", "치즈", "포포", "루미", "누누", "피코"];
const read = (p) => fs.readFileSync(path.join(ROOT, p), "utf8").replace(/\r\n/g, "\n");
const write = (p, data) => fs.writeFileSync(path.join(ROOT, p), JSON.stringify(data, null, 2) + "\n", "utf8");
const END = "(?![\\s\\S])"; // Python's \Z

function blocks(text) {
  const out = [];
  for (let p of text.trim().split(/\n\s*\n/)) {
    p = p.trim();
    if (!p) continue;
    const m = p.match(/^\*\*([^:*]+):\*\*\s*([\s\S]*)$/);
    if (m) {
      let line = m[2].trim();
      if (line.startsWith("“") && line.endsWith("”")) line = line.slice(1, -1);
      out.push({ speaker: m[1], text: line });
    } else out.push({ speaker: null, text: p });
  }
  return out;
}

function paginate(title, text) {
  const out = [];
  let current = [], length = 0;
  for (const block of blocks(text)) {
    const size = [...block.text].length;
    if (current.length && (length + size > 270 || current.length >= 4)) {
      out.push({ title, blocks: current }); current = []; length = 0;
    }
    current.push(block); length += size;
  }
  if (current.length) out.push({ title, blocks: current });
  return out;
}

/** Scenes "### n. title" or "#### n. title" with their text. */
function scenes(text, hashes) {
  const re = new RegExp(`^${hashes} (.*?)\\n([\\s\\S]*?)(?=^${hashes} |${END})`, "gm");
  return [...text.matchAll(re)].map((m) => ({ title: m[1], text: m[2] }));
}

const entry = (cid, suffix, title, body, status, kind, order, pages, distance, summary, after = "") => ({
  id: cid + suffix, characterId: cid, title, body, status, kind, order,
  unlockDistance: distance, summary, afterReading: after, pages,
});

/** One dialogue box holds about two lines: long paragraphs are split at sentence ends. */
function chunks(text, max = 80) {
  const sentences = text.replace(/\s*\n\s*/g, " ").split(/(?<=[.!?…”])\s+/).filter((s) => s.trim());
  const out = [];
  for (const s of sentences) {
    const last = out.length - 1;
    if (last >= 0 && [...out[last]].length + 1 + [...s].length <= max) out[last] += " " + s;
    else out.push(s.trim());
  }
  return out;
}

/** Scene list → village talk lines: a small scene title card, then narration and dialogue. */
function talkLines(sceneList) {
  const lines = [];
  for (const scene of sceneList) {
    lines.push({ speaker: "narrator", text: "— " + scene.title.replace(/^\d+\.\s*/, "") + " —" });
    for (const b of blocks(scene.text)) {
      const speaker = b.speaker == null ? "narrator" : IDS[NAMES.indexOf(b.speaker)];
      if (!speaker) throw new Error("unknown speaker " + b.speaker);
      for (const text of chunks(b.text)) lines.push({ speaker, text });
    }
  }
  return lines;
}

const records = [];
const episodes = {}; // cid -> {1: {...}, 2: {...}, 3: {...}} for the village talks
IDS.forEach((cid, index) => {
  const name = NAMES[index];
  const source = read(`docs/STORY_MANUSCRIPT_0${Math.floor(index / 2) + 1}.md`);
  const section = source.match(new RegExp(`^## ${name}:[\\s\\S]*?(?=^## |${END})`, "m"))[0];
  const background = section.match(/### 배경 이야기\s*\n([\s\S]*?)\n### 첫 에피소드/)[1].trim();
  const ep = section.match(/### 첫 에피소드 — (.*?)\n([\s\S]*?)\n### 도감용/);
  const summary = section.match(/- 목록 요약: (.*)/)[1];
  const after = section.match(/- 읽은 뒤 문구: (.*)/)[1];
  const ep1Scenes = scenes(ep[2], "####");
  const pages = ep1Scenes.flatMap((s) => paginate(s.title, s.text));
  records.push(entry(cid, "_intro", name + "의 집과 작은 바람", background, "ready", "background", 0,
    paginate("집과 작은 바람", background), 0, background.split("\n\n")[0]));
  const bodyOf = (ps) => ps.flatMap((p) => p.blocks).map((b) => (b.speaker ? b.speaker + ": " : "") + b.text).join("\n\n");
  records.push(entry(cid, "_episode_1", ep[1].trim(), bodyOf(pages), "ready", "episode", 1, pages, 100, summary, after));
  episodes[cid] = { 1: { title: ep[1].trim(), summary, lines: talkLines(ep1Scenes) } };

  const outlines = [...section.matchAll(/\*\*([23])편 — (.*?)\*\*\s*\n([\s\S]*?)(?=\n대사 방향|\n\*\*[23]편|\n## |(?![\s\S]))/g)];
  if (outlines.length !== 2) throw new Error(cid + " outlines " + outlines.length);

  // Episode 2: A's approved expansion text replaces the outline (same id, title and unlock distance).
  const expansion = read(`docs/STORY_EXPANSION_0${Math.floor(index / 2) + 1}.md`);
  const ex = expansion.match(new RegExp(`^## ${name} — (.*?)\\n([\\s\\S]*?)(?=^## |${END})`, "m"));
  if (!ex || !ex[2].includes(`대상 ID: ${cid}_episode_2`)) throw new Error(cid + " expansion missing");
  const exSummary = ex[2].match(/목록 요약: (.*)/)[1];
  const exAfter = ex[2].match(/읽은 뒤 문구: (.*)/)[1];
  const ep2Scenes = scenes(ex[2], "###");
  const ep2Pages = ep2Scenes.flatMap((s) => paginate(s.title.replace(/^\d+\.\s*/, (d) => d), s.text));

  for (const m of outlines) {
    if (m[1] === "2") {
      if (m[2] !== ex[1].trim()) throw new Error(`${cid}: expansion title "${ex[1]}" != outline "${m[2]}"`);
      records.push(entry(cid, "_episode_2", m[2], bodyOf(ep2Pages), "ready", "episode", 2, ep2Pages, 0, exSummary, exAfter));
      episodes[cid][2] = { title: m[2], summary: exSummary, lines: talkLines(ep2Scenes) };
    } else {
      const body = m[3].trim();
      records.push(entry(cid, "_episode_" + m[1], m[2], body, "outline", "episode", Number(m[1]), [], 0, "다음 이야기를 준비하고 있어요."));
      episodes[cid][3] = { title: m[2], summary: body };
    }
  }
});

write("app/src/main/assets/content/stories.json", records);
const chars = JSON.parse(read("app/src/main/assets/content/characters.json"));
for (const c of chars) c.storyIds = records.filter((s) => s.characterId === c.id).map((s) => s.id);
write("app/src/main/assets/content/characters.json", chars);

// Village visits: keep each intro as it is, then episodes 1-3 in order.
const villagePath = "app/src/main/assets/content/village_stories.json";
const oldVillage = JSON.parse(read(villagePath));
const village = [];
for (const cid of IDS) {
  const intro = oldVillage.find((s) => s.id === cid + "_intro");
  if (!intro) throw new Error(cid + " intro missing");
  village.push(intro);
  let previous = intro.id;
  for (const n of [1, 2, 3]) {
    const e = episodes[cid][n];
    const story = {
      id: `${cid}_episode_${n}`, characterId: cid, title: e.title, body: e.summary,
      status: e.lines ? "ready" : "outline", unlockDistance: 0, unlock: { afterStory: previous },
    };
    if (e.lines) story.lines = e.lines;
    village.push(story);
    previous = story.id;
  }
}
write(villagePath, village);

for (const cid of IDS) {
  const e = episodes[cid];
  console.log(cid, "village talk lines: episode 1", e[1].lines.length, "· episode 2", e[2].lines.length);
}
