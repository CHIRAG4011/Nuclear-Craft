'use strict';
const PDFDocument = require('pdfkit');
const fs = require('fs');

// ─── Document ──────────────────────────────────────────────────────────────────
const doc = new PDFDocument({
  margin: 44, size: 'A4', autoFirstPage: false,
  info: { Title: 'NuclearCraft: Plutonium Age — Complete Player Guide' }
});
const stream = fs.createWriteStream('/home/runner/workspace/NuclearCraft-Guide.pdf');
doc.pipe(stream);

// ─── Dimensions ───────────────────────────────────────────────────────────────
const PW = 595.28, PH = 841.89;
const ML = 44, MR = 44, CW = PW - ML - MR;
const BOTTOM = PH - 38;

// ─── Colours ──────────────────────────────────────────────────────────────────
const BG='#0f0f0f', PANEL='#191919', GRN='#39ff14', CYAN='#00cfff',
      YLW='#ffd000', ORG='#ff8800', RED='#ff3300', PRP='#9933ff',
      LIME='#7fff00', WHT='#e0e0e0', DIM='#777777', EDGE='#1e5c1e';

// ─── Page management ──────────────────────────────────────────────────────────
let pageNum = 0;

// Fill background on EVERY page — catches auto-overflow pages too
doc.on('pageAdded', () => {
  doc.rect(0, 0, PW, PH).fill(BG);
  doc.x = ML;
  if (doc.y < 38) doc.y = 40;
});

function newPage() {
  // write footer on current page before leaving
  if (pageNum > 0) writeFooter();
  doc.addPage();
  pageNum++;
  doc.y = 40;
}

function writeFooter() {
  doc.save();
  doc.rect(ML, PH - 30, CW, 0.6).fill(EDGE);
  doc.fontSize(7.5).fillColor(DIM).font('Helvetica')
     .text('NuclearCraft: Plutonium Age  ·  Complete Player Guide  ·  Page ' + pageNum,
           ML, PH - 24, { width: CW, align: 'center', lineBreak: false });
  doc.restore();
}

function need(h) {
  if (doc.y + h > BOTTOM) newPage();
}

// ─── Typography helpers ────────────────────────────────────────────────────────
function sp(n) { doc.moveDown(n == null ? 0.5 : n); }

function H1(txt, col) {
  need(28);
  col = col || GRN;
  const y = doc.y;
  doc.rect(ML, y, CW, 22).fill(PANEL);
  doc.rect(ML, y, 4, 22).fill(col);
  doc.fontSize(13).fillColor(col).font('Helvetica-Bold')
     .text('  ' + txt, ML + 8, y + 5, { width: CW - 12, lineBreak: false });
  doc.y = y + 26; sp(0.15);
}

function H2(txt, col) {
  need(22);
  col = col || CYAN;
  sp(0.3);
  doc.fontSize(10.5).fillColor(col).font('Helvetica-Bold')
     .text(txt, ML, doc.y, { width: CW });
  doc.rect(ML, doc.y, CW, 0.7).fill(col);
  sp(0.5);
}

function P(txt, col) {
  doc.fontSize(9).fillColor(col || WHT).font('Helvetica')
     .text(txt, ML, doc.y, { width: CW });
  sp(0.3);
}

function NOTE(txt) {
  doc.fontSize(8).fillColor(DIM).font('Helvetica-Oblique')
     .text('☢  ' + txt, ML + 8, doc.y, { width: CW - 8 });
  sp(0.15);
}

function KV(k, v, kc, vc, kw) {
  kw = kw || 195;
  need(14);
  const y = doc.y;
  doc.fontSize(9).fillColor(kc || YLW).font('Helvetica-Bold')
     .text(k, ML + 4, y, { width: kw, lineBreak: false });
  doc.fontSize(9).fillColor(vc || WHT).font('Helvetica')
     .text(v, ML + kw + 6, y, { width: CW - kw - 6 });
  if (doc.y === y) { doc.y = y + 13; }
  sp(0.05);
}

// ─── Table helpers ─────────────────────────────────────────────────────────────
function TH(cols, ws) {
  need(17);
  const y = doc.y;
  doc.rect(ML, y, CW, 15).fill('#222222');
  let x = ML + 3;
  cols.forEach((c, i) => {
    doc.fontSize(7.5).fillColor(GRN).font('Helvetica-Bold')
       .text(c, x, y + 4, { width: ws[i], lineBreak: false });
    x += ws[i];
  });
  doc.y = y + 16;
}

function TR(vals, ws, cols, even) {
  need(15);
  const y = doc.y;
  if (even) doc.rect(ML, y, CW, 14).fill(PANEL);
  let x = ML + 3;
  vals.forEach((v, i) => {
    doc.fontSize(8.5).fillColor((cols && cols[i]) || WHT).font('Helvetica')
       .text(String(v), x, y + 2, { width: ws[i], lineBreak: false });
    x += ws[i];
  });
  doc.y = y + 15;
}

// ─── Recipe grid (computed height upfront, no mid-draw page breaks) ────────────
const CELL = 48, CGAP = 2;

function recipeGrid(title, rows, col, notes) {
  col = col || GRN;
  const numRows = rows.length;
  const numCols = Math.max(...rows.map(r => r.length));
  const gridH = numRows * (CELL + CGAP);
  const noteLines = notes ? notes.split('\n').length : 0;
  const totalH = 18 + gridH + noteLines * 11 + 10;

  need(totalH); // reserve ALL space before drawing anything

  const y0 = doc.y;
  // Title bar
  doc.rect(ML, y0, CW, 16).fill(PANEL);
  doc.fontSize(9.5).fillColor(col).font('Helvetica-Bold')
     .text('  ' + title, ML, y0 + 4, { width: CW, lineBreak: false });

  // Grid cells
  let gy = y0 + 18;
  rows.forEach(row => {
    for (let c = 0; c < numCols; c++) {
      const cx = ML + c * (CELL + CGAP);
      const cy = gy;
      doc.rect(cx, cy, CELL, CELL - 2).lineWidth(0.7).stroke(col);
      const item = (row[c] || '').trim();
      if (item) {
        const lines = item.split('\n');
        const lineH = 8;
        const totalTH = lines.length * lineH;
        lines.forEach((ln, li) => {
          doc.fontSize(6.5).fillColor(WHT).font('Helvetica')
             .text(ln.trim(), cx + 1, cy + (CELL - 2 - totalTH) / 2 + li * lineH,
                  { width: CELL - 2, align: 'center', lineBreak: false });
        });
      }
    }
    gy += CELL + CGAP;
  });

  if (notes) {
    doc.fontSize(8).fillColor(DIM).font('Helvetica')
       .text(notes, ML, gy + 2, { width: CW });
    doc.y = gy + noteLines * 11 + 8;
  } else {
    doc.y = gy + 6;
  }
  sp(0.3);
}

// Two recipe grids side-by-side
function twoGrids(t1, r1, c1, notes1, t2, r2, c2, notes2) {
  const SCELL = 42, SGAP = 2;
  const numR1 = r1.length, numR2 = r2.length;
  const gridH = Math.max(numR1, numR2) * (SCELL + SGAP);
  const note1Lines = notes1 ? notes1.split('\n').length : 0;
  const note2Lines = notes2 ? notes2.split('\n').length : 0;
  const notesH = Math.max(note1Lines, note2Lines) * 10;
  const totalH = 18 + gridH + notesH + 20;

  need(totalH);

  const y0 = doc.y;
  const halfW = (CW - 16) / 2;

  // Titles
  doc.fontSize(9.5).fillColor(c1 || GRN).font('Helvetica-Bold')
     .text(t1, ML, y0, { width: halfW, lineBreak: false });
  doc.fontSize(9.5).fillColor(c2 || CYAN).font('Helvetica-Bold')
     .text(t2, ML + halfW + 16, y0, { width: halfW, lineBreak: false });

  let gy = y0 + 16;

  // Draw left grid
  r1.forEach((row, ri) => {
    for (let c = 0; c < 3; c++) {
      const cx = ML + c * (SCELL + SGAP);
      const cy = gy + ri * (SCELL + SGAP);
      doc.rect(cx, cy, SCELL, SCELL - 1).lineWidth(0.7).stroke(c1 || GRN);
      const item = (row[c] || '').trim();
      if (item) {
        doc.fontSize(6).fillColor(WHT).font('Helvetica')
           .text(item, cx + 1, cy + SCELL / 2 - 7,
                { width: SCELL - 2, align: 'center', lineBreak: false });
      }
    }
  });

  // Draw right grid
  r2.forEach((row, ri) => {
    for (let c = 0; c < 3; c++) {
      const cx = ML + halfW + 16 + c * (SCELL + SGAP);
      const cy = gy + ri * (SCELL + SGAP);
      doc.rect(cx, cy, SCELL, SCELL - 1).lineWidth(0.7).stroke(c2 || CYAN);
      const item = (row[c] || '').trim();
      if (item) {
        doc.fontSize(6).fillColor(WHT).font('Helvetica')
           .text(item, cx + 1, cy + SCELL / 2 - 7,
                { width: SCELL - 2, align: 'center', lineBreak: false });
      }
    }
  });

  const gridBottom = gy + Math.max(numR1, numR2) * (SCELL + SGAP);

  if (notes1) {
    doc.fontSize(7.5).fillColor(DIM).font('Helvetica')
       .text(notes1, ML, gridBottom + 3, { width: halfW, lineBreak: true });
  }
  if (notes2) {
    doc.fontSize(7.5).fillColor(DIM).font('Helvetica')
       .text(notes2, ML + halfW + 16, gridBottom + 3, { width: halfW, lineBreak: true });
  }

  doc.y = gridBottom + notesH + 14;
  sp(0.2);
}

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 1 — COVER
// ══════════════════════════════════════════════════════════════════════════════
newPage();
doc.rect(24, 24, PW - 48, PH - 48).lineWidth(1.5).stroke(EDGE);
doc.rect(29, 29, PW - 58, PH - 58).lineWidth(0.5).stroke('#1e3c1e');
doc.y = 210;
doc.fontSize(44).fillColor(GRN).font('Helvetica-Bold').text('☢  NuclearCraft', { align:'center' });
doc.fontSize(30).fillColor(CYAN).font('Helvetica-Bold').text('Plutonium Age', { align:'center' });
sp(0.5);
doc.rect(110, doc.y, PW - 220, 2.5).fill(GRN);
sp(0.8);
doc.fontSize(18).fillColor(YLW).font('Helvetica-Bold').text('Complete Player Guide', { align:'center' });
sp(0.4);
doc.fontSize(11).fillColor(WHT).font('Helvetica')
   .text('Items & Materials  ·  All Crafting Recipes  ·  Machines', { align:'center' });
doc.fontSize(11).fillColor(WHT).font('Helvetica')
   .text('Radiation System  ·  Zombies  ·  Plutonium Titan Boss', { align:'center' });
doc.y = PH - 100;
doc.rect(110, doc.y, PW - 220, 1).fill(EDGE);
sp(0.5);
doc.fontSize(10).fillColor(DIM).font('Helvetica')
   .text('PaperMC 1.21+   ·   Plugin Version 1.0   ·   Phases 1–10', { align:'center' });
