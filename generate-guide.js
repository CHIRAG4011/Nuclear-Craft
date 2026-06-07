const PDFDocument = require('pdfkit');
const fs = require('fs');

const doc = new PDFDocument({
  size: 'A4',
  margins: { top: 50, bottom: 50, left: 55, right: 55 },
  autoFirstPage: false,
  bufferPages: true,
});

const out = fs.createWriteStream('NuclearCraft-Guide.pdf');
doc.pipe(out);

const C = {
  green:    '#39ff14',
  darkGreen:'#1a7a00',
  lime:     '#a8e63d',
  yellow:   '#f5c518',
  orange:   '#e07b20',
  red:      '#e03030',
  white:    '#ffffff',
  light:    '#e8e8e8',
  mid:      '#aaaaaa',
  dark:     '#333333',
  bg:       '#111a0d',
  bgCard:   '#1a2810',
  bgHeader: '#0d1a08',
  accent:   '#2ecc71',
};

function newPage() {
  doc.addPage();
  doc.rect(0, 0, doc.page.width, doc.page.height).fill(C.bg);
}

function heading1(text) {
  doc.fontSize(24).fillColor(C.green).font('Helvetica-Bold').text(text, { align: 'center' });
  const y = doc.y;
  doc.moveTo(55, y + 4).lineTo(doc.page.width - 55, y + 4)
     .strokeColor(C.darkGreen).lineWidth(1.5).stroke();
  doc.moveDown(0.6);
}

function heading2(text) {
  doc.moveDown(0.4);
  doc.fontSize(14).fillColor(C.lime).font('Helvetica-Bold').text(text);
  doc.moveDown(0.2);
}

function body(text) {
  doc.fontSize(9.5).fillColor(C.light).font('Helvetica').text(text, { lineGap: 2 });
  doc.moveDown(0.3);
}

function code(text) {
  const x = 55, w = doc.page.width - 110;
  const lines = text.split('\n');
  const h = lines.length * 13 + 14;
  doc.rect(x, doc.y, w, h).fill(C.bgHeader);
  const savedY = doc.y;
  doc.fontSize(8.5).fillColor(C.green).font('Courier')
     .text(text, x + 8, savedY + 7, { width: w - 16, lineGap: 2 });
  doc.y = savedY + h + 8;
}

function cmdEntry(cmd, desc, perm) {
  const y = doc.y;
  const h = perm ? 44 : 32;
  doc.rect(55, y, doc.page.width - 110, h).fill(C.bgCard);
  doc.rect(55, y, 3, h).fill(C.green);
  doc.fontSize(9).fillColor(C.green).font('Courier')
     .text(cmd, 64, y + 6, { width: doc.page.width - 128 });
  doc.fontSize(8.5).fillColor(C.light).font('Helvetica')
     .text(desc, 64, y + 19, { width: doc.page.width - 128 });
  if (perm) {
    doc.fontSize(7.5).fillColor(C.mid).font('Helvetica')
       .text('Permission: ' + perm, 64, y + 31, { width: doc.page.width - 128 });
  }
  doc.y = y + h + 4;
}

function infoBox(text, color) {
  const x = 55, w = doc.page.width - 110;
  const textH = doc.heightOfString(text, { width: w - 24, fontSize: 9 });
  const h = textH + 18;
  const savedY = doc.y;
  doc.rect(x, savedY, 4, h).fill(color || C.green);
  doc.rect(x + 4, savedY, w - 4, h).fill(C.bgCard);
  doc.fontSize(9).fillColor(C.light).font('Helvetica')
     .text(text, x + 14, savedY + 9, { width: w - 24, lineGap: 2 });
  doc.y = savedY + h + 8;
}

function divider() {
  doc.moveDown(0.3);
  doc.moveTo(55, doc.y).lineTo(doc.page.width - 55, doc.y)
     .strokeColor(C.darkGreen).lineWidth(0.5).stroke();
  doc.moveDown(0.4);
}

function checkSpace(needed) {
  if (doc.y + needed > doc.page.height - 60) newPage();
}

function tableRow(cells, widths, colors, isHeader) {
  const y = doc.y;
  const h = 20;
  let x = 55;
  widths.forEach((w, i) => {
    doc.rect(x, y, w, h).fill(colors[i] || C.bgCard);
    const txt = cells[i] || '';
    if (isHeader) {
      doc.fontSize(8.5).fillColor(C.white).font('Helvetica-Bold').text(txt, x + 6, y + 5, { width: w - 12 });
    } else {
      doc.fontSize(8).fillColor(C.light).font(i === 0 ? 'Courier' : 'Helvetica').text(txt, x + 6, y + 5, { width: w - 12 });
    }
    x += w;
  });
  doc.y = y + h + 1;
}

// ════════════════════════════════════════════════════════════════════
// COVER PAGE
// ════════════════════════════════════════════════════════════════════
newPage();

doc.moveDown(1.5);
doc.fontSize(42).fillColor(C.green).font('Helvetica-Bold').text('☢ NuclearCraft', { align: 'center' });
doc.fontSize(24).fillColor(C.lime).font('Helvetica-Bold').text('Plutonium Age', { align: 'center' });
doc.moveDown(0.4);
doc.fontSize(13).fillColor(C.yellow).font('Helvetica-Bold')
   .text('v1.0.0 Production — Complete Setup & Command Guide', { align: 'center' });
doc.moveDown(0.2);
doc.fontSize(9).fillColor(C.mid).font('Helvetica')
   .text('Paper / Purpur 1.21+  ·  Java 21+  ·  SQLite / MySQL', { align: 'center' });

doc.moveDown(1.2);
doc.moveTo(80, doc.y).lineTo(doc.page.width - 80, doc.y).strokeColor(C.darkGreen).lineWidth(2).stroke();
doc.moveDown(1);

const cardY = doc.y;
doc.rect(55, cardY, doc.page.width - 110, 150).fill(C.bgCard);
doc.rect(55, cardY, 4, 150).fill(C.green);

