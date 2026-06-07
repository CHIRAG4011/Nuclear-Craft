'use strict';
const PDFDocument = require('pdfkit');
const fs = require('fs');

// ── Document setup ────────────────────────────────────────────────────────────
const doc = new PDFDocument({
  margin: 45,
  size: 'A4',
  autoFirstPage: false,
  info: { Title: 'NuclearCraft: Plutonium Age — Complete Player Guide' }
});
const out = fs.createWriteStream('/home/runner/workspace/NuclearCraft-Guide.pdf');
doc.pipe(out);

// ── Constants ─────────────────────────────────────────────────────────────────
const PW = 595.28, PH = 841.89;
const ML = 45, MR = 45, MT = 45;
const CW = PW - ML - MR;     // content width

// ── Colours ───────────────────────────────────────────────────────────────────
const BG   = '#0f0f0f';
const PANL = '#1c1c1c';
const GRN  = '#39ff14';
const CYAN = '#00cfff';
const YLW  = '#ffd000';
const ORG  = '#ff8800';
const RED  = '#ff3300';
const PRP  = '#9933ff';
const LIME = '#7fff00';
const WHT  = '#e8e8e8';
const DIM  = '#888888';
const BDR  = '#2d6e2d';

// ── Page management ───────────────────────────────────────────────────────────
let pageNum = 0;

function addPage() {
  doc.addPage();
  pageNum++;
  doc.rect(0, 0, PW, PH).fill(BG);
  doc.y = MT;
  doc.x = ML;
}

function footerLine() {
  const fy = PH - 28;
  doc.rect(ML, fy - 4, CW, 0.5).fill(BDR);
  doc.fontSize(7.5).fillColor(DIM).font('Helvetica')
     .text('NuclearCraft: Plutonium Age  ·  Complete Player Guide  ·  Page ' + pageNum,
           ML, fy + 2, { width: CW, align: 'center' });
}

// ── Layout helpers ────────────────────────────────────────────────────────────
function remainingY() { return PH - 36 - doc.y; }
function ensureSpace(needed) {
  if (remainingY() < needed) { footerLine(); addPage(); }
}

function sectionTitle(text, color) {
  ensureSpace(40);
  color = color || GRN;
  doc.moveDown(0.5);
  const y = doc.y;
  doc.rect(ML, y, CW, 22).fill(PANL);
  doc.rect(ML, y, 4, 22).fill(color);
  doc.fontSize(13).fillColor(color).font('Helvetica-Bold')
     .text('  ' + text, ML + 8, y + 5, { width: CW - 12 });
  doc.y = y + 28;
}

function subTitle(text, color) {
  ensureSpace(28);
  color = color || CYAN;
  doc.moveDown(0.2);
  doc.fontSize(11).fillColor(color).font('Helvetica-Bold').text(text, ML, doc.y);
  doc.rect(ML, doc.y, CW, 0.8).fill(color);
  doc.moveDown(0.6);
}

function para(text, color) {
  doc.fontSize(9.5).fillColor(color || WHT).font('Helvetica').text(text, ML, doc.y, { width: CW });
  doc.moveDown(0.4);
}

function note(text) {
  doc.fontSize(8.5).fillColor(DIM).font('Helvetica-Oblique')
     .text('☢  ' + text, ML + 8, doc.y, { width: CW - 8 });
  doc.moveDown(0.3);
}

// ── Row helpers ───────────────────────────────────────────────────────────────
function twoCol(left, right, lColor, rColor, lW) {
  lW = lW || 200;
  const rW = CW - lW - 8;
  const y = doc.y;
  doc.fontSize(9.5).fillColor(lColor || YLW).font('Helvetica-Bold')
     .text(left, ML, y, { width: lW, lineBreak: false });
  doc.fontSize(9.5).fillColor(rColor || WHT).font('Helvetica')
     .text(right, ML + lW + 8, y, { width: rW });
  if (doc.y === y) doc.moveDown(0.5); else doc.moveDown(0.2);
}

function tableHeader(cols, widths) {
  ensureSpace(20);
  const y = doc.y;
  doc.rect(ML, y, CW, 16).fill('#222222');
  let x = ML + 4;
  cols.forEach((c, i) => {
    doc.fontSize(8).fillColor(GRN).font('Helvetica-Bold')
       .text(c, x, y + 4, { width: widths[i], lineBreak: false });
    x += widths[i];
  });
  doc.y = y + 18;
}

function tableRow(vals, widths, colors, even) {
  ensureSpace(18);
  const y = doc.y;
  if (even) doc.rect(ML, y, CW, 15).fill('#181818');
  let x = ML + 4;
  vals.forEach((v, i) => {
    doc.fontSize(8.5).fillColor(colors && colors[i] ? colors[i] : WHT).font('Helvetica')
       .text(v, x, y + 3, { width: widths[i], lineBreak: false });
    x += widths[i];
  });
  doc.y = y + 17;
}

// ── Crafting grid renderer ────────────────────────────────────────────────────
function craftGrid(grid, label, borderColor, offsetX) {
  ensureSpace(140);
  borderColor = borderColor || GRN;
  offsetX = offsetX || 0;
  const cell = 46;
  const gap  = 3;
  const gx   = ML + offsetX;
  const gy   = doc.y;
  const rows = grid.length;
  const cols = Math.max(...grid.map(r => r.length));

  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const cx = gx + c * (cell + gap);
      const cy = gy + r * (cell + gap);
      doc.rect(cx, cy, cell, cell).lineWidth(0.8).stroke(borderColor);
      const item = (grid[r] && grid[r][c]) || '';
      if (item.trim()) {
        const lines = item.split('\n');
        const lineH = 8;
        const totalH = lines.length * lineH;
        lines.forEach((ln, li) => {
          doc.fontSize(6.5).fillColor(WHT).font('Helvetica')
             .text(ln, cx + 1, cy + (cell - totalH) / 2 + li * lineH, { width: cell - 2, align: 'center', lineBreak: false });
        });
      }
    }
  }
  const totalH = rows * (cell + gap) + 6;
  if (label) {
    doc.fontSize(8).fillColor(DIM).font('Helvetica')
       .text(label, gx, gy + totalH, { width: cols * (cell + gap) });
    doc.y = gy + totalH + 14;
  } else {
    doc.y = gy + totalH;
  }
}

// side-by-side two grids
function twoGrids(g1, l1, c1, g2, l2, c2) {
  ensureSpace(160);
  const cell = 44, gap = 3, gridW = 3 * (cell + gap);
  const midX = ML + gridW + 30;

  const startY = doc.y;

  // Left grid
  const rows1 = g1.length;
  for (let r = 0; r < rows1; r++) {
    for (let c = 0; c < 3; c++) {
      const cx = ML + c * (cell + gap);
      const cy = startY + r * (cell + gap);
      doc.rect(cx, cy, cell, cell).lineWidth(0.8).stroke(c1 || GRN);
      const item = (g1[r] && g1[r][c]) || '';
      if (item.trim()) {
        doc.fontSize(6).fillColor(WHT).font('Helvetica')
           .text(item, cx + 1, cy + cell/2 - 7, { width: cell - 2, align: 'center' });
      }
    }
  }
  const h1 = rows1 * (cell + gap) + 4;
  doc.fontSize(8).fillColor(DIM).font('Helvetica').text(l1 || '', ML, startY + h1, { width: gridW });

  // Right grid
  const rows2 = g2.length;
  for (let r = 0; r < rows2; r++) {
    for (let c = 0; c < 3; c++) {
      const cx = midX + c * (cell + gap);
      const cy = startY + r * (cell + gap);
      doc.rect(cx, cy, cell, cell).lineWidth(0.8).stroke(c2 || CYAN);
      const item = (g2[r] && g2[r][c]) || '';
      if (item.trim()) {
        doc.fontSize(6).fillColor(WHT).font('Helvetica')
           .text(item, cx + 1, cy + cell/2 - 7, { width: cell - 2, align: 'center' });
      }
    }
  }
  const h2 = rows2 * (cell + gap) + 4;
  doc.fontSize(8).fillColor(DIM).font('Helvetica').text(l2 || '', midX, startY + h2, { width: gridW });

  doc.y = startY + Math.max(h1, h2) + 18;
}

// ════════════════════════════════════════════════════════════════════════════
// PAGE 1 — COVER
// ════════════════════════════════════════════════════════════════════════════
addPage();

// decorative border
doc.rect(25, 25, PW - 50, PH - 50).lineWidth(1.5).stroke(BDR);
doc.rect(30, 30, PW - 60, PH - 60).lineWidth(0.5).stroke('#1e4a1e');

doc.y = 220;
doc.fontSize(42).fillColor(GRN).font('Helvetica-Bold').text('☢ NuclearCraft', { align: 'center' });
doc.fontSize(30).fillColor(CYAN).font('Helvetica-Bold').text('Plutonium Age', { align: 'center' });
doc.moveDown(0.6);
doc.rect(100, doc.y, PW - 200, 2).fill(GRN);
doc.moveDown(0.8);
doc.fontSize(18).fillColor(YLW).font('Helvetica-Bold').text('Complete Player Guide', { align: 'center' });
doc.moveDown(0.4);
doc.fontSize(11).fillColor(WHT).font('Helvetica')
   .text('Crafting Recipes  ·  Custom Items  ·  Machines  ·  Radiation  ·  Combat  ·  Boss', { align: 'center' });