sp(0.3);
doc.fontSize(9).fillColor(DIM).font('Helvetica-Oblique')
   .text('All values configurable via YAML config files in /plugins/NuclearCraft/', { align:'center' });
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 2 — TABLE OF CONTENTS
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Table of Contents', YLW);
sp(0.3);
const toc = [
  ['Page 1',  'Cover'],
  ['Page 2',  'Table of Contents'],
  ['Page 3',  'Material Substitution Reference'],
  ['Page 4',  'All Custom Items — Materials, Tools & Consumables'],
  ['Page 5',  'All Custom Items — Equipment & Boss Items'],
  ['Page 6',  'Crafting Recipes — Machines & Blocks'],
  ['Page 7',  'Crafting Recipes — Plutonium Tools & Weapons'],
  ['Page 8',  'Crafting Recipes — Plutonium Armor'],
  ['Page 9',  'Crafting Recipes — Hazmat Suit & Cures'],
  ['Page 10', 'Radiation System — Stages, Sources & Spread'],
  ['Page 11', 'Radiation System — Protection, Cures & Visuals'],
  ['Page 12', 'Nuclear Smelter — Machine Guide'],
  ['Page 13', 'Nuclear Forge — Upgrade Machine Guide'],
  ['Page 14', 'World — Ore, Mining & World Generation'],
  ['Page 15', 'Radioactive Farming & Cure System'],
  ['Page 16', 'Irradiated Zombies — Levels, Loot & Events'],
  ['Page 17', 'Plutonium Titan — Boss Guide'],
  ['Page 18', 'Combat System, Mastery & Repair'],
  ['Page 19', 'Admin Commands & Config Overview'],
];
TH(['Page', 'Section'], [70, CW - 70]);
toc.forEach(([pg, sec], i) => TR([pg, sec], [70, CW - 70], [YLW, WHT], i % 2 === 0));
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 3 — MATERIAL SUBSTITUTION REFERENCE
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Material Substitution Reference', YLW);
P('NuclearCraft custom items cannot be placed directly in vanilla crafting grids because\nMinecraft\'s recipe engine does not understand CustomModelData. Each custom item uses a specific\nvanilla material as a proxy in crafting. The plugin intercepts the crafting event and checks\nthe real item via its CustomModelData tag before allowing the recipe to proceed.', WHT);
NOTE('Always obtain the custom item first (from gameplay or /nc give commands). A plain vanilla Echo Shard without the correct CMD tag will NOT work in recipes.');
sp(0.3);

H2('Crafting Grid Proxies', YLW);
TH(['Vanilla Material (place in grid)', 'Custom Item It Represents', 'CMD'], [215, 215, 75]);
[
  ['Echo Shard',             'Refined Plutonium Ingot',                    '1103'],
  ['Yellow Wool',            'Industrial Fabric',                           '1315'],
  ['Magma Cream',            'Radioactive Core',                            '1101'],
  ['Prismarine Crystals',    'Raw Plutonium Fragment',                      '1102'],
  ['Heart of the Sea',       'Irradiated Heart',                            '1106'],
  ['Pink Petals',            'Healing Petal',                               '1105'],
  ['Wheat Seeds',            'Mutated Seed',                                '1104'],
  ['Honey Bottle',           'Radiation Antidote (consumable)',             '1301'],
  ['Glass Bottle',           'Radiation Serum (consumable)',                '1302'],
].forEach(([v, c, cmd], i) => TR([v, c, cmd], [215, 215, 75], [YLW, CYAN, DIM], i % 2 === 0));

H2('Block & Equipment Base Materials', CYAN);
TH(['Vanilla Base', 'What It Represents', 'CMD'], [220, 215, 70]);
[
  ['Blast Furnace (block)',              'Nuclear Smelter (machine block)',           '1201'],
  ['Smithing Table (block)',             'Nuclear Forge (machine block)',             '1401'],
  ['Diamond Pickaxe',                   'Radiation Drill (tool)',                    '1108'],
  ['Leather Helmet',                    'Hazmat Helmet',                             '1306'],
  ['Leather Chestplate',                'Hazmat Chestplate',                         '1307'],
  ['Leather Leggings',                  'Hazmat Leggings',                           '1308'],
  ['Leather Boots',                     'Hazmat Boots',                              '1309'],
  ['Netherite Helmet',                  'Plutonium Helmet',                          '3001'],
  ['Netherite Chestplate',              'Plutonium Chestplate',                      '3002'],
  ['Netherite Leggings',                'Plutonium Leggings',                        '3003'],
  ['Netherite Boots',                   'Plutonium Boots',                           '3004'],
  ['Netherite Sword',                   'Plutonium Sword',                           '2002'],
  ['Netherite Pickaxe',                 'Plutonium Pickaxe',                         '2001'],
  ['Netherite Axe',                     'Plutonium Axe',                             '2003'],
  ['Netherite Shovel',                  'Plutonium Shovel',                          '2004'],
  ['Netherite Hoe',                     'Plutonium Hoe',                             '2005'],
  ['Arrow (vanilla)',                   'Plutonium Arrow (ingredient role)',          '—'],
  ['Nether Star',                       'Titan Core  OR  Reactor Heart (by context)','1107/1109'],
  ['Amethyst Shard',                    'Titan Fragment  OR  Mutated Crystal',       '1108/1111'],
  ['Paper',                             'Ancient Reactor Blueprint',                 '1110'],
].forEach(([v, c, cmd], i) => TR([v, c, cmd], [220, 215, 70], [YLW, WHT, DIM], i % 2 === 0));
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 4 — ALL CUSTOM ITEMS (MATERIALS, MACHINES, CONSUMABLES)
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('All Custom Items — Materials, Machines & Consumables', CYAN);

H2('Raw Materials & Components', GRN);
TH(['Item Name', 'Vanilla Base', 'CMD', 'Description'], [150, 130, 45, CW - 325]);
[
  ['Raw Plutonium Fragment',  'Prismarine Crystals','1102','Mined from Plutonium Ore (Y -64 to -58) using Radiation Drill. 1–4 per ore with Fortune. Primary input for the Nuclear Smelter. Emits low radiation while in inventory.'],
  ['Refined Plutonium Ingot', 'Echo Shard',         '1103','Output of the Nuclear Smelter (1 Fragment → 1 Ingot, 15 seconds). Primary crafting material for all Plutonium weapons, tools, and armor. Also the repair material for Plutonium gear (+300 durability per Anvil use).'],
  ['Radioactive Core',        'Magma Cream',         '1101','Dropped by Irradiated Zombies. Serves as fuel for the Nuclear Forge (2,000 energy per Core). Also ingredient in several recipes. High radiation source — store in Lead-Lined Crate.'],
  ['Irradiated Heart',        'Heart of the Sea',    '1106','Rare drop from Irradiated Zombies. Required for MK-3 (1 Heart) and MK-4 (2 Hearts) Nuclear Forge upgrades. Also consumed in the Titan summoning ritual (1 Heart). Emits strong radiation.'],
  ['Industrial Fabric',       'Yellow Wool',         '1315','Radiation-hardened woven material. Used to craft all four Hazmat Suit pieces. Also the repair material for Hazmat armor in an Anvil (+200 durability per repair).'],
  ['Mutated Seed',            'Wheat Seeds',         '1104','Dropped by Irradiated Zombies. Plant ONLY on Radioactive Farmland (not normal farmland). Grows into Mutated Healing Plants through 5 stages. Stage 4 harvest yields Healing Petals.'],
  ['Healing Petal',           'Pink Petals',         '1105','Harvested from fully grown Mutated Healing Plants (1–3 per harvest). Primary ingredient for Radiation Antidote and Radiation Serum. Right-clicking it directly gives Regeneration II for 5s and removes 25 radiation.'],
].forEach(([n, b, cmd, d], i) => {
  need(28);
  const y = doc.y;
  const rowH = 26;
  if (i % 2 === 0) doc.rect(ML, y, CW, rowH).fill(PANEL);
  doc.fontSize(9).fillColor(GRN).font('Helvetica-Bold').text(n, ML + 3, y + 2, { width: 147, lineBreak: false });
  doc.fontSize(8.5).fillColor(YLW).font('Helvetica').text(b, ML + 153, y + 2, { width: 127, lineBreak: false });
  doc.fontSize(8.5).fillColor(DIM).font('Helvetica').text(cmd, ML + 283, y + 2, { width: 42, lineBreak: false });
  doc.fontSize(8).fillColor(WHT).font('Helvetica').text(d, ML + 328, y + 2, { width: CW - 328 });
  doc.y = Math.max(doc.y, y + rowH);
  sp(0.05);
});

H2('Machines & Tools', YLW);
[
  { n:'Nuclear Smelter',    b:'Blast Furnace',   cmd:'1201', c:YLW,
    d:'Industrial refining machine. Converts Raw Plutonium Fragments into Refined Plutonium Ingots (15 seconds each). Requires fuel. Minimum 500°C before processing begins. Emits +8 radiation per 5 seconds within 3 blocks while active. Right-click to open GUI.' },
  { n:'Nuclear Forge',      b:'Smithing Table',  cmd:'1401', c:ORG,
    d:'Advanced equipment upgrade machine. Upgrades Plutonium tools/armor and Hazmat gear from base to MK-1, MK-2, MK-3, and MK-4. Each tier improves damage, speed, armor, and durability. Powered by Radioactive Cores (2,000 energy each). Right-click to open GUI.' },
  { n:'Radiation Drill',    b:'Diamond Pickaxe', cmd:'1108', c:CYAN,
    d:'The ONLY tool that can safely mine Plutonium Ore. Mining ore with any other tool gives +25 radiation burst and drops nothing. 20% faster than Diamond Pickaxe (×1.2 speed modifier). 1,800 durability. Also has 10% chance to uncover extra Radioactive Debris while mining.' },
].forEach(({ n, b, cmd, c, d }) => {
  need(44);
  const y = doc.y;
  doc.rect(ML, y, CW, 40).fill(PANEL);
  doc.rect(ML, y, 3, 40).fill(c);
  doc.fontSize(10).fillColor(c).font('Helvetica-Bold').text(n, ML + 8, y + 3, { width: 210, lineBreak: false });
  doc.fontSize(8.5).fillColor(DIM).font('Helvetica').text(b + '  ·  CMD: ' + cmd, ML + 225, y + 4, { width: CW - 230, lineBreak: false });
  doc.fontSize(9).fillColor(WHT).font('Helvetica').text(d, ML + 8, y + 17, { width: CW - 14 });
  doc.y = y + 44; sp(0.1);
});

