'use strict';
const PDFDocument = require('pdfkit');
const fs = require('fs');

const doc = new PDFDocument({ margin: 42, size: 'A4', autoFirstPage: false,
  info: { Title: 'NuclearCraft: Plutonium Age — Player Guide' } });
const OUT = fs.createWriteStream('/home/runner/workspace/NuclearCraft-Guide.pdf');
doc.pipe(OUT);

const PW = 595.28, PH = 841.89, ML = 42, MR = 42, CW = PW - ML - MR;
const BG='#0f0f0f', PANEL='#181818', GRN='#39ff14', CYAN='#00cfff',
      YLW='#ffd000', ORG='#ff8800', RED='#ff3300', PRP='#9933ff',
      LIME='#7fff00', WHT='#e0e0e0', DIM='#777777', BDR='#1e5c1e';

let pg = 0;

function addPage() {
  doc.addPage();
  pg++;
  doc.rect(0, 0, PW, PH).fill(BG);
  doc.y = 40; doc.x = ML;
}
function footer() {
  doc.fontSize(7.5).fillColor(DIM).font('Helvetica')
     .text('NuclearCraft: Plutonium Age  ·  Complete Guide  ·  p.' + pg,
           ML, PH - 24, { width: CW, align: 'center' });
  doc.rect(ML, PH - 30, CW, 0.5).fill(BDR);
}
function space(n) { doc.moveDown(n || 0.5); }
function guard(h) {
  if (PH - 32 - doc.y < (h || 30)) { footer(); addPage(); }
}

// ── Section header ──────────────────────────────────────────────
function H1(text, col) {
  guard(32);
  col = col || GRN;
  const y = doc.y;
  doc.rect(ML, y, CW, 20).fill(PANEL);
  doc.rect(ML, y, 4, 20).fill(col);
  doc.fontSize(12).fillColor(col).font('Helvetica-Bold')
     .text('  ' + text, ML + 8, y + 4, { width: CW - 12 });
  doc.y = y + 24; space(0.2);
}
// ── Sub-header ──────────────────────────────────────────────────
function H2(text, col) {
  guard(22);
  col = col || CYAN;
  space(0.3);
  doc.fontSize(10.5).fillColor(col).font('Helvetica-Bold').text(text, ML, doc.y, { width: CW });
  doc.rect(ML, doc.y, CW, 0.7).fill(col);
  space(0.5);
}
// ── Body text ───────────────────────────────────────────────────
function P(text, col) {
  guard(14);
  doc.fontSize(9).fillColor(col || WHT).font('Helvetica').text(text, ML, doc.y, { width: CW });
  space(0.3);
}
// ── Note ────────────────────────────────────────────────────────
function N(text) {
  guard(12);
  doc.fontSize(8).fillColor(DIM).font('Helvetica-Oblique')
     .text('☢  ' + text, ML + 8, doc.y, { width: CW - 8 });
  space(0.2);
}
// ── Two-column key: value ────────────────────────────────────────
function KV(k, v, kCol, vCol, kW) {
  guard(14);
  kW = kW || 195;
  const y = doc.y;
  doc.fontSize(9).fillColor(kCol || YLW).font('Helvetica-Bold')
     .text(k, ML + 4, y, { width: kW, lineBreak: false });
  doc.fontSize(9).fillColor(vCol || WHT).font('Helvetica')
     .text(v, ML + kW + 8, y, { width: CW - kW - 8 });
  if (doc.y === y) space(0.45); else space(0.1);
}
// ── Table ───────────────────────────────────────────────────────
function TH(cols, ws) {
  guard(18);
  const y = doc.y;
  doc.rect(ML, y, CW, 15).fill('#222222');
  let x = ML + 4;
  cols.forEach((c, i) => {
    doc.fontSize(7.5).fillColor(GRN).font('Helvetica-Bold')
       .text(c, x, y + 4, { width: ws[i], lineBreak: false });
    x += ws[i];
  });
  doc.y = y + 17;
}
function TR(vals, ws, cols, even) {
  guard(15);
  const y = doc.y;
  if (even) doc.rect(ML, y, CW, 14).fill(PANEL);
  let x = ML + 4;
  vals.forEach((v, i) => {
    doc.fontSize(8.5).fillColor(cols && cols[i] || WHT).font('Helvetica')
       .text(v, x, y + 2, { width: ws[i], lineBreak: false });
    x += ws[i];
  });
  doc.y = y + 15;
}
// ── ASCII-style recipe block ────────────────────────────────────
function recipe(title, rows, col, result) {
  guard(80);
  col = col || GRN;
  const y = doc.y;
  doc.rect(ML, y, CW, 14).fill(PANEL);
  doc.fontSize(9.5).fillColor(col).font('Helvetica-Bold')
     .text(title, ML + 6, y + 3, { width: CW - 6 });
  doc.y = y + 16;

  // draw 3×N grid as cells
  const cell = 50, gap = 2;
  rows.forEach(row => {
    guard(cell + 4);
    const ry = doc.y;
    row.forEach((item, ci) => {
      const cx = ML + ci * (cell + gap);
      doc.rect(cx, ry, cell, cell - 4).lineWidth(0.7).stroke(col);
      if (item && item.trim()) {
        doc.fontSize(6.5).fillColor(WHT).font('Helvetica')
           .text(item, cx + 1, ry + (cell - 4) / 2 - 8, { width: cell - 2, align: 'center' });
      }
    });
    doc.y = ry + cell - 2;
  });
  if (result) {
    doc.fontSize(8).fillColor(DIM).font('Helvetica')
       .text('→ ' + result, ML, doc.y + 2, { width: CW });
    space(0.5);
  } else {
    space(0.4);
  }
}