doc.y = PH - 120;
doc.rect(100, doc.y, PW - 200, 1).fill(BDR);
doc.moveDown(0.5);
doc.fontSize(10).fillColor(DIM).font('Helvetica').text('PaperMC 1.21+   ·   Plugin Version 1.0   ·   Phases 1–10', { align: 'center' });
doc.moveDown(0.3);
doc.fontSize(9).fillColor(DIM).font('Helvetica-Oblique')
   .text('All values configurable via YAML config files in the plugin folder', { align: 'center' });

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 2 — TABLE OF CONTENTS
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Table of Contents', YLW);
doc.moveDown(0.5);

const toc = [
  ['Page 2',  'Table of Contents'],
  ['Page 3',  'Material Substitution Reference'],
  ['Page 4',  'All Custom Items — Materials & Components'],
  ['Page 5',  'All Custom Items — Equipment, Machines & Consumables'],
  ['Page 6',  'Crafting Recipes — Machines & Tools'],
  ['Page 7',  'Crafting Recipes — Plutonium Weapons & Tools'],
  ['Page 8',  'Crafting Recipes — Armor (Plutonium & Hazmat)'],
  ['Page 9',  'Crafting Recipes — Cures & Arrows'],
  ['Page 10', 'Radiation System — Stages, Sources & Spread'],
  ['Page 11', 'Radiation System — Protection & Armor'],
  ['Page 12', 'Nuclear Smelter — Machine Guide'],
  ['Page 13', 'Nuclear Forge — Upgrade Machine Guide'],
  ['Page 14', 'World: Ore, Mining & Farming'],
  ['Page 15', 'Irradiated Zombies — All Levels & Loot'],
  ['Page 16', 'Plutonium Titan — Boss Guide'],
  ['Page 17', 'Combat System, Progression & Commands'],
];

toc.forEach(([page, title], i) => {
  ensureSpace(22);
  const y = doc.y;
  if (i % 2 === 0) doc.rect(ML, y, CW, 18).fill('#181818');
  doc.fontSize(10).fillColor(YLW).font('Helvetica-Bold')
     .text(page, ML + 6, y + 4, { width: 55, lineBreak: false });
  doc.fontSize(10).fillColor(WHT).font('Helvetica')
     .text(title, ML + 65, y + 4, { width: CW - 65 });
  doc.y = y + 20;
});

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 3 — MATERIAL SUBSTITUTION REFERENCE
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Material Substitution Reference', YLW);
para('NuclearCraft custom items cannot be directly used in vanilla crafting grids because\nMinecraft\'s recipe system does not understand Custom Model Data. Instead, each custom item\nuses a vanilla material as a stand-in. When you place that material in a crafting grid, the\nplugin validates the real item via its CustomModelData tag before accepting the recipe.', WHT);
doc.moveDown(0.3);

subTitle('Crafting Grid Substitutions', YLW);
tableHeader(['Vanilla Material (place in grid)', 'Represents Custom Item', 'CMD'], [240, 220, 45]);
const subs = [
  ['Echo Shard', 'Refined Plutonium Ingot', '1103'],
  ['Yellow Wool', 'Industrial Fabric', '1315'],
  ['Magma Cream', 'Radioactive Core', '1101'],
  ['Prismarine Crystals', 'Raw Plutonium Fragment', '1102'],
  ['Heart of the Sea', 'Irradiated Heart', '1106'],
  ['Pink Petals', 'Healing Petal', '1105'],
  ['Wheat Seeds', 'Mutated Seed', '1104'],
  ['Nether Star', 'Titan Core  OR  Reactor Heart (context-dependent)', '1107/1109'],
  ['Amethyst Shard', 'Titan Fragment  OR  Mutated Crystal', '1108/1111'],
];
subs.forEach(([v, c, cmd], i) => {
  tableRow([v, c, cmd], [240, 220, 45], [YLW, CYAN, DIM], i % 2 === 0);
});

doc.moveDown(0.5);
subTitle('Block & Item Base Materials', CYAN);
tableHeader(['Vanilla Base', 'Represents', 'CMD'], [220, 240, 45]);
const base = [
  ['Blast Furnace', 'Nuclear Smelter block', '1201'],
  ['Smithing Table', 'Nuclear Forge block', '1401'],
  ['Diamond Pickaxe', 'Radiation Drill', '1108'],
  ['Leather Helmet/Chest/Legs/Boots', 'Hazmat Suit pieces', '1306–1309'],
  ['Netherite Helmet/Chest/Legs/Boots', 'Plutonium Armor pieces', '3001–3004'],
  ['Netherite Sword/Pickaxe/Axe/Shovel/Hoe', 'Plutonium Tools', '2001–2005'],
  ['Honey Bottle', 'Radiation Antidote', '1301'],
  ['Glass Bottle', 'Radiation Serum', '1302'],
  ['Paper', 'Ancient Reactor Blueprint', '1110'],
];
base.forEach(([v, c, cmd], i) => {
  tableRow([v, c, cmd], [220, 240, 45], [YLW, WHT, DIM], i % 2 === 0);
});

doc.moveDown(0.4);
note('CMD = CustomModelData — the unique number that makes each item visually distinct in-game.');
note('Always use the correct custom item (obtained via /nc give commands or gameplay) — placing a plain vanilla Echo Shard without CMD will not work.');

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 4 — ALL CUSTOM ITEMS (MATERIALS & COMPONENTS)
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('All Custom Items — Materials & Components', CYAN);

subTitle('Raw Materials', GRN);
const rawItems = [
  ['Raw Plutonium Fragment', 'Prismarine Crystals', '1102', 'Mined from Plutonium Ore (Y: -64 to -58). 1–4 per ore block. Low radiation while in inventory. Feed into Nuclear Smelter to refine.'],
  ['Refined Plutonium Ingot', 'Echo Shard', '1103', 'Output of the Nuclear Smelter. Primary crafting material for all plutonium weapons, tools, and armor. Also used as repair material in an Anvil (+300 durability per ingot).'],
  ['Radioactive Core', 'Magma Cream', '1101', 'Dropped by Irradiated Zombies. Two uses: fuel for the Nuclear Forge (2,000 energy per Core) and ingredient in some recipes. High radiation source — keep in lead-lined storage.'],
  ['Irradiated Heart', 'Heart of the Sea', '1106', 'Dropped by Irradiated Zombies (rare). Required ingredient for MK-3 and MK-4 Nuclear Forge upgrades. Also required for the Titan summoning ritual (1 Heart). Emits strong radiation.'],
  ['Industrial Fabric', 'Yellow Wool', '1315', 'Radiation-hardened woven material. Used to craft all four Hazmat Suit pieces. Also the repair material for Hazmat armor in an Anvil (+200 durability per piece).'],
  ['Mutated Seed', 'Wheat Seeds', '1104', 'Dropped by Irradiated Zombies (5–50% chance by level). Plant on Radioactive Farmland to grow Mutated Healing Plants. Cannot be planted on normal farmland.'],
  ['Healing Petal', 'Pink Petals', '1105', 'Harvested from fully grown Mutated Healing Plants (1–3 per harvest). Primary ingredient for Radiation Antidote and Radiation Serum. Right-clicking also gives Regen II for 5s and removes 25 radiation.'],
];

rawItems.forEach(([name, base, cmd, desc]) => {
  ensureSpace(48);
  const y = doc.y;
  doc.rect(ML, y, CW, 42).fill(PANL);
  doc.fontSize(10).fillColor(GRN).font('Helvetica-Bold')
     .text(name, ML + 6, y + 4, { width: 220, lineBreak: false });
  doc.fontSize(8.5).fillColor(DIM).font('Helvetica')
     .text('Base: ' + base + '  |  CMD: ' + cmd, ML + 230, y + 5, { width: CW - 236 });
  doc.fontSize(9).fillColor(WHT).font('Helvetica')
     .text(desc, ML + 6, y + 19, { width: CW - 12 });
  doc.y = y + 46;
  doc.moveDown(0.1);
});

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 5 — ALL CUSTOM ITEMS (EQUIPMENT, MACHINES, CONSUMABLES, BOSS)
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('All Custom Items — Equipment, Machines & Consumables', CYAN);