H2('Consumables & Cures', LIME);
[
  { n:'Radiation Antidote', b:'Honey Bottle', cmd:'1301', c:LIME,
    d:'Right-click to consume instantly. Clears ALL radiation points, removes infection status, and removes all active radiation debuffs (Weakness, Slowness, Hunger, Nausea). Does NOT grant any immunity — you can be infected again immediately. Crafted with 2 Healing Petals + 1 Honey Bottle (shapeless).' },
  { n:'Radiation Serum',    b:'Glass Bottle', cmd:'1302', c:PRP,
    d:'Right-click to consume. Instantly clears ALL radiation and all debuffs (same as Antidote) AND grants 10 full minutes of radiation immunity — you cannot receive any radiation during this window. More expensive and rarer to craft than the Antidote.' },
].forEach(({ n, b, cmd, c, d }) => {
  need(44);
  const y = doc.y;
  doc.rect(ML, y, CW, 40).fill(PANEL);
  doc.rect(ML, y, 3, 40).fill(c);
  doc.fontSize(10).fillColor(c).font('Helvetica-Bold').text(n, ML + 8, y + 3, { width: 210, lineBreak: false });
  doc.fontSize(8.5).fillColor(DIM).font('Helvetica').text(b + '  ·  CMD: ' + cmd, ML + 225, y + 4, { width: CW - 230, lineBreak: false });
  doc.fontSize(9).fillColor(WHT).font('Helvetica').text(d, ML + 8, y + 17, { width: CW - 14 });
  doc.y = y + 44; sp(0.1);
});
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 5 — ALL CUSTOM ITEMS (EQUIPMENT & BOSS ITEMS)
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('All Custom Items — Equipment & Boss Items', CYAN);

H2('Plutonium Tools & Weapons', RED);
TH(['Item', 'Base', 'CMD', 'Key Stats'], [160, 120, 45, CW - 325]);
[
  ['Plutonium Sword',    'Netherite Sword',   '2002', '+10 DMG  ·  Speed 1.7/s  ·  2,500 dur  ·  +10 rad/hit  ·  Crit: +5 bonus rad  ·  vs Stage 3+: +5 bonus rad'],
  ['Plutonium Axe',      'Netherite Axe',     '2003', '+11 DMG  ·  Speed 1.0/s  ·  2,600 dur  ·  15% shockwave (3-block AoE +15 rad burst)'],
  ['Plutonium Pickaxe',  'Netherite Pickaxe', '2001', '2,800 dur  ·  10% chance to uncover Radioactive Debris  ·  Requires Radiation Drill for Plutonium Ore'],
  ['Plutonium Shovel',   'Netherite Shovel',  '2004', '2,500 dur  ·  15% chance to convert dug soil to Radioactive Farmland'],
  ['Plutonium Hoe',      'Netherite Hoe',     '2005', '2,000 dur  ·  Fast crop tilling'],
  ['Plutonium Arrow',    'Arrow (×4 output)', '—',    '+25 rad on hit  ·  Poison I 4s  ·  Glowing 5s  ·  Full-charge crit: +20 bonus rad  ·  Headshot: +10 bonus rad'],
  ['Radiation Drill',    'Diamond Pickaxe',   '1108', '1,800 dur  ·  ×1.2 mining speed  ·  Only safe mining tool for Plutonium Ore'],
].forEach(([n, b, cmd, stats], i) => TR([n, b, cmd, stats], [160, 120, 45, CW - 325], [RED, YLW, DIM, WHT], i % 2 === 0));

H2('Plutonium Armor Set', PRP);
P('All Plutonium Armor uses Netherite as base material. All four pieces share: Protection III + Unbreaking III enchantments. Each piece provides -15% radiation reduction.', DIM);
P('FULL SET BONUS (all 4 pieces): Complete environmental radiation IMMUNITY + Speed II + Fire Resistance (permanent while worn). Set uses Netherite-tier stats.', LIME);
TH(['Piece', 'Base', 'CMD', 'Armor', 'Toughness', 'Durability', 'Special'], [140, 110, 45, 50, 70, 75, CW - 490]);
[
  ['Plutonium Helmet',     'Netherite Helmet',     '3001', '4',  '2', '450', 'Night Vision (passive)'],
  ['Plutonium Chestplate', 'Netherite Chestplate', '3002', '8',  '2', '640', 'Regen (passive)'],
  ['Plutonium Leggings',   'Netherite Leggings',   '3003', '6',  '2', '590', 'Speed (passive)'],
  ['Plutonium Boots',      'Netherite Boots',       '3004', '4',  '2', '480', 'Feather Fall II  ·  No ground radiation'],
].forEach(([n, b, cmd, ar, to, du, sp2], i) => TR([n, b, cmd, ar, to, du, sp2], [140, 110, 45, 50, 70, 75, CW - 490], [PRP, YLW, DIM, WHT, WHT, WHT, GRN], i % 2 === 0));

H2('Hazmat Suit', YLW);
P('Hazmat Suit uses Leather armor as base. Provides strong radiation protection without requiring Plutonium Ingots. Full set provides 80% radiation reduction.', DIM);
P('FULL SET BONUS: 80% total radiation reduction (best non-plutonium radiation shield).', YLW);
TH(['Piece', 'Base', 'CMD', 'Armor', 'Toughness', 'Durability', 'Rad Reduction'], [140, 110, 45, 50, 70, 75, CW - 490]);
[
  ['Hazmat Helmet',     'Leather Helmet',     '1306', '2',  '0.5', '363', '-20%'],
  ['Hazmat Chestplate', 'Leather Chestplate', '1307', '5',  '0.5', '529', '-30%'],
  ['Hazmat Leggings',   'Leather Leggings',   '1308', '4',  '0.5', '496', '-20%'],
  ['Hazmat Boots',      'Leather Boots',      '1309', '2',  '0.5', '430', '-10%'],
].forEach(([n, b, cmd, ar, to, du, rr], i) => TR([n, b, cmd, ar, to, du, rr], [140, 110, 45, 50, 70, 75, CW - 490], [YLW, YLW, DIM, WHT, WHT, WHT, GRN], i % 2 === 0));

H2('Boss Items (Dropped by Plutonium Titan)', PRP);
[
  { n:'Titan Core',                b:'Nether Star',   cmd:'1107', c:RED,
    d:'4 Titan Cores consumed in summoning ritual. Extreme radiation. Can also be used in high-tier Phase 11 crafting.' },
  { n:'Titan Fragment',            b:'Amethyst Shard', cmd:'1108', c:PRP,
    d:'Drops from Titan on kill. Scales with contribution. Used in future high-tier crafting (Phase 11+). Extreme radiation.' },
  { n:'Reactor Heart',             b:'Nether Star',   cmd:'1109', c:RED,
    d:'10% chance per qualifying player. REQUIRED for MK-3 (1 Heart) and MK-4 (2 Hearts) Nuclear Forge upgrades.' },
  { n:'Mutated Crystal',           b:'Amethyst Shard', cmd:'1111', c:CYAN,
    d:'8% chance per qualifying player. Used in future crafting (Phase 11+).' },
  { n:'Ancient Reactor Blueprint', b:'Paper',          cmd:'1110', c:YLW,
    d:'5% chance per qualifying player. Unlocks advanced Phase 11 reactor crafting. No current gameplay use — save it.' },
].forEach(({ n, b, cmd, c, d }) => {
  need(32);
  const y = doc.y;
  doc.rect(ML, y, CW, 28).fill(PANEL);
  doc.fontSize(9.5).fillColor(c).font('Helvetica-Bold').text(n + '  ', ML + 4, y + 3, { width: 200, lineBreak: false });
  doc.fontSize(8).fillColor(DIM).font('Helvetica').text('[' + b + ' · CMD ' + cmd + ']', ML + 206, y + 4, { width: CW - 210, lineBreak: false });
  doc.fontSize(8.5).fillColor(WHT).font('Helvetica').text(d, ML + 4, y + 15, { width: CW - 8 });
  doc.y = y + 31; sp(0.1);
});
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 6 — CRAFTING RECIPES: MACHINES & BLOCKS
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Crafting Recipes — Machines & Blocks', GRN);
P('All recipes crafted at a standard Crafting Table unless otherwise noted. Remember: proxy materials in grids must be the actual custom items with their CustomModelData tag.', DIM);
sp(0.3);

H2('Nuclear Smelter', YLW);
P('D = Diamond   O = Obsidian   F = Furnace (vanilla)', DIM);
recipeGrid('Nuclear Smelter',
  [['Diamond',  'Obsidian', 'Diamond'],
   ['Obsidian', 'Furnace',  'Obsidian'],
   ['Diamond',  'Obsidian', 'Diamond']],
  YLW, 'Result: 1× Nuclear Smelter block');

H2('Nuclear Forge', ORG);
P('I = Echo Shard (= Refined Plutonium Ingot)   D = Ancient Debris   O = Obsidian   S = Smithing Table (vanilla)', DIM);
recipeGrid('Nuclear Forge',
  [['Obsidian',   'Anc.Debris', 'Obsidian'],
   ['Ref. Ingot', 'Sm. Table',  'Ref. Ingot'],
   ['Obsidian',   'Anc.Debris', 'Obsidian']],
  ORG, 'Result: 1× Nuclear Forge block   |   Ref. Ingot = Echo Shard proxy');

H2('Additional Craftable Blocks', CYAN);
twoGrids(
  'Plutonium Block',
  [['P','P','P'],['P','P','P'],['P','P','P']],
  LIME, 'P = Echo Shard (Refined Ingot)\nResult: 1× Plutonium Block\n(compact storage — emits more radiation than loose ingots)',
  'Lead-Lined Storage Crate',
  [['Iron','Iron','Iron'],['Iron','Chest','Iron'],['Iron','Iron','Iron']],
  CYAN, 'Result: 1× Lead-Lined Crate\nStores fragments with ZERO radiation\nleak to nearby players'
);
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 7 — CRAFTING RECIPES: PLUTONIUM TOOLS & WEAPONS
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Crafting Recipes — Plutonium Tools & Weapons', GRN);
P('P = Echo Shard (= Refined Plutonium Ingot)   S = Stick   R = Magma Cream (= Radioactive Core)', DIM);
sp(0.2);

H2('Radiation Drill', CYAN);
recipeGrid('Radiation Drill  —  Only safe mining tool for Plutonium Ore',
  [['R-Core', 'Diamond', 'R-Core'],
   ['Diamond','Diamond', 'Diamond'],
   ['',       'Stick',   '']],
  CYAN, 'R-Core = Radioactive Core (Magma Cream proxy)  |  1,800 durability  |  ×1.2 mining speed');

H2('Plutonium Weapons', RED);
twoGrids(
  'Plutonium Sword',
  [['','P',''],['','P',''],['','S','']],
  RED, '+10 DMG  ·  Speed 1.7/s\n2,500 durability\n+10 rad/hit  ·  Crit: +5 bonus',
  'Plutonium Axe',
  [['P','P',''],['P','S',''],['','S','']],
  ORG, '+11 DMG  ·  Speed 1.0/s\n2,600 durability\n15% shockwave (3-block +15 rad)'
);

H2('Plutonium Tools', GRN);
twoGrids(
  'Plutonium Pickaxe',
  [['P','P','P'],['','S',''],['','S','']],
  CYAN, '2,800 durability\n10% debris find chance\nNote: still needs Radiation Drill for Plutonium Ore',
  'Plutonium Shovel',
  [['','P',''],['','S',''],['','S','']],
  LIME, '2,500 durability\n15% soil → Radioactive Farmland\non each dig'
);