const bullets = [
  '14 development phases — fully complete and production ready',
  'Radiation system with 5 escalating stages and contagion spread',
  'Nuclear Smelter, Forge & Titan Reactor Forge machines',
  'Plutonium Titan multi-phase boss encounter (up to 50+ players)',
  'Irradiated zombies, mutated crops, custom ores & combat mastery',
  'Full admin suite: health checks, diagnostics, memory monitoring, data repair',
  'SQLite (zero config) or MySQL for high player count servers',
  'Resource pack: custom 3D models + 30 custom sound events',
];
bullets.forEach((b, i) => {
  doc.fontSize(9).fillColor(C.light).font('Helvetica')
     .text('  ✔  ' + b, 68, cardY + 12 + i * 16, { width: doc.page.width - 140 });
});
doc.y = cardY + 162;

doc.moveDown(0.8);
doc.fontSize(9).fillColor(C.mid).font('Helvetica')
   .text('Generated: ' + new Date().toISOString().slice(0, 10), { align: 'center' });

// ════════════════════════════════════════════════════════════════════
// PAGE 2 — Installation
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('1. Installation & First Boot');

heading2('Server Requirements');

const reqW = [140, doc.page.width - 205];
tableRow(['Requirement', 'Details'], reqW, [C.darkGreen, C.darkGreen], true);
const reqs = [
  ['Server software', 'Paper 1.21+ or Purpur 1.21+  (1.21.4 recommended)'],
  ['Java version',    'Java 21 or newer (Temurin / OpenJDK 21)'],
  ['RAM',             'Minimum 2 GB allocated (-Xmx2G), 4 GB+ recommended for SMP'],
  ['Database',        'SQLite (built in, zero config) or MySQL / MariaDB for 50+ players'],
  ['Permissions',     'LuckPerms recommended, any Bukkit-compatible plugin works'],
];
reqs.forEach(([a, b], i) => {
  tableRow([a, b], reqW, [C.bgHeader, i % 2 === 0 ? C.bgCard : C.bgHeader]);
});

doc.moveDown(0.5);
heading2('Step-by-Step Installation');

const steps = [
  ['1', 'Download', 'Get NuclearCraft-1.0.0.jar from your release package.'],
  ['2', 'Place JAR', 'Copy NuclearCraft-1.0.0.jar into your server\'s /plugins/ folder.'],
  ['3', 'First boot', 'Start the server. NuclearCraft auto-generates all config files. Look for the green startup banner in console.'],
  ['4', 'Verify', 'Run /nc version in-game or console. Expect: "NuclearCraft: Plutonium Age v1.0.0 Production"'],
  ['5', 'Configure', 'Edit files in plugins/NuclearCraft/ to tune radiation, machines, boss and balance. Run /nc reload to apply.'],
  ['6', 'Resource pack', 'Host the .zip at a public HTTPS URL and configure resourcepack.yml (see Section 3).'],
];
steps.forEach(([n, title, desc]) => {
  checkSpace(42);
  const y = doc.y;
  doc.rect(55, y, 26, 36).fill(C.darkGreen);
  doc.rect(81, y, doc.page.width - 136, 36).fill(C.bgCard);
  doc.fontSize(13).fillColor(C.dark).font('Helvetica-Bold').text(n, 55, y + 9, { width: 26, align: 'center' });
  doc.fontSize(9).fillColor(C.lime).font('Helvetica-Bold').text(title, 90, y + 4, { width: doc.page.width - 155 });
  doc.fontSize(8.5).fillColor(C.light).font('Helvetica').text(desc, 90, y + 16, { width: doc.page.width - 155, lineGap: 1 });
  doc.y = y + 40;
});

doc.moveDown(0.3);
heading2('Generated Folder Structure');
code('plugins/NuclearCraft/\n├── config.yml           Main: database, performance, worlds\n├── radiation.yml        Stages, damage, decay, contagion\n├── titan.yml            Boss HP phases, ability timing\n├── titan_items.yml      Boss loot table\n├── smelter.yml          Recipes, fuel, overheat\n├── forge.yml            Upgrade tiers, energy cost\n├── farming.yml          Crop growth, antidote recipes\n├── zombies.yml          Spawn rates, surge settings\n├── ore.yml              Vein size, depth, rarity\n├── combat.yml           Weapon mastery, damage modifiers\n├── equipment.yml        Plutonium / Hazmat armor stats\n├── messages.yml         All player-visible strings\n├── resourcepack.yml     Pack URL, hash, prompt\n├── balance.yml          Economy rewards, XP rates\n└── dumps/               Created by /nc dumpdata');

// ════════════════════════════════════════════════════════════════════
// PAGE 3 — Database & Performance
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('2. Database & Performance Config');

heading2('SQLite (Default — Zero Config)');
body('SQLite is enabled by default. The database file (nuclearcraft.db) is created automatically in the plugin folder. No changes needed for servers up to ~50 players.');

heading2('MySQL / MariaDB Setup');
body('Switch to MySQL for larger SMP servers (50+ concurrent players) for better concurrent write throughput.');
code('# plugins/NuclearCraft/config.yml\ndatabase:\n  type: mysql            # Change from "sqlite"\n  host: "127.0.0.1"\n  port: 3306\n  name: "nuclearcraft"\n  username: "nc_user"\n  password: "your_password"\n  pool-size: 10\n  connection-timeout: 30000');

infoBox('MySQL setup SQL:\n  CREATE DATABASE nuclearcraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n  CREATE USER \'nc_user\'@\'localhost\' IDENTIFIED BY \'your_password\';\n  GRANT ALL PRIVILEGES ON nuclearcraft.* TO \'nc_user\'@\'localhost\';\n  FLUSH PRIVILEGES;', C.orange);

heading2('Performance Settings');
code('# config.yml\nperformance:\n  monitor-interval-seconds: 30\n  log-stats: false\n  particle-throttle-percent: 50   # Reduce particles below this TPS\n\nadmin:\n  enable-memory-sampler: true\n  memory-warn-threshold-percent: 85\n  dump-dir: "dumps"');

heading2('World Whitelist');
body('NuclearCraft is active in all worlds by default. Restrict it with:');
code('# config.yml\nworlds:\n  whitelist-enabled: false\n  whitelist:\n    - world\n    - world_nether\n    - world_the_end');