subTitle('Machines & Tools', YLW);
const machItems = [
  ['Nuclear Smelter', 'Blast Furnace', '1201', YLW, 'Converts Raw Plutonium Fragments → Refined Ingots. Right-click to open GUI. Requires fuel (Coal, Charcoal, Coal Block, Blaze Rod, Lava Bucket). Emits 8 radiation per 5 seconds within 3 blocks while active.'],
  ['Nuclear Forge', 'Smithing Table', '1401', ORG, 'Upgrades plutonium and hazmat gear to MK-I through MK-IV. Uses Radioactive Cores as energy fuel (2,000 energy each). Right-click to open GUI. Overloading causes a radiation burst.'],
  ['Radiation Drill', 'Diamond Pickaxe', '1108', CYAN, 'The ONLY tool that safely mines Plutonium Ore. Mining without it gives a +25 radiation burst and drops nothing. Diamond+ mining speed (×1.2 modifier). 1,800 durability. Also has a 10% chance to find extra Radioactive Debris.'],
];
machItems.forEach(([name, base, cmd, color, desc]) => {
  ensureSpace(42);
  const y = doc.y;
  doc.rect(ML, y, CW, 38).fill(PANL);
  doc.fontSize(10).fillColor(color).font('Helvetica-Bold')
     .text(name, ML + 6, y + 4, { width: 220, lineBreak: false });
  doc.fontSize(8.5).fillColor(DIM).font('Helvetica')
     .text('Base: ' + base + '  |  CMD: ' + cmd, ML + 230, y + 5, { width: CW - 236 });
  doc.fontSize(9).fillColor(WHT).font('Helvetica')
     .text(desc, ML + 6, y + 18, { width: CW - 12 });
  doc.y = y + 42;
  doc.moveDown(0.1);
});

subTitle('Consumables & Cures', LIME);
const cureItems = [
  ['Radiation Antidote', 'Honey Bottle', '1301', LIME, 'Right-click to consume. Clears ALL radiation instantly. Removes infection status and all radiation debuffs. Does NOT grant immunity — you can be re-infected immediately. Crafted with Healing Petals + Honey Bottle.'],
  ['Radiation Serum', 'Glass Bottle', '1302', PRP, 'Right-click to consume. Instantly clears ALL radiation. Removes all debuffs. ALSO grants 10 minutes of full radiation immunity — you cannot be irradiated during this window. Expensive to craft.'],
];
cureItems.forEach(([name, base, cmd, color, desc]) => {
  ensureSpace(42);
  const y = doc.y;
  doc.rect(ML, y, CW, 38).fill(PANL);
  doc.fontSize(10).fillColor(color).font('Helvetica-Bold')
     .text(name, ML + 6, y + 4, { width: 220, lineBreak: false });
  doc.fontSize(8.5).fillColor(DIM).font('Helvetica')
     .text('Base: ' + base + '  |  CMD: ' + cmd, ML + 230, y + 5, { width: CW - 236 });
  doc.fontSize(9).fillColor(WHT).font('Helvetica')
     .text(desc, ML + 6, y + 18, { width: CW - 12 });
  doc.y = y + 42;
  doc.moveDown(0.1);
});

subTitle('Boss Items (Titan Drops)', PRP);
const bossItems = [
  ['Titan Core', 'Nether Star', '1107', RED, 'Crystallised essence of the Plutonium Titan. 4 Titan Cores are consumed when performing the summoning ritual to call the Titan. EXTREME radiation hazard — keep in lead-lined storage.'],
  ['Titan Fragment', 'Amethyst Shard', '1108', PRP, 'Shard of the Titan\'s reactor plating. Dropped on kill. Used in high-tier crafting (Phase 11+). Emits extreme radiation.'],
  ['Reactor Heart', 'Nether Star', '1109', RED, 'The beating core of the Titan\'s reactor. Rare drop (10% chance). Required for MK-3 and MK-4 Nuclear Forge upgrades. Critical radiation hazard.'],
  ['Mutated Crystal', 'Amethyst Shard', '1111', CYAN, 'Crystallised radioactive energy grown inside the Titan. 8% drop chance. Used in future high-tier crafting.'],
  ['Ancient Reactor Blueprint', 'Paper', '1110', YLW, 'Very rare Titan drop (5% chance). Unlocks advanced reactor crafting in Phase 11. No current gameplay use — save for future updates.'],
];
bossItems.forEach(([name, base, cmd, color, desc]) => {
  ensureSpace(38);
  const y = doc.y;
  doc.rect(ML, y, CW, 34).fill(PANL);
  doc.fontSize(10).fillColor(color).font('Helvetica-Bold')
     .text(name, ML + 6, y + 4, { width: 220, lineBreak: false });
  doc.fontSize(8.5).fillColor(DIM).font('Helvetica')
     .text('Base: ' + base + '  |  CMD: ' + cmd, ML + 230, y + 5, { width: CW - 236 });
  doc.fontSize(9).fillColor(WHT).font('Helvetica')
     .text(desc, ML + 6, y + 17, { width: CW - 12 });
  doc.y = y + 38;
  doc.moveDown(0.1);
});

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 6 — CRAFTING RECIPES: MACHINES & SPECIAL ITEMS
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Crafting Recipes — Machines & Special Items', GRN);
para('All recipes crafted at a standard Crafting Table unless otherwise noted.\nProxy materials in parentheses are what you actually hold; the plugin checks for the custom item.', DIM);
doc.moveDown(0.3);

subTitle('Nuclear Smelter', YLW);
para('D = Diamond   O = Obsidian   F = Furnace', DIM);
craftGrid([
  ['Diamond',   'Obsidian', 'Diamond'],
  ['Obsidian',  'Furnace',  'Obsidian'],
  ['Diamond',   'Obsidian', 'Diamond'],
], 'Result: 1× Nuclear Smelter block', YLW);

subTitle('Nuclear Forge', ORG);
para('I = Refined Plutonium Ingot (Echo Shard proxy)   D = Ancient Debris   O = Obsidian   T = Smithing Table', DIM);
craftGrid([
  ['Obsidian', 'Anc.Debris', 'Obsidian'],
  ['Ref.Ingot', 'Sm.Table', 'Ref.Ingot'],
  ['Obsidian', 'Anc.Debris', 'Obsidian'],
], 'Result: 1× Nuclear Forge block  |  Ref.Ingot = Echo Shard in grid', ORG);

subTitle('Lead-Lined Storage Crate', CYAN);
para('L = Lead (Iron Ingot proxy)   W = Wood Planks   C = Chest', DIM);
craftGrid([
  ['Iron Ingot', 'Iron Ingot', 'Iron Ingot'],
  ['Iron Ingot', 'Chest',      'Iron Ingot'],
  ['Iron Ingot', 'Iron Ingot', 'Iron Ingot'],
], 'Result: 1× Lead-Lined Storage Crate  |  Stores fragments WITHOUT emitting radiation to nearby players', CYAN);

subTitle('Plutonium Block', LIME);
craftGrid([
  ['Ref.Ingot', 'Ref.Ingot', 'Ref.Ingot'],
  ['Ref.Ingot', 'Ref.Ingot', 'Ref.Ingot'],
  ['Ref.Ingot', 'Ref.Ingot', 'Ref.Ingot'],
], 'Result: 1× Plutonium Block (compact storage)  |  Ref.Ingot = Echo Shard proxy  |  Emits more radiation than single ingots — handle with full Plutonium armor', LIME);

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 7 — CRAFTING RECIPES: TOOLS
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Crafting Recipes — Plutonium Tools', GRN);
para('P = Echo Shard (= Refined Plutonium Ingot)   S = Stick   All tools use Netherite base.', DIM);
doc.moveDown(0.2);

subTitle('Radiation Drill', CYAN);
para('R = Radioactive Core (Magma Cream proxy)   D = Diamond', DIM);
craftGrid([
  ['R-Core', 'Diamond', 'R-Core'],
  ['Diamond', 'Diamond', 'Diamond'],
  ['',       'Stick',   ''],
], 'Result: 1× Radiation Drill  |  Only tool that safely mines Plutonium Ore  |  ×1.2 mining speed vs diamond  |  1,800 durability', CYAN);
doc.moveDown(0.2);

subTitle('Plutonium Sword', RED);
twoGrids(
  [['','Ref.Ingot',''],['','Ref.Ingot',''],['','Stick','']],
  'DMG: +10  ·  Speed: 1.7/s  ·  Dur: 2,500\n+10 radiation on hit  ·  +20 on crit\n+5 rad vs Stage 3+ players',
  RED,
  [['Ref.Ingot','Ref.Ingot',''],['Ref.Ingot','Stick',''],['','Stick','']],
  'DMG: +11  ·  Speed: 1.0/s  ·  Dur: 2,600\n15% shockwave (3-block radius, +15 rad)\nAoE radiation burst on shockwave',
  ORG
);
doc.fontSize(10).fillColor(RED).font('Helvetica-Bold').text('Plutonium Sword', ML, doc.y - 92, { lineBreak: false });
doc.fontSize(10).fillColor(ORG).font('Helvetica-Bold').text('Plutonium Axe', ML + 265, doc.y, { lineBreak: false });
doc.moveDown(4.5);

subTitle('Plutonium Pickaxe', CYAN);
twoGrids(
  [['Ref.Ingot','Ref.Ingot','Ref.Ingot'],['','Stick',''],['','Stick','']],
  'Durability: 2,800\n10% chance to find Radioactive Debris\nRequires Radiation Drill to mine Plutonium Ore',
  CYAN,
  [['','Ref.Ingot',''],['','Stick',''],['','Stick','']],
  'Durability: 2,500\n15% chance to convert soil to\nRadioactive Farmland on dig',
  LIME
);
doc.fontSize(10).fillColor(CYAN).font('Helvetica-Bold').text('Plutonium Pickaxe', ML, doc.y - 90, { lineBreak: false });
doc.fontSize(10).fillColor(LIME).font('Helvetica-Bold').text('Plutonium Shovel', ML + 265, doc.y, { lineBreak: false });
doc.moveDown(4.2);

