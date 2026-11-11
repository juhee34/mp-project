// Synthesises the dialogue sound effects into app/src/main/res/raw (16-bit mono 22050 Hz WAV).
// Simple tones made here, not recordings:  node scripts/make-sfx.cjs
"use strict";
const fs = require("fs");
const path = require("path");
const RATE = 22050;
const out = (name, samples) => {
  const data = Buffer.alloc(samples.length * 2);
  samples.forEach((v, i) => data.writeInt16LE(Math.max(-1, Math.min(1, v)) * 32767 | 0, i * 2));
  const h = Buffer.alloc(44);
  h.write("RIFF", 0); h.writeUInt32LE(36 + data.length, 4); h.write("WAVE", 8); h.write("fmt ", 12);
  h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22); h.writeUInt32LE(RATE, 24);
  h.writeUInt32LE(RATE * 2, 28); h.writeUInt16LE(2, 32); h.writeUInt16LE(16, 34); h.write("data", 36); h.writeUInt32LE(data.length, 40);
  fs.writeFileSync(path.join(__dirname, "..", "app/src/main/res/raw", name + ".wav"), Buffer.concat([h, data]));
};
/** One note: frequency, length (s), loudness, wave (sine or a soft square), with a quick attack and decay. */
const note = (freq, len, amp, wave = "sine") => Array.from({ length: Math.round(RATE * len) }, (_, i) => {
  const t = i / RATE, env = Math.min(1, t / 0.004) * Math.exp(-t / (len * 0.35));
  const s = Math.sin(2 * Math.PI * freq * t);
  return amp * env * (wave === "square" ? Math.sign(s) * 0.6 + s * 0.4 : s);
});
const seq = (...parts) => parts.flat();
const rest = (len) => new Array(Math.round(RATE * len)).fill(0);

// Typing: a tiny soft blip (the talk scene changes its pitch per friend).
out("sfx_blip", note(740, 0.045, 0.35, "square"));
// Next line: a small rising pop.
out("sfx_next", seq(note(520, 0.05, 0.4), note(780, 0.07, 0.35)));
// A choice or the item panel appears: two gentle notes.
out("sfx_choice", seq(note(660, 0.08, 0.4), rest(0.02), note(880, 0.12, 0.35)));
// Items handed over: a bright little arpeggio (C E G C).
out("sfx_give", seq(note(523, 0.09, 0.4), note(659, 0.09, 0.4), note(784, 0.09, 0.4), note(1047, 0.28, 0.45)));
console.log("wrote sfx_blip, sfx_next, sfx_choice, sfx_give");