heading2('Recommended JVM Flags');
code('java -Xms2G -Xmx4G \\\n  -XX:+UseG1GC -XX:+ParallelRefProcEnabled \\\n  -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions \\\n  -XX:+DisableExplicitGC -XX:+AlwaysPreTouch \\\n  -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 \\\n  -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 \\\n  -XX:InitiatingHeapOccupancyPercent=15 \\\n  -Daikars.new.flags=true \\\n  -jar paper.jar --nogui');

// ════════════════════════════════════════════════════════════════════
// PAGE 4 — Resource Pack
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('3. Resource Pack Setup');

heading2('What the Resource Pack Provides');
const rpItems = [
  'Custom 3D item models for all Plutonium and Titan equipment',
  'Custom models for Nuclear Smelter, Forge, and Titan Reactor Forge',
  '30 unique sound events: radiation, machines, boss, combat',
  'Visual identifiers to distinguish NuclearCraft items from vanilla',
];
rpItems.forEach(r => body('  •  ' + r));

heading2('Step 1 — Host the ZIP at a Public HTTPS URL');
const hostOpts = [
  ['GitHub Releases', 'Upload to a GitHub release, copy the "Download" link directly.'],
  ['Dropbox',         'Upload and share. Change ?dl=0 to ?dl=1 in the link.'],
  ['Your Web Server', 'Serve via nginx/Apache with HTTPS. Any static file host works.'],
  ['Modrinth CDN',    'Upload as a project file and use their CDN download URL.'],
];
const hw = [110, doc.page.width - 165];
tableRow(['Host Option', 'How'], hw, [C.darkGreen, C.darkGreen], true);
hostOpts.forEach(([opt, how], i) => tableRow([opt, how], hw, [C.bgHeader, i % 2 === 0 ? C.bgCard : C.bgHeader]));

doc.moveDown(0.4);
heading2('Step 2 — Generate the SHA-1 Hash');
body('The hash allows clients to skip re-downloading an unchanged pack.');
code('# Linux / macOS\nsha1sum "NuclearCraft Resource Pack 1.0.1.zip"\n\n# Windows (PowerShell)\nGet-FileHash "NuclearCraft Resource Pack 1.0.1.zip" -Algorithm SHA1\n\n# Output example (40 hex characters):\n# a3f2c1d4e5b6a7c8d9e0f1a2b3c4d5e6f7a8b9c0');

heading2('Step 3 — Configure resourcepack.yml');
code('# plugins/NuclearCraft/resourcepack.yml\nresource-pack:\n  enabled: true\n  url: "https://yourdomain.com/NuclearCraft-ResourcePack-v1.0.0.zip"\n  hash: "a3f2c1d4e5b6a7c8d9e0f1a2b3c4d5e6f7a8b9c0"   # your actual hash\n  required: false      # set true to kick players who decline\n  prompt: "NuclearCraft requires a resource pack for custom models and sounds."\n  log-status: true');

heading2('Step 4 — Reload');
body('Run /nc reload. All new players joining will automatically receive the pack prompt. Existing online players must relog to receive it.');

infoBox('TIP: Set required: true on competitive SMP servers so every player sees correct item models. Players who decline on required: false will see vanilla placeholders instead of NuclearCraft 3D models.', C.accent);

doc.moveDown(0.3);
heading2('Sound Events Reference');
body('These 30 sound keys must be declared in your resource pack\'s sounds.json:');
const sounds = [
  'nuclearcraft.radiation.gain         nuclearcraft.radiation.stage_change',
  'nuclearcraft.radiation.cure         nuclearcraft.radiation.surge',
  'nuclearcraft.radiation.death        nuclearcraft.item.antidote_drink',
  'nuclearcraft.item.serum_drink       nuclearcraft.ore.discover',
  'nuclearcraft.ore.mine               nuclearcraft.ore.drill',
  'nuclearcraft.machine.smelter_start  nuclearcraft.machine.smelter_complete',
  'nuclearcraft.machine.smelter_overheat  nuclearcraft.machine.forge_start',
  'nuclearcraft.machine.forge_complete    nuclearcraft.machine.forge_overload',
  'nuclearcraft.machine.titan_forge_start nuclearcraft.machine.titan_forge_complete',
  'nuclearcraft.upgrade.success        nuclearcraft.upgrade.fail',
  'nuclearcraft.combat.radiation_hit   nuclearcraft.combat.aura_pulse',
  'nuclearcraft.titan.spawn            nuclearcraft.titan.death',
  'nuclearcraft.titan.phase_change     nuclearcraft.titan.roar',
  'nuclearcraft.environment.radiation_night_start  nuclearcraft.environment.radiation_night_end',
  'nuclearcraft.environment.radiation_cloud',
];
code(sounds.join('\n'));

// ════════════════════════════════════════════════════════════════════
// PAGE 5 — Permissions
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('4. Permissions Reference');

body('NuclearCraft uses a hierarchical tree. nuclearcraft.admin grants all sub-nodes automatically.');

const pw = [205, 55, doc.page.width - 315];
tableRow(['Permission Node', 'Default', 'Description'], pw, [C.darkGreen, C.darkGreen, C.darkGreen], true);
const perms = [
  ['nuclearcraft.use',             'all',   'Use /nc radiation check on yourself'],
  ['nuclearcraft.reload',          'op',    '/nc reload — reload all configs'],
  ['nuclearcraft.debug',           'op',    '/nc debug — toggle debug logging'],
  ['nuclearcraft.give',            'op',    '/nc give — give custom items'],
  ['nuclearcraft.admin',           'op',    'Full admin access (grants all children below)'],
  ['nuclearcraft.admin.radiation', 'op',    'Radiation admin: add/remove/set/clear/check others'],
  ['nuclearcraft.admin.zombies',   'op',    'Zombie admin: spawn types, surge control'],
  ['nuclearcraft.admin.ore',       'op',    'Ore admin: spawn blocks, give drill/fragments'],
  ['nuclearcraft.admin.smelter',   'op',    'Smelter admin: give, stats, debug'],
  ['nuclearcraft.admin.equipment', 'op',    'Equipment admin: give plutonium/hazmat gear'],
  ['nuclearcraft.admin.farming',   'op',    'Farming admin: give seeds/cures, growall'],
  ['nuclearcraft.admin.forge',     'op',    'Forge admin: give, energy, tier upgrade'],
  ['nuclearcraft.admin.combat',    'op',    'Combat admin: stats, mastery, kills'],
  ['nuclearcraft.admin.titan',     'op',    'Titan admin: spawn, kill, phase, stats + Titan Tech'],
  ['nuclearcraft.admin.debug',     'op',    '/nc debug <system> diagnostic subcommands'],
];
perms.forEach(([node, def, desc], i) => {
  const bg = i % 2 === 0 ? C.bgCard : C.bgHeader;
  const defColor = def === 'all' ? C.accent : C.yellow;
  const y = doc.y;
  checkSpace(22);
  doc.rect(55, doc.y, pw[0], 20).fill(bg);
  doc.rect(55 + pw[0], doc.y, pw[1], 20).fill(bg);
  doc.rect(55 + pw[0] + pw[1], doc.y, pw[2], 20).fill(bg);
  doc.fontSize(8).fillColor(C.green).font('Courier').text(node, 61, doc.y + 5, { width: pw[0] - 12 });
  doc.fontSize(8).fillColor(defColor).font('Helvetica-Bold').text(def, 55 + pw[0] + 5, doc.y - 15, { width: pw[1] - 10 });
  doc.fontSize(8).fillColor(C.light).font('Helvetica').text(desc, 55 + pw[0] + pw[1] + 6, doc.y - 15, { width: pw[2] - 12 });
  doc.y = y + 21;
});