subTitle('Plutonium Hoe', GRN);
craftGrid([
  ['Ref.Ingot', 'Ref.Ingot', ''],
  ['',          'Stick',     ''],
  ['',          'Stick',     ''],
], 'Result: Plutonium Hoe  |  Durability: 2,000  |  Fast crop tilling', GRN, 80);

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 8 — CRAFTING RECIPES: ARMOR
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Crafting Recipes — Armor', PRP);
para('P = Echo Shard (= Refined Plutonium Ingot)  ·  W = Yellow Wool (= Industrial Fabric)', DIM);
para('Plutonium Armor uses Netherite as base; Hazmat Suit uses Leather.', DIM);
doc.moveDown(0.3);

subTitle('Plutonium Armor (P = Echo Shard)', PRP);
doc.fontSize(9).fillColor(DIM).font('Helvetica')
   .text('Full set: Prot III + Unbreaking III on each piece  |  Full set grants environmental radiation IMMUNITY + Speed II + Fire Resistance', ML, doc.y, { width: CW });
doc.moveDown(0.4);

twoGrids(
  [['P','P','P'],['P','','P'],['','','']],
  'HELMET — 4 armor  ·  2 tough  ·  450 dur\n-15% radiation per piece',
  PRP,
  [['P','','P'],['P','P','P'],['P','P','P']],
  'CHESTPLATE — 8 armor  ·  2 tough  ·  640 dur\n-15% radiation per piece',
  PRP
);
doc.fontSize(9).fillColor(PRP).font('Helvetica-Bold').text('Plutonium Helmet', ML, doc.y - 95, { lineBreak: false });
doc.fontSize(9).fillColor(PRP).font('Helvetica-Bold').text('Plutonium Chestplate', ML + 265, doc.y, { lineBreak: false });
doc.moveDown(4.5);

twoGrids(
  [['P','P','P'],['P','','P'],['P','','P']],
  'LEGGINGS — 6 armor  ·  2 tough  ·  590 dur\n-15% radiation per piece',
  PRP,
  [['','',''],['P','','P'],['P','','P']],
  'BOOTS — 4 armor  ·  2 tough  ·  480 dur\n-15% radiation  ·  Feather Falling II\nNo radiation from ground contact',
  PRP
);
doc.fontSize(9).fillColor(PRP).font('Helvetica-Bold').text('Plutonium Leggings', ML, doc.y - 95, { lineBreak: false });
doc.fontSize(9).fillColor(PRP).font('Helvetica-Bold').text('Plutonium Boots', ML + 265, doc.y, { lineBreak: false });
doc.moveDown(4.5);

subTitle('Hazmat Suit (W = Yellow Wool)', YLW);
doc.fontSize(9).fillColor(DIM).font('Helvetica')
   .text('Full set bonus: 80% radiation reduction total  |  Based on Leather armor  |  Repair with Industrial Fabric', ML, doc.y, { width: CW });
doc.moveDown(0.4);

twoGrids(
  [['W','W','W'],['W','','W'],['','','']],
  'HELMET — 2 armor  ·  363 dur  ·  -20% rad',
  YLW,
  [['W','','W'],['W','W','W'],['W','W','W']],
  'CHESTPLATE — 5 armor  ·  529 dur  ·  -30% rad',
  YLW
);
doc.fontSize(9).fillColor(YLW).font('Helvetica-Bold').text('Hazmat Helmet', ML, doc.y - 88, { lineBreak: false });
doc.fontSize(9).fillColor(YLW).font('Helvetica-Bold').text('Hazmat Chestplate', ML + 265, doc.y, { lineBreak: false });
doc.moveDown(4.2);

twoGrids(
  [['W','W','W'],['W','','W'],['W','','W']],
  'LEGGINGS — 4 armor  ·  496 dur  ·  -20% rad',
  YLW,
  [['','',''],['W','','W'],['W','','W']],
  'BOOTS — 2 armor  ·  430 dur  ·  -10% rad',
  YLW
);
doc.fontSize(9).fillColor(YLW).font('Helvetica-Bold').text('Hazmat Leggings', ML, doc.y - 88, { lineBreak: false });
doc.fontSize(9).fillColor(YLW).font('Helvetica-Bold').text('Hazmat Boots', ML + 265, doc.y, { lineBreak: false });
doc.moveDown(4.2);

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 9 — CRAFTING RECIPES: CURES & ARROWS
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Crafting Recipes — Cures & Arrows', LIME);

subTitle('Radiation Antidote  (Shapeless Recipe)', LIME);
para('Shapeless = ingredients can be in any slots in the crafting grid.', DIM);
doc.moveDown(0.2);
// Show shapeless as a single row
const antY = doc.y;
const antItems = ['Healing\nPetal', 'Healing\nPetal', 'Honey\nBottle'];
antItems.forEach((item, i) => {
  const cx = ML + i * 56;
  doc.rect(cx, antY, 50, 50).lineWidth(0.8).stroke(LIME);
  doc.fontSize(8).fillColor(WHT).font('Helvetica')
     .text(item, cx + 1, antY + 14, { width: 48, align: 'center' });
});
doc.fontSize(20).fillColor(GRN).font('Helvetica-Bold').text('→', ML + 178, antY + 14, { lineBreak: false });
doc.rect(ML + 210, antY, 50, 50).lineWidth(1).stroke(LIME);
doc.fontSize(8).fillColor(LIME).font('Helvetica-Bold')
   .text('Radiation\nAntidote\n×1', ML + 211, antY + 8, { width: 48, align: 'center' });
doc.y = antY + 60;
doc.fontSize(9).fillColor(DIM).font('Helvetica')
   .text('Effect: Instantly clears ALL radiation, infection, and debuffs. Does NOT grant immunity.', ML, doc.y, { width: CW });
doc.moveDown(0.8);

subTitle('Radiation Serum  (Shaped Recipe)', PRP);
para('H = Healing Petal   R = Radioactive Core (Magma Cream proxy)   G = Gold Nugget\nA = Golden Apple   B = Glass Bottle', DIM);
craftGrid([
  ['Healing\nPetal', 'R-Core', 'Healing\nPetal'],
  ['Gold\nNugget',   'Golden\nApple',  'Gold\nNugget'],
  ['Healing\nPetal', 'Glass\nBottle', 'Healing\nPetal'],
], 'Result: 1× Radiation Serum  |  Effect: Clears ALL radiation + grants 10 MINUTES of full radiation immunity\nR-Core = Radioactive Core (Magma Cream proxy)', PRP);

subTitle('Plutonium Arrow  (Shaped Recipe)', GRN);
para('P = Refined Plutonium Ingot (Echo Shard proxy)   A = Vanilla Arrow   F = Feather', DIM);
craftGrid([
  ['', 'Ref.Ingot', ''],
  ['', 'Arrow',     ''],
  ['', 'Feather',   ''],
], 'Result: 4× Plutonium Arrows\nOn hit: +25 radiation  ·  Poison I (4 seconds)  ·  Glowing (5 seconds)\nCritical hit (full bow charge): +20 bonus radiation', GRN, 80);

doc.moveDown(0.4);
subTitle('Smelter Recipe: Raw Fragment → Refined Ingot', YLW);
doc.rect(ML, doc.y, CW, 50).fill(PANL);
const sy = doc.y + 8;
doc.fontSize(10).fillColor(WHT).font('Helvetica-Bold').text('INPUT', ML + 10, sy, { lineBreak: false });
doc.rect(ML + 60, sy - 3, 60, 35).lineWidth(0.8).stroke(GRN);
doc.fontSize(8).fillColor(GRN).font('Helvetica').text('Raw\nPlutFrag', ML + 61, sy + 6, { width: 58, align: 'center' });
doc.fontSize(20).fillColor(YLW).font('Helvetica-Bold').text('→', ML + 135, sy + 6, { lineBreak: false });
doc.rect(ML + 170, sy - 3, 60, 35).lineWidth(0.8).stroke(YLW);
doc.fontSize(8).fillColor(YLW).font('Helvetica').text('Refined\nIngot', ML + 171, sy + 6, { width: 58, align: 'center' });
doc.fontSize(9).fillColor(DIM).font('Helvetica')
   .text('Time: 15 seconds  ·  Requires fuel  ·  Machine must reach 500°C', ML + 240, sy, { width: 255 });
doc.y = sy + 42;

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 10 — RADIATION SYSTEM (STAGES & SOURCES)
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Radiation System — Stages & Sources', RED);
para('Radiation is measured in points (0–1000). It accumulates from exposure to radioactive materials,\nzombies, and machines. As your level rises, debuffs worsen and direct damage begins.', WHT);
doc.moveDown(0.3);