// ══════════════════════════════════════════════════════════════
// PAGE 1 — COVER
// ══════════════════════════════════════════════════════════════
addPage();
doc.rect(24, 24, PW - 48, PH - 48).lineWidth(1.5).stroke(BDR);
doc.y = 230;
doc.fontSize(40).fillColor(GRN).font('Helvetica-Bold').text('☢ NuclearCraft', { align:'center' });
doc.fontSize(28).fillColor(CYAN).font('Helvetica-Bold').text('Plutonium Age', { align:'center' });
space(0.5);
doc.rect(100, doc.y, PW-200, 2).fill(GRN); space(0.7);
doc.fontSize(16).fillColor(YLW).font('Helvetica-Bold').text('Complete Player Guide', { align:'center' });
space(0.3);
doc.fontSize(10).fillColor(WHT).font('Helvetica')
   .text('Items  ·  Recipes  ·  Machines  ·  Radiation  ·  Zombies  ·  Boss', { align:'center' });
doc.y = PH - 90;
doc.fontSize(9).fillColor(DIM).font('Helvetica')
   .text('PaperMC 1.21+   ·   Plugin v1.0   ·   Phases 1–10\nAll values configurable via YAML config files', { align:'center' });
footer();

// ══════════════════════════════════════════════════════════════
// PAGE 2 — MATERIAL SUBSTITUTIONS + CUSTOM ITEMS
// ══════════════════════════════════════════════════════════════
addPage();
H1('Material Substitution Reference', YLW);
P('Custom items cannot be placed directly in crafting grids. Use these vanilla materials as proxies — the plugin validates the real custom item automatically.', DIM);
space(0.2);
TH(['Vanilla Material (place in grid)', 'Represents (custom item)', 'CMD'], [215, 215, 75]);
[
  ['Echo Shard',           'Refined Plutonium Ingot',  '1103'],
  ['Yellow Wool',          'Industrial Fabric',         '1315'],
  ['Magma Cream',          'Radioactive Core',          '1101'],
  ['Prismarine Crystals',  'Raw Plutonium Fragment',    '1102'],
  ['Heart of the Sea',     'Irradiated Heart',          '1106'],
  ['Pink Petals',          'Healing Petal',             '1105'],
  ['Wheat Seeds',          'Mutated Seed',              '1104'],
  ['Blast Furnace (block)','Nuclear Smelter (machine)', '1201'],
  ['Smithing Table (block)','Nuclear Forge (machine)',  '1401'],
  ['Diamond Pickaxe',      'Radiation Drill',           '1108'],
  ['Leather armor pieces', 'Hazmat Suit pieces',        '1306–1309'],
  ['Netherite armor pieces','Plutonium Armor pieces',   '3001–3004'],
  ['Honey Bottle',         'Radiation Antidote',        '1301'],
  ['Glass Bottle',         'Radiation Serum',           '1302'],
].forEach(([v,c,cmd],i) => TR([v,c,cmd],[215,215,75],[YLW,CYAN,DIM],i%2===0));