doc.moveDown(0.5);
heading2('LuckPerms Setup');
code('# Grant full admin to the admin group:\nlp group admin permission set nuclearcraft.admin true\n\n# Grant basic use to all players (default group):\nlp group default permission set nuclearcraft.use true\n\n# Grant a single specific node:\nlp user Steve permission set nuclearcraft.admin.titan true');

// ════════════════════════════════════════════════════════════════════
// PAGE 6 — Core & Debug Commands
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('5. Commands — Core & Debug');

body('All commands support the alias /nc (short for /nuclearcraft). Commands shown below work in-game and in the server console (console omits the /). Tab completion is available for all commands.');

heading2('Core Commands');
[
  ['/nc help',                    'Show the help menu filtered to your permissions',                  'nuclearcraft.use'],
  ['/nc version',                 'Display plugin version, Java version, server platform',            'nuclearcraft.use'],
  ['/nc info',                    'Show runtime counts: ores, machines, crops, players, boss status', 'nuclearcraft.use'],
  ['/nc reload',                  'Reload all config files without restarting the server',            'nuclearcraft.reload'],
  ['/nc give <player> <item> [n]','Give a custom NuclearCraft item to any player',                   'nuclearcraft.give'],
  ['/nc debug',                   'Toggle debug logging on/off',                                      'nuclearcraft.debug'],
].forEach(([cmd, desc, perm]) => { checkSpace(50); cmdEntry(cmd, desc, perm); });

heading2('Debug Subcommands  (/nc debug <system>)');
body('Each debug subcommand prints a live diagnostic report for that subsystem. Use /nc debug all for a full system dump.');

const dbgCmds = [
  '/nc debug radiation', '/nc debug zombies',  '/nc debug ore',     '/nc debug smelter',
  '/nc debug forge',     '/nc debug farming',  '/nc debug combat',  '/nc debug titan',
  '/nc debug performance', '/nc debug memory', '/nc debug all',
];
const colW2 = (doc.page.width - 130) / 3;
let dCols = [55, 55 + colW2 + 5, 55 + (colW2 + 5) * 2];
let dRows = [doc.y, doc.y, doc.y];
dbgCmds.forEach((cmd, i) => {
  const col = i % 3;
  doc.rect(dCols[col], dRows[col], colW2, 18).fill(C.bgCard);
  doc.fontSize(8).fillColor(C.green).font('Courier').text(cmd, dCols[col] + 6, dRows[col] + 4, { width: colW2 - 12 });
  dRows[col] += 22;
});
doc.y = Math.max(...dRows) + 6;

// ════════════════════════════════════════════════════════════════════
// PAGE 7 — Admin System Commands
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('5. Commands — Admin System');

heading2('Server Administration  (nuclearcraft.admin)');
[
  ['/nc health',       'Live health snapshot: TPS, heap memory, machine count, titan status, database OK'],
  ['/nc diagnostics',  'Full startup checks + config validation suite — shows pass/fail for every check'],
  ['/nc performance',  'Detailed metrics: TPS, tracked objects, heap %, GC collections, machine load'],
  ['/nc cleanup',      'Purge all internal caches and request JVM garbage collection'],
  ['/nc fixdata',      'Validate all player data + configs; auto-repairs values that are out of range'],
  ['/nc dumpdata',     'Write full debug snapshot to plugins/NuclearCraft/dumps/<timestamp>.txt'],
  ['/nc serverreport', 'Print production summary: version, uptime, TPS, machines, validation results'],
].forEach(([cmd, desc]) => { checkSpace(38); cmdEntry(cmd, desc, 'nuclearcraft.admin'); });

divider();
heading2('Radiation Admin  (nuclearcraft.admin.radiation)');
[
  ['/nc radiation check [player]',    'Show radiation level, stage, contagion flag, and immunity timer'],
  ['/nc radiation add <player> <n>',  'Add N radiation points to a player (scale 0–1000)'],
  ['/nc radiation remove <player> <n>','Remove N radiation points from a player'],
  ['/nc radiation set <player> <n>',  'Set radiation to an exact value'],
  ['/nc radiation clear <player>',    'Remove all radiation instantly'],
].forEach(([cmd, desc]) => { checkSpace(38); cmdEntry(cmd, desc, 'nuclearcraft.admin.radiation'); });

divider();
heading2('Zombie Admin  (nuclearcraft.admin.zombies)');
[
  ['/nc zombie spawn irradiated', 'Spawn an Irradiated Zombie at your location'],
  ['/nc zombie spawn alpha',      'Spawn an Alpha Zombie (boss-tier, higher stats)'],
  ['/nc zombie stats',            'Show totals: spawned, active, alpha count'],
  ['/nc zombie surge start',      'Force-start a Radiation Surge event (mass zombie wave)'],
  ['/nc zombie surge stop',       'Force-stop the active Radiation Surge'],
].forEach(([cmd, desc]) => { checkSpace(38); cmdEntry(cmd, desc, 'nuclearcraft.admin.zombies'); });