subTitle('Radiation Stages', RED);
const stages = [
  ['Stage 0', 'Healthy', '0–99', 'None', GRN],
  ['Stage 1', 'Minor Exposure', '100–249', 'Weakness I  ·  Nausea', YLW],
  ['Stage 2', 'Moderate Exposure', '250–499', 'Weakness I  ·  Slowness I  ·  Nausea', ORG],
  ['Stage 3', 'Severe Exposure', '500–749', 'Weakness II  ·  Slowness II  ·  Hunger I  ·  0.5 HP damage per cycle', '#ff5500'],
  ['Stage 4', 'Critical Poisoning', '750–1000', 'Weakness III  ·  Slowness III  ·  Hunger II  ·  1.0 HP damage per cycle  ·  Contagious!', RED],
];
tableHeader(['Stage', 'Name', 'Range', 'Effects'], [70, 140, 75, 220]);
stages.forEach(([stage, name, range, effects, c], i) => {
  tableRow([stage, name, range, effects], [70, 140, 75, 220], [c, WHT, YLW, WHT], i % 2 === 0);
});

doc.moveDown(0.4);
subTitle('Natural Progression & Decay', CYAN);
twoCol('Progression (while infected)', 'Stage 1: +5 pts every 60s  ·  Stage 2: +10  ·  Stage 3: +15  ·  Stage 4: +20', CYAN, WHT, 230);
twoCol('Natural Decay (when clean)', 'Requires 10 minutes with NO new radiation → removes 2 pts per 60 seconds', CYAN, WHT, 230);
doc.moveDown(0.3);

subTitle('All Radiation Sources', RED);
tableHeader(['Source', 'Amount', 'Trigger'], [230, 80, 185]);
const sources = [
  ['Plutonium Ore (within 1 block)', '+5 pts', 'Every 5 seconds'],
  ['Plutonium Ore (within 2 blocks)', '+2 pts', 'Every 5 seconds'],
  ['Plutonium Ore (within 3 blocks)', '+1 pt', 'Every 5 seconds'],
  ['Mining ore WITHOUT Radiation Drill', '+25 burst', 'Per attempt (5s cooldown)'],
  ['Raw Plutonium Fragment in inventory', '+1 pt', 'Every 30 seconds'],
  ['Plutonium Block (within 1 block)', '+10 pts', 'Every 5 seconds'],
  ['Plutonium Block (within 2 blocks)', '+5 pts', 'Every 5 seconds'],
  ['Plutonium Block (within 3 blocks)', '+2 pts', 'Every 5 seconds'],
  ['Radioactive Debris (within 5 blocks)', '+0.5/s', 'Continuous passive'],
  ['Nuclear Smelter active (3-block radius)', '+8 pts', 'Every 5 seconds'],
  ['Radiation Cloud (zombie death)', '+5/s', '10 seconds in 3-block radius'],
  ['Irradiated Zombie Lv.1 hit', '+10 pts', 'Per melee hit'],
  ['Irradiated Zombie Lv.2 hit', '+20 pts', 'Per melee hit'],
  ['Irradiated Zombie Lv.3 hit', '+35 pts', 'Per melee hit'],
  ['Alpha Zombie Lv.4 hit', '+50 pts', 'Per melee hit'],
  ['Irradiated Zombie aura (nearby)', '+2 pts', 'Every 5 seconds within range'],
  ['Titan Boss — Radiation Aura', '+3/s', '10-block radius, all phases'],
  ['Titan Boss — Titan Slam', '+15 pts', 'Per slam hit'],
  ['Titan Boss — Radiation Wave', '+40 pts', '20-block radius burst'],
  ['Titan Boss — Reactor Overload', '+70 pts', '18-block radius burst'],
  ['Titan Boss — Energy Beam', '+60 pts', 'Targeted player hit'],
  ['Titan Boss — Nuclear Catastrophe', '+80 pts', '30-block radius (Phase 4 only)'],
  ['MK-IV Forge Overload', '+150 pts', '8-block radius burst'],
  ['Toxic Bloom (farming hazard)', '+8 pts', 'Every 2 seconds, 4-block radius'],
];
sources.forEach(([src, amt, trigger], i) => {
  tableRow([src, amt, trigger], [230, 80, 185], [WHT, RED, DIM], i % 2 === 0);
});

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 11 — RADIATION SYSTEM (PROTECTION & CONTAGION)
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Radiation System — Protection & Contagion', RED);

subTitle('Radiation Cures', LIME);
tableHeader(['Item', 'How to Use', 'Effect'], [160, 120, 225]);
const cures = [
  ['Healing Petal (direct)', 'Right-click to consume', 'Removes 25 radiation  ·  Regen II for 5 seconds'],
  ['Radiation Antidote', 'Right-click to consume', 'Clears ALL radiation, infection & debuffs  ·  No immunity'],
  ['Radiation Serum', 'Right-click to consume', 'Clears ALL radiation & debuffs  ·  10 min full immunity'],
];
cures.forEach(([item, use, effect], i) => {
  tableRow([item, use, effect], [160, 120, 225], [LIME, DIM, WHT], i % 2 === 0);
});

doc.moveDown(0.4);
subTitle('Armor Radiation Protection', CYAN);
para('Armor reduces all incoming radiation exposure by a flat multiplier per piece.', WHT);
tableHeader(['Armor Type / Situation', 'Radiation Reduction', 'Notes'], [220, 130, 155]);
const prot = [
  ['Any single armor piece (generic)', '-15% per piece', 'Up to -60% for full 4-piece set'],
  ['Hazmat Helmet', '-20%', 'Leather base'],
  ['Hazmat Chestplate', '-30%', 'Best single piece for rad protection'],
  ['Hazmat Leggings', '-20%', 'Leather base'],
  ['Hazmat Boots', '-10%', 'No ground radiation special'],
  ['Full Hazmat Suit (all 4 pieces)', '-80% total', 'Best accessible radiation shield'],
  ['Plutonium Armor — full set (4/4)', 'IMMUNE', 'Zero environmental radiation'],
  ['Plutonium Boots (special)', 'No ground radiation', 'Negates radioactive farmland etc.'],
  ['Near Plutonium Ore (hazmat)', '×0.20 multiplier', '80% of ore radiation blocked'],
  ['Near Plutonium Ore (plut. armor)', '×0.00 multiplier', 'Complete immunity to ore zones'],
];
prot.forEach(([armor, red, note2], i) => {
  tableRow([armor, red, note2], [220, 130, 155], [WHT, GRN, DIM], i % 2 === 0);
});

doc.moveDown(0.4);
subTitle('Contagion — Radiation Spread Between Players', RED);
para('Irradiated players at Stage 2+ can spread radiation to others through proximity,\nphysical contact, or shared vehicles.', WHT);
doc.moveDown(0.2);
tableHeader(['Spread Method', 'Condition', 'Amount', 'Chance'], [185, 155, 70, 95]);
const spread = [
  ['Proximity (within 3 blocks)', 'Infected is Stage 2', '+25 pts', '5% per 5s'],
  ['Proximity (within 3 blocks)', 'Infected is Stage 3', '+25 pts', '15% per 5s'],
  ['Proximity (within 3 blocks)', 'Infected is Stage 4', '+25 pts', '25% per 5s'],
  ['Physical contact (melee hit)', 'Any stage', '+50 pts', '35% per hit'],
  ['Shared vehicle (boat/minecart)', 'Any stage', '+25 pts', '20% per 15s'],
];
spread.forEach(([method, cond, amt, chance], i) => {
  tableRow([method, cond, amt, chance], [185, 155, 70, 95], [RED, DIM, RED, YLW], i % 2 === 0);
});

doc.moveDown(0.4);
subTitle('Immunity Details', GRN);
tableHeader(['Source', 'Duration', 'Stacks?'], [230, 160, 115]);
const immunity = [
  ['Radiation Serum (item)', '10 minutes', 'No — resets timer'],
  ['Plutonium Armor (environmental)', 'Permanent while worn', 'Only blocks environmental sources'],
  ['Base game immunity (built-in)', '30s default / up to 5 min', '60s cooldown between refreshes'],
];
immunity.forEach(([src, dur, st], i) => {
  tableRow([src, dur, st], [230, 160, 115], [LIME, WHT, DIM], i % 2 === 0);
});

doc.moveDown(0.3);
subTitle('Particle & Sound Indicators', DIM);
para('Radiation stages are visually indicated by coloured particles around the player:', WHT);
twoCol('Stage 1 (Minor)',    'Bright green particles  [RGB 57, 255, 20]', GRN, WHT, 190);
twoCol('Stage 2 (Moderate)', 'Yellow particles  [RGB 255, 220, 0]', YLW, WHT, 190);
twoCol('Stage 3 (Severe)',   'Orange particles  [RGB 255, 100, 0]', ORG, WHT, 190);
twoCol('Stage 4 (Critical)', 'Dark red particles  [RGB 200, 0, 0]', RED, WHT, 190);
para('A Geiger counter sound effect also plays based on your current radiation level.', DIM);

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 12 — NUCLEAR SMELTER GUIDE
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Nuclear Smelter — Machine Guide', YLW);