twoGrids(
  'Plutonium Hoe',
  [['P','P',''],['','S',''],['','S','']],
  GRN, '2,000 durability\nFast crop tilling',
  'Plutonium Arrow  (×4 output)',
  [['','P',''],['','Arrow',''],['','Feather','']],
  GRN, 'P = Echo Shard proxy\n+25 rad on hit  ·  Poison I (4s)\nGlowing (5s)  ·  Crit: +20 bonus rad'
);
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 8 — CRAFTING RECIPES: PLUTONIUM ARMOR
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Crafting Recipes — Plutonium Armor', PRP);
P('P = Echo Shard (= Refined Plutonium Ingot) in all slots shown.\nAll four pieces have: Protection III + Unbreaking III. Full 4/4 set grants environmental radiation IMMUNITY + Speed II + Fire Resistance.', DIM);
sp(0.3);

H2('Helmet & Chestplate', PRP);
twoGrids(
  'Plutonium Helmet',
  [['P','P','P'],['P','','P'],['','','']],
  PRP, '4 Armor  ·  2 Toughness  ·  450 dur\n-15% radiation exposure\nPassive Night Vision',
  'Plutonium Chestplate',
  [['P','','P'],['P','P','P'],['P','P','P']],
  PRP, '8 Armor  ·  2 Toughness  ·  640 dur\n-15% radiation exposure\nPassive Regeneration'
);

H2('Leggings & Boots', PRP);
twoGrids(
  'Plutonium Leggings',
  [['P','P','P'],['P','','P'],['P','','P']],
  PRP, '6 Armor  ·  2 Toughness  ·  590 dur\n-15% radiation exposure\nPassive Speed boost',
  'Plutonium Boots',
  [['','',''],['P','','P'],['P','','P']],
  PRP, '4 Armor  ·  2 Toughness  ·  480 dur\n-15% radiation  ·  Feather Fall II\nNO ground-contact radiation'
);

H2('Repair', LIME);
KV('Repair material for Plutonium gear', 'Refined Plutonium Ingot (Echo Shard proxy) — use Anvil', LIME, WHT, 240);
KV('Durability restored per ingot', '+300 durability per ingot used in Anvil', LIME, WHT, 240);
sp(0.3);

H2('Plutonium Armor Full Set Stats Summary', YLW);
TH(['Piece', 'Armor', 'Toughness', 'Durability', 'Rad Reduction', 'Passives'], [140, 50, 75, 75, 90, CW - 430]);
[
  ['Plutonium Helmet',     '4', '2', '450', '-15%', 'Night Vision'],
  ['Plutonium Chestplate', '8', '2', '640', '-15%', 'Regeneration'],
  ['Plutonium Leggings',   '6', '2', '590', '-15%', 'Speed'],
  ['Plutonium Boots',      '4', '2', '480', '-15%', 'Feather Fall II  ·  No ground rad'],
  ['FULL SET BONUS',       '—', '—', '—',   'IMMUNE','Speed II + Fire Resistance (permanent)'],
].forEach(([n, ar, to, du, rr, pass], i) => {
  const c = n.startsWith('FULL') ? GRN : PRP;
  TR([n, ar, to, du, rr, pass], [140, 50, 75, 75, 90, CW - 430], [c, WHT, WHT, WHT, GRN, LIME], i % 2 === 0);
});
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 9 — CRAFTING RECIPES: HAZMAT SUIT & CURES
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Crafting Recipes — Hazmat Suit & Cure Items', YLW);
P('W = Yellow Wool (= Industrial Fabric proxy). Hazmat Suit uses Leather armor as base.', DIM);
sp(0.2);

H2('Hazmat Helmet & Chestplate', YLW);
twoGrids(
  'Hazmat Helmet',
  [['W','W','W'],['W','','W'],['','','']],
  YLW, '2 Armor  ·  0.5 Tough  ·  363 dur\n-20% radiation reduction\nLeather base material',
  'Hazmat Chestplate',
  [['W','','W'],['W','W','W'],['W','W','W']],
  YLW, '5 Armor  ·  0.5 Tough  ·  529 dur\n-30% radiation reduction\nBest single Hazmat piece'
);

H2('Hazmat Leggings & Boots', YLW);
twoGrids(
  'Hazmat Leggings',
  [['W','W','W'],['W','','W'],['W','','W']],
  YLW, '4 Armor  ·  0.5 Tough  ·  496 dur\n-20% radiation reduction',
  'Hazmat Boots',
  [['','',''],['W','','W'],['W','','W']],
  YLW, '2 Armor  ·  0.5 Tough  ·  430 dur\n-10% radiation reduction'
);

H2('Hazmat Suit Repair & Full Set', ORG);
KV('Repair material for Hazmat Suit', 'Industrial Fabric (Yellow Wool proxy) — use Anvil', YLW, WHT, 240);
KV('Durability per repair', '+200 durability per Industrial Fabric used', YLW, WHT, 240);
KV('Full 4-piece set bonus', '-80% total radiation reduction', GRN, GRN, 240);
sp(0.3);

H2('Radiation Antidote  (Shapeless Recipe)', LIME);
P('Shapeless = ingredients can be placed in ANY slots in the crafting grid, in any order.', DIM);
recipeGrid('Radiation Antidote  —  SHAPELESS (any arrangement)',
  [['Healing\nPetal', 'Healing\nPetal', 'Honey\nBottle']],
  LIME, 'Result: 1× Radiation Antidote  |  Effect: Clears ALL radiation, infection & debuffs instantly  |  Does NOT grant immunity');

H2('Radiation Serum  (Shaped Recipe)', PRP);
P('H = Healing Petal   R = Magma Cream (Radioactive Core)   G = Gold Nugget   A = Golden Apple   B = Glass Bottle', DIM);
recipeGrid('Radiation Serum',
  [['Heal.Petal', 'R-Core',     'Heal.Petal'],
   ['Gold Nugget','Golden Apple','Gold Nugget'],
   ['Heal.Petal', 'Glass Bottle','Heal.Petal']],
  PRP, 'Result: 1× Radiation Serum  |  Effect: Clears ALL radiation + grants 10 MINUTES of full radiation immunity\nR-Core = Radioactive Core (Magma Cream proxy)  |  Golden Apple = vanilla Golden Apple');

H2('Nuclear Smelter Processing Recipe', YLW);
KV('Input', '1× Raw Plutonium Fragment', YLW, WHT, 120);
KV('Output', '1× Refined Plutonium Ingot', YLW, GRN, 120);
KV('Processing time', '15 seconds (300 ticks at 20 TPS)', YLW, WHT, 120);
KV('Required machine', 'Nuclear Smelter at ≥500°C with fuel loaded', YLW, WHT, 120);
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 10 — RADIATION SYSTEM: STAGES & SOURCES
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Radiation System — Stages & Sources', RED);
P('Radiation is measured in points from 0 to 1000. It accumulates through exposure to radioactive materials, zombie attacks, machines, and the Titan boss. As radiation rises, you progress through increasingly dangerous stages with worsening debuffs and eventual direct damage.', WHT);
sp(0.3);

H2('Radiation Stages', RED);
TH(['Stage', 'Name', 'Points', 'Debuffs & Effects'], [50, 140, 75, CW - 265]);
[
  ['0', 'Healthy',              '0 – 99',    'No effects. Normal gameplay.',                                                            GRN],
  ['1', 'Minor Exposure',       '100 – 249', 'Weakness I  ·  Nausea',                                                                  YLW],
  ['2', 'Moderate Exposure',    '250 – 499', 'Weakness I  ·  Slowness I  ·  Nausea  ·  5% proximity spread chance',                   ORG],
  ['3', 'Severe Exposure',      '500 – 749', 'Weakness II  ·  Slowness II  ·  Hunger I  ·  0.5 HP damage per cycle  ·  15% spread', '#ff5500'],
  ['4', 'Critical Poisoning',   '750 – 1000','Weakness III  ·  Slowness III  ·  Hunger II  ·  1.0 HP damage per cycle  ·  25% spread  ·  Death message on fatal damage', RED],
].forEach(([s, n, p, e, c], i) => TR([s, n, p, e], [50, 140, 75, CW - 265], [c, WHT, YLW, WHT], i % 2 === 0));

H2('Natural Progression & Decay', CYAN);
KV('Natural progression', 'While infected: Stage 1 +5pts/60s  ·  Stage 2 +10pts/60s  ·  Stage 3 +15pts/60s  ·  Stage 4 +20pts/60s', CYAN, WHT, 170);
KV('Natural decay', 'Requires 10 minutes with NO new radiation → -2 points per 60 seconds until clear', CYAN, WHT, 170);
sp(0.3);

H2('Complete Radiation Sources', RED);
TH(['Source', 'Radiation', 'Trigger / Interval'], [240, 80, CW - 320]);
[
  ['Plutonium Ore — within 1 block',         '+5 pts',   'Every 5 seconds passively'],
  ['Plutonium Ore — within 2 blocks',         '+2 pts',   'Every 5 seconds passively'],
  ['Plutonium Ore — within 3 blocks',         '+1 pt',    'Every 5 seconds passively'],
  ['Mining without Radiation Drill',           '+25 burst','Per attempt — 5 second cooldown'],
  ['Raw Plutonium Fragment in inventory',      '+1 pt',    'Every 30 seconds'],
  ['Plutonium Block — within 1 block',         '+10 pts',  'Every 5 seconds'],
  ['Plutonium Block — within 2 blocks',        '+5 pts',   'Every 5 seconds'],
  ['Plutonium Block — within 3 blocks',        '+2 pts',   'Every 5 seconds'],
  ['Radioactive Debris — within 5 blocks',     '+0.5/s',   'Continuous passive'],
  ['Nuclear Smelter active — 3-block radius',  '+8 pts',   'Every 5 seconds'],
  ['Radiation Cloud (on zombie death)',         '+5/s',     '10 seconds, 3-block radius — 20% spawn chance'],
  ['Irradiated Zombie Lv.1 melee hit',         '+10 pts',  'Per hit'],
  ['Irradiated Zombie Lv.2 melee hit',         '+20 pts',  'Per hit'],
  ['Irradiated Zombie Lv.3 melee hit',         '+35 pts',  'Per hit'],
  ['Alpha Zombie Lv.4 melee hit',              '+50 pts',  'Per hit'],
  ['Irradiated Zombie aura (proximity)',        '+2 pts',   'Every 5 seconds within range'],
  ['Titan Boss — Radiation Aura (passive)',     '+3/s',     '10-block radius, all phases continuously'],
  ['Titan Boss — Titan Slam (Phase 1+)',        '+15 pts',  'Per slam hit on player — 5-block radius'],
  ['Titan Boss — Radiation Wave (Phase 2+)',    '+40 pts',  '20-block radius burst — 15s cooldown'],
  ['Titan Boss — Reactor Overload (Phase 3+)', '+70 pts',  '18-block radius burst — 40s cooldown'],
  ['Titan Boss — Energy Beam (Phase 3+)',       '+60 pts',  'Targeted player direct hit — 20s cooldown'],
  ['Titan Boss — Nuclear Catastrophe (Ph.4)',  '+80 pts',  '30-block radius + sets all HP to 1 — 90s CD'],
  ['Nuclear Forge Overload',                   '+150 pts', '8-block radius burst when Forge overloads'],
  ['MK-IV Equipment Aura (enemy players)',      '+15/tick', 'Every 2 seconds within 3 blocks'],
  ['Toxic Bloom (farming hazard)',              '+8 pts',   'Every 2 seconds, 4-block radius'],
].forEach(([s, r, t], i) => TR([s, r, t], [240, 80, CW - 320], [WHT, RED, DIM], i % 2 === 0));
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 11 — RADIATION SYSTEM: PROTECTION, CURES & CONTAGION
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Radiation System — Protection, Cures & Contagion', RED);