// ════════════════════════════════════════════════════════════════════
// PAGE 8 — Machine & World Commands
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('5. Commands — Machines & World');

heading2('Ore Admin  (nuclearcraft.admin.ore)');
[
  ['/nc ore spawn plutonium',   'Spawn a Plutonium Ore block at your location'],
  ['/nc ore give fragment [n]', 'Give Raw Plutonium Fragments to yourself'],
  ['/nc ore give drill',        'Give a Radiation Drill (required to mine ore safely)'],
  ['/nc ore stats',             'Show tracked ore count and mining statistics'],
].forEach(([cmd, desc]) => { checkSpace(38); cmdEntry(cmd, desc, 'nuclearcraft.admin.ore'); });

divider();
heading2('Smelter Admin  (nuclearcraft.admin.smelter)');
[
  ['/nc smelter give [player]', 'Give a Nuclear Smelter block to yourself or a player'],
  ['/nc smelter stats',         'Show active machine count and aggregate processing stats'],
  ['/nc smelter debug',         'List every active smelter: location, state, progress %'],
].forEach(([cmd, desc]) => { checkSpace(38); cmdEntry(cmd, desc, 'nuclearcraft.admin.smelter'); });

divider();
heading2('Forge Admin  (nuclearcraft.admin.forge)');
[
  ['/nc forge give [player]',     'Give a Nuclear Forge block'],
  ['/nc forge energy set <n>',    'Set energy of the forge you are looking at'],
  ['/nc forge energy add <n>',    'Add energy to the forge you are looking at'],
  ['/nc forge energy clear',      'Remove all energy from the forge you are looking at'],
  ['/nc forge upgrade <mk1-mk4>', 'Force-upgrade the targeted item to a specific tier'],
  ['/nc forge stats',             'Successes, failures, MK4 crafts, overload count'],
].forEach(([cmd, desc]) => { checkSpace(38); cmdEntry(cmd, desc, 'nuclearcraft.admin.forge'); });

divider();
heading2('Equipment Admin  (nuclearcraft.admin.equipment)');
cmdEntry(
  '/nc equipment give <type> [player]',
  'Give Plutonium or Hazmat equipment.\nTypes: plutonium-sword, plutonium-axe, plutonium-pickaxe, plutonium-shovel, plutonium-hoe,\n       plutonium-helmet, plutonium-chestplate, plutonium-leggings, plutonium-boots,\n       hazmat-helmet, hazmat-chestplate, hazmat-leggings, hazmat-boots,\n       plutonium-arrow, refined-plutonium-ingot, industrial-fabric',
  'nuclearcraft.admin.equipment'
);
cmdEntry('/nc equipment stats', 'Show equipment worn by all online players', 'nuclearcraft.admin.equipment');

divider();
heading2('Farming Admin  (nuclearcraft.admin.farming)');
[
  ['/nc farming give mutated-seed [n]',      'Give Mutated Seeds'],
  ['/nc farming give healing-petal [n]',     'Give Healing Petals (antidote component)'],
  ['/nc farming give radiation-antidote [n]','Give Radiation Antidote (removes ~250 radiation)'],
  ['/nc farming give radiation-serum [n]',   'Give Radiation Serum (120s radiation immunity)'],
  ['/nc farming growall [radius]',           'Force-grow all mutated crops within radius (default 10)'],
  ['/nc farming stats',                      'Show active crops, bloom count, harvest statistics'],
].forEach(([cmd, desc]) => { checkSpace(38); cmdEntry(cmd, desc, 'nuclearcraft.admin.farming'); });

// ════════════════════════════════════════════════════════════════════
// PAGE 9 — Titan Commands
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('5. Commands — Titan Boss & Titan Tech');

heading2('Titan Boss Admin  (nuclearcraft.admin.titan)');
[
  ['/nc titan spawn',          'Spawn the Plutonium Titan at your location (one Titan active at a time)'],
  ['/nc titan kill',           'Instantly remove the active Titan (no loot drops)'],
  ['/nc titan phase <1-4>',    'Force active Titan to a phase (1=Dormant, 2=Enraged, 3=Devastator, 4=Final)'],
  ['/nc titan stats [player]', 'Titan history: summons, kills, deaths, damage dealt/taken, catastrophes survived, cores'],
].forEach(([cmd, desc]) => { checkSpace(42); cmdEntry(cmd, desc, 'nuclearcraft.admin.titan'); });

divider();
heading2('Titan Technology Admin  (nuclearcraft.admin.titan)');
cmdEntry(
  '/nc titantech give <item> [n]',
  'Give Titan Tech items. Valid types:\n  titan-reactor-forge, titan-helmet, titan-chestplate, titan-leggings, titan-boots,\n  titan-sword, titan-axe, titan-pickaxe, titan-shovel, titan-hoe,\n  titan-bow, titan-arrow, titan-fragment, titan-core',
  'nuclearcraft.admin.titan'
);
[
  ['/nc titantech stats [player]',  'Show Titan Tech stats: gear crafted, radiation reflected, sword hits, arrows fired, full set active'],
  ['/nc titantech aura',            'Display Titan weapon aura radius and trigger interval'],
  ['/nc titantech setbonusinfo',    'List all active set bonus effects (requires full Titan armor set)'],
].forEach(([cmd, desc]) => { checkSpace(38); cmdEntry(cmd, desc, 'nuclearcraft.admin.titan'); });

divider();
heading2('Combat Admin  (nuclearcraft.admin.combat)');
[
  ['/nc combat stats [player]',     'Radiation kills, PvP hits, infections, total melee hits'],
  ['/nc combat mastery [player]',   'Weapon mastery progress per type: sword, axe, bow, trident'],
  ['/nc combat radiation [player]', 'Radiation combat stats: arrow hits, aura damage dealt'],
].forEach(([cmd, desc]) => { checkSpace(38); cmdEntry(cmd, desc, 'nuclearcraft.admin.combat'); });