H1('All Custom Items', CYAN);
H2('Materials & Components', GRN);
TH(['Item','Base Material','CMD','Notes'],[145,135,45,180]);
[
  ['Raw Plutonium Fragment','Prismarine Crystals','1102','Mined from ore. Smelter input.'],
  ['Refined Plutonium Ingot','Echo Shard','1103','Smelter output. Main crafting mat. Repairs plut. gear (+300 dur).'],
  ['Radioactive Core','Magma Cream','1101','Zombie drop. Forge fuel (2,000 energy). High radiation.'],
  ['Irradiated Heart','Heart of the Sea','1106','Rare zombie drop. MK-3/4 upgrade + Titan ritual.'],
  ['Industrial Fabric','Yellow Wool','1315','Crafts & repairs Hazmat Suit (+200 dur per anvil use).'],
  ['Mutated Seed','Wheat Seeds','1104','Zombie drop. Plant on Radioactive Farmland only.'],
  ['Healing Petal','Pink Petals','1105','Farm harvest. Antidote/serum ingredient. Direct: -25 rad + Regen II.'],
  ['Titan Core','Nether Star','1107','Dropped by Titan or crafted (Phase 11). 4 needed to summon Titan.'],
  ['Titan Fragment','Amethyst Shard','1108','Titan loot. High-tier crafting (Phase 11+).'],
  ['Reactor Heart','Nether Star','1109','Rare Titan drop (10%). MK-3/4 upgrade ingredient.'],
  ['Mutated Crystal','Amethyst Shard','1111','Titan drop (8%). Future crafting.'],
  ['Ancient Reactor Blueprint','Paper','1110','Very rare Titan drop (5%). Phase 11+ unlock.'],
].forEach(([n,b,cmd,notes],i) => TR([n,b,cmd,notes],[145,135,45,180],[GRN,YLW,DIM,WHT],i%2===0));

H2('Machines, Tools & Consumables', YLW);
TH(['Item','Base Material','CMD','Function'],[145,140,45,175]);
[
  ['Nuclear Smelter','Blast Furnace','1201','Refines Raw Fragments → Ingots. Emits 8 rad/5s within 3 blocks.'],
  ['Nuclear Forge','Smithing Table','1401','Upgrades gear MK-1→MK-4. Radioactive Core fuel.'],
  ['Radiation Drill','Diamond Pickaxe','1108','Only safe Plutonium Ore mining tool. ×1.2 speed. 1,800 dur.'],
  ['Radiation Antidote','Honey Bottle','1301','Right-click: clears ALL radiation & debuffs. No immunity.'],
  ['Radiation Serum','Glass Bottle','1302','Right-click: clears ALL radiation + 10 min immunity.'],
].forEach(([n,b,cmd,fn],i) => TR([n,b,cmd,fn],[145,140,45,175],[ORG,YLW,DIM,WHT],i%2===0));
footer();

// ══════════════════════════════════════════════════════════════
// PAGE 3 — CRAFTING RECIPES (MACHINES, TOOLS, WEAPONS)
// ══════════════════════════════════════════════════════════════
addPage();
H1('Crafting Recipes', GRN);
P('P = Echo Shard (Refined Plutonium Ingot)   W = Yellow Wool (Industrial Fabric)   S = Stick   R = Magma Cream (Radioactive Core)', DIM);
space(0.3);

H2('Machine Recipes', YLW);
// Two recipes side by side using KV text format to avoid positioning bugs
// Show recipe patterns as text grids in a panel
const recPanels = [
  { title:'Nuclear Smelter', col:YLW, rows:[['Diamond','Obsidian','Diamond'],['Obsidian','Furnace','Obsidian'],['Diamond','Obsidian','Diamond']], note:'D=Diamond O=Obsidian F=Furnace' },
  { title:'Nuclear Forge', col:ORG, rows:[['Obsidian','Anc.Debris','Obsidian'],['P (Ingot)','Sm.Table','P (Ingot)'],['Obsidian','Anc.Debris','Obsidian']], note:'P=Echo Shard (Ref.Ingot proxy)' },
  { title:'Plutonium Block', col:LIME, rows:[['P','P','P'],['P','P','P'],['P','P','P']], note:'9× Refined Plutonium Ingots → compact storage' },
  { title:'Lead-Lined Crate', col:CYAN, rows:[['Iron','Iron','Iron'],['Iron','Chest','Iron'],['Iron','Iron','Iron']], note:'Stores fragments with zero radiation leak' },
];
recPanels.forEach(r => recipe(r.title, r.rows, r.col, r.note));