H2('Armor Radiation Protection', CYAN);
P('Armor reduces all incoming radiation exposure by a flat percentage multiplier per piece worn.', WHT);
TH(['Armor / Situation', 'Radiation Reduction', 'Notes'], [220, 130, CW - 350]);
[
  ['Any single armor piece (generic)',       '-15% per piece',  'Stacks: 4 pieces = up to -60% total'],
  ['Hazmat Helmet',                          '-20%',            'Leather base'],
  ['Hazmat Chestplate',                      '-30%',            'Best single piece for radiation protection'],
  ['Hazmat Leggings',                        '-20%',            'Leather base'],
  ['Hazmat Boots',                           '-10%',            'Leather base'],
  ['Full Hazmat Suit (all 4 pieces)',         '-80% TOTAL',      'Best accessible radiation protection'],
  ['Plutonium Helmet only',                  '-15%',            'Netherite base'],
  ['Plutonium Armor — full set (4/4)',        'FULL IMMUNITY',   'Zero environmental radiation while worn'],
  ['Plutonium Boots special property',        'Ground radiation','Negates radioactive farmland & ground sources'],
  ['Near Plutonium Ore with Hazmat Suit',     '×0.20 multiplier','80% of ore radiation blocked'],
  ['Near Plutonium Ore with Plutonium Armor', '×0.00 multiplier','Complete immunity to ore radiation zones'],
].forEach(([a, r, n], i) => TR([a, r, n], [220, 130, CW - 350], [WHT, GRN, DIM], i % 2 === 0));

H2('Cure & Immunity Summary', LIME);
TH(['Cure Method', 'How to Use', 'Effect'], [170, 115, CW - 285]);
[
  ['Healing Petal (direct use)',   'Right-click to consume',  'Removes 25 radiation  ·  Regeneration II for 5 seconds'],
  ['Radiation Antidote',          'Right-click to consume',  'Clears ALL radiation, all debuffs & infection  ·  No immunity granted'],
  ['Radiation Serum',             'Right-click to consume',  'Clears ALL radiation & debuffs  ·  10 min full radiation immunity'],
  ['Plutonium Armor (full set)',   'Wear all 4 pieces',       'Permanent immunity to all environmental radiation while worn'],
  ['Natural decay (passive)',      'Avoid radiation for 10m','−2 radiation per 60 seconds until Stage 0'],
].forEach(([c, h, e], i) => TR([c, h, e], [170, 115, CW - 285], [LIME, DIM, WHT], i % 2 === 0));

H2('Contagion — Radiation Spreading Between Players', RED);
P('Irradiated players at Stage 2 or higher can spread radiation to others through proximity, contact, or shared vehicles. Even healthy players standing near infected ones are at risk.', WHT);
TH(['Spread Method', 'Condition', 'Radiation', 'Chance'], [195, 145, 70, CW - 410]);
[
  ['Proximity — within 3 blocks',        'Infected at Stage 2', '+25 pts', '5% per 5 seconds'],
  ['Proximity — within 3 blocks',        'Infected at Stage 3', '+25 pts', '15% per 5 seconds'],
  ['Proximity — within 3 blocks',        'Infected at Stage 4', '+25 pts', '25% per 5 seconds'],
  ['Physical contact — melee hit',        'Any stage',           '+50 pts', '35% per hit'],
  ['Shared vehicle (boat or minecart)',   'Any stage',           '+25 pts', '20% per 15 seconds'],
].forEach(([m, c, r, ch], i) => TR([m, c, r, ch], [195, 145, 70, CW - 410], [RED, DIM, RED, YLW], i % 2 === 0));

H2('Visual & Audio Indicators', DIM);
P('Each radiation stage displays different coloured particles around the player:', WHT);
KV('Stage 1 — Minor',    'Bright green particles  [RGB: 57, 255, 20]',   GRN,  WHT, 180);
KV('Stage 2 — Moderate', 'Yellow particles  [RGB: 255, 220, 0]',          YLW,  WHT, 180);
KV('Stage 3 — Severe',   'Orange particles  [RGB: 255, 100, 0]',          ORG,  WHT, 180);
KV('Stage 4 — Critical', 'Dark red particles  [RGB: 200, 0, 0]',          RED,  WHT, 180);
P('A Geiger counter clicking sound effect plays and intensifies as your radiation level rises.', DIM);
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 12 — NUCLEAR SMELTER GUIDE
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Nuclear Smelter — Complete Machine Guide', YLW);

H2('Overview', YLW);
P('The Nuclear Smelter converts Raw Plutonium Fragments into Refined Plutonium Ingots — the primary crafting material for all Plutonium-tier equipment. It is an industrial machine that requires fuel and must reach a minimum operating temperature before processing begins.', WHT);
sp(0.3);

H2('Placement & Usage Steps', CYAN);
P('1. Craft the Nuclear Smelter (Diamond + Obsidian + Furnace — see Page 6)\n2. Place it like any block in the world\n3. Right-click the Smelter block to open the GUI\n4. Insert Raw Plutonium Fragments in the INPUT slot\n5. Insert fuel in the FUEL slot (Coal, Charcoal, Coal Block, Blaze Rod, or Lava Bucket)\n6. The machine starts HEATING — wait for it to reach 500°C\n7. Processing begins automatically once temperature is reached\n8. Collect Refined Plutonium Ingots from the OUTPUT slot', WHT);
sp(0.3);

H2('Fuel Values', ORG);
TH(['Fuel Item', 'Fuel Units', 'Processing Time (approx.)', 'Notes'], [160, 80, 160, CW - 400]);
[
  ['Coal',          '100 units', '~20 seconds',          'Basic fuel'],
  ['Charcoal',      '80 units',  '~16 seconds',          'Slightly worse than Coal'],
  ['Coal Block',    '900 units', '~3 minutes',           'Very efficient per slot'],
  ['Blaze Rod',     '120 units', '~24 seconds',          'Slightly better than Coal'],
  ['Lava Bucket',   '1,000 units','~3 min 20 seconds',   'BEST fuel per inventory slot'],
].forEach(([f, u, t, n], i) => TR([f, u, t, n], [160, 80, 160, CW - 400], [WHT, YLW, DIM, DIM], i % 2 === 0));
NOTE('Lava Bucket gives the most processing time per inventory slot — ideal for long refining sessions.');

H2('Temperature System', RED);
TH(['Parameter', 'Value'], [270, CW - 270]);
[
  ['Ambient temperature (idle, no fuel)',      '20°C'],
  ['Minimum temperature to begin processing',  '500°C  —  machine heats up first before any processing'],
  ['Maximum safe operating temperature',       '1,500°C'],
  ['Heating rate (fueled, idle)',              '+5°C per tick cycle (every 0.2 seconds)'],
  ['Active heating rate (while processing)',   '+2.5°C per tick cycle'],
  ['Cooling rate (no fuel, idle)',             '−1°C per tick cycle'],
  ['Overheat duration',                        '10,000 ms (10 seconds) before cooling begins'],
  ['Overheat cooling rate',                    '−2°C per tick cycle (twice as fast as normal)'],
  ['Restart threshold (after cooling)',        '80% of 500°C = 400°C  (avoids full cold start delay)'],
].forEach(([p, v], i) => TR([p, v], [270, CW - 270], [WHT, YLW], i % 2 === 0));

H2('Processing Recipe', GRN);
KV('Input',           '1× Raw Plutonium Fragment',                                      YLW, WHT, 120);
KV('Output',          '1× Refined Plutonium Ingot',                                     YLW, GRN, 120);
KV('Processing time', '15 seconds (300 ticks at 20 TPS)',                               YLW, WHT, 120);
KV('Fuel cost',       '1 fuel unit consumed per tick cycle while processing',            YLW, WHT, 120);
sp(0.3);

H2('Radiation Hazard', RED);
P('The Nuclear Smelter emits passive radiation to ALL players within 3 blocks while active.', WHT);
KV('Passive radiation amount',  '+8 radiation points per check', RED, WHT, 220);
KV('Check interval',            'Every 5 seconds (100 ticks)',   RED, WHT, 220);
KV('Affected radius',           '3 blocks around the machine',   RED, WHT, 220);
NOTE('Build the Smelter in an isolated room. Full Hazmat Suit reduces incoming radiation by 80% — at 8 points per check, you receive only ~1.6 pts/check with full Hazmat.');
NOTE('Maximum smelters per player: unlimited (configurable in smelter.yml).');
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 13 — NUCLEAR FORGE GUIDE
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Nuclear Forge — Complete Machine Guide', ORG);

H2('Overview', ORG);
P('The Nuclear Forge upgrades Plutonium and Hazmat gear from base tier to MK-I, MK-II, MK-III, and finally MK-IV. Each upgrade tier adds percentage bonuses to damage, attack speed, armor, and durability. The Forge is powered by Radioactive Cores (each provides 2,000 energy).', WHT);
sp(0.3);

H2('Usage Steps', CYAN);
P('1. Craft and place a Nuclear Forge block (see Page 6 for recipe)\n2. Right-click the Forge block to open the upgrade GUI\n3. Place the item you want to upgrade in the ITEM slot\n4. Insert Radioactive Cores in the FUEL slot (each = 2,000 energy)\n5. Insert Refined Plutonium Ingots in the MATERIAL slot\n6. For MK-3 and MK-4: also insert Irradiated Hearts in the HEART slot\n7. Click the UPGRADE button — the process takes 3–12 seconds\n8. On SUCCESS: item is upgraded to next tier\n   On FAILURE: item stays at current tier\n   On FAILURE at MK-4: 5% additional chance of item downgrading to MK-3', WHT);
sp(0.3);

H2('Upgrade Tiers — Full Requirements', ORG);
TH(['Tier', 'Ingots', 'Cores', 'Hearts', 'Energy', 'Success%', 'Time', 'Stat Bonus', 'Fail Risk'], [44, 44, 44, 50, 55, 60, 50, 85, CW-432]);
[
  ['MK-1', '2',  '1', '0', '500',   '100%', '3 sec',  '+5% all stats',  'None'],
  ['MK-2', '4',  '2', '0', '1,200', '90%',  '5 sec',  '+10% all stats', 'None'],
  ['MK-3', '8',  '4', '1', '2,500', '75%',  '8 sec',  '+20% all stats', 'None'],
  ['MK-4', '16', '8', '2', '5,000', '50%',  '12 sec', '+35% all stats', '5% downgrade → MK-3'],
].forEach(([t, i2, c, h, e, s, tm, b, f], i) => {
  const sc = s === '100%' ? GRN : s === '90%' ? YLW : s === '75%' ? ORG : RED;
  TR([t, i2, c, h, e, s, tm, b, f], [44, 44, 44, 50, 55, 60, 50, 85, CW-432], [YLW, WHT, WHT, WHT, WHT, sc, DIM, GRN, RED], i % 2 === 0);
});
NOTE('Ingots = Refined Plutonium Ingots (Echo Shard proxy)  ·  Cores = Radioactive Cores (Magma Cream proxy)');
NOTE('Hearts = Irradiated Hearts (Heart of the Sea proxy)  ·  All stats = damage, speed, armor, durability');