doc.moveDown(0.3);
infoBox(
  'Full Titan Armor Set Bonuses:\n' +
  '• Radiation Immunity — complete immunity while all 4 pieces are worn\n' +
  '• Speed II + Jump Boost II — enhanced movement\n' +
  '• Resistance + Fire Resistance — reduced physical and fire damage\n' +
  '• +12 Max HP bonus (chestplate +4, full set +8 = 6 extra hearts)\n' +
  '• 30% radiation reflection — damage you receive reflects back to attacker\n' +
  '• Auto-cure — removes 100 radiation every 20 seconds automatically',
  C.accent
);

// ════════════════════════════════════════════════════════════════════
// PAGE 10 — Radiation System
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('6. Radiation System');

heading2('Radiation Stages');
body('All players start at 0 (Healthy). Radiation accumulates from sources below and decays slowly when not exposed. Scale: 0 to 1000.');

const stageW = [22, 130, 90, doc.page.width - 297];
tableRow(['#', 'Stage', 'Range', 'Effects'], stageW, [C.darkGreen, C.darkGreen, C.darkGreen, C.darkGreen], true);
const stageColors = [C.accent, C.lime, C.yellow, C.orange, C.red];
const stageBg     = [C.bgCard, C.bgHeader, C.bgCard, C.bgHeader, C.bgCard];
[
  ['0', 'Healthy',           '0',       'No effects'],
  ['1', 'Minor Exposure',    '1–249',   'Weakness I, mild nausea'],
  ['2', 'Moderate Exposure', '250–499', 'Weakness II, Slowness I, Hunger'],
  ['3', 'Severe Exposure',   '500–749', 'Weakness III, Slowness II, Nausea, periodic damage'],
  ['4', 'Critical Poisoning','750–1000','All above + rapid health drain + contagion spread'],
].forEach(([n, name, range, fx], i) => {
  const y = doc.y;
  doc.rect(55, y, stageW[0], 20).fill(stageColors[i]);
  doc.rect(55 + stageW[0], y, stageW[1], 20).fill(stageBg[i]);
  doc.rect(55 + stageW[0] + stageW[1], y, stageW[2], 20).fill(stageBg[i]);
  doc.rect(55 + stageW[0] + stageW[1] + stageW[2], y, stageW[3], 20).fill(stageBg[i]);
  doc.fontSize(9).fillColor(C.dark).font('Helvetica-Bold').text(n, 55, y + 5, { width: stageW[0], align: 'center' });
  doc.fontSize(8.5).fillColor(C.light).font('Helvetica-Bold').text(name, 55 + stageW[0] + 5, y + 5, { width: stageW[1] - 10 });
  doc.fontSize(8.5).fillColor(C.mid).font('Helvetica').text(range, 55 + stageW[0] + stageW[1] + 5, y + 5, { width: stageW[2] - 10 });
  doc.fontSize(8.5).fillColor(C.light).font('Helvetica').text(fx, 55 + stageW[0] + stageW[1] + stageW[2] + 5, y + 5, { width: stageW[3] - 10 });
  doc.y = y + 21;
});

doc.moveDown(0.4);
heading2('Radiation Sources');
[
  'Mining Plutonium Ore without a Radiation Drill',
  'Being hit by Irradiated Zombies or Alpha Zombies',
  'Standing near an active Nuclear Smelter (machine aura)',
  'Entering a Radiation Cloud spawned during zombie events',
  'Being hit by another player\'s Plutonium weapon aura (PvP)',
  'Being struck by Plutonium Arrows',
  'Entering a Radiation Night event zone',
  'Stage 4 contagion — nearby Stage 0–2 players gain radiation passively',
].forEach(s => body('  ▸  ' + s));

heading2('Curing Radiation');
const cureW = [145, doc.page.width - 200];
tableRow(['Item / Method', 'Effect'], cureW, [C.darkGreen, C.darkGreen], true);
[
  ['Radiation Antidote',      'Removes ~250 radiation. Crafted from Healing Petals. Drops from mutated plants.'],
  ['Radiation Serum',         'Grants 120s full immunity. Crafted from Antidotes + Irradiated Hearts.'],
  ['Hazmat Armor (partial)',  'Each piece reduces radiation gain rate passively.'],
  ['Titan Armor (full set)',  'Complete radiation immunity while all 4 pieces are worn.'],
  ['Natural Decay',           'Radiation decays slowly over time when not exposed to sources.'],
].forEach(([item, eff], i) => tableRow([item, eff], cureW, [C.bgHeader, i % 2 === 0 ? C.bgCard : C.bgHeader]));

// ════════════════════════════════════════════════════════════════════
// PAGE 11 — Machines Guide
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('7. Machines Guide');

heading2('Nuclear Smelter');
body('Processes Raw Plutonium Fragments into Refined Plutonium Ingots. Place like a furnace. Right-click to open GUI. Requires fuel to operate.');
[
  'Input: Raw Plutonium Fragment or Plutonium Ore',
  'Fuel: Coal, Charcoal, Blaze Rod, Lava Bucket, or any burnable',
  'Output: Refined Plutonium Ingot',
  'Overheat: prolonged use causes temporary shutdown — let it cool between batches',
  'Machine emits a radiation aura — wear hazmat armor while near a running smelter',
  'Machines in unloaded chunks are suspended and resume when the chunk loads',
].forEach(f => body('  ▸  ' + f));

divider();
heading2('Nuclear Forge');
body('Upgrades Plutonium equipment through MK I–IV tiers using Radioactive Cores. Higher tiers = better stats + higher risk.');

const tkW = [60, doc.page.width - 115];
tableRow(['Tier', 'Notes'], tkW, [C.darkGreen, C.darkGreen], true);
[
  ['MK I',  'Low failure risk. Low core cost. Safe starting tier.'],
  ['MK II', 'Moderate failure risk. More cores required.'],
  ['MK III','High failure risk. Item may be destroyed on failure.'],
  ['MK IV', 'Maximum tier. Very high failure risk. Powerful stats on success.'],
].forEach(([t, d], i) => tableRow([t, d], tkW, [C.orange, i % 2 === 0 ? C.bgCard : C.bgHeader]));

doc.moveDown(0.3);
divider();
heading2('Titan Reactor Forge');
body('End-game machine for crafting Titan Technology items. Uses Titan Cores and Titan Fragments obtained by killing the Plutonium Titan. Produces Titan Armor, weapons and tools with unique abilities. Give it with:');
code('/nc titantech give titan-reactor-forge');