H2('Radiation Drill', CYAN);
recipe('Radiation Drill — only tool that safely mines Plutonium Ore',
  [['R-Core','Diamond','R-Core'],['Diamond','Diamond','Diamond'],['','Stick','']], CYAN,
  'R-Core=Radioactive Core (Magma Cream proxy)  ·  ×1.2 speed  ·  1,800 dur');

H2('Plutonium Weapons & Tools', RED);
recipe('Plutonium Sword  (+10 DMG, +10 rad/hit, crit +5 rad, 2,500 dur)',
  [['','P',''],['','P',''],['','S','']], RED, null);
recipe('Plutonium Axe  (+11 DMG, 15% shockwave 3-block +15 rad, 2,600 dur)',
  [['P','P',''],['P','S',''],['','S','']], ORG, null);
recipe('Plutonium Pickaxe  (10% debris find chance, 2,800 dur)',
  [['P','P','P'],['','S',''],['','S','']], CYAN, null);
recipe('Plutonium Shovel  (15% soil→Radioactive Farmland, 2,500 dur)',
  [['','P',''],['','S',''],['','S','']], LIME, null);
recipe('Plutonium Hoe  (2,000 dur)',
  [['P','P',''],['','S',''],['','S','']], GRN, null);
recipe('Plutonium Arrow  (×4 output — +25 rad, Poison I 4s, Glowing 5s)',
  [['','P',''],['','Arrow',''],['','Feather','']], GRN,
  'P=Echo Shard  ·  Full charge crit: +20 bonus radiation');
footer();

// ══════════════════════════════════════════════════════════════
// PAGE 4 — ARMOR + CURE RECIPES
// ══════════════════════════════════════════════════════════════
addPage();
H1('Armor & Cure Recipes', PRP);

H2('Plutonium Armor  (P = Echo Shard proxy)', PRP);
P('All pieces: Protection III + Unbreaking III  ·  Full 4-piece set: environmental radiation IMMUNITY + Speed II + Fire Resistance', DIM);
[
  {name:'Plutonium Helmet      (4A 2T 450dur -15%rad)',     rows:[['P','P','P'],['P','','P'],['','','']]},
  {name:'Plutonium Chestplate  (8A 2T 640dur -15%rad)',     rows:[['P','','P'],['P','P','P'],['P','P','P']]},
  {name:'Plutonium Leggings    (6A 2T 590dur -15%rad)',     rows:[['P','P','P'],['P','','P'],['P','','P']]},
  {name:'Plutonium Boots       (4A 2T 480dur -15%rad Feather Fall II)',rows:[['','',''],['P','','P'],['P','','P']]},
].forEach(r => recipe(r.name, r.rows, PRP, null));

H2('Hazmat Suit  (W = Yellow Wool proxy)', YLW);
P('Full 4-piece set: 80% radiation reduction  ·  Leather base  ·  Repair with Industrial Fabric', DIM);
[
  {name:'Hazmat Helmet      (2A 0.5T 363dur -20%rad)',    rows:[['W','W','W'],['W','','W'],['','','']]},
  {name:'Hazmat Chestplate  (5A 0.5T 529dur -30%rad)',    rows:[['W','','W'],['W','W','W'],['W','W','W']]},
  {name:'Hazmat Leggings    (4A 0.5T 496dur -20%rad)',    rows:[['W','W','W'],['W','','W'],['W','','W']]},
  {name:'Hazmat Boots       (2A 0.5T 430dur -10%rad)',    rows:[['','',''],['W','','W'],['W','','W']]},
].forEach(r => recipe(r.name, r.rows, YLW, null));

H2('Cure Recipes', LIME);
recipe('Radiation Antidote  (SHAPELESS — any slot order)',
  [['Healing Petal','Healing Petal','Honey Bottle']], LIME,
  'Clears ALL radiation, debuffs & infection. No immunity granted.');