subTitle('Placement & Usage', YLW);
para('1. Craft a Nuclear Smelter (Diamond + Obsidian + Furnace pattern — see Page 6)\n2. Place it like any block in the world\n3. Right-click the Smelter to open its GUI\n4. Insert Raw Plutonium Fragments in the INPUT slot\n5. Insert fuel in the FUEL slot\n6. The machine heats up, then begins processing\n7. Collect Refined Plutonium Ingots from the OUTPUT slot', WHT);
doc.moveDown(0.4);

subTitle('GUI Slot Layout', CYAN);
const slots = [['Input Slot','Place Raw Plutonium Fragments here'],['Fuel Slot','Insert Coal, Charcoal, Coal Block, Blaze Rod, or Lava Bucket'],['Output Slot','Collect your Refined Plutonium Ingots here'],['Progress Bar','Shows current processing progress and temperature']];
slots.forEach(([slot, desc]) => twoCol(slot, desc, CYAN, WHT, 160));

doc.moveDown(0.3);
subTitle('Fuel Values', ORG);
tableHeader(['Fuel Item', 'Units', 'Approx. Processing Time'], [200, 80, 225]);
const fuels = [
  ['Coal', '100 units', '~20 seconds of active processing'],
  ['Charcoal', '80 units', '~16 seconds'],
  ['Coal Block', '900 units', '~3 minutes  (most efficient per slot)'],
  ['Blaze Rod', '120 units', '~24 seconds'],
  ['Lava Bucket', '1,000 units', '~3 min 20 sec  ← BEST fuel per bucket'],
];
fuels.forEach(([item, units, time], i) => {
  tableRow([item, units, time], [200, 80, 225], [WHT, YLW, DIM], i % 2 === 0);
});

doc.moveDown(0.4);
subTitle('Temperature System', RED);
para('The Smelter must reach a minimum temperature before processing begins.', WHT);
tableHeader(['Parameter', 'Value'], [280, 225]);
const temps = [
  ['Ambient (idle) temperature', '20°C'],
  ['Minimum temperature to process', '500°C  (must be reached before any processing)'],
  ['Maximum safe temperature', '1,500°C'],
  ['Heating rate (fueled, idle)', '+5°C per tick cycle'],
  ['Active heating rate (processing)', '+2.5°C per tick cycle'],
  ['Cooling rate (idle, no fuel)', '-1°C per tick cycle'],
  ['Overheat duration', '10,000 ms (10 seconds) before cooling begins'],
  ['Overheat cooling rate', '-2°C per tick (twice as fast as normal)'],
  ['Restart threshold', '80% of 500°C = 400°C  (no cold start needed)'],
];
temps.forEach(([param, val], i) => {
  tableRow([param, val], [280, 225], [WHT, YLW], i % 2 === 0);
});

doc.moveDown(0.4);
subTitle('Radiation Hazard', RED);
para('The active Smelter emits passive radiation to all players within 3 blocks.', WHT);
twoCol('Passive radiation amount', '+8 radiation points per check', RED, WHT, 240);
twoCol('Check interval', 'Every 5 seconds (100 ticks)', RED, WHT, 240);
twoCol('Affected radius', '3 blocks around the machine block', RED, WHT, 240);
note('Build the Smelter in an enclosed room with no direct access. Full Hazmat Suit reduces this to ~1.6 pts/check.');
note('Lava Bucket gives the best processing time per inventory slot — carry several for long sessions.');

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 13 — NUCLEAR FORGE GUIDE
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Nuclear Forge — Upgrade Machine Guide', ORG);

subTitle('What It Does', ORG);
para('The Nuclear Forge upgrades Plutonium tools, weapons, and Hazmat armor pieces\nfrom their base state up to MK-I, then to MK-II, MK-III, and finally MK-IV.\nEach upgrade tier adds percentage bonuses to damage, speed, armor, and durability.', WHT);
doc.moveDown(0.3);

subTitle('How to Use', CYAN);
para('1. Craft and place a Nuclear Forge block (see Page 6 for recipe)\n2. Right-click to open the upgrade GUI\n3. Place your item in the INPUT slot\n4. Insert Radioactive Cores in the FUEL slot (each Core = 2,000 energy)\n5. Insert Refined Plutonium Ingots in the MATERIAL slot\n6. For MK-3 and MK-4: also insert Irradiated Hearts in the HEART slot\n7. Click UPGRADE — the process takes several seconds\n8. On success: your item is upgraded  |  On failure: item stays the same (MK-4 has 5% downgrade risk)', WHT);

doc.moveDown(0.4);
subTitle('Upgrade Tiers — Full Details', ORG);
tableHeader(['Tier', 'Ingots', 'Cores', 'Hearts', 'Energy', 'Success%', 'Time', 'Bonus'], [50, 50, 48, 50, 65, 65, 55, 122]);
const upgrades = [
  ['MK-1', '2', '1', '0', '500', '100%', '3 sec', '+5% dmg, speed, armor, dur'],
  ['MK-2', '4', '2', '0', '1,200', '90%', '5 sec', '+10% all stats'],
  ['MK-3', '8', '4', '1', '2,500', '75%', '8 sec', '+20% all stats'],
  ['MK-4', '16', '8', '2', '5,000', '50%', '12 sec', '+35% all stats  ·  5% downgrade risk on fail'],
];
upgrades.forEach(([tier, ing, cores, hearts, energy, succ, time, bonus], i) => {
  const sc = succ === '100%' ? GRN : succ === '90%' ? YLW : succ === '75%' ? ORG : RED;
  tableRow([tier, ing, cores, hearts, energy, succ, time, bonus], [50,50,48,50,65,65,55,122], [YLW, WHT, WHT, WHT, WHT, sc, DIM, WHT], i % 2 === 0);
});
note('Ingots = Refined Plutonium Ingots (Echo Shard proxy)  ·  Cores = Radioactive Cores  ·  Hearts = Irradiated Hearts');
note('MK-4 50% success: on failure, item keeps current tier. 5% additional chance of downgrade to MK-3.');

doc.moveDown(0.4);
subTitle('MK-IV Special — Radiation Aura', PRP);
para('Wearing any MK-IV upgraded item activates a passive Radiation Aura around the player.', WHT);
tableHeader(['Aura Parameter', 'Value'], [280, 225]);
const aura = [
  ['Aura radius', '3 blocks around player'],
  ['Radiation to hostile mobs per tick', '+25 radiation'],
  ['Radiation to enemy players (PvP)', '+15 radiation per tick'],
  ['Aura tick rate', 'Every 2 seconds (40 ticks)'],
  ['Affects team-mates?', 'No  (only hostile mobs and enemies)'],
];
aura.forEach(([p, v], i) => tableRow([p, v], [280, 225], [WHT, GRN], i % 2 === 0));

doc.moveDown(0.4);
subTitle('Overload System', RED);
para('If too much energy is loaded (>10,000 units) the Forge OVERLOADS.', WHT);
tableHeader(['Overload Parameter', 'Value'], [280, 225]);
const overload = [
  ['Max energy before overload', '10,000 units'],
  ['Energy per Radioactive Core', '2,000 units'],
  ['Energy decay (idle)', '-0.5 units per tick (auto-bleeds excess)'],
  ['Overload shutdown duration', '200 ticks (10 seconds)'],
  ['Radiation burst on overload', '+150 radiation to all players in 8-block radius'],
  ['Block destroyed on overload?', 'No  (safe — forge survives)'],
];
overload.forEach(([p, v], i) => tableRow([p, v], [280, 225], [WHT, RED], i % 2 === 0));
note('Never insert more than 5 Radioactive Cores at once — that reaches the max energy limit.');

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 14 — WORLD: ORE, MINING & FARMING
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('World — Ore, Mining & Farming', GRN);

subTitle('Plutonium Ore Spawning', YLW);
tableHeader(['Parameter', 'Value'], [240, 265]);
const oreGen = [
  ['Y-level range', 'Y: -64 to -58  (deepest deepslate layer only)'],
  ['Dimension', 'Overworld only'],
  ['Spawn chance per chunk', '60% of newly-generated chunks get a vein'],
  ['Vein size', '1–2 blocks per vein (very small — rare resource)'],
  ['Valid host blocks', 'Deepslate, Stone, Cobbled Deepslate, Tuff'],
  ['Discovery alert range', '5 blocks — chat notification when nearby'],
];
oreGen.forEach(([p, v], i) => tableRow([p, v], [240, 265], [WHT, GRN], i % 2 === 0));

doc.moveDown(0.3);
subTitle('Mining Rules', RED);
para('Plutonium Ore is dangerous. You MUST use the Radiation Drill to safely extract it.', WHT);
tableHeader(['Scenario', 'Outcome'], [240, 265]);
const mining = [
  ['Mine with any tool EXCEPT Radiation Drill', '+25 radiation burst  ·  Ore does NOT drop  ·  5s cooldown'],
  ['Mine with Radiation Drill (no Fortune)', '1 Raw Plutonium Fragment  ·  3–7 XP  ·  No radiation burst'],
  ['Mine with Radiation Drill + Fortune I', '1–2 Raw Plutonium Fragments'],
  ['Mine with Radiation Drill + Fortune II', '1–3 Raw Plutonium Fragments'],
  ['Mine with Radiation Drill + Fortune III', '1–4 Raw Plutonium Fragments'],
  ['Mine with Radiation Drill + Silk Touch', '1× Plutonium Ore block (no fragments)'],
];
mining.forEach(([sc, out], i) => tableRow([sc, out], [240, 265], [WHT, YLW], i % 2 === 0));

