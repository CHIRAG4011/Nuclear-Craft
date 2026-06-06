const PDFDocument = require('pdfkit');
const fs = require('fs');

const doc = new PDFDocument({ 
  margin: 50, 
  size: 'A4',
  info: { Title: 'NuclearCraft: Plutonium Age — Complete Guide', Author: 'NuclearCraft' }
});

const out = fs.createWriteStream('/home/runner/workspace/NuclearCraft-Guide.pdf');
doc.pipe(out);

// ── Colour palette ──────────────────────────────────────────────
const C = {
  green:  '#39ff14',
  lime:   '#00ff88',
  cyan:   '#00bfff',
  yellow: '#ffcc00',
  orange: '#ff8800',
  red:    '#ff2200',
  purple: '#7700ff',
  white:  '#ffffff',
  light:  '#cccccc',
  dim:    '#888888',
  bg:     '#0d0d0d',
  panel:  '#1a1a1a',
  border: '#39ff14',
};

// ── Helpers ─────────────────────────────────────────────────────

function fillPage(color) {
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(color || C.bg);
}

function header(text, color) {
  color = color || C.green;
  doc.fontSize(22).fillColor(color).font('Helvetica-Bold').text(text, { align: 'center' });
  doc.moveDown(0.3);
  doc.rect(50, doc.y, doc.page.width - 100, 2).fill(color);
  doc.moveDown(0.8);
}

function subHeader(text, color) {
  color = color || C.cyan;
  doc.fontSize(14).fillColor(color).font('Helvetica-Bold').text(text);
  doc.rect(50, doc.y, doc.page.width - 100, 1).fill(color).opacity(0.5);
  doc.opacity(1);
  doc.moveDown(0.5);
}

function body(text, color) {
  doc.fontSize(10).fillColor(color || C.light).font('Helvetica').text(text);
}

function bullet(label, value, labelColor, valueColor) {
  const x = doc.x;
  doc.fontSize(10).fillColor(labelColor || C.yellow).font('Helvetica-Bold')
     .text('• ' + label + ': ', { continued: true });
  doc.fontSize(10).fillColor(valueColor || C.light).font('Helvetica').text(value || '');
}

function note(text) {
  doc.fontSize(9).fillColor(C.dim).font('Helvetica-Oblique').text('  ☢ ' + text);
}

function spacer(n) { doc.moveDown(n || 0.5); }

function recipeBox(title, grid, notes, color) {
  color = color || C.green;
  const startY = doc.y;
  const boxW = doc.page.width - 100;
  
  // Panel background
  doc.rect(50, startY, boxW, 0).fill(C.panel); // placeholder
  
  doc.fontSize(11).fillColor(color).font('Helvetica-Bold').text('  ' + title, 52, startY + 6);
  doc.moveDown(0.3);
  
  // Grid display
  const cellSize = 52;
  const gridX = 60;
  const gridY = doc.y;
  
  for (let row = 0; row < grid.length; row++) {
    for (let col = 0; col < grid[row].length; col++) {
      const cx = gridX + col * (cellSize + 4);
      const cy = gridY + row * (cellSize + 4);
      const cell = grid[row][col];
      doc.rect(cx, cy, cellSize, cellSize).lineWidth(1).stroke(C.border);
      if (cell) {
        doc.fontSize(7).fillColor(C.light).font('Helvetica')
           .text(cell, cx + 2, cy + cellSize/2 - 6, { width: cellSize - 4, align: 'center' });
      }
    }
  }
  
  const gridH = grid.length * (cellSize + 4) + 4;
  doc.y = gridY + gridH + 6;
  
  if (notes && notes.length) {
    notes.forEach(n => {
      doc.fontSize(9).fillColor(C.dim).font('Helvetica').text('  → ' + n);
    });
  }
  doc.moveDown(0.8);
}

function statTable(rows, col1W) {
  col1W = col1W || 180;
  const col2X = 50 + col1W + 10;
  rows.forEach(([label, value, vc]) => {
    const y = doc.y;
    doc.fontSize(10).fillColor(C.yellow).font('Helvetica-Bold').text(label, 55, y, { width: col1W, lineBreak: false });
    doc.fontSize(10).fillColor(vc || C.light).font('Helvetica').text(value, col2X, y);
    doc.moveDown(0.05);
  });
  doc.moveDown(0.4);
}

function newPage() {
  doc.addPage();
  fillPage();
}

function pageNum(n, total) {
  doc.fontSize(8).fillColor(C.dim)
     .text('NuclearCraft: Plutonium Age — Complete Guide  |  Page ' + n,
           50, doc.page.height - 35, { align: 'center', width: doc.page.width - 100 });
}

// ════════════════════════════════════════════════════════════════
//  PAGE 1 — COVER
// ════════════════════════════════════════════════════════════════
fillPage();

doc.y = 180;
doc.fontSize(36).fillColor(C.green).font('Helvetica-Bold')
   .text('☢ NuclearCraft', { align: 'center' });
doc.fontSize(28).fillColor(C.cyan).font('Helvetica-Bold')
   .text('Plutonium Age', { align: 'center' });
doc.moveDown(0.5);
doc.rect(100, doc.y, doc.page.width - 200, 3).fill(C.green);
doc.moveDown(1);
doc.fontSize(16).fillColor(C.yellow).font('Helvetica-Bold')
   .text('Complete Player Guide', { align: 'center' });
doc.moveDown(0.4);
doc.fontSize(11).fillColor(C.light).font('Helvetica')
   .text('Crafting Recipes  ·  Materials  ·  Machines  ·  Combat  ·  Boss', { align: 'center' });

doc.y = 560;
doc.fontSize(9).fillColor(C.dim).font('Helvetica')
   .text('PaperMC 1.21+  ·  Plugin Version 1.0  ·  Phase 1–10', { align: 'center' });