recipe('Radiation Serum',
  [['Heal.Petal','R-Core','Heal.Petal'],['Gold Nugget','Gold Apple','Gold Nugget'],['Heal.Petal','Glass Bottle','Heal.Petal']],
  PRP, 'R-Core=Radioactive Core  ·  Clears ALL radiation + 10 min full immunity');

H2('Nuclear Smelter Processing', YLW);
KV('Input  →  Output', 'Raw Plutonium Fragment  →  1× Refined Plutonium Ingot', YLW, GRN, 130);
KV('Processing time', '15 seconds (300 ticks)', WHT, WHT, 130);
KV('Fuel required', 'Coal (100u) · Charcoal (80u) · Coal Block (900u) · Blaze Rod (120u) · Lava Bucket (1,000u ← best)', WHT, WHT, 130);
KV('Min temperature', '500°C  (machine heats up first, then processes)', WHT, WHT, 130);
footer();

// ══════════════════════════════════════════════════════════════
// PAGE 5 — RADIATION SYSTEM
// ══════════════════════════════════════════════════════════════
addPage();
H1('Radiation System', RED);

H2('Stages', RED);
TH(['Stage','Name','Points','Effects'],[50,130,70,255]);
[
  ['0','Healthy','0–99','None',GRN],
  ['1','Minor Exposure','100–249','Weakness I  ·  Nausea',YLW],
  ['2','Moderate Exposure','250–499','Weakness I  ·  Slowness I  ·  Nausea  ·  5% proximity spread',ORG],
  ['3','Severe Exposure','500–749','Weakness II  ·  Slowness II  ·  Hunger I  ·  -0.5 HP/cycle  ·  15% spread','#ff5500'],
  ['4','Critical Poisoning','750–1000','Weakness III  ·  Slowness III  ·  Hunger II  ·  -1 HP/cycle  ·  25% spread',RED],
].forEach(([s,n,p,e,c],i) => TR([s,n,p,e],[50,130,70,255],[c,WHT,YLW,WHT],i%2===0));

H2('Key Radiation Sources', RED);
TH(['Source','Amount','Trigger'],[230,75,200]);
[
  ['Plutonium Ore ≤1 block','+5 pts','every 5s'],
  ['Plutonium Ore ≤2 blocks','+2 pts','every 5s'],
  ['Mining ore WITHOUT Radiation Drill','+25 burst','per attempt (5s cooldown)'],
  ['Radioactive Debris (5-block radius)','+0.5/s','continuous'],
  ['Nuclear Smelter active (3-block radius)','+8 pts','every 5s'],
  ['Radiation Cloud (20% on zombie death)','+5/s','10s, 3-block radius'],
  ['Irradiated Zombie Lv.1–4 hit','+10/+20/+35/+50','per melee hit by level'],
  ['Titan — Radiation Aura','+3/s','10-block radius, all phases'],
  ['Titan — Radiation Wave','+40 pts','20-block radius burst (Phase 2+)'],
  ['Titan — Reactor Overload','+70 pts','18-block radius (Phase 3+)'],
  ['Titan — Nuclear Catastrophe','+80 pts  + sets HP to 1','30-block radius (Phase 4)'],
  ['MK-IV Forge Overload','+150 pts','8-block radius burst'],
].forEach(([s,a,t],i) => TR([s,a,t],[230,75,200],[WHT,RED,DIM],i%2===0));

H2('Protection & Cures', GRN);
TH(['Protection Method','Radiation Reduction'],[280,225]);
[
  ['Generic armor — per piece','-15% per piece (max -60% full set)'],
  ['Full Hazmat Suit (4/4)','-80% total'],
  ['Full Plutonium Armor (4/4)','IMMUNE — zero environmental radiation'],
  ['Plutonium Boots special','No ground-contact radiation'],
  ['Near ore with Hazmat suit','×0.20 multiplier (80% blocked)'],
  ['Near ore with Plutonium armor','×0.00 (complete immunity to ore zone)'],
].forEach(([p,r],i) => TR([p,r],[280,225],[WHT,GRN],i%2===0));