doc.moveDown(0.3);
subTitle('Radioactive Farming — How to Farm', LIME);
para('Step-by-step farming process:', WHT);
para('1. Get Mutated Seeds from Irradiated Zombie drops (all levels)\n2. Create Radioactive Farmland: dig normal dirt/grass with a Plutonium Shovel (15% chance per block)\n3. Right-click to plant Mutated Seeds on Radioactive Farmland ONLY — other farmland rejects them\n4. Wait for the plant to grow through 5 stages (0 = seed planted, 4 = fully bloomed)\n5. Right-click to harvest at Stage 4 — you get Healing Petals and bonus seeds\n6. The crop replants itself (some seeds drop)', WHT);

doc.moveDown(0.3);
subTitle('Farming Stats & Drops', LIME);
tableHeader(['Parameter', 'Value'], [240, 265]);
const farm = [
  ['Growth stages', '0 (seed) → 1 → 2 → 3 → 4 (mature blossom)'],
  ['Radioactive Farmland growth bonus', '+50% extra random-tick growth chance'],
  ['Bone meal compatible?', 'Yes — vanilla bone meal works normally'],
  ['Healing Petals per harvest', '1–3 Petals'],
  ['Mutated Seeds per harvest', '0–2 Seeds (self-sustaining supply)'],
  ['XP per harvest', '2–5 XP'],
  ['Toxic Bloom chance', '1% — fully grown crop turns dangerous instead of dropping'],
  ['Farmland passive radiation', '+1 pt every 2 seconds to nearby players'],
];
farm.forEach(([p, v], i) => tableRow([p, v], [240, 265], [WHT, LIME], i % 2 === 0));

doc.moveDown(0.3);
subTitle('Toxic Bloom — Farming Hazard', RED);
para('Rarely (1%), a fully-grown Mutated Healing Plant mutates into a Toxic Bloom instead of\ndropping normally. It immediately begins irradiating the area.', WHT);
tableHeader(['Toxic Bloom Parameter', 'Value'], [240, 265]);
const toxic = [
  ['Radiation radius', '4 blocks'],
  ['Radiation amount', '+8 points per 2 seconds'],
  ['How to remove', 'Break it with any tool — it drops nothing'],
];
toxic.forEach(([p, v], i) => tableRow([p, v], [240, 265], [WHT, RED], i % 2 === 0));
note('Always harvest Mutated Healing Plants as soon as they reach Stage 4 to minimise Toxic Bloom risk.');

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 15 — IRRADIATED ZOMBIES
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Irradiated Zombies', GRN);

subTitle('Spawning Rules', YLW);
para('60% of all naturally-spawning zombies become Irradiated Zombies. Baby zombies are excluded.\nEligible spawn reasons: Natural, Jockey, Mount, Reinforcements, Village Defence, Chunk Gen.', WHT);
doc.moveDown(0.3);

subTitle('Level Stats', RED);
tableHeader(['Level (Spawn %)', 'HP', 'DMG', 'Speed', 'KB Res', 'Rad/Hit', 'XP'], [130, 44, 44, 70, 60, 60, 97]);
const zombStats = [
  ['Lv.1 — Standard (80%)', '35 HP', '5 DMG', '×1.15', '0.15', '+10 pts', '10 XP'],
  ['Lv.2 — Enhanced (15%)', '45 HP', '7 DMG', '×1.20', '0.20', '+20 pts', '20 XP'],
  ['Lv.3 — Powerful (4%)', '60 HP', '9 DMG', '×1.25', '0.30', '+35 pts', '40 XP'],
  ['Lv.4 — Alpha (1%)', '80 HP', '12 DMG', '×1.30', '0.50', '+50 pts', '100 XP'],
];
const zombC = [WHT, YLW, ORG, RED];
zombStats.forEach(([lv, hp, dmg, spd, kb, rad, xp], i) => {
  tableRow([lv, hp, dmg, spd, kb, rad, xp], [130,44,44,70,60,60,97], [zombC[i], WHT, WHT, WHT, WHT, RED, YLW], i % 2 === 0);
});
note('Level 4 Alpha: displays green Bossbar "☢ Alpha Irradiated Zombie ☢", Glowing effect, visible from 20 blocks. Treat as a mini-boss.');

doc.moveDown(0.4);
subTitle('Loot Drops', YLW);
tableHeader(['Item', 'Lv.1', 'Lv.2', 'Lv.3', 'Lv.4 Alpha'], [210, 65, 65, 65, 100]);
const loot = [
  ['Rotten Flesh', '0–3 always', '0–3 always', '0–3 always', '0–3 always'],
  ['Radioactive Core (Magma Cream)', '15%', '25%', '40%', '100% (ALWAYS)'],
  ['Mutated Seed (Wheat Seeds)', '5%', '10%', '20%', '50%'],
  ['Irradiated Heart (Heart of Sea)', '1%', '3%', '7%', '25%'],
];
loot.forEach(([item, l1, l2, l3, l4], i) => {
  const vc = [WHT, DIM, DIM, DIM, l4.includes('ALWAYS') ? GRN : YLW];
  tableRow([item, l1, l2, l3, l4], [210,65,65,65,100], vc, i % 2 === 0);
});

doc.moveDown(0.4);
subTitle('Radiation Cloud (On Zombie Death)', RED);
twoCol('Spawn chance per death', '20% chance', RED, WHT, 230);
twoCol('Cloud radius', '3 blocks', RED, WHT, 230);
twoCol('Duration', '10 seconds', RED, WHT, 230);
twoCol('Radiation inside cloud', '+5 points per second', RED, WHT, 230);
note('Avoid standing near freshly-killed Irradiated Zombies — even at low levels the cloud is dangerous.');

doc.moveDown(0.4);
subTitle('Radiation Surge Night Event', PRP);
twoCol('Trigger chance per night', '5%', PRP, WHT, 230);
twoCol('Effect on radiation', 'ALL radiation damage doubled for the full night', PRP, WHT, 230);
twoCol('Effect on loot', 'ALL zombie drops doubled', PRP, WHT, 230);
twoCol('Player alert', 'Bossbar shown: ☢ RADIATION SURGE ACTIVE ☢', PRP, WHT, 230);
twoCol('Chat broadcast', '"☢ RADIATION SURGE! A wave of radioactive energy sweeps the world!"', PRP, WHT, 230);

doc.moveDown(0.4);
subTitle('Zombie Advancements & XP Rewards', GRN);
twoCol('First Exposure', '+50 XP — first radiation hit from any zombie', GRN, WHT, 230);
twoCol('Mutant Hunter', '+100 XP — kill first Irradiated Zombie', GRN, WHT, 230);
twoCol('Core Collector', '+100 XP — collect first Radioactive Core', GRN, WHT, 230);
twoCol('Alpha Slayer', '+500 XP — kill first Level 4 Alpha Zombie', GRN, WHT, 230);

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 16 — PLUTONIUM TITAN BOSS
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Plutonium Titan — Boss Guide', PRP);

subTitle('Summoning Ritual', RED);
para('You need: 4× Titan Cores in your inventory  +  1× Irradiated Heart in your hand.', WHT);
para('ALTAR LAYOUT:\n  Center block: Crying Obsidian\n  4 Corner blocks: Obsidian  (placed 2 blocks diagonally from the center on the same Y level)\n\nSTEPS:\n  1. Build the altar (Crying Obsidian center, Obsidian corners)\n  2. Have 4× Titan Cores anywhere in your inventory\n  3. Right-click the center Crying Obsidian while holding an Irradiated Heart\n  4. All 4 Titan Cores are consumed immediately from your inventory\n  5. A 15-second summoning animation plays\n  6. The Plutonium Titan spawns at the altar center\n\nCOOLDOWN: 60 minutes between summons.  Only one Titan can exist at a time.', WHT);

doc.moveDown(0.3);
subTitle('Health Scaling (by nearby player count)', YLW);
tableHeader(['Players Nearby', 'Titan HP'], [240, 265]);
const hp = [['1–3 players', '5,000 HP'],['4–6 players', '6,500 HP'],['7–10 players', '8,500 HP'],['10+ players', '10,000 HP']];
hp.forEach(([p, v], i) => tableRow([p, v], [240, 265], [WHT, RED], i % 2 === 0));