H2('MK-IV Special — Radiation Aura', PRP);
P('Any player wearing at least one MK-IV upgraded piece gains a passive Radiation Aura.', WHT);
TH(['Aura Parameter', 'Value'], [250, CW - 250]);
[
  ['Aura radius',                          '3 blocks around the player'],
  ['Radiation applied to hostile mobs',    '+25 radiation per aura tick'],
  ['Radiation applied to enemy players',   '+15 radiation per aura tick (PvP)'],
  ['Aura tick rate',                       'Every 2 seconds (40 ticks)'],
  ['Affects friendly/team players?',       'No — only hostile mobs and enemies'],
].forEach(([p, v], i) => TR([p, v], [250, CW - 250], [WHT, GRN], i % 2 === 0));

H2('Forge Energy System & Overload', RED);
TH(['Energy Parameter', 'Value'], [250, CW - 250]);
[
  ['Energy per Radioactive Core',          '2,000 units'],
  ['Maximum safe energy storage',          '10,000 units (5 cores max at once)'],
  ['Energy decay rate (idle)',             '−0.5 units per tick (prevents idle overload)'],
  ['Overload threshold',                   '>10,000 energy triggers overload'],
  ['Overload shutdown duration',           '200 ticks (10 seconds) — Forge is disabled'],
  ['Overload radiation burst',             '+150 radiation to all players within 8 blocks'],
  ['Forge block destroyed on overload?',   'NO — block is safe, only radiation burst'],
].forEach(([p, v], i) => TR([p, v], [250, CW - 250], [WHT, RED], i % 2 === 0));
NOTE('Never insert more than 5 Radioactive Cores at once — that hits the 10,000 energy limit and triggers overload.');
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 14 — WORLD: ORE & MINING
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('World — Ore & Mining', GRN);

H2('Plutonium Ore World Generation', YLW);
TH(['Parameter', 'Value'], [230, CW - 230]);
[
  ['Dimension',                   'Overworld only (Normal environment)'],
  ['Y-level spawn range',         'Y: −64 to −58  (deepest deepslate layer ONLY)'],
  ['Spawn chance per chunk',      '60% of newly generated chunks contain a vein'],
  ['Vein size',                   '1–2 blocks per vein (deliberately rare)'],
  ['Valid host blocks',           'Deepslate, Stone, Cobbled Deepslate, Tuff'],
  ['Discovery alert',             'Players receive a chat notification when within 5 blocks'],
].forEach(([p, v], i) => TR([p, v], [230, CW - 230], [WHT, YLW], i % 2 === 0));
NOTE('Plutonium Ore is significantly rarer than diamonds at this depth (~40% rarer). Explore deep cave systems at Y -60 or lower.');

H2('Mining Rules', RED);
TH(['Scenario', 'Outcome'], [240, CW - 240]);
[
  ['Mine with ANY tool EXCEPT Radiation Drill', '+25 radiation burst immediately  ·  Ore block does NOT drop anything  ·  5-second cooldown before next burst'],
  ['Mine with Radiation Drill (no enchants)',   '1× Raw Plutonium Fragment  ·  3–7 XP orbs  ·  No radiation burst'],
  ['Mine with Radiation Drill + Fortune I',     '1–2× Raw Plutonium Fragments'],
  ['Mine with Radiation Drill + Fortune II',    '1–3× Raw Plutonium Fragments'],
  ['Mine with Radiation Drill + Fortune III',   '1–4× Raw Plutonium Fragments'],
  ['Mine with Radiation Drill + Silk Touch',    '1× Plutonium Ore block (no fragments)'],
].forEach(([sc, out], i) => TR([sc, out], [240, CW - 240], [WHT, YLW], i % 2 === 0));

H2('Ore Radiation Zones', RED);
P('Standing near Plutonium Ore passively irradiates you every 5 seconds. Use proper protection.', WHT);
TH(['Distance from Ore', 'Radiation per 5s', 'With Hazmat Suit', 'With Plutonium Armor'], [160, 100, 130, CW - 390]);
[
  ['Within 1 block (≤1)',  '+5 pts',  '+1 pt (~80% blocked)', '0 pts (IMMUNE)'],
  ['Within 2 blocks (≤2)', '+2 pts',  '+0.4 pts',             '0 pts (IMMUNE)'],
  ['Within 3 blocks (≤3)', '+1 pt',   '+0.2 pts',             '0 pts (IMMUNE)'],
].forEach(([d, r, h, p2], i) => TR([d, r, h, p2], [160, 100, 130, CW - 390], [WHT, RED, YLW, GRN], i % 2 === 0));

H2('Plutonium Block', LIME);
P('Craft by placing 9× Refined Plutonium Ingots in a 3×3 crafting grid. Used for compact storage. Emits more radiation than individual ingots.', WHT);
TH(['Plutonium Block Radiation', 'Points per 5s'], [280, CW - 280]);
[
  ['Within 1 block of Plutonium Block', '+10 pts per 5 seconds'],
  ['Within 2 blocks',                   '+5 pts per 5 seconds'],
  ['Within 3 blocks',                   '+2 pts per 5 seconds'],
].forEach(([p, v], i) => TR([p, v], [280, CW - 280], [WHT, RED], i % 2 === 0));
NOTE('Keep Plutonium Blocks in a sealed vault. Even full Hazmat Suit only reduces the nearest tier to +2 pts per 5s.');

H2('Radioactive Debris', ORG);
TH(['Debris Parameter', 'Value'], [230, CW - 230]);
[
  ['Location',              'Y: −64 to −32 underground (rare)'],
  ['Rarity',                '0.1% spawn chance per chunk (10× rarer than Plutonium Ore)'],
  ['Mining drop',           '15% chance to drop 1× Radioactive Core'],
  ['Passive radiation',     '+0.5 radiation per second within 5 blocks (continuous)'],
].forEach(([p, v], i) => TR([p, v], [230, CW - 230], [WHT, ORG], i % 2 === 0));

H2('Lead-Lined Storage Crate', CYAN);
P('Crafted from 8 Iron Ingots around a Chest in a standard Crafting Table (iron surrounds center chest). Stores Raw Plutonium Fragments and other radioactive items WITHOUT emitting any radiation to nearby players — unlike a normal chest which leaks +1 radiation per 10 seconds within 3 blocks.', WHT);
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 15 — RADIOACTIVE FARMING
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Radioactive Farming & Cure System', LIME);

H2('Getting Started', LIME);
P('Radioactive Farming is the primary way to produce Healing Petals — the ingredient for radiation cures. You need Mutated Seeds (from zombie drops) and Radioactive Farmland (created with a Plutonium Shovel).', WHT);
sp(0.3);

H2('Step-by-Step Farming Guide', LIME);
P('STEP 1 — Get Mutated Seeds:\n  Kill Irradiated Zombies. All levels can drop Mutated Seeds (5% at Lv.1, up to 50% at Lv.4 Alpha).\n\nSTEP 2 — Create Radioactive Farmland:\n  Dig soil blocks (Dirt, Grass Block, Coarse Dirt) with a Plutonium Shovel.\n  Each dig has a 15% chance to convert the block to Radioactive Farmland.\n  Normal farmland rejects Mutated Seeds — you MUST use Radioactive Farmland.\n\nSTEP 3 — Plant Seeds:\n  Right-click to plant Mutated Seeds on Radioactive Farmland.\n  Attempting to plant on regular farmland shows an error message and cancels.\n\nSTEP 4 — Let It Grow:\n  Plants grow through 5 stages: 0 (seed just planted) → 1 → 2 → 3 → 4 (fully bloomed).\n  Radioactive Farmland gives +50% extra growth chance per random tick.\n  Vanilla Bone Meal works and forces growth stages normally.\n\nSTEP 5 — Harvest at Stage 4:\n  Right-click the fully grown plant (Stage 4 blossom).\n  Receive 1–3 Healing Petals and 0–2 bonus Mutated Seeds.\n  Receive 2–5 XP per harvest.\n\nSTEP 6 — Watch for Toxic Blooms:\n  1% chance a Stage 4 plant mutates into a Toxic Bloom instead of dropping normally.\n  Destroy it immediately with any tool — it drops nothing but stops irradiating.', WHT);

H2('Farming Stats', YLW);
TH(['Parameter', 'Value'], [230, CW - 230]);
[
  ['Growth stages total',            '5 stages (0 = planted, 4 = ready to harvest)'],
  ['Radioactive Farmland growth bonus', '+50% extra chance per random tick'],
  ['Bone meal compatible',            'Yes — vanilla Bone Meal forces growth stages'],
  ['Petals per harvest',              '1–3 Healing Petals'],
  ['Seeds per harvest',               '0–2 Mutated Seeds (self-sustaining farm supply)'],
  ['XP per harvest',                  '2–5 XP orbs'],
  ['Toxic Bloom mutation chance',     '1% — fully grown crop becomes a radiation hazard'],
].forEach(([p, v], i) => TR([p, v], [230, CW - 230], [WHT, LIME], i % 2 === 0));

H2('Toxic Bloom — Farming Hazard', RED);
TH(['Toxic Bloom Parameter', 'Value'], [230, CW - 230]);
[
  ['What it is',               'A mutated fully-grown crop that emits radiation instead of dropping petals'],
  ['Radiation radius',         '4 blocks around the Toxic Bloom block'],
  ['Radiation amount',         '+8 radiation points every 2 seconds'],
  ['How to destroy',           'Break it with any tool (instantly) — drops nothing'],
  ['Particle indicator',       'Green radiation particles visible around the block'],
].forEach(([p, v], i) => TR([p, v], [230, CW - 230], [WHT, RED], i % 2 === 0));
NOTE('Always harvest at Stage 4 as soon as possible — the longer you wait, the more time for a Toxic Bloom to appear.');

H2('Radioactive Farmland Radiation', ORG);
KV('Passive radiation', '+1 radiation point every 2 seconds to nearby players', ORG, WHT, 200);
KV('Plutonium Boots special', 'Completely negates ground-contact radiation from Radioactive Farmland', GRN, WHT, 200);
NOTE('Wear Plutonium Boots while farming to cancel all ground radiation. Pair with Hazmat Suit top pieces for full protection.');

H2('Normal Chest vs Lead-Lined Crate (Storage Warning)', RED);
TH(['Container', 'Radiation Leak', 'Effect'], [180, 130, CW - 310]);
[
  ['Normal Chest (with fragments inside)', 'YES — leaks radiation', '+1 radiation per 10s within 3 blocks'],
  ['Lead-Lined Storage Crate',             'NO — zero leak',        'Completely safe storage for fragments'],
].forEach(([c, l, e], i) => TR([c, l, e], [180, 130, CW - 310], [WHT, RED, YLW], i % 2 === 0));
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 16 — IRRADIATED ZOMBIES
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Irradiated Zombies — Levels, Loot & Events', GRN);