H2('Contagion (Spread Between Players)', RED);
TH(['Method','Condition','Amount','Chance'],[185,150,65,105]);
[
  ['Proximity (≤3 blocks)','Infected Stage 2','+25 pts','5% / 5s'],
  ['Proximity (≤3 blocks)','Infected Stage 3','+25 pts','15% / 5s'],
  ['Proximity (≤3 blocks)','Infected Stage 4','+25 pts','25% / 5s'],
  ['Melee hit (physical contact)','Any stage','+50 pts','35% / hit'],
  ['Shared vehicle (boat/cart)','Any stage','+25 pts','20% / 15s'],
].forEach(([m,c,a,ch],i) => TR([m,c,a,ch],[185,150,65,105],[RED,DIM,RED,YLW],i%2===0));
space(0.3);
KV('Natural decay','Requires 10 min clean → -2 pts per 60s', CYAN, WHT, 150);
KV('Radiation Antidote','Clears ALL radiation + debuffs instantly (no immunity)', LIME, WHT, 150);
KV('Radiation Serum','Clears ALL + grants 10 min full immunity', PRP, WHT, 150);
footer();

// ══════════════════════════════════════════════════════════════
// PAGE 6 — MACHINES + ORE + FARMING
// ══════════════════════════════════════════════════════════════
addPage();
H1('Machines — Nuclear Smelter & Nuclear Forge', YLW);

H2('Nuclear Smelter — Usage', YLW);
P('1. Craft Smelter (Diamond+Obsidian+Furnace pattern)\n2. Place block, right-click to open GUI\n3. Add Raw Plutonium Fragments (Input) and fuel (Fuel slot)\n4. Machine heats to 500°C then processes — collect Refined Ingots from Output', WHT);
TH(['Fuel','Units','Duration'],[150,80,275]);
[['Coal','100u','~20s'],['Charcoal','80u','~16s'],['Coal Block','900u','~3 min'],['Blaze Rod','120u','~24s'],['Lava Bucket','1,000u','~3m20s  ← most efficient per slot']].forEach(([f,u,d],i)=>TR([f,u,d],[150,80,275],[WHT,YLW,DIM],i%2===0));
N('Active smelter emits +8 radiation every 5 seconds within 3 blocks. Keep it in a sealed room.');

H2('Nuclear Forge — Upgrade Tiers', ORG);
P('Place block, insert Radioactive Cores (fuel), place item + materials, click UPGRADE.', WHT);
TH(['Tier','Ingots','Cores','Hearts','Energy','Success','Bonus'],[52,52,52,52,65,65,167]);
[
  ['MK-1','2','1','0','500','100%','+5% dmg/speed/armor/dur'],
  ['MK-2','4','2','0','1,200','90%','+10% all stats'],
  ['MK-3','8','4','1','2,500','75%','+20% all stats'],
  ['MK-4','16','8','2','5,000','50%','+35% all stats  ·  5% downgrade on fail'],
].forEach(([t,i2,c,h,e,s,b],i)=>{
  const sc=s==='100%'?GRN:s==='90%'?YLW:s==='75%'?ORG:RED;
  TR([t,i2,c,h,e,s,b],[52,52,52,52,65,65,167],[YLW,WHT,WHT,WHT,WHT,sc,WHT],i%2===0);
});
N('MK-IV equips a Radiation Aura: +25 rad to nearby mobs, +15 to enemy players, every 2s within 3 blocks.');
N('Forge Overload (>10,000 energy): +150 radiation burst in 8-block radius — never load more than 5 Cores.');

H1('Ore, Mining & Radioactive Farming', GRN);
H2('Plutonium Ore', GRN);
TH(['Parameter','Value'],[220,285]);
[
  ['Y-level','−64 to −58  (deepest deepslate layer, Overworld only)'],
  ['Spawn rate','60% of newly generated chunks  ·  Vein size: 1–2 blocks'],
  ['Mine WITHOUT Radiation Drill','+25 radiation burst  ·  Ore gives nothing  ·  5s cooldown'],
  ['Mine WITH Radiation Drill','1 Raw Plutonium Fragment (Fortune I: 1–2, II: 1–3, III: 1–4)  ·  3–7 XP'],
  ['Near ore passive radiation','≤1 block: +5/5s  ·  ≤2 blocks: +2/5s  ·  ≤3 blocks: +1/5s'],
  ['Hazmat suit near ore','80% radiation reduction (×0.20 multiplier)'],
  ['Plutonium armor near ore','Complete immunity (×0.00 multiplier)'],
].forEach(([p,v],i) => TR([p,v],[220,285],[WHT,YLW],i%2===0));