infoBox('If your server crashes unexpectedly, run /nc fixdata on next startup to repair any orphaned machine states left by the unclean shutdown.', C.yellow);

doc.moveDown(0.3);
heading2('Machine Admin Tips');
[
  'Use /nc smelter debug to list all active smelters with state and progress %',
  'Place multiple smelters in parallel for higher throughput on large servers',
  'Machines drop as usable items when broken — custom data is preserved',
  'Use /nc forge stats to monitor upgrade successes, failures, and MK4 crafts',
  'The radiation aura is automatically throttled when server TPS drops below threshold',
].forEach(t => body('  ✦  ' + t));

// ════════════════════════════════════════════════════════════════════
// PAGE 12 — Boss Guide
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('8. Plutonium Titan Boss');

heading2('Overview');
body('The Plutonium Titan is a custom multi-phase boss with unique AI, bossbar, AOE abilities, and proportional loot distribution. One Titan active at a time across all worlds. Recommended: 5–50 players per encounter.');

heading2('Phases');
const phaseColors = [C.accent, C.yellow, C.orange, C.red];
[
  ['Phase 1 — Dormant',    '100%–60% HP',  'Standard melee, periodic radiation bursts. Summons Irradiated Zombie waves.'],
  ['Phase 2 — Enraged',    '60%–30% HP',   'Speed + damage boost. Radiation Slam (AOE) unlocked. Larger zombie waves.'],
  ['Phase 3 — Devastator', '30%–10% HP',   'All Phase 2. Radiation Beam attack. Triggers Radiation Night in arena area.'],
  ['Phase 4 — Final',      'Below 10% HP', 'All abilities at reduced cooldowns, massive stat boost. Death = large loot drop.'],
].forEach(([name, hp, desc], i) => {
  checkSpace(48);
  const y = doc.y;
  doc.rect(55, y, doc.page.width - 110, 42).fill(C.bgCard);
  doc.rect(55, y, 4, 42).fill(phaseColors[i]);
  doc.fontSize(10).fillColor(phaseColors[i]).font('Helvetica-Bold').text(name, 68, y + 5, { width: 200 });
  doc.fontSize(8.5).fillColor(C.yellow).font('Helvetica').text(hp, 68, y + 17, { width: 120 });
  doc.fontSize(8.5).fillColor(C.light).font('Helvetica').text(desc, 68, y + 28, { width: doc.page.width - 140, lineGap: 1 });
  doc.y = y + 47;
});

doc.moveDown(0.3);
heading2('Summoning');
body('The Titan spawns automatically via server events (configurable in titan.yml). Admins can also manually spawn it:');
code('/nc titan spawn        # Spawns at your location\n/nc titan phase 2      # Jump to Enraged phase for testing\n/nc titan kill         # Remove without loot (testing)');

heading2('Loot');
body('Loot is proportional to damage dealt. High-damage players receive rarer drops. All participants receive at least a consolation reward. Configure loot in titan_items.yml.');

heading2('Titan Items (via Titan Reactor Forge)');
const titanItems = [
  ['titan-reactor-forge', 'Craft all Titan items from Cores + Fragments'],
  ['titan-helmet/chestplate/leggings/boots', 'Full Titan armor set with all set bonuses'],
  ['titan-sword/axe/pickaxe/shovel/hoe',     'Titan weapons with radiation aura on hit'],
  ['titan-bow + titan-arrow',                'Ranged weapon firing plutonium arrows'],
  ['titan-fragment',                          'Raw crafting material dropped by the Titan'],
  ['titan-core',                              'Rare core used for highest-tier recipes'],
];
const tiW = [175, doc.page.width - 230];
tableRow(['Item', 'Description'], tiW, [C.darkGreen, C.darkGreen], true);
titanItems.forEach(([item, desc], i) => tableRow([item, desc], tiW, [C.bgHeader, i % 2 === 0 ? C.bgCard : C.bgHeader]));

// ════════════════════════════════════════════════════════════════════
// PAGE 13 — Troubleshooting
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('9. Troubleshooting');

heading2('Expected Console Output on Clean Startup');
code('[NuclearCraft] =============================================\n[NuclearCraft]   NuclearCraft: Plutonium Age v1.0.0\n[NuclearCraft]   Phase 14 Production — All systems active\n[NuclearCraft] =============================================\n[TestingManager]    All 6 startup checks PASSED.\n[ValidationManager] All 12 validations passed.\n[RecoveryManager]   No orphaned records. Clean shutdown confirmed.\n[ReleaseManager]    Java 21 ✔  |  Heap 4096MB ✔  |  Paper 1.21.4 ✔');

heading2('Common Problems & Solutions');
[
  ['Plugin fails to load',          'Ensure Java 21+ and Paper/Purpur 1.21+. Check console for the exact error. Another plugin may conflict with /nc — check command aliases.'],
  ['Database connection refused',   'Verify credentials in config.yml. Confirm MySQL is running, the database exists, and firewall allows the connection.'],
  ['Items show as vanilla textures','Resource pack is not configured or player declined it. Verify URL and hash in resourcepack.yml. Set required: true to enforce.'],
  ['Low TPS with many players',     'Run /nc performance to identify bottlenecks. Reduce particle-throttle-percent. Limit active smelters per chunk. Upgrade hardware.'],
  ['Memory warnings in console',    'Run /nc cleanup to flush caches and request GC. If persistent, increase -Xmx (e.g. -Xmx4G). Run /nc dumpdata for analysis.'],
  ['Radiation not working',         'Confirm the world is not excluded in config.yml worlds whitelist. Run /nc debug radiation for live system state.'],
  ['Machines not saving on restart','Only /stop cleanly shuts down machines. After any crash, run /nc fixdata on next boot.'],
  ['Bossbar not visible',           'Player must be within the Titan\'s bossbar range (titan.yml). Run /nc debug titan to check active boss state.'],
  ['Config changes not applying',   'Run /nc reload after any edit. For database changes you must fully restart the server.'],
].forEach(([prob, sol]) => {
  checkSpace(40);
  const y = doc.y;
  const textH = doc.heightOfString(sol, { width: doc.page.width - 145, fontSize: 8.5 });
  const h = Math.max(34, textH + 20);
  doc.rect(55, y, doc.page.width - 110, h).fill(C.bgCard);
  doc.rect(55, y, 4, h).fill(C.orange);
  doc.fontSize(9).fillColor(C.orange).font('Helvetica-Bold').text(prob, 68, y + 5, { width: doc.page.width - 140 });
  doc.fontSize(8.5).fillColor(C.light).font('Helvetica').text(sol, 68, y + 18, { width: doc.page.width - 140, lineGap: 1 });
  doc.y = y + h + 4;
});