doc.moveDown(0.3);
subTitle('Phase System', RED);
tableHeader(['Phase (HP%)', 'Speed Mult.', 'DMG Mult.', 'Active Abilities'], [130, 85, 85, 205]);
const phases = [
  ['Phase 1 (100%–75%)', '×1.00', '×1.00', 'Radiation Aura  ·  Titan Slam'],
  ['Phase 2 (75%–50%)', '×1.10', '×1.20', 'Phase 1 +  Radiation Wave  ·  Mutant Summoning'],
  ['Phase 3 (50%–25%)', '×1.25', '×1.50', 'Phase 2 +  Reactor Overload  ·  Energy Beam'],
  ['Phase 4 (25%–0%)', '×1.50', '×2.00', 'Phase 3 +  Nuclear Catastrophe  ·  Final Frenzy'],
];
const phaseC = [GRN, YLW, ORG, RED];
phases.forEach(([ph, sp, dm, ab], i) => {
  tableRow([ph, sp, dm, ab], [130,85,85,205], [phaseC[i], YLW, RED, WHT], i % 2 === 0);
});

doc.moveDown(0.3);
subTitle('All Titan Abilities', RED);
tableHeader(['Ability', 'Phases', 'Radius', 'Damage/Radiation', 'Cooldown'], [160, 60, 55, 170, 60]);
const abilities = [
  ['Radiation Aura (passive)', 'All', '10 blocks', '+3 rad/sec continuously', 'None'],
  ['Titan Slam', '1, 2, 3, 4', '5 blocks', '12 DMG + 15 rad + knockback', '10 sec'],
  ['Radiation Wave', '2, 3, 4', '20 blocks', '+40 radiation burst (ring)', '15 sec'],
  ['Mutant Summon', '2, 3, 4', '—', '3 Lv.1 + 1 Lv.4 Alpha zombie', '25 sec'],
  ['Reactor Overload', '3, 4', '18 blocks', '+70 radiation burst', '40 sec'],
  ['Energy Beam', '3, 4', 'Targeted', '25 DMG + 60 radiation', '20 sec'],
  ['Nuclear Catastrophe', '4 only', '30 blocks', '+80 rad  ·  sets all HP to 1', '90 sec'],
  ['Final Frenzy', '4 only', '—', '+50% attack speed forever', 'One-time'],
];
abilities.forEach(([name, ph, rad, dmg, cd], i) => {
  tableRow([name, ph, rad, dmg, cd], [160,60,55,170,60], [RED, DIM, YLW, WHT, DIM], i % 2 === 0);
});
note('Nuclear Catastrophe: 10-second charge-up before the blast — watch for the animation and retreat!');
note('Mutant Summon spawns a Level 4 Alpha every 25 seconds from Phase 2 — clear them quickly!');

doc.moveDown(0.3);
subTitle('Titan Rewards (Per Contributing Player)', YLW);
tableHeader(['Reward', 'Amount / Chance', 'Condition'], [220, 140, 145]);
const rewards = [
  ['Server XP', '5,000 XP total (split by contribution)', 'Any contribution'],
  ['Titan Fragments', '1–8+ depending on contribution %', 'Any contribution'],
  ['Refined Plutonium Ingots', 'Scales with contribution', 'Minimum 5% contribution'],
  ['Reactor Heart (Nether Star)', '10% chance', 'Qualifying players'],
  ['Ancient Reactor Blueprint', '5% chance', 'Qualifying players'],
  ['Mutated Crystal', '8% chance', 'Qualifying players'],
  ['Loot Chest at death location', '1× chest with Cores, Fragments, Ingots', 'All participants'],
];
rewards.forEach(([rew, amt, cond], i) => {
  tableRow([rew, amt, cond], [220, 140, 145], [YLW, GRN, DIM], i % 2 === 0);
});

footerLine();

// ════════════════════════════════════════════════════════════════════════════
// PAGE 17 — COMBAT & COMMANDS
// ════════════════════════════════════════════════════════════════════════════
addPage();
sectionTitle('Combat System & Admin Commands', CYAN);

subTitle('Plutonium Weapon Effects', RED);
tableHeader(['Weapon / Hit Type', 'Radiation Applied', 'Additional Effect'], [200, 110, 195]);
const combat = [
  ['Plutonium Sword (base hit)', '+10 radiation', 'None'],
  ['Sword — Critical hit (falling)', '+10 + 5 bonus = 15', 'Extra radiation on crit'],
  ['Sword — vs Stage 3+ victim', '+10 + 5 bonus = 15', 'Bonus when victim already infected'],
  ['Plutonium Axe (base hit)', '+0 base radiation', '15% chance: 3-block shockwave +15 rad'],
  ['Axe — Shockwave (15%)', 'AoE +15 radiation', '3-block radius, all targets hit'],
  ['Plutonium Arrow (hit)', '+25 radiation', 'Poison I (4s)  ·  Glowing (5s)'],
  ['Arrow — Critical (full charge)', '+25 + 20 = 45', 'Bonus radiation on full pull'],
  ['Arrow — Headshot bonus', '+25 + 10 = 35', 'Approximated headshot detection'],
];
combat.forEach(([w, r, e], i) => tableRow([w, r, e], [200,110,195], [WHT, RED, DIM], i % 2 === 0));

doc.moveDown(0.4);
subTitle('Combo System', YLW);
twoCol('Max combo stack', '8 hits', YLW, WHT, 210);
twoCol('Combo reset time', '6 seconds after last hit', YLW, WHT, 210);
twoCol('Bonus radiation per stack', '+5 per hit above the first (hit 2 = +5, hit 3 = +10…)', YLW, WHT, 210);
twoCol('Maximum combo bonus', '+35 radiation (capped regardless of hit count)', YLW, WHT, 210);

doc.moveDown(0.3);
subTitle('Weapon Mastery Levels', PRP);
tableHeader(['Level', 'XP Required', 'Unlocks / Bonus'], [100, 100, 305]);
const mastery = [
  ['Novice', '0 XP', 'Starting level — no bonuses'],
  ['Experienced', '100 XP', '+1 XP/sword hit  ·  +10 XP/kill'],
  ['Veteran', '500 XP', '+1 XP/axe hit  ·  +10 XP/kill'],
  ['Elite', '1,500 XP', '+2 XP/bow hit  ·  +15 XP/kill'],
  ['Master', '4,000 XP', '+10% radiation on all weapon hits  ·  +10% arrow radiation  ·  +1.5 block axe shockwave radius'],
];
mastery.forEach(([lv, xp, bonus], i) => tableRow([lv, xp, bonus], [100,100,305], [PRP, YLW, WHT], i % 2 === 0));

doc.moveDown(0.4);
subTitle('Equipment Repair', LIME);
twoCol('Repair Plutonium tools/armor', 'Anvil + Refined Plutonium Ingot (Echo Shard proxy)  →  +300 durability per ingot', LIME, WHT, 230);
twoCol('Repair Hazmat Suit pieces', 'Anvil + Industrial Fabric (Yellow Wool proxy)  →  +200 durability per piece', LIME, WHT, 230);

doc.moveDown(0.4);
subTitle('Admin / Operator Commands  (/nuclearcraft  or  /nc)', GRN);
tableHeader(['Command', 'Description'], [270, 235]);
const cmds = [
  ['/nc radiation set <player> <0-1000>', 'Set a player\'s radiation to a specific value'],
  ['/nc radiation add <player> <amount>', 'Add radiation points to a player'],
  ['/nc radiation clear <player>', 'Clear all radiation from a player'],
  ['/nc radiation status [player]', 'View current radiation stage and points'],
  ['/nc ore give fragment [amount]', 'Give Raw Plutonium Fragments'],
  ['/nc ore give drill', 'Give a Radiation Drill'],
  ['/nc equipment give <type>', 'Give plutonium/hazmat item (sword/axe/pickaxe/shovel/hoe/helmet/chestplate/leggings/boots/hazmat-helmet/hazmat-chestplate/hazmat-leggings/hazmat-boots)'],
  ['/nc farming give seed [amount]', 'Give Mutated Seeds'],
  ['/nc farming give petal [amount]', 'Give Healing Petals'],
  ['/nc farming give antidote [amount]', 'Give Radiation Antidotes'],
  ['/nc farming give serum [amount]', 'Give Radiation Serums'],
  ['/nc smelter give', 'Give a Nuclear Smelter block'],
  ['/nc forge give', 'Give a Nuclear Forge block'],
  ['/nc titan spawn', 'Force-spawn the Plutonium Titan at your location'],
  ['/nc titan kill', 'Instantly despawn the active Titan'],
  ['/nc titan phase <1-4>', 'Force the Titan to a specific combat phase'],
  ['/nc reload', 'Reload all YAML config files without restart'],
];
cmds.forEach(([cmd, desc], i) => tableRow([cmd, desc], [270, 235], [GRN, WHT], i % 2 === 0));

doc.moveDown(0.4);
// Final note
doc.rect(ML, doc.y, CW, 30).fill(PANL);
const ly = doc.y + 8;
doc.fontSize(9.5).fillColor(YLW).font('Helvetica-Bold')
   .text('All values are configurable in the plugin YAML files inside the server\'s /plugins/NuclearCraft/ folder.', ML + 8, ly, { width: CW - 16, align: 'center' });

footerLine();

// ── Finish ────────────────────────────────────────────────────────────────────
doc.end();
out.on('finish', () => console.log('✅  PDF generated: NuclearCraft-Guide.pdf  (' + pageNum + ' pages)'));
out.on('error', e => console.error('ERROR:', e));