H2('Radioactive Farming', LIME);
P('Steps: Get Mutated Seeds from zombies → dig soil with Plutonium Shovel (15% farmland conversion) → plant seeds on Radioactive Farmland → harvest at Stage 4 (5 stages total) → get 1–3 Healing Petals + 0–2 bonus seeds.', WHT);
TH(['Parameter','Value'],[220,285]);
[
  ['Radioactive Farmland growth bonus','+50% extra growth chance per random tick'],
  ['Harvest drops','1–3 Healing Petals  ·  0–2 Mutated Seeds  ·  2–5 XP'],
  ['Toxic Bloom chance','1% — fully grown crop becomes a radiation hazard instead of dropping'],
  ['Toxic Bloom danger','+8 radiation every 2s in 4-block radius — break immediately'],
  ['Farmland passive radiation','+1 pt / 2s to nearby players (Plutonium Boots negate this)'],
].forEach(([p,v],i) => TR([p,v],[220,285],[WHT,LIME],i%2===0));
footer();

// ══════════════════════════════════════════════════════════════
// PAGE 7 — ZOMBIES + BOSS + COMBAT + COMMANDS
// ══════════════════════════════════════════════════════════════
addPage();
H1('Irradiated Zombies', GRN);
P('60% of natural zombie spawns become Irradiated. Baby zombies excluded.', DIM);

H2('Level Stats & Loot', RED);
TH(['Level (spawn%)','HP','DMG','Speed','KB Res','Rad/Hit','XP','Core%','Heart%'],[115,30,35,55,50,55,32,42,60]);
[
  ['Lv.1 Standard (80%)','35','5',  '×1.15','0.15','+10','10', '15%','1%'],
  ['Lv.2 Enhanced (15%)','45','7',  '×1.20','0.20','+20','20', '25%','3%'],
  ['Lv.3 Powerful (4%)', '60','9',  '×1.25','0.30','+35','40', '40%','7%'],
  ['Lv.4 Alpha (1%)',    '80','12', '×1.30','0.50','+50','100','100%','25%'],
].forEach(([lv,hp,d,sp,kb,rad,xp,c,h],i)=>{
  const lc=[WHT,YLW,ORG,RED][i];
  TR([lv,hp,d,sp,kb,rad,xp,c,h],[115,30,35,55,50,55,32,42,60],[lc,WHT,WHT,WHT,WHT,RED,YLW,GRN,LIME],i%2===0);
});
N('Level 4 Alpha: green Bossbar, Glowing effect. Also drops 50% Mutated Seeds. Treat as a mini-boss.');
N('Death cloud: 20% chance per zombie → +5 rad/s, 3-block radius, 10 seconds. Move away immediately.');
N('Radiation Surge: 5% chance per night — doubles all radiation AND all loot for the entire night.');

H1('Plutonium Titan Boss', PRP);
H2('Summoning Ritual', RED);
P('ALTAR: Crying Obsidian at center. Obsidian blocks at 4 corners (2 blocks diagonal from center, same Y).\nREQUIRED: 4× Titan Cores (consumed from inventory) + 1× Irradiated Heart (in hand, right-click center block).\nANIMATION: 15 seconds → Titan spawns. COOLDOWN: 60 minutes. Only one Titan at a time.', WHT);

H2('Health & Phases', RED);
TH(['Players Nearby','Titan HP'],[180,325]);
[['1–3','5,000 HP'],['4–6','6,500 HP'],['7–10','8,500 HP'],['10+','10,000 HP']].forEach(([p,h],i)=>TR([p,h],[180,325],[WHT,RED],i%2===0));
space(0.2);
TH(['Phase (HP%)','Spd','DMG','Abilities Active'],[120,50,50,285]);
[
  ['Phase 1 (100–75%)','×1.00','×1.00','Radiation Aura  ·  Titan Slam'],
  ['Phase 2  (75–50%)','×1.10','×1.20','+ Radiation Wave  ·  Mutant Summon (3+1Alpha)'],
  ['Phase 3  (50–25%)','×1.25','×1.50','+ Reactor Overload  ·  Energy Beam'],
  ['Phase 4   (25–0%)','×1.50','×2.00','+ Nuclear Catastrophe  ·  Final Frenzy (+50% ATK speed, one-time)'],
].forEach(([ph,sp,dm,ab],i)=>{
  const c=[GRN,YLW,ORG,RED][i];
  TR([ph,sp,dm,ab],[120,50,50,285],[c,YLW,RED,WHT],i%2===0);
});
N('Nuclear Catastrophe (Phase 4): 10s charge → 30-block radius → +80 radiation + sets all players to 1 HP. Retreat!');