// ════════════════════════════════════════════════════════════════════
// PAGE 14 — Quick Reference
// ════════════════════════════════════════════════════════════════════
newPage();
heading1('10. Quick Reference Card');

heading2('Copy-Paste Admin Commands');

const qr = [
  ['Check server health',         '/nc health'],
  ['Run full diagnostics',        '/nc diagnostics'],
  ['View performance metrics',    '/nc performance'],
  ['Reload all configs',          '/nc reload'],
  ['Flush caches / request GC',   '/nc cleanup'],
  ['Repair corrupt player data',  '/nc fixdata'],
  ['Write debug dump to disk',    '/nc dumpdata'],
  ['Print production report',     '/nc serverreport'],
  ['Toggle debug logging',        '/nc debug'],
  ['Full debug dump all systems', '/nc debug all'],
  ['Memory / heap diagnostic',    '/nc debug memory'],
  ['Check player radiation',      '/nc radiation check <player>'],
  ['Clear player radiation',      '/nc radiation clear <player>'],
  ['Set player radiation to 0',   '/nc radiation set <player> 0'],
  ['Spawn Titan boss',            '/nc titan spawn'],
  ['Force Titan to final phase',  '/nc titan phase 4'],
  ['Remove Titan (no loot)',       '/nc titan kill'],
  ['Give Titan chestplate',       '/nc titantech give titan-chestplate'],
  ['Spawn irradiated zombie',     '/nc zombie spawn irradiated'],
  ['Start radiation surge',       '/nc zombie surge start'],
  ['Stop radiation surge',        '/nc zombie surge stop'],
  ['Give radiation antidote x10', '/nc farming give radiation-antidote 10'],
  ['Give radiation serum x5',     '/nc farming give radiation-serum 5'],
  ['Give radiation drill',        '/nc ore give drill'],
  ['Give nuclear smelter',        '/nc smelter give'],
  ['Give nuclear forge',          '/nc forge give'],
];

const qW = (doc.page.width - 120) / 2;
const qCols = [55, 55 + qW + 10];
const qRows = [doc.y, doc.y];

qr.forEach(([desc, cmd], i) => {
  const col = i % 2;
  const x = qCols[col];
  const y = qRows[col];
  if (y + 28 > doc.page.height - 60) return;
  doc.rect(x, y, qW, 26).fill(C.bgCard);
  doc.fontSize(7.5).fillColor(C.mid).font('Helvetica').text(desc, x + 6, y + 3, { width: qW - 12 });
  doc.fontSize(8).fillColor(C.green).font('Courier').text(cmd, x + 6, y + 13, { width: qW - 12 });
  qRows[col] += 29;
});

doc.y = Math.max(qRows[0], qRows[1]) + 8;
divider();

heading2('Config Files at a Glance');
const cfgW = [(doc.page.width - 120) / 2, (doc.page.width - 120) / 2];
const cfgData = [
  ['config.yml',       'Main: database, performance, worlds, admin'],
  ['radiation.yml',    'Stages, damage/tick, decay, contagion range'],
  ['titan.yml',        'Boss HP phases, ability timing, bossbar range'],
  ['titan_items.yml',  'Boss loot table and drop weights'],
  ['smelter.yml',      'Recipes, fuel values, overheat threshold'],
  ['forge.yml',        'Upgrade tiers, energy cost, success/fail rates'],
  ['farming.yml',      'Crop growth, mutation chance, antidote recipes'],
  ['zombies.yml',      'Spawn rates, alpha chance, surge config'],
  ['ore.yml',          'Vein size, depth, rarity per world'],
  ['messages.yml',     'All player-facing strings (localisable)'],
  ['resourcepack.yml', 'Pack URL, SHA-1 hash, required flag'],
  ['balance.yml',      'Economy rewards, XP rates, loot multipliers'],
];
const cfCols2 = [55, 55 + cfgW[0] + 5];
const cfRows2 = [doc.y, doc.y];
cfgData.forEach(([file, desc], i) => {
  const col = i % 2;
  const x = cfCols2[col];
  const y = cfRows2[col];
  const bg = Math.floor(i / 2) % 2 === 0 ? C.bgCard : C.bgHeader;
  doc.rect(x, y, cfgW[col], 22).fill(bg);
  doc.fontSize(8).fillColor(C.yellow).font('Courier').text(file, x + 6, y + 4, { width: cfgW[col] - 12 });
  doc.fontSize(7.5).fillColor(C.mid).font('Helvetica').text(desc, x + 6, y + 14, { width: cfgW[col] - 12 });
  cfRows2[col] += 25;
});

doc.y = Math.max(cfRows2[0], cfRows2[1]) + 10;
doc.moveDown(0.5);
doc.fontSize(8.5).fillColor(C.darkGreen).font('Helvetica')
   .text('NuclearCraft: Plutonium Age v1.0.0 Production  |  Paper / Purpur 1.21+  |  Java 21+', { align: 'center' });

// ── Footer on every page except cover ────────────────────────────────────────
const pageCount = doc.bufferedPageRange().count;
for (let i = 1; i < pageCount; i++) {
  doc.switchToPage(i);
  doc.fontSize(7.5).fillColor(C.darkGreen).font('Helvetica')
     .text('NuclearCraft: Plutonium Age v1.0.0', 55, doc.page.height - 28, { width: 200 });
  doc.fontSize(7.5).fillColor(C.darkGreen).font('Helvetica')
     .text('Page ' + (i + 1) + ' of ' + pageCount, 55, doc.page.height - 28,
           { width: doc.page.width - 110, align: 'right' });
}

doc.end();
out.on('finish', () => console.log('PDF done: NuclearCraft-Guide.pdf (' + pageCount + ' pages)'));
out.on('error', e => { console.error(e); process.exit(1); });