H2('Spawning Rules', YLW);
P('60% of all naturally-spawning zombies become Irradiated Zombies. Baby zombies are excluded. Mob spawners do NOT produce Irradiated Zombies (configurable). Eligible spawn reasons: Natural, Jockey, Mount, Reinforcements, Village Defence, Chunk Generation.', WHT);
sp(0.3);

H2('Level Stats', RED);
TH(['Level (Spawn Weight)', 'HP', 'Damage', 'Speed Mult', 'KB Resist', 'Rad/Hit', 'XP Reward'], [150, 38, 55, 80, 72, 65, CW - 460]);
[
  ['Lv.1 — Standard   (80%)', '35 HP', '5 DMG',  '×1.15', '0.15', '+10 pts', '10 XP'],
  ['Lv.2 — Enhanced   (15%)', '45 HP', '7 DMG',  '×1.20', '0.20', '+20 pts', '20 XP'],
  ['Lv.3 — Powerful   (4%)',  '60 HP', '9 DMG',  '×1.25', '0.30', '+35 pts', '40 XP'],
  ['Lv.4 — Alpha      (1%)',  '80 HP', '12 DMG', '×1.30', '0.50', '+50 pts', '100 XP'],
].forEach(([lv, hp, d, sp2, kb, rad, xp], i) => {
  const c = [WHT, YLW, ORG, RED][i];
  TR([lv, hp, d, sp2, kb, rad, xp], [150, 38, 55, 80, 72, 65, CW - 460], [c, WHT, WHT, WHT, WHT, RED, YLW], i % 2 === 0);
});
NOTE('Level 4 Alpha: displays a green Bossbar "☢ Alpha Irradiated Zombie ☢" and a permanent Glowing effect. Visible from 20 blocks. Treat as a mini-boss encounter.');

H2('Loot Drop Chances', YLW);
TH(['Drop Item', 'Level 1', 'Level 2', 'Level 3', 'Level 4 Alpha'], [195, 70, 70, 70, CW - 405]);
[
  ['Rotten Flesh',                  '0–3 (always)', '0–3 (always)', '0–3 (always)', '0–3 (always)'],
  ['Radioactive Core (Magma Cream)','15%',           '25%',          '40%',          '100% (GUARANTEED)'],
  ['Mutated Seed (Wheat Seeds)',    '5%',            '10%',          '20%',          '50%'],
  ['Irradiated Heart (H.o.t.Sea)', '1%',            '3%',           '7%',           '25%'],
].forEach(([item, l1, l2, l3, l4], i) => {
  const l4c = l4.includes('GUAR') ? GRN : YLW;
  TR([item, l1, l2, l3, l4], [195, 70, 70, 70, CW - 405], [WHT, DIM, DIM, DIM, l4c], i % 2 === 0);
});

H2('Radiation Cloud (On Zombie Death)', RED);
KV('Spawn chance per zombie death', '20% chance', RED, WHT, 220);
KV('Cloud radius',                  '3 blocks',   RED, WHT, 220);
KV('Cloud duration',                '10 seconds', RED, WHT, 220);
KV('Radiation inside cloud',        '+5 radiation per second to players inside', RED, WHT, 220);
KV('Visual indicator',              'Green radiation particles fill the cloud area', GRN, WHT, 220);
NOTE('Never stand on a freshly killed Irradiated Zombie — even a Level 1 cloud deals +50 radiation over 10 seconds.');

H2('Night Events — Radiation Surge', PRP);
P('Each in-game night has a 5% chance to trigger a Radiation Surge — a server-wide event that amplifies all radiation for the entire night.', WHT);
TH(['Surge Parameter', 'Value'], [240, CW - 240]);
[
  ['Trigger chance',        '5% per in-game night'],
  ['Radiation effect',      'ALL radiation damage DOUBLED for the full night'],
  ['Loot effect',           'ALL zombie drops DOUBLED for the full night'],
  ['Player notification',   'Bossbar shown to all players: ☢ RADIATION SURGE ACTIVE ☢'],
  ['Bossbar colour',        'Green'],
  ['Start broadcast',       '"☢ RADIATION SURGE! A wave of radioactive energy sweeps the world!"'],
  ['End broadcast',         '"☢ Radiation Surge has ended. The air clears..."'],
].forEach(([p, v], i) => TR([p, v], [240, CW - 240], [WHT, PRP], i % 2 === 0));
NOTE('During a Radiation Surge, a Level 4 Alpha Zombie delivers +100 radiation per hit (doubled) and has a 50% Core drop rate and a 100% Irradiated Heart drop rate (doubled). Use Serums before venturing out on Surge nights.');

H2('Advancements', GRN);
TH(['Advancement', 'Trigger', 'XP Reward'], [180, 240, CW - 420]);
[
  ['First Exposure',  'Receive radiation for the first time from any zombie', '+50 XP'],
  ['Mutant Hunter',   'Kill your first Irradiated Zombie',                    '+100 XP'],
  ['Core Collector',  'Collect your first Radioactive Core',                  '+100 XP'],
  ['Alpha Slayer',    'Kill your first Level 4 Alpha Zombie',                 '+500 XP'],
].forEach(([adv, trig, xp], i) => TR([adv, trig, xp], [180, 240, CW - 420], [GRN, WHT, YLW], i % 2 === 0));
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 17 — PLUTONIUM TITAN BOSS
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Plutonium Titan — Complete Boss Guide', PRP);

H2('Summoning the Titan', RED);
P('REQUIREMENTS:\n  • 4× Titan Cores in your inventory (consumed on summoning)\n  • 1× Irradiated Heart in your main hand\n  • A completed Summoning Altar\n\nALTAR CONSTRUCTION:\n  Center block: Crying Obsidian\n  4 Corner blocks: Obsidian (placed exactly 2 blocks diagonally from the center, same Y level)\n  No other blocks are required — the 5-block altar is minimal.\n\nSUMMONING STEPS:\n  1. Build the Summoning Altar as described above\n  2. Ensure 4× Titan Cores are anywhere in your inventory\n  3. Hold 1× Irradiated Heart in your main hand\n  4. Right-click the center Crying Obsidian block\n  5. All 4 Titan Cores are immediately consumed from your inventory\n  6. A 15-second summoning animation begins\n  7. The Plutonium Titan spawns at the altar center\n\nCOOLDOWN: 60 minutes between summons. Only one Titan may exist at a time.', WHT);
sp(0.3);

H2('Health Scaling', YLW);
P('Titan health scales dynamically based on the number of players nearby at time of summon.', WHT);
TH(['Players Nearby', 'Titan HP'], [220, CW - 220]);
[['1–3 players', '5,000 HP'],['4–6 players', '6,500 HP'],['7–10 players', '8,500 HP'],['10+ players', '10,000 HP']].forEach(([p, h], i) => TR([p, h], [220, CW - 220], [WHT, RED], i % 2 === 0));
KV('Base melee damage', '18.0 base damage  ·  Follow range: 80 blocks  ·  Melee range: 3.5 blocks', RED, WHT, 165);
KV('Melee speed', 'Attacks every 1.2 seconds (24 ticks)', RED, WHT, 165);

H2('Combat Phases', RED);
TH(['Phase', 'HP Range', 'Speed', 'DMG', 'New Abilities Unlocked in This Phase'], [65, 90, 55, 55, CW - 265]);
[
  ['Phase 1','100% – 75%','×1.00','×1.00','Radiation Aura (passive)  ·  Titan Slam'],
  ['Phase 2','75% – 50%', '×1.10','×1.20','+ Radiation Wave  ·  Mutant Summoning'],
  ['Phase 3','50% – 25%', '×1.25','×1.50','+ Reactor Overload  ·  Energy Beam'],
  ['Phase 4','25% – 0%',  '×1.50','×2.00','+ Nuclear Catastrophe  ·  Final Frenzy (one-time trigger)'],
].forEach(([ph, hp, sp2, dm, ab], i) => {
  const c = [GRN, YLW, ORG, RED][i];
  TR([ph, hp, sp2, dm, ab], [65, 90, 55, 55, CW - 265], [c, WHT, YLW, RED, WHT], i % 2 === 0);
});

H2('All Titan Abilities', RED);
TH(['Ability', 'Active From', 'Details', 'Cooldown'], [160, 75, CW - 310, 75]);
[
  ['Radiation Aura', 'Phase 1', '+3 radiation per second to all players within 10 blocks — constant passive', 'None'],
  ['Titan Slam', 'Phase 1', '5-block radius  ·  12 base DMG  ·  +15 radiation  ·  knockback on targets', '10 sec'],
  ['Radiation Wave', 'Phase 2', '20-block expanding ring  ·  +40 radiation burst to all caught in ring', '15 sec'],
  ['Mutant Summoning', 'Phase 2', 'Summons 3 regular Irradiated Zombies + 1 Level 4 Alpha Zombie', '25 sec'],
  ['Reactor Overload', 'Phase 3', '5-second charge animation → 18-block radius explosion → +70 radiation', '40 sec'],
  ['Energy Beam', 'Phase 3', 'Targets a random player  ·  Deals 25 direct DMG + 60 radiation on contact', '20 sec'],
  ['Nuclear Catastrophe', 'Phase 4', '10-second charge → 30-block radius → +80 radiation → sets ALL nearby players to 1 HP', '90 sec'],
  ['Final Frenzy', 'Phase 4', 'Triggered once on entering Phase 4 — grants permanent +50% attack speed for rest of fight', 'Once'],
].forEach(([name, phase, detail, cd], i) => TR([name, phase, detail, cd], [160, 75, CW - 310, 75], [RED, DIM, WHT, YLW], i % 2 === 0));
NOTE('Nuclear Catastrophe: Retreat beyond 30 blocks during the 10-second charge animation. You survive with 1 HP but are set there regardless of health.');
NOTE('Mutant Summon spawns a Level 4 Alpha every 25 seconds from Phase 2 onward — prioritise killing them quickly or radiation accumulates rapidly.');

H2('Arena Hazard', ORG);
KV('Hazard radius', '15 blocks from the Titan summon point', ORG, WHT, 180);
KV('Arena radiation', '+2 radiation per check within the hazard zone', ORG, WHT, 180);

H2('Titan Kill Rewards', YLW);
TH(['Reward', 'Condition', 'Amount / Chance'], [215, 160, CW - 375]);
[
  ['Server XP',                 'Any contribution',              '5,000 XP total (distributed by contribution %)'],
  ['Titan Fragments',           'Any contribution',              '1–8+ pieces (scales with contribution %)'],
  ['Refined Plutonium Ingots',  'Minimum 5% contribution',       'Scales with contribution percentage'],
  ['Reactor Heart',             'Qualifying players',            '10% chance per player'],
  ['Ancient Reactor Blueprint', 'Qualifying players',            '5% chance per player'],
  ['Mutated Crystal',           'Qualifying players',            '8% chance per player'],
  ['Loot Chest (at death loc.)', 'All participants',             '1× chest containing Cores, Fragments, Ingots'],
  ['Kill Broadcast',            'Always on kill',                '"[player] has slain the Plutonium Titan!"'],
].forEach(([rew, cond, amt], i) => TR([rew, cond, amt], [215, 160, CW - 375], [YLW, DIM, GRN], i % 2 === 0));
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 18 — COMBAT SYSTEM
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Combat System — Weapons, Mastery & Repair', CYAN);