H2('Titan Rewards', YLW);
TH(['Reward','Chance / Amount'],[280,225]);
[
  ['Titan Fragments','Guaranteed  ·  1–8+ by contribution'],
  ['Refined Plutonium Ingots','Scales with contribution %'],
  ['Reactor Heart','10% per qualifying player'],
  ['Ancient Reactor Blueprint','5% per qualifying player'],
  ['Mutated Crystal','8% per qualifying player'],
  ['Loot Chest at death location','Always — Cores, Fragments, Ingots inside'],
  ['Server XP','5,000 XP total, split by contribution'],
].forEach(([r,a],i) => TR([r,a],[280,225],[YLW,GRN],i%2===0));

H1('Combat System & Admin Commands', CYAN);
H2('Weapon Effects & Mastery', RED);
TH(['Weapon / Hit','Radiation','Extra Effect'],[190,95,220]);
[
  ['Plutonium Sword base hit','+10 rad','—'],
  ['Sword crit (falling attack)','+15 rad','+5 bonus on crit'],
  ['Sword vs Stage 3+ victim','+15 rad','+5 bonus vs infected'],
  ['Plutonium Axe shockwave (15%)','+15 rad AoE','3-block radius burst'],
  ['Plutonium Arrow hit','+25 rad','Poison I (4s)  ·  Glowing (5s)'],
  ['Arrow — full charge crit','+45 rad','+20 bonus on max draw'],
].forEach(([w,r,e],i) => TR([w,r,e],[190,95,220],[WHT,RED,DIM],i%2===0));
space(0.2);
KV('Combo system','Up to 8-hit stack  ·  +5 rad per stack  ·  max +35 bonus  ·  resets after 6s idle', YLW, WHT, 140);
KV('Weapon Mastery','Novice→Experienced(100XP)→Veteran(500)→Elite(1,500)→Master(4,000): at Master +10% all weapon radiation', YLW, WHT, 140);
KV('Repair Plutonium gear','Anvil + Echo Shard (Refined Ingot) → +300 durability per ingot', LIME, WHT, 140);
KV('Repair Hazmat Suit','Anvil + Yellow Wool (Industrial Fabric) → +200 durability per piece', LIME, WHT, 140);

H2('Admin Commands  (/nuclearcraft  or  /nc)', GRN);
TH(['Command','Description'],[265,240]);
[
  ['/nc radiation set <player> <0-1000>','Set a player\'s radiation level'],
  ['/nc radiation add <player> <amount>','Add radiation to a player'],
  ['/nc radiation clear <player>','Clear all radiation'],
  ['/nc radiation status [player]','Check radiation stage and points'],
  ['/nc ore give fragment [amt]','Give Raw Plutonium Fragments'],
  ['/nc ore give drill','Give a Radiation Drill'],
  ['/nc equipment give <type>','Give plutonium/hazmat gear piece'],
  ['/nc farming give <seed|petal|antidote|serum>','Give farming/cure items'],
  ['/nc smelter give','Give a Nuclear Smelter block'],
  ['/nc forge give','Give a Nuclear Forge block'],
  ['/nc titan spawn','Force-spawn the Titan at your location'],
  ['/nc titan kill','Despawn the active Titan'],
  ['/nc titan phase <1-4>','Force Titan to a specific phase'],
  ['/nc reload','Reload all YAML configs without restart'],
].forEach(([c,d],i) => TR([c,d],[265,240],[GRN,WHT],i%2===0));
footer();

// ── Finish ────────────────────────────────────────────────────
doc.end();
OUT.on('finish', () => console.log('✅  PDF: NuclearCraft-Guide.pdf  (' + pg + ' pages)'));
OUT.on('error', e => console.error('ERROR:', e));