pageNum(1, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 2 — MATERIAL SUBSTITUTIONS (Key Reference)
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  Material Substitution Reference', C.yellow);
body('Because Minecraft\'s vanilla crafting system cannot filter items by custom data,\nNuclearCraft uses vanilla base materials as stand-ins in crafting grids.\nWhen you see these materials in a recipe, the plugin checks your actual custom item.', C.light);
spacer(0.8);

subHeader('Crafting Grid Substitutions', C.yellow);
const subs = [
  ['Echo Shard', 'Refined Plutonium Ingot', 'ECHO_SHARD', '1103'],
  ['Yellow Wool', 'Industrial Fabric', 'YELLOW_WOOL', '1315'],
  ['Magma Cream', 'Radioactive Core', 'MAGMA_CREAM', '1101'],
  ['Prismarine Crystals', 'Raw Plutonium Fragment', 'PRISMARINE_CRYSTALS', '1102'],
  ['Heart of the Sea', 'Irradiated Heart', 'HEART_OF_THE_SEA', '1106'],
  ['Nether Star (1)', 'Reactor Heart', 'NETHER_STAR', '1109'],
  ['Nether Star (2)', 'Titan Core', 'NETHER_STAR', '1107'],
  ['Pink Petals', 'Healing Petal', 'PINK_PETALS', '1105'],
  ['Wheat Seeds', 'Mutated Seed', 'WHEAT_SEEDS', '1104'],
];

// Table header
const tw = doc.page.width - 100;
doc.rect(50, doc.y, tw, 18).fill('#222222');
doc.fontSize(9).fillColor(C.green).font('Helvetica-Bold')
   .text('Vanilla Material (in recipe)', 54, doc.y - 14, { width: 170, lineBreak: false });
doc.fontSize(9).fillColor(C.green).font('Helvetica-Bold')
   .text('Represents Custom Item', 230, doc.y - 14, { width: 160, lineBreak: false });
doc.fontSize(9).fillColor(C.green).font('Helvetica-Bold')
   .text('CMD', 400, doc.y - 14, { width: 50, lineBreak: false });
doc.moveDown(0.15);

subs.forEach(([vanilla, custom, mat, cmd], i) => {
  const rowY = doc.y;
  if (i % 2 === 0) doc.rect(50, rowY, tw, 16).fill('#181818');
  doc.fontSize(9).fillColor(C.yellow).font('Helvetica-Bold')
     .text(vanilla, 54, rowY + 3, { width: 170, lineBreak: false });
  doc.fontSize(9).fillColor(C.cyan).font('Helvetica')
     .text(custom, 230, rowY + 3, { width: 160, lineBreak: false });
  doc.fontSize(8).fillColor(C.dim).font('Helvetica')
     .text(cmd, 400, rowY + 3, { width: 70 });
  doc.moveDown(0.15);
});

spacer(1);
subHeader('Base Materials Used for Machine Blocks', C.cyan);
const machSubs = [
  ['Blast Furnace', 'Nuclear Smelter block', '1201'],
  ['Smithing Table', 'Nuclear Forge block', '1401'],
  ['Diamond Pickaxe', 'Radiation Drill', '1108'],
  ['Leather Helmet/Chest/Legs/Boots', 'Hazmat Suit pieces', '1306–1309'],
  ['Netherite Helmet/Chest/Legs/Boots', 'Plutonium Armor pieces', '3001–3004'],
  ['Honey Bottle', 'Radiation Antidote', '1301'],
  ['Glass Bottle', 'Radiation Serum', '1302'],
  ['Paper', 'Ancient Reactor Blueprint', '1110'],
  ['Amethyst Shard (1)', 'Titan Fragment', '1108'],
  ['Amethyst Shard (2)', 'Mutated Crystal', '1111'],
];
machSubs.forEach(([vanilla, custom, cmd], i) => {
  const rowY = doc.y;
  if (i % 2 === 0) doc.rect(50, rowY, tw, 16).fill('#181818');
  doc.fontSize(9).fillColor(C.yellow).font('Helvetica-Bold')
     .text(vanilla, 54, rowY + 3, { width: 190, lineBreak: false });
  doc.fontSize(9).fillColor(C.light).font('Helvetica')
     .text(custom, 250, rowY + 3, { width: 180, lineBreak: false });
  doc.fontSize(8).fillColor(C.dim).font('Helvetica')
     .text(cmd, 440, rowY + 3, { width: 80 });
  doc.moveDown(0.15);
});

spacer(0.5);
note('CMD = CustomModelData — the internal number that makes each item visually unique in-game.');

pageNum(2, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 3 — ALL CUSTOM ITEMS
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  All Custom Items', C.cyan);

const itemSections = [
  {
    title: 'Raw Materials & Components',
    color: C.green,
    items: [
      ['Raw Plutonium Fragment', 'Mined from Plutonium Ore (deepest layer). Base processing input.', 'Prismarine Crystals', '1102', 'Low radiation while carried'],
      ['Refined Plutonium Ingot', 'Output of Nuclear Smelter. Primary crafting material for all plutonium gear.', 'Echo Shard', '1103', 'Moderate radiation source'],
      ['Radioactive Core', 'Dropped by Irradiated Zombies. Fuel for Nuclear Forge. High radiation.', 'Magma Cream', '1101', 'High radiation — dangerous'],
      ['Irradiated Heart', 'Dropped by Irradiated Zombies. Required for MK-3/4 upgrades and Titan summoning.', 'Heart of the Sea', '1106', 'Pulsing with corrupted energy'],
      ['Industrial Fabric', 'Used to craft and repair Hazmat Suits.', 'Yellow Wool', '1315', 'Radiation-resistant weave'],
      ['Mutated Seed', 'Dropped by zombies. Plant on Radioactive Farmland to grow Healing Plants.', 'Wheat Seeds', '1104', 'Irradiated by plutonium'],
      ['Healing Petal', 'Harvested from Mutated Healing Plants. Ingredient for radiation cures.', 'Pink Petals', '1105', 'Primary cure ingredient'],
    ]
  },
  {
    title: 'Machines & Tools',
    color: C.yellow,
    items: [
      ['Nuclear Smelter', 'Industrial refining machine. Converts Raw Fragments → Refined Ingots.', 'Blast Furnace', '1201', 'Right-click to open'],
      ['Nuclear Forge', 'Upgrade machine. Upgrades plutonium & hazmat gear from MK-1 to MK-4.', 'Smithing Table', '1401', 'Requires Radioactive Core fuel'],
      ['Radiation Drill', 'Only tool that safely mines Plutonium Ore. Diamond+ speed.', 'Diamond Pickaxe', '1108', 'No radiation burst on use'],
    ]
  },
  {
    title: 'Consumables & Cures',
    color: C.lime,
    items: [
      ['Radiation Antidote', 'Clears all radiation, infection, and debuffs. Does NOT grant immunity.', 'Honey Bottle', '1301', 'Right-click to consume'],
      ['Radiation Serum', 'Instantly clears radiation AND grants 10 min immunity. Rare & expensive.', 'Glass Bottle', '1302', 'Right-click to consume'],
    ]
  },
  {
    title: 'Titan / Boss Items',
    color: C.purple,
    items: [
      ['Titan Core', 'Crystallized essence of the Plutonium Titan. 4 required for summoning ritual.', 'Nether Star', '1107', 'EXTREME radiation hazard'],
      ['Titan Fragment', 'Drop from the Titan. Used in high-tier crafting.', 'Amethyst Shard', '1108', 'Extreme radiation'],
      ['Reactor Heart', 'Rare Titan drop. Required for MK-3/4 Forge upgrades.', 'Nether Star', '1109', 'Critical radiation hazard'],
      ['Mutated Crystal', 'Rare Titan drop. Grown inside the Titan\'s body.', 'Amethyst Shard', '1111', 'Handle with care'],
      ['Ancient Reactor Blueprint', 'Very rare Titan drop. Unlocks Phase 11 advanced crafting.', 'Paper', '1110', 'Future content'],
    ]
  },
];

itemSections.forEach(section => {
  subHeader(section.title, section.color);
  section.items.forEach(([name, desc, base, cmd, lore]) => {
    const y = doc.y;
    doc.fontSize(10).fillColor(section.color).font('Helvetica-Bold')
       .text(name + '  ', 55, y, { continued: true });
    doc.fontSize(8).fillColor(C.dim).font('Helvetica')
       .text('[' + base + ' · CMD ' + cmd + ']');
    doc.fontSize(9).fillColor(C.light).font('Helvetica')
       .text('  ' + desc, 55, doc.y);
    doc.fontSize(8).fillColor(C.dim).font('Helvetica-Oblique')
       .text('  ☢ ' + lore, 55, doc.y);
    spacer(0.4);
  });
  spacer(0.3);
});

pageNum(3, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 4 — CRAFTING RECIPES (Machines + Tools)
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  Crafting Recipes — Machines & Tools', C.green);

// Nuclear Smelter
subHeader('Nuclear Smelter', C.yellow);
note('Crafted at a normal Crafting Table.');
spacer(0.3);
recipeBox('Nuclear Smelter', [
  ['Diamond', 'Obsidian', 'Diamond'],
  ['Obsidian', 'Furnace',  'Obsidian'],
  ['Diamond', 'Obsidian', 'Diamond'],
], ['Produces 1× Nuclear Smelter block', 'D = Diamond  ·  O = Obsidian  ·  F = Furnace'], C.yellow);

// Nuclear Forge
subHeader('Nuclear Forge', C.orange);
note('Requires Echo Shard (= Refined Plutonium Ingot) in your inventory.');
spacer(0.3);
recipeBox('Nuclear Forge', [
  ['Obsidian',   'Ancient Debris', 'Obsidian'],
  ['Echo Shard', 'Smithing Table', 'Echo Shard'],
  ['Obsidian',   'Ancient Debris', 'Obsidian'],
], ['Produces 1× Nuclear Forge block', 'Echo Shard = Refined Plutonium Ingot (proxy material)'], C.orange);

// Radiation Drill
subHeader('Radiation Drill', C.cyan);
note('The ONLY tool that can mine Plutonium Ore safely.');
spacer(0.3);
recipeBox('Radiation Drill', [
  ['R-Core', 'Diamond', 'R-Core'],
  ['Diamond', 'Diamond', 'Diamond'],
  ['',       'Stick',   ''],
], ['Produces 1× Radiation Drill', 'R-Core = Radioactive Core  ·  Uses Diamond Pickaxe as base'], C.cyan);

spacer(0.3);
subHeader('Plutonium Arrow', C.lime);
spacer(0.2);
recipeBox('Plutonium Arrow (×4)', [
  ['',           'Ref. Ingot', ''],
  ['',           'Arrow',      ''],
  ['',           'Feather',    ''],
], ['Ref. Ingot = Refined Plutonium Ingot (Echo Shard proxy)',
    'Applies 25 radiation + Poison + Glowing on hit'], C.lime);

pageNum(4, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 5 — CRAFTING RECIPES (Weapons & Armor)
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  Crafting Recipes — Weapons & Armor', C.green);
body('All recipes below use Echo Shard as a proxy for Refined Plutonium Ingot.', C.dim);
body('P = Echo Shard (Refined Plutonium Ingot)   S = Stick   W = Yellow Wool (Industrial Fabric)', C.dim);
spacer(0.6);

// Tools — 2 per row
const toolRecipes = [
  { name: 'Plutonium Sword', color: C.red, grid: [[' ','P',' '],[' ','P',' '],[' ','S',' ']], notes: ['10 DMG  ·  +10 Rad/hit  ·  2,500 durability'] },
  { name: 'Plutonium Pickaxe', color: C.cyan, grid: [['P','P','P'],[' ','S',' '],[' ','S',' ']], notes: ['10% chance to find extra Debris  ·  2,800 dur.'] },
  { name: 'Plutonium Axe', color: C.orange, grid: [['P','P',' '],['P','S',' '],[' ','S',' ']], notes: ['11 DMG  ·  15% shockwave (3-block rad)  ·  2,600 dur.'] },
  { name: 'Plutonium Shovel', color: C.lime, grid: [[' ','P',' '],[' ','S',' '],[' ','S',' ']], notes: ['15% soil conversion  ·  2,500 dur.'] },
  { name: 'Plutonium Hoe', color: C.green, grid: [['P','P',' '],[' ','S',' '],[' ','S',' ']], notes: ['Fast farming  ·  2,000 dur.'] },
];

// Lay tools out 2-per-row
let col = 0;
let rowStartY = doc.y;
toolRecipes.forEach((recipe, idx) => {
  const xOff = col === 0 ? 0 : 260;
  const cellS = 38;
  doc.fontSize(10).fillColor(recipe.color).font('Helvetica-Bold')
     .text(recipe.name, 52 + xOff, rowStartY, { width: 240 });
  const gy = rowStartY + 18;
  recipe.grid.forEach((row, ri) => {
    row.forEach((cell, ci) => {
      const cx = 52 + xOff + ci * (cellS + 3);
      const cy = gy + ri * (cellS + 3);
      doc.rect(cx, cy, cellS, cellS).lineWidth(0.8).stroke(C.border);
      if (cell && cell.trim()) {
        doc.fontSize(6).fillColor(C.light).font('Helvetica')
           .text(cell, cx + 1, cy + cellS/2 - 5, { width: cellS - 2, align: 'center' });
      }
    });
  });
  const gh = recipe.grid.length * (cellS + 3);
  recipe.notes.forEach((n, ni) => {
    doc.fontSize(8).fillColor(C.dim).font('Helvetica')
       .text('→ ' + n, 52 + xOff, gy + gh + 4 + ni * 12, { width: 240 });
  });
  
  if (col === 1 || idx === toolRecipes.length - 1) {
    rowStartY = gy + gh + 4 + 12 * 2 + 14;
    doc.y = rowStartY;
    col = 0;
  } else {
    col = 1;
  }
});

spacer(0.5);
subHeader('Plutonium Armor', C.purple);
body('P = Echo Shard (Refined Plutonium Ingot)  ·  All pieces enchanted: Protection III, Unbreaking III', C.dim);
spacer(0.3);

const pluArmor = [
  { name: 'Plutonium Helmet', grid: [['P','P','P'],['P',' ','P'],['','','']],
    notes: ['4 Armor  ·  2 Toughness  ·  450 dur.  ·  -15% Radiation'] },
  { name: 'Plutonium Chestplate', grid: [['P',' ','P'],['P','P','P'],['P','P','P']],
    notes: ['8 Armor  ·  2 Toughness  ·  640 dur.  ·  -15% Radiation'] },
  { name: 'Plutonium Leggings', grid: [['P','P','P'],['P',' ','P'],['P',' ','P']],
    notes: ['6 Armor  ·  2 Toughness  ·  590 dur.  ·  -15% Radiation'] },
  { name: 'Plutonium Boots', grid: [['','',''],['P',' ','P'],['P',' ','P']],
    notes: ['4 Armor  ·  2 Toughness  ·  480 dur.  ·  -15% Radiation  ·  Feather Fall II'] },
];

col = 0;
rowStartY = doc.y;
pluArmor.forEach((recipe, idx) => {
  const xOff = col === 0 ? 0 : 260;
  const cellS = 36;
  doc.fontSize(9).fillColor(C.purple).font('Helvetica-Bold')
     .text(recipe.name, 52 + xOff, rowStartY, { width: 240 });
  const gy = rowStartY + 16;
  recipe.grid.forEach((row, ri) => {
    row.forEach((cell, ci) => {
      const cx = 52 + xOff + ci * (cellS + 3);
      const cy = gy + ri * (cellS + 3);
      doc.rect(cx, cy, cellS, cellS).lineWidth(0.8).stroke('#7700ff');
      if (cell && cell.trim()) {
        doc.fontSize(6).fillColor(C.light).font('Helvetica')
           .text(cell, cx + 1, cy + cellS/2 - 5, { width: cellS - 2, align: 'center' });
      }
    });
  });
  const gh = recipe.grid.length * (cellS + 3);
  recipe.notes.forEach((n, ni) => {
    doc.fontSize(7.5).fillColor(C.dim).font('Helvetica')
       .text('→ ' + n, 52 + xOff, gy + gh + 3 + ni * 11, { width: 240 });
  });
  
  if (col === 1 || idx === pluArmor.length - 1) {
    rowStartY = gy + gh + 3 + 11 * 2 + 12;
    doc.y = rowStartY;
    col = 0;
  } else {
    col = 1;
  }
});

pageNum(5, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 6 — HAZMAT ARMOR + CURE RECIPES
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  Crafting Recipes — Hazmat Suit & Cures', C.green);

subHeader('Hazmat Suit', C.yellow);
body('W = Yellow Wool (Industrial Fabric proxy)  ·  Based on Leather armor', C.dim);
body('Full set bonus: 80% radiation reduction (best non-plutonium protection)', C.lime);
spacer(0.3);

const hazArmor = [
  { name: 'Hazmat Helmet', grid: [['W','W','W'],['W',' ','W'],['','','']],
    notes: ['2 Armor  ·  0.5 Tough  ·  363 dur.  ·  -20% Rad'] },
  { name: 'Hazmat Chestplate', grid: [['W',' ','W'],['W','W','W'],['W','W','W']],
    notes: ['5 Armor  ·  0.5 Tough  ·  529 dur.  ·  -30% Rad'] },
  { name: 'Hazmat Leggings', grid: [['W','W','W'],['W',' ','W'],['W',' ','W']],
    notes: ['4 Armor  ·  0.5 Tough  ·  496 dur.  ·  -20% Rad'] },
  { name: 'Hazmat Boots', grid: [['','',''],['W',' ','W'],['W',' ','W']],
    notes: ['2 Armor  ·  0.5 Tough  ·  430 dur.  ·  -10% Rad'] },
];

col = 0;
rowStartY = doc.y;
hazArmor.forEach((recipe, idx) => {
  const xOff = col === 0 ? 0 : 260;
  const cellS = 36;
  doc.fontSize(9).fillColor(C.yellow).font('Helvetica-Bold')
     .text(recipe.name, 52 + xOff, rowStartY, { width: 240 });
  const gy = rowStartY + 16;
  recipe.grid.forEach((row, ri) => {
    row.forEach((cell, ci) => {
      const cx = 52 + xOff + ci * (cellS + 3);
      const cy = gy + ri * (cellS + 3);
      doc.rect(cx, cy, cellS, cellS).lineWidth(0.8).stroke(C.yellow);
      if (cell && cell.trim()) {
        doc.fontSize(6).fillColor(C.light).font('Helvetica')
           .text(cell, cx + 1, cy + cellS/2 - 5, { width: cellS - 2, align: 'center' });
      }
    });
  });
  const gh = recipe.grid.length * (cellS + 3);
  recipe.notes.forEach((n, ni) => {
    doc.fontSize(7.5).fillColor(C.dim).font('Helvetica')
       .text('→ ' + n, 52 + xOff, gy + gh + 3 + ni * 11, { width: 240 });
  });
  if (col === 1 || idx === hazArmor.length - 1) {
    rowStartY = gy + gh + 3 + 12 * 2 + 12;
    doc.y = rowStartY;
    col = 0;
  } else {
    col = 1;
  }
});

spacer(0.5);
subHeader('Radiation Cures', C.lime);
spacer(0.2);

// Antidote — shapeless
doc.fontSize(10).fillColor(C.lime).font('Helvetica-Bold').text('Radiation Antidote  (Shapeless Recipe)');
spacer(0.2);
const antX = 55;
const antY = doc.y;
const antItems = ['Healing Petal', 'Healing Petal', 'Honey Bottle'];
antItems.forEach((item, i) => {
  doc.rect(antX + i * 70, antY, 64, 36).lineWidth(0.8).stroke(C.lime);
  doc.fontSize(7).fillColor(C.light).font('Helvetica')
     .text(item, antX + i * 70 + 2, antY + 10, { width: 60, align: 'center' });
});
doc.fontSize(10).fillColor(C.light).font('Helvetica').text(' → ', antX + 220, antY + 10, { continued: true });
doc.fontSize(10).fillColor(C.lime).font('Helvetica-Bold').text('Radiation Antidote ×1');
doc.y = antY + 45;
doc.fontSize(9).fillColor(C.dim).font('Helvetica')
   .text('  Effect: Clears ALL radiation, infection & debuffs. No immunity granted.');
spacer(0.8);

// Serum — shaped
doc.fontSize(10).fillColor(C.purple).font('Helvetica-Bold').text('Radiation Serum  (Shaped Recipe)');
spacer(0.3);
recipeBox('Radiation Serum', [
  ['Healing Petal', 'R-Core', 'Healing Petal'],
  ['Gold Nugget',   'Gold Apple', 'Gold Nugget'],
  ['Healing Petal', 'Glass Bottle', 'Healing Petal'],
], ['R-Core = Radioactive Core', 'Effect: Clears ALL radiation + grants 10 MINUTES of immunity'], C.purple);

pageNum(6, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 7 — RADIATION SYSTEM
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  Radiation System', C.red);

subHeader('Radiation Stages', C.red);
const stages = [
  ['Stage 0 — Healthy', '0–99 points', 'No effects. Normal gameplay.', C.green],
  ['Stage 1 — Minor Exposure', '100–249 points', 'Weakness I + Nausea', C.yellow],
  ['Stage 2 — Moderate Exposure', '250–499 points', 'Weakness I + Slowness I + Nausea  ·  5% spread chance to nearby players', C.orange],
  ['Stage 3 — Severe Exposure', '500–749 points', 'Weakness II + Slowness II + Hunger I  ·  0.5 HP damage/cycle  ·  15% spread chance', '#ff6600'],
  ['Stage 4 — Critical Poisoning', '750–1000 points', 'Weakness III + Slowness III + Hunger II  ·  1.0 HP damage/cycle  ·  25% spread chance', C.red],
];
stages.forEach(([stage, range, effects, color]) => {
  const y = doc.y;
  doc.rect(50, y, doc.page.width - 100, 30).fill('#181818');
  doc.fontSize(10).fillColor(color).font('Helvetica-Bold')
     .text(stage, 55, y + 4, { width: 200, lineBreak: false });
  doc.fontSize(9).fillColor(C.yellow).font('Helvetica')
     .text(range, 260, y + 4, { width: 100, lineBreak: false });
  doc.fontSize(9).fillColor(C.light).font('Helvetica')
     .text(effects, 370, y + 4, { width: 185 });
  doc.moveDown(0.15);
});

spacer(0.6);
subHeader('Radiation Sources', C.orange);
statTable([
  ['Plutonium Ore (nearby)', '+5 per check (5s interval)'],
  ['Mining without Radiation Drill', '+25 burst (5s cooldown)'],
  ['Irradiated Zombie hit', '+10 per hit'],
  ['Alpha Zombie (Lv.4) hit', '+50 per hit'],
  ['Nuclear Smelter (nearby)', '+8 per 5 seconds (3-block radius)'],
  ['Radioactive Debris', '+0.5/s in 5-block radius'],
  ['Radiation Cloud (zombie death)', '+5 per second for 10 seconds'],
  ['Titan Boss Aura', '+3 per second in 10-block radius'],
  ['Titan Boss Slam', '+15 per hit'],
  ['Titan Radiation Wave', '+40 (20-block radius)'],
  ['Titan Reactor Overload', '+70 (18-block radius)'],
  ['Titan Energy Beam (targeted)', '+60 (direct hit)'],
  ['Titan Nuclear Catastrophe', '+80 (30-block radius)'],
  ['Healing Petal (direct consume)', '-25 points'],
  ['Radiation Antidote', 'Clears ALL radiation'],
  ['Radiation Serum', 'Clears ALL + 10 min immunity'],
]);

spacer(0.3);
subHeader('Armor Protection', C.cyan);
body('Each armor piece reduces incoming radiation by a flat percentage.', C.light);
spacer(0.2);
statTable([
  ['Any armor piece', '-15% per piece (max 60% for full set)'],
  ['Hazmat Suit — per piece', '-10% to -30% per piece'],
  ['Hazmat Suit — full set', '-80% total radiation reduction'],
  ['Plutonium Armor — full set', 'Full environmental radiation IMMUNITY'],
  ['Plutonium Boots special', 'No radiation from ground contact'],
]);

spacer(0.3);
subHeader('Contagion (Radiation Spread)', C.red);
statTable([
  ['Proximity spread (Stage 2)', '5% chance / 5 seconds within 3 blocks  ·  +25 rad'],
  ['Proximity spread (Stage 3)', '15% chance / 5 seconds  ·  +25 rad'],
  ['Proximity spread (Stage 4)', '25% chance / 5 seconds  ·  +25 rad'],
  ['Physical contact (melee hit)', '35% chance  ·  +50 rad to victim'],
  ['Vehicle sharing (boat/minecart)', '20% chance / 15 seconds  ·  +25 rad'],
]);

spacer(0.3);
subHeader('Natural Decay', C.green);
statTable([
  ['Decay interval', 'Every 60 seconds'],
  ['Grace period required', 'No new radiation for 10 minutes'],
  ['Amount removed', '-2 radiation per decay tick'],
]);

pageNum(7, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 8 — MACHINES: NUCLEAR SMELTER & NUCLEAR FORGE
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  Machines — Nuclear Smelter', C.yellow);

subHeader('How It Works', C.yellow);
body('The Nuclear Smelter converts Raw Plutonium Fragments into Refined Plutonium Ingots.\nRight-click the placed Smelter block to open its GUI interface.', C.light);
spacer(0.5);

subHeader('GUI Slots', C.cyan);
statTable([
  ['Input Slot', 'Place Raw Plutonium Fragments here'],
  ['Fuel Slot', 'Insert fuel items (Coal, Charcoal, Blaze Rod, Lava Bucket)'],
  ['Output Slot', 'Collect Refined Plutonium Ingots'],
]);

subHeader('Fuel Values', C.orange);
statTable([
  ['Coal', '100 fuel units (~20 seconds of processing)'],
  ['Charcoal', '80 fuel units'],
  ['Coal Block', '900 fuel units (~3 minutes)'],
  ['Blaze Rod', '120 fuel units'],
  ['Lava Bucket', '1,000 fuel units (~3.3 minutes)  ← BEST'],
]);

subHeader('Temperature System', C.red);
statTable([
  ['Min processing temp', '500°C — must reach this before processing begins'],
  ['Max safe temp', '1,500°C — overheats above this'],
  ['Heating rate', '+5°C per tick cycle while fueled'],
  ['Active rate', '+2.5°C per tick while processing'],
  ['Cooling rate', '-1°C per tick while idle'],
  ['Overheat cooldown', '10 seconds overheated → then cools down'],
  ['Restart threshold', 'Machine restarts at 80% of minimum temp (avoids cold start)'],
]);

subHeader('Processing Recipe', C.green);
statTable([
  ['Input', '1× Raw Plutonium Fragment'],
  ['Output', '1× Refined Plutonium Ingot'],
  ['Time', '15 seconds (300 ticks at 20 TPS)'],
  ['Fuel cost', '1 fuel unit per tick cycle'],
]);

subHeader('Radiation Hazard', C.red);
body('The Nuclear Smelter emits passive radiation while active.', C.light);
statTable([
  ['Passive radius', '3 blocks around the machine'],
  ['Radiation per check', '+8 points'],
  ['Check interval', 'Every 5 seconds (100 ticks)'],
]);
note('Keep the Smelter in a dedicated room away from your base!');

spacer(1.5);
header('☢  Machines — Nuclear Forge', C.orange);

subHeader('How It Works', C.orange);
body('The Nuclear Forge upgrades Plutonium tools, weapons, and Hazmat armor to MK-I through MK-IV.\nInsert Radioactive Cores as fuel, then right-click to open the upgrade GUI.', C.light);
spacer(0.4);

subHeader('Upgrade Tiers', C.cyan);
const tiers = [
  ['MK-1', '2 Ingots + 1 Core', '500', '100%', '+5% damage, speed, armor, durability', '0%'],
  ['MK-2', '4 Ingots + 2 Cores', '1,200', '90%', '+10% all stats', '0%'],
  ['MK-3', '8 Ingots + 4 Cores + 1 Heart', '2,500', '75%', '+20% all stats', '0%'],
  ['MK-4', '16 Ingots + 8 Cores + 2 Hearts', '5,000', '50%', '+35% all stats', '5% → MK-3'],
];

const tw2 = doc.page.width - 100;
doc.rect(50, doc.y, tw2, 16).fill('#222222');
['Tier','Materials','Energy','Success','Bonus','Fail Risk'].forEach((h, i) => {
  const xs = [54, 95, 235, 290, 345, 455];
  doc.fontSize(8).fillColor(C.green).font('Helvetica-Bold').text(h, xs[i], doc.y - 12, { width: 80, lineBreak: false });
});
doc.moveDown(0.2);

tiers.forEach(([tier, mats, energy, success, bonus, fail], i) => {
  const y = doc.y;
  if (i % 2 === 0) doc.rect(50, y, tw2, 22).fill('#181818');
  const xs = [54, 95, 235, 290, 345, 455];
  const vals = [tier, mats, energy, success, bonus, fail];
  const colors = [C.yellow, C.light, C.orange, success === '100%' ? C.green : C.orange, C.cyan, C.red];
  vals.forEach((v, ci) => {
    doc.fontSize(8).fillColor(colors[ci]).font('Helvetica').text(v, xs[ci], y + 4, { width: ci < 5 ? 135 : 80, lineBreak: false });
  });
  doc.moveDown(0.35);
});

spacer(0.4);
subHeader('MK-IV Special — Radiation Aura', C.purple);
statTable([
  ['Aura radius', '3 blocks around player'],
  ['Effect on hostile mobs', '+25 radiation per aura tick'],
  ['Effect on enemy players (PvP)', '+15 radiation per aura tick'],
  ['Aura tick rate', 'Every 2 seconds (40 ticks)'],
  ['Forge Overload burst', '+150 radiation to nearby players in 8-block radius'],
]);

pageNum(8, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 9 — ORE, FARMING, WORLD
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  World: Ore, Mining & Farming', C.green);

subHeader('Plutonium Ore', C.yellow);
statTable([
  ['Location', 'Deep underground — Y: -64 to -58 (deepslate layer)'],
  ['Spawn chance', '60% of newly generated chunks get a vein'],
  ['Vein size', '1–2 blocks per vein'],
  ['Host blocks', 'Deepslate, Stone, Cobbled Deepslate, Tuff'],
]);
spacer(0.2);
subHeader('Mining Rules', C.red);
statTable([
  ['Without Radiation Drill', '+25 radiation burst (5s cooldown) — ore does NOT drop'],
  ['With Radiation Drill', 'Safe extraction — ore drops normally, no burst'],
  ['Base drop', '1 Raw Plutonium Fragment per ore'],
  ['Fortune I', '1–2 fragments'],
  ['Fortune II', '1–3 fragments'],
  ['Fortune III', '1–4 fragments'],
  ['XP per mine', '3–7 XP orbs'],
]);
spacer(0.2);
subHeader('Ore Radiation Zones', C.red);
statTable([
  ['Within 1 block of ore', '+5 radiation / 5 seconds'],
  ['Within 2 blocks', '+2 radiation / 5 seconds'],
  ['Within 3 blocks', '+1 radiation / 5 seconds'],
  ['Hazmat suit nearby', '80% of radiation blocked (×0.20 multiplier)'],
  ['Plutonium armor nearby', '100% blocked — complete immunity'],
]);

spacer(0.4);
subHeader('Plutonium Block', C.cyan);
statTable([
  ['Recipe', '9× Refined Plutonium Ingots in a 3×3 grid (compact storage)'],
  ['Radiation tier 1', '+10 per check within 1 block'],
  ['Radiation tier 2', '+5 per check within 2 blocks'],
  ['Radiation tier 3', '+2 per check within 3 blocks'],
]);

spacer(0.4);
subHeader('Radioactive Debris', C.orange);
statTable([
  ['Location', 'Y: -64 to -32 underground (rare)'],
  ['Rarity', '0.1% chance per chunk'],
  ['Drop on mine', '15% chance: Radioactive Core'],
  ['Passive radiation', '+0.5 rad/s within 5 blocks'],
]);

spacer(0.6);
header('☢  Radioactive Farming', C.lime);

subHeader('How to Farm', C.lime);
body('1. Obtain Mutated Seeds (dropped by Irradiated Zombies)\n2. Find or convert farmland into Radioactive Farmland (use Plutonium Shovel — 15% chance)\n3. Plant Mutated Seeds on Radioactive Farmland only\n4. Let the plant grow through 5 stages (0 = seed → 4 = mature blossom)\n5. Harvest to get Healing Petals (1–3) and bonus Mutated Seeds (0–2)', C.light);
spacer(0.4);

subHeader('Farming Stats', C.yellow);
statTable([
  ['Growth bonus (Radioactive Farmland)', '+50% extra growth chance per tick'],
  ['Petals per harvest', '1–3 Healing Petals'],
  ['Seeds per harvest', '0–2 Mutated Seeds (replanting supply)'],
  ['XP per harvest', '2–5 XP'],
  ['Toxic Bloom chance', '1% — fully-grown crop mutates instead of dropping normally'],
]);

spacer(0.3);
subHeader('Toxic Bloom Hazard', C.red);
statTable([
  ['Radiation range', '4-block radius around the bloom'],
  ['Radiation amount', '+8 per 2 seconds (40 ticks)'],
  ['Tip', 'Break it immediately with any tool to stop it radiating'],
]);

spacer(0.3);
subHeader('Radioactive Farmland Radiation', C.orange);
statTable([
  ['Passive exposure', '+1 radiation per 2 seconds to nearby players'],
  ['Hazmat boots special', 'Completely negates ground-contact radiation'],
]);

pageNum(9, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 10 — IRRADIATED ZOMBIES
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  Irradiated Zombies', C.green);

subHeader('Spawning', C.yellow);
body('60% of naturally spawning zombies become Irradiated Zombies. Baby zombies are excluded.', C.light);
spacer(0.4);

subHeader('Zombie Levels & Stats', C.red);
const zombLevels = [
  ['Level 1 (80%)', '35 HP', '5 DMG', '×1.15 speed', '0.15 KB resist', '+10 rad/hit', '10 XP'],
  ['Level 2 (15%)', '45 HP', '7 DMG', '×1.20 speed', '0.20 KB resist', '+20 rad/hit', '20 XP'],
  ['Level 3 (4%)', '60 HP', '9 DMG', '×1.25 speed', '0.30 KB resist', '+35 rad/hit', '40 XP'],
  ['Level 4 Alpha (1%)', '80 HP', '12 DMG', '×1.30 speed', '0.50 KB resist', '+50 rad/hit', '100 XP'],
];

const headers = ['Level', 'HP', 'DMG', 'Speed', 'KB Res', 'Radiation', 'XP'];
const xs3 = [54, 145, 195, 235, 290, 345, 435];
doc.rect(50, doc.y, doc.page.width - 100, 16).fill('#222222');
headers.forEach((h, i) => {
  doc.fontSize(8).fillColor(C.green).font('Helvetica-Bold').text(h, xs3[i], doc.y - 12, { width: 80, lineBreak: false });
});
doc.moveDown(0.2);

zombLevels.forEach((row, i) => {
  const y = doc.y;
  if (i % 2 === 0) doc.rect(50, y, doc.page.width - 100, 18).fill('#181818');
  const color = [C.light, C.yellow, C.orange, C.red][i];
  row.forEach((val, ci) => {
    doc.fontSize(8.5).fillColor(ci === 0 ? color : C.light).font(ci === 0 ? 'Helvetica-Bold' : 'Helvetica')
       .text(val, xs3[ci], y + 3, { width: 80, lineBreak: false });
  });
  doc.moveDown(0.3);
});

note('Level 4 Alpha: has green Bossbar, glows, visible from afar. Extremely dangerous.');
spacer(0.5);

subHeader('Loot Drops', C.yellow);
const lootTable = [
  ['Rotten Flesh', '0–3 always', '0–3 always', '0–3 always', '0–3 always'],
  ['Radioactive Core', '15%', '25%', '40%', '100% (always!)'],
  ['Mutated Seed', '5%', '10%', '20%', '50%'],
  ['Irradiated Heart', '1%', '3%', '7%', '25%'],
];
const lootHeaders = ['Drop', 'Lv.1', 'Lv.2', 'Lv.3', 'Lv.4 Alpha'];
const lx = [54, 230, 300, 370, 440];
doc.rect(50, doc.y, doc.page.width - 100, 16).fill('#222222');
lootHeaders.forEach((h, i) => {
  doc.fontSize(8).fillColor(C.green).font('Helvetica-Bold').text(h, lx[i], doc.y - 12, { width: 80, lineBreak: false });
});
doc.moveDown(0.2);
lootTable.forEach((row, i) => {
  const y = doc.y;
  if (i % 2 === 0) doc.rect(50, y, doc.page.width - 100, 16).fill('#181818');
  row.forEach((val, ci) => {
    const c = ci === 0 ? C.light : (val.includes('100') ? C.green : val.includes('%') ? C.yellow : C.dim);
    doc.fontSize(8.5).fillColor(c).font('Helvetica').text(val, lx[ci], y + 3, { width: 80, lineBreak: false });
  });
  doc.moveDown(0.28);
});

spacer(0.5);
subHeader('Radiation Cloud (On Death)', C.red);
statTable([
  ['Spawn chance', '20% per zombie death'],
  ['Cloud radius', '3 blocks'],
  ['Duration', '10 seconds'],
  ['Radiation', '+5 per second to players inside'],
]);

spacer(0.5);
subHeader('Radiation Surge Night Event', C.purple);
statTable([
  ['Chance per night', '5%'],
  ['Effect', 'ALL radiation damage doubled, ALL loot drops doubled'],
  ['Duration', 'One full in-game night'],
  ['Warning', 'Bossbar shown to all players: ☢ RADIATION SURGE ACTIVE ☢'],
]);

spacer(0.5);
subHeader('Combat System vs. Zombies', C.cyan);
statTable([
  ['Plutonium Sword vs zombie', '+10 radiation per hit on zombie'],
  ['Axe shockwave (15% chance)', '3-block radius  ·  +15 radiation to all in area'],
  ['Plutonium Arrow hit', '+25 radiation  ·  Poison effect  ·  Glowing'],
  ['MK-IV Aura', 'Passive +25 radiation per 2s to nearby mobs'],
]);

pageNum(10, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 11 — PLUTONIUM TITAN BOSS
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  Plutonium Titan Boss', C.purple);

subHeader('Summoning Ritual', C.red);
body('Build the Summoning Altar with Crying Obsidian at the center and 4 Obsidian blocks at\neach corner (2 blocks away diagonally). Stand at the center and:\n  1. Hold 4× Titan Cores in your inventory\n  2. Right-click the center Crying Obsidian with an Irradiated Heart\n  3. All 4 Titan Cores are consumed immediately\n  4. A 15-second summoning animation begins\n  5. The Plutonium Titan spawns at the altar center', C.light);
spacer(0.3);
note('60-minute cooldown between summons. Only one Titan can be alive at a time.');

spacer(0.5);
subHeader('Health Scaling (by players nearby)', C.yellow);
statTable([
  ['1–3 players nearby', '5,000 HP'],
  ['4–6 players nearby', '6,500 HP'],
  ['7–10 players nearby', '8,500 HP'],
  ['10+ players nearby', '10,000 HP'],
]);

spacer(0.4);
subHeader('Phase System', C.red);
const phases = [
  ['Phase 1 (100% – 75% HP)', '×1.0 speed, ×1.0 damage', 'Titan Slam  ·  Radiation Aura', C.green],
  ['Phase 2 (75% – 50% HP)', '×1.1 speed, ×1.2 damage', 'Radiation Wave  ·  Mutant Summoning  ·  Aura', C.yellow],
  ['Phase 3 (50% – 25% HP)', '×1.25 speed, ×1.5 damage', 'Reactor Overload  ·  Energy Beam  ·  All above', C.orange],
  ['Phase 4 (25% – 0% HP)', '×1.5 speed, ×2.0 damage', 'Nuclear Catastrophe  ·  Final Frenzy  ·  All above', C.red],
];
phases.forEach(([phase, mults, abilities, color]) => {
  const y = doc.y;
  doc.rect(50, y, doc.page.width - 100, 30).fill('#181818');
  doc.fontSize(9).fillColor(color).font('Helvetica-Bold').text(phase, 55, y + 4, { width: 175, lineBreak: false });
  doc.fontSize(8).fillColor(C.yellow).font('Helvetica').text(mults, 237, y + 4, { width: 130, lineBreak: false });
  doc.fontSize(8).fillColor(C.light).font('Helvetica').text(abilities, 375, y + 4, { width: 175 });
  doc.moveDown(0.3);
});

spacer(0.5);
subHeader('All Titan Abilities', C.red);
const abilities = [
  ['Radiation Aura (Passive)', 'ALL Phases', '+3 rad/sec in 10-block radius at all times'],
  ['Titan Slam', 'Phase 1+', '5-block radius  ·  12 base DMG  ·  +15 radiation  ·  knockback  ·  10s CD'],
  ['Radiation Wave', 'Phase 2+', '20-block expanding ring  ·  +40 radiation  ·  15s CD'],
  ['Mutant Summoning', 'Phase 2+', 'Summons 3 regular + 1 Alpha zombie  ·  25s CD'],
  ['Reactor Overload', 'Phase 3+', '5-second charge  ·  18-block radius  ·  +70 radiation  ·  40s CD'],
  ['Energy Beam', 'Phase 3+', 'Targets random player  ·  25 DMG + 60 radiation  ·  20s CD'],
  ['Nuclear Catastrophe', 'Phase 4 only', '10-second charge  ·  30-block radius  ·  sets HP to 1  ·  +80 rad  ·  90s CD'],
  ['Final Frenzy', 'Phase 4 only', 'One-time trigger  ·  +50% attack speed permanently for rest of fight'],
];
const ax = [54, 195, 300];
doc.rect(50, doc.y, doc.page.width - 100, 14).fill('#222222');
['Ability', 'Phase', 'Details'].forEach((h, i) => {
  doc.fontSize(8).fillColor(C.green).font('Helvetica-Bold').text(h, ax[i], doc.y - 10, { width: 120, lineBreak: false });
});
doc.moveDown(0.15);
abilities.forEach(([name, phase, desc], i) => {
  const y = doc.y;
  if (i % 2 === 0) doc.rect(50, y, doc.page.width - 100, 16).fill('#181818');
  doc.fontSize(8.5).fillColor(C.red).font('Helvetica-Bold').text(name, ax[0], y + 3, { width: 138, lineBreak: false });
  doc.fontSize(8).fillColor(C.yellow).font('Helvetica').text(phase, ax[1], y + 3, { width: 100, lineBreak: false });
  doc.fontSize(8).fillColor(C.light).font('Helvetica').text(desc, ax[2], y + 3, { width: 245 });
  doc.moveDown(0.28);
});

spacer(0.3);
subHeader('Arena Hazard', C.orange);
statTable([
  ['Hazard radius', '15 blocks from Titan spawn point'],
  ['Radiation inside arena', '+2 radiation per check'],
  ['Visual', 'Particle effects mark the arena boundary'],
]);

subHeader('Titan Rewards', C.yellow);
statTable([
  ['XP on kill', '5,000 XP (split by contribution)'],
  ['Titan Fragment (guaranteed)', '1–8+ depending on % contribution'],
  ['Refined Plutonium Ingots', 'Scales with contribution (≥5%)'],
  ['Reactor Heart (rare)', '10% chance per qualifying player'],
  ['Ancient Reactor Blueprint', '5% chance per qualifying player'],
  ['Mutated Crystal', '8% chance per qualifying player'],
  ['Loot Chest', 'Spawns at death location with bonus Cores, Fragments & Ingots'],
]);

pageNum(11, 12);

// ════════════════════════════════════════════════════════════════
//  PAGE 12 — COMBAT, PROGRESSION & COMMANDS
// ════════════════════════════════════════════════════════════════
newPage();
header('☢  Combat System & Progression', C.cyan);

subHeader('Weapon Radiation Effects', C.red);
statTable([
  ['Plutonium Sword (base)', '+10 radiation per hit'],
  ['Sword critical hit (falling)', '+5 bonus radiation'],
  ['Sword vs high-stage player', '+5 bonus when victim is Stage 3+'],
  ['Plutonium Axe (base)', '+11 DMG'],
  ['Axe shockwave (15% chance)', '3-block radius radiation burst  ·  +15 rad'],
  ['Plutonium Arrow (hit)', '+25 radiation  ·  Poison (4s)  ·  Glowing (5s)'],
  ['Arrow critical (full charge)', '+20 bonus radiation'],
  ['Arrow headshot bonus', '+10 radiation'],
]);

spacer(0.3);
subHeader('Combo System', C.yellow);
statTable([
  ['Max combo stack', '8 hits'],
  ['Combo reset time', '6 seconds since last hit'],
  ['Bonus per stack', '+5 extra radiation per hit above 1'],
  ['Maximum bonus', '+35 radiation from combo (capped)'],
]);

spacer(0.3);
subHeader('Radiation Surge (PvP)', C.red);
statTable([
  ['Trigger condition', 'Both fighters must be Stage 2+ radiation'],
  ['Trigger chance', '5% per hit'],
  ['Effect on both fighters', '+30 radiation each'],
  ['Effect on bystanders', '+15 radiation (8-block radius)'],
  ['Cooldown', '15 seconds per pair'],
]);

spacer(0.3);
subHeader('Weapon Mastery System', C.purple);
statTable([
  ['Novice (0 XP)', 'Starting level'],
  ['Experienced (100 XP)', 'Sword: +1 XP/hit, +10/kill'],
  ['Veteran (500 XP)', 'Axe: +1 XP/hit, +10/kill'],
  ['Elite (1,500 XP)', 'Bow: +2 XP/hit, +15/kill'],
  ['Master (4,000 XP)', '+10% radiation bonus on all weapon hits'],
]);

spacer(0.3);
subHeader('Equipment Repair', C.lime);
statTable([
  ['Plutonium tools & armor', 'Repair with Refined Plutonium Ingot (+300 durability per ingot)'],
  ['Hazmat Suit', 'Repair with Industrial Fabric (+200 durability per piece)'],
  ['Method', 'Use vanilla Anvil with the repair material'],
]);

spacer(0.4);
header('☢  Admin Commands', C.dim);

const commands = [
  ['/nc radiation set <player> <amount>', 'Set a player\'s radiation level directly'],
  ['/nc radiation add <player> <amount>', 'Add radiation to a player'],
  ['/nc radiation clear <player>', 'Clear all radiation from a player'],
  ['/nc radiation status <player>', 'Check a player\'s radiation stage and level'],
  ['/nc ore give fragment [amount]', 'Give Raw Plutonium Fragments'],
  ['/nc ore give drill', 'Give a Radiation Drill'],
  ['/nc equipment give <type>', 'Give plutonium/hazmat gear (sword, axe, helmet, etc.)'],
  ['/nc farming give <seed|petal|antidote|serum>', 'Give farming/cure items'],
  ['/nc smelter give', 'Give a Nuclear Smelter block'],
  ['/nc forge give', 'Give a Nuclear Forge block'],
  ['/nc titan spawn', 'Force-spawn the Plutonium Titan at your location'],
  ['/nc titan kill', 'Force-kill the active Titan'],
  ['/nc titan phase <1-4>', 'Force the Titan to a specific phase'],
  ['/nc reload', 'Reload all plugin configs'],
];
commands.forEach(([cmd, desc]) => {
  const y = doc.y;
  doc.fontSize(8).fillColor(C.green).font('Helvetica-Bold')
     .text(cmd, 55, y, { width: 260, lineBreak: false });
  doc.fontSize(8).fillColor(C.light).font('Helvetica')
     .text(desc, 320, y, { width: 230 });
  doc.moveDown(0.3);
});

// Footer
doc.y = doc.page.height - 70;
doc.rect(50, doc.y, doc.page.width - 100, 1).fill(C.green);
doc.moveDown(0.3);
doc.fontSize(9).fillColor(C.dim).font('Helvetica')
   .text('NuclearCraft: Plutonium Age  ·  Plugin v1.0  ·  PaperMC 1.21+  ·  Phases 1–10 Complete', { align: 'center' });
doc.fontSize(8).fillColor(C.dim).font('Helvetica')
   .text('All values are configurable via the YAML config files in the plugin folder.', { align: 'center' });

pageNum(12, 12);

// ── Finalise ──────────────────────────────────────────────────
doc.end();
out.on('finish', () => console.log('PDF generated: NuclearCraft-Guide.pdf'));
out.on('error', (e) => console.error('ERROR:', e));