H2('Plutonium Weapon Radiation Effects', RED);
TH(['Weapon / Attack Type', 'Radiation Applied', 'Additional Effect'], [210, 110, CW - 320]);
[
  ['Plutonium Sword — base hit',           '+10 radiation',     '—'],
  ['Sword — Critical hit (falling attack)', '+15 radiation',     '+5 bonus radiation on crit'],
  ['Sword — vs Stage 3+ victim',           '+15 radiation',     '+5 bonus when victim already infected'],
  ['Plutonium Axe — base hit',             '0 base radiation',  '—'],
  ['Axe — Shockwave (15% chance)',         '+15 AoE radiation', '3-block radius AoE burst to all targets'],
  ['Plutonium Arrow — direct hit',         '+25 radiation',     'Poison I (4 seconds)  ·  Glowing (5 seconds)'],
  ['Arrow — Critical (full bow charge)',    '+45 radiation',     '+20 bonus radiation on max draw'],
  ['Arrow — Headshot bonus',               '+35 radiation',     '+10 bonus (approximated headshot)'],
  ['MK-IV Aura — on mobs',                '+25/tick',          'Passive every 2 seconds within 3 blocks'],
  ['MK-IV Aura — on enemy players (PvP)', '+15/tick',          'Passive every 2 seconds within 3 blocks'],
].forEach(([w, r, e], i) => TR([w, r, e], [210, 110, CW - 320], [WHT, RED, DIM], i % 2 === 0));

H2('Combo System', YLW);
P('The combo system rewards sustained combat with bonus radiation on consecutive hits.', WHT);
TH(['Combo Parameter', 'Value'], [240, CW - 240]);
[
  ['Maximum combo stack',        '8 hits'],
  ['Combo reset time',           '6 seconds after the last hit'],
  ['Bonus radiation per stack',  '+5 extra radiation per hit ABOVE the first (hit 2 = +5, hit 3 = +10…)'],
  ['Maximum combo radiation',    '+35 radiation bonus cap (regardless of stack count)'],
].forEach(([p, v], i) => TR([p, v], [240, CW - 240], [WHT, YLW], i % 2 === 0));

H2('PvP Radiation Surge (Two Infected Players Fighting)', RED);
TH(['Surge Parameter', 'Value'], [240, CW - 240]);
[
  ['Trigger condition',       'BOTH combatants must be at Stage 2 radiation or higher'],
  ['Trigger chance',          '5% per hit during combat'],
  ['Radiation to BOTH fighters','+ 30 radiation each'],
  ['Radiation to bystanders','+ 15 radiation to players within 8 blocks of the duel'],
  ['Cooldown between surges', '15 seconds per pair'],
].forEach(([p, v], i) => TR([p, v], [240, CW - 240], [WHT, RED], i % 2 === 0));

H2('Weapon Mastery System', PRP);
P('Each weapon type (Sword, Axe, Bow) gains mastery XP from hits and kills. Higher mastery tiers provide bonuses.', WHT);
TH(['Mastery Level', 'XP Required', 'XP Gain per Action', 'Master Tier Bonus'], [120, 90, 160, CW - 370]);
[
  ['Novice',      '0 XP',     'Starting level — no bonuses',                 '—'],
  ['Experienced', '100 XP',   'Sword: +1/hit, +10/kill',                     '—'],
  ['Veteran',     '500 XP',   'Axe: +1/hit, +10/kill',                       '—'],
  ['Elite',       '1,500 XP', 'Bow: +2/hit, +15/kill',                       '—'],
  ['Master',      '4,000 XP', 'All weapons accumulate faster',               '+10% radiation on all weapon hits  ·  +1.5 block axe shockwave radius'],
].forEach(([lv, xp, gain, bonus], i) => TR([lv, xp, gain, bonus], [120, 90, 160, CW - 370], [PRP, YLW, DIM, GRN], i % 2 === 0));

H2('Equipment Repair', LIME);
TH(['Equipment Type', 'Repair Material (Anvil)', 'Durability per Repair'], [190, 220, CW - 410]);
[
  ['Plutonium Sword/Axe/Pickaxe/Shovel/Hoe', 'Refined Plutonium Ingot (Echo Shard proxy)', '+300 durability per ingot'],
  ['Plutonium Helmet/Chestplate/Leggings/Boots', 'Refined Plutonium Ingot (Echo Shard proxy)', '+300 durability per ingot'],
  ['Hazmat Helmet/Chestplate/Leggings/Boots', 'Industrial Fabric (Yellow Wool proxy)', '+200 durability per piece'],
  ['Radiation Drill', 'Diamond (vanilla)', 'Standard diamond pickaxe repair rules'],
].forEach(([eq, mat, dur], i) => TR([eq, mat, dur], [190, 220, CW - 410], [WHT, YLW, GRN], i % 2 === 0));
NOTE('Use an Anvil to repair — place the item in the first slot and the repair material in the second slot.');
writeFooter();

// ══════════════════════════════════════════════════════════════════════════════
// PAGE 19 — ADMIN COMMANDS
// ══════════════════════════════════════════════════════════════════════════════
newPage();
H1('Admin Commands & Configuration', GRN);
P('All commands use /nuclearcraft or the short alias /nc. Commands require operator permissions (OP) unless otherwise configured in plugin.yml.', DIM);
sp(0.3);

H2('Radiation Commands', RED);
TH(['Command', 'Description'], [270, CW - 270]);
[
  ['/nc radiation set <player> <0-1000>', 'Set a player\'s radiation to an exact value (0 clears all radiation)'],
  ['/nc radiation add <player> <amount>', 'Add a specified number of radiation points to a player'],
  ['/nc radiation clear <player>',        'Instantly clear ALL radiation from a player (equivalent to set 0)'],
  ['/nc radiation status [player]',       'Display the target player\'s current radiation stage and exact point total'],
].forEach(([c, d], i) => TR([c, d], [270, CW - 270], [GRN, WHT], i % 2 === 0));

H2('Item & Equipment Commands', YLW);
TH(['Command', 'Description'], [270, CW - 270]);
[
  ['/nc ore give fragment [amount]',     'Give Raw Plutonium Fragments to yourself'],
  ['/nc ore give drill',                 'Give a Radiation Drill'],
  ['/nc equipment give sword',           'Give a Plutonium Sword'],
  ['/nc equipment give axe',             'Give a Plutonium Axe'],
  ['/nc equipment give pickaxe',         'Give a Plutonium Pickaxe'],
  ['/nc equipment give shovel',          'Give a Plutonium Shovel'],
  ['/nc equipment give hoe',             'Give a Plutonium Hoe'],
  ['/nc equipment give helmet',          'Give a Plutonium Helmet'],
  ['/nc equipment give chestplate',      'Give a Plutonium Chestplate'],
  ['/nc equipment give leggings',        'Give a Plutonium Leggings'],
  ['/nc equipment give boots',           'Give a Plutonium Boots'],
  ['/nc equipment give hazmat-helmet',   'Give a Hazmat Helmet'],
  ['/nc equipment give hazmat-chestplate','Give a Hazmat Chestplate'],
  ['/nc equipment give hazmat-leggings', 'Give a Hazmat Leggings'],
  ['/nc equipment give hazmat-boots',    'Give a Hazmat Boots'],
  ['/nc farming give seed [amount]',     'Give Mutated Seeds'],
  ['/nc farming give petal [amount]',    'Give Healing Petals'],
  ['/nc farming give antidote [amount]', 'Give Radiation Antidotes'],
  ['/nc farming give serum [amount]',    'Give Radiation Serums'],
  ['/nc smelter give',                   'Give a Nuclear Smelter block'],
  ['/nc forge give',                     'Give a Nuclear Forge block'],
].forEach(([c, d], i) => TR([c, d], [270, CW - 270], [YLW, WHT], i % 2 === 0));

H2('Titan Boss Commands', PRP);
TH(['Command', 'Description'], [270, CW - 270]);
[
  ['/nc titan spawn',         'Force-spawn the Plutonium Titan at your current location'],
  ['/nc titan kill',          'Instantly despawn the active Titan (gives no rewards)'],
  ['/nc titan phase <1-4>',   'Force the Titan to a specific combat phase immediately'],
].forEach(([c, d], i) => TR([c, d], [270, CW - 270], [PRP, WHT], i % 2 === 0));

H2('Server Commands', CYAN);
TH(['Command', 'Description'], [270, CW - 270]);
[
  ['/nc reload', 'Reload ALL YAML config files without restarting the server'],
  ['/nc help',   'Show all available commands and their syntax'],
].forEach(([c, d], i) => TR([c, d], [270, CW - 270], [CYAN, WHT], i % 2 === 0));

H2('Configuration Files', YLW);
P('All values in this guide are configurable. Files are located in /plugins/NuclearCraft/ on the server.', WHT);
TH(['Config File', 'Controls'], [160, CW - 160]);
[
  ['config.yml',    'Global plugin enable/disable, debug mode'],
  ['radiation.yml', 'Radiation stages, progression, decay, contagion, armor reduction, immunity'],
  ['zombies.yml',   'Zombie spawn chance, level weights, stats, loot tables, radiation cloud, night events'],
  ['ore.yml',       'Plutonium Ore generation, mining, radiation zones, debris, storage crate'],
  ['smelter.yml',   'Smelter tick rate, temperature system, fuel values, processing time, radiation emission'],
  ['forge.yml',     'Forge energy, upgrade tier requirements, success chances, MK-IV aura, overload'],
  ['equipment.yml', 'Tool/weapon stats, armor stats, arrow stats, soil/debris mechanics, repair values'],
  ['armors.yml',    'Plutonium armor stats, enchantments, set bonus, radiation reduction per piece'],
  ['farming.yml',   'Seed growth stages, harvest drops, Toxic Bloom chance, cure durations'],
  ['combat.yml',    'Weapon radiation values, combo system, PvP surge, weapon mastery levels/XP'],
  ['titan.yml',     'Boss HP scaling, phase thresholds, ability stats, summoning cooldown, rewards'],
  ['boss.yml',      'Additional boss configuration and balance values'],
].forEach(([f, c], i) => TR([f, c], [160, CW - 160], [YLW, WHT], i % 2 === 0));
NOTE('Use /nc reload after editing any config file to apply changes without server restart.');

sp(0.5);
need(35);
const boty = doc.y;
doc.rect(ML, boty, CW, 28).fill(PANEL);
doc.fontSize(10).fillColor(YLW).font('Helvetica-Bold')
   .text('NuclearCraft: Plutonium Age  ·  Plugin Version 1.0  ·  PaperMC 1.21+  ·  Phases 1–10 Complete', ML + 8, boty + 9, { width: CW - 16, align: 'center' });
writeFooter();

// ─── Finalise ──────────────────────────────────────────────────────────────────
doc.end();
stream.on('finish', () => console.log('✅  NuclearCraft-Guide.pdf  (' + pageNum + ' pages)'));
stream.on('error',  e  => console.error('ERROR:', e));
