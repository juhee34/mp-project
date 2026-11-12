// Builds app/src/main/assets/content/short_lines_greetings.json from the 12 optional greetings in
// docs/character-copy/friendly-copy.ko.json: two "select" lines per friend with their own ids, kept apart from
// the 240 approved short lines. Run: node scripts/build-greeting-lines.cjs
const fs = require("fs");
const path = require("path");
const root = path.join(__dirname, "..");
const copy = JSON.parse(fs.readFileSync(path.join(root, "docs/character-copy/friendly-copy.ko.json"), "utf8"));
const lines = [];
for (const c of copy.characters) {
  c.greetings.forEach((text, i) => lines.push({
    id: `${c.id}_select_greeting_0${i + 1}`, characterId: c.id, event: "select", text, requiresReadStoryIds: [],
  }));
}
const out = {
  schemaVersion: 1,
  status: "user_approved",
  source: "docs/character-copy/friendly-copy.ko.json (greetings)",
  notes: "선택 인사 후보 12개. 기존 240개와 별도 ID로 select 이벤트에 추가한다. 생성 파일이므로 직접 고치지 말고 원본 JSON을 고친 뒤 다시 만든다.",
  lines,
};
fs.writeFileSync(path.join(root, "app/src/main/assets/content/short_lines_greetings.json"), JSON.stringify(out, null, 2) + "\n");
console.log(`greetings: ${lines.length}`);
