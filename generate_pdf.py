#!/usr/bin/env python3
"""
NuclearCraft: Plutonium Age — Plugin Guide PDF Generator v1.0.1
"""

from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer, Table,
                                 TableStyle, HRFlowable, PageBreak, KeepTogether)
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_RIGHT
import os

# ─── Colour palette ────────────────────────────────────────────────────────
GREEN      = colors.HexColor("#39ff14")
DARK_GREEN = colors.HexColor("#1a7a00")
MID_GREEN  = colors.HexColor("#00c832")
YELLOW     = colors.HexColor("#ffd700")
ORANGE     = colors.HexColor("#ff8c00")
RED        = colors.HexColor("#ff3333")
CYAN       = colors.HexColor("#00e5ff")
PURPLE     = colors.HexColor("#9933ff")
WHITE      = colors.white
BG_DARK    = colors.HexColor("#0d0d0d")
BG_MID     = colors.HexColor("#141414")
BG_CARD    = colors.HexColor("#1a1a1a")
BG_HEADER  = colors.HexColor("#0a1a00")
BORDER     = colors.HexColor("#2a4a00")
BORDER2    = colors.HexColor("#33550a")

# ─── Document setup ────────────────────────────────────────────────────────
PDF_NAME = "NuclearCraft Plutonium Age — Plugin Guide 1.0.1.pdf"
doc = SimpleDocTemplate(
    PDF_NAME,
    pagesize=A4,
    leftMargin=18*mm, rightMargin=18*mm,
    topMargin=14*mm,  bottomMargin=14*mm,
    title="NuclearCraft: Plutonium Age — Plugin Guide 1.0.1",
    author="NuclearCraft Dev",
)
W, H = A4
CW = W - 36*mm  # content width

# ─── Styles ────────────────────────────────────────────────────────────────
styles = getSampleStyleSheet()

def S(name, **kw):
    return ParagraphStyle(name, **kw)

TITLE_STYLE  = S("Title2",   fontName="Helvetica-Bold",   fontSize=26, textColor=GREEN,    alignment=TA_CENTER,  spaceAfter=4)
SUBTITLE_STYLE=S("Sub2",     fontName="Helvetica",        fontSize=11, textColor=YELLOW,   alignment=TA_CENTER,  spaceAfter=2)
VER_STYLE    = S("Ver",      fontName="Helvetica-Oblique",fontSize=9,  textColor=MID_GREEN, alignment=TA_CENTER, spaceAfter=14)
H1_STYLE     = S("H1",       fontName="Helvetica-Bold",   fontSize=15, textColor=GREEN,    spaceBefore=10, spaceAfter=4)
H2_STYLE     = S("H2",       fontName="Helvetica-Bold",   fontSize=12, textColor=YELLOW,   spaceBefore=7,  spaceAfter=3)
H3_STYLE     = S("H3",       fontName="Helvetica-Bold",   fontSize=10, textColor=CYAN,     spaceBefore=5,  spaceAfter=2)
BODY_STYLE   = S("Body2",    fontName="Helvetica",        fontSize=8.5,textColor=WHITE,    spaceBefore=1,  spaceAfter=2, leading=13)
SMALL_STYLE  = S("Small",    fontName="Helvetica",        fontSize=7.5,textColor=colors.HexColor("#aaaaaa"), spaceAfter=1, leading=11)
CODE_STYLE   = S("Code2",    fontName="Courier",          fontSize=7.5,textColor=MID_GREEN, spaceBefore=2, spaceAfter=2, leading=11, leftIndent=6)
WARN_STYLE   = S("Warn",     fontName="Helvetica-Bold",   fontSize=8,  textColor=YELLOW,   spaceBefore=2,  spaceAfter=2)
RED_STYLE    = S("Red",      fontName="Helvetica-Bold",   fontSize=8,  textColor=RED,      spaceBefore=1,  spaceAfter=1)

def p(text, style=BODY_STYLE): return Paragraph(text, style)
def sp(h=4):                   return Spacer(1, h)
def hr(color=BORDER):          return HRFlowable(width="100%", thickness=0.5, color=color, spaceAfter=4, spaceBefore=2)

# ─── Table helpers ─────────────────────────────────────────────────────────
BASE_TS = [
    ("BACKGROUND",  (0,0), (-1,0),  BG_HEADER),
    ("TEXTCOLOR",   (0,0), (-1,0),  GREEN),
    ("FONTNAME",    (0,0), (-1,0),  "Helvetica-Bold"),
    ("FONTSIZE",    (0,0), (-1,-1), 7.5),
    ("FONTNAME",    (0,1), (-1,-1), "Helvetica"),
    ("TEXTCOLOR",   (0,1), (-1,-1), WHITE),
    ("BACKGROUND",  (0,1), (-1,-1), BG_CARD),
    ("ROWBACKGROUNDS",(0,1),(-1,-1),[BG_CARD, BG_MID]),
    ("GRID",        (0,0), (-1,-1), 0.4, BORDER),
    ("ALIGN",       (0,0), (-1,-1), "LEFT"),
    ("VALIGN",      (0,0), (-1,-1), "TOP"),
    ("LEFTPADDING", (0,0), (-1,-1), 5),
    ("RIGHTPADDING",(0,0), (-1,-1), 5),
    ("TOPPADDING",  (0,0), (-1,-1), 3),
    ("BOTTOMPADDING",(0,0),(-1,-1), 3),
]

def tbl(data, col_widths, extra_style=None):
    ts = TableStyle(BASE_TS + (extra_style or []))
    t  = Table([[Paragraph(str(c), S("tc", fontName="Helvetica", fontSize=7.5,
                  textColor=WHITE if i > 0 else GREEN,
                  fontName2="Helvetica-Bold" if i == 0 else "Helvetica"))
                 if isinstance(c, str) else c
                 for i, c in enumerate(row)] for row in data],
               colWidths=col_widths, style=ts, repeatRows=1)
    return t

def simple_tbl(data, col_widths, extra_style=None):
    rows = []
    for ri, row in enumerate(data):
        cells = []
        for ci, c in enumerate(row):
            fn = "Helvetica-Bold" if ri == 0 else "Helvetica"
            tc = GREEN if ri == 0 else (YELLOW if ci == 0 and ri > 0 else WHITE)
            cells.append(Paragraph(str(c), S(f"r{ri}c{ci}", fontName=fn, fontSize=7.5, textColor=tc, leading=11)))
        rows.append(cells)
    ts = TableStyle(BASE_TS + (extra_style or []))
    return Table(rows, colWidths=col_widths, style=ts, repeatRows=1)

# ═══════════════════════════════════════════════════════════════════════════
# CONTENT BUILDER
# ═══════════════════════════════════════════════════════════════════════════
story = []

# ─── COVER ────────────────────────────────────────────────────────────────
story += [
    sp(20),
    p("☢  NuclearCraft: Plutonium Age", TITLE_STYLE),
    p("Plugin Guide &amp; Crafting Reference", SUBTITLE_STYLE),
    p("Version 1.0.1  ·  PaperMC 1.21+  ·  Java 21", VER_STYLE),
    hr(GREEN),
    sp(6),
    p("A comprehensive guide covering all items, recipes, mechanics, and systems added by the "
      "NuclearCraft: Plutonium Age plugin. Radiation is not just a hazard — it is a resource, "
      "a weapon, and a way of life.", BODY_STYLE),
    sp(4),
    p("☢  Radiation permeates every system in this plugin. Handle all plutonium materials with care.", WARN_STYLE),
    sp(12),
    PageBreak(),
]

# ─── TABLE OF CONTENTS ────────────────────────────────────────────────────
story += [
    p("Table of Contents", H1_STYLE), hr(),
    simple_tbl([
        ["Section", "Topic"],
        ["1", "Custom Items & Materials"],
        ["2", "Radiation System"],
        ["3", "Nuclear Smelter"],
        ["4", "Irradiated Zombies & Mob Events"],
        ["5", "Plutonium Ore & Mining"],
        ["6", "Equipment — Tools, Weapons & Armor"],
        ["7", "Farming — Mutated Crops & Cure Crafting"],
        ["8", "Nuclear Forge & Equipment Upgrades"],
        ["9", "Advanced Combat & PvP Radiation Mechanics"],
        ["10", "Commands Reference"],
        ["11", "Permissions"],
        ["12", "Configuration Files"],
    ], [18*mm, CW - 18*mm]),
    sp(6),
    PageBreak(),
]

# ─── SECTION 1: ITEMS ────────────────────────────────────────────────────
story += [
    p("1. Custom Items &amp; Materials", H1_STYLE), hr(),
    p("All NuclearCraft items are crafted using the <b>Custom Model Data</b> (CMD) system — "
      "they visually replace vanilla items but retain full custom behaviour via the resource pack.", BODY_STYLE),
    sp(4),
    p("Core Materials", H2_STYLE),
    simple_tbl([
        ["Item", "Base Item", "CMD", "Description"],
        ["Radioactive Core",       "Magma Cream",       "1101", "Pulsing nuclear energy core. Crafting material & forge fuel."],
        ["Raw Plutonium Fragment", "Prismarine Crystals","1102", "Mined from Plutonium Ore. Must be refined before use."],
        ["Refined Plutonium Ingot","Echo Shard",         "1103", "Output of the Nuclear Smelter. Used in all gear crafting."],
        ["Irradiated Heart",      "Heart of the Sea",   "1106", "Dropped by Irradiated Zombies. Boss summon component."],
        ["Titan Core",            "Nether Star",         "1107", "Crystallised essence of the Plutonium Titan. Boss summon."],
        ["Industrial Fabric",     "Yellow Wool",         "1315", "Radiation-hardened material for Hazmat Suit crafting/repair."],
    ], [36*mm, 32*mm, 14*mm, CW-82*mm]),
    sp(6),
    p("Consumables &amp; Cure Items", H2_STYLE),
    simple_tbl([
        ["Item", "Base Item", "CMD", "Effect"],
        ["Healing Petal",      "Pink Petals",  "1105", "Harvested from Mutated Healing Plants. Cure ingredient."],
        ["Radiation Antidote", "Honey Bottle", "1301", "Right-click to consume. Clears all radiation &amp; infection. No immunity."],
        ["Radiation Serum",    "Glass Bottle", "1302", "Right-click to consume. Clears radiation + 10 min immunity. Rare."],
    ], [36*mm, 28*mm, 14*mm, CW-78*mm]),
    sp(6),
    p("Machines", H2_STYLE),
    simple_tbl([
        ["Item", "Base Item", "CMD", "Function"],
        ["Nuclear Smelter", "Blast Furnace",   "1201", "Refines Raw Plutonium Fragments into Refined Ingots. Requires fuel."],
        ["Nuclear Forge",   "Smithing Table",  "1401", "Upgrades Plutonium &amp; Hazmat gear from MK-I to MK-IV. Requires Radioactive Cores."],
    ], [36*mm, 32*mm, 14*mm, CW-82*mm]),
    sp(6),
    p("Tools &amp; Weapons (Quick Reference)", H2_STYLE),
    simple_tbl([
        ["Item", "Base Item", "CMD", "Notes"],
        ["Radiation Drill",    "Diamond Pickaxe",   "1108", "Only tool that safely mines Plutonium Ore without radiation burst."],
        ["Plutonium Sword",    "Netherite Sword",   "1301", "High damage + radiation-on-hit. Gains mastery XP with each kill."],
        ["Plutonium Axe",      "Netherite Axe",     "1302", "Cleave damage + axe-class mastery bonuses."],
        ["Plutonium Pickaxe",  "Netherite Pickaxe", "1303", "Fastest mining speed tier."],
        ["Plutonium Shovel",   "Netherite Shovel",  "1304", "Converts terrain to radioactive farmland on use."],
        ["Plutonium Hoe",      "Netherite Hoe",     "1305", "Tills radioactive farmland. Required for mutated crop planting."],
        ["Plutonium Arrow",    "Arrow",              "1314", "Fired from any bow. Injects radiation directly into target. Phase 9 PvP weapon."],
    ], [36*mm, 36*mm, 14*mm, CW-86*mm]),
    sp(4),
    PageBreak(),
]

# ─── SECTION 2: RADIATION ──────────────────────────────────────────────────
story += [
    p("2. Radiation System", H1_STYLE), hr(),
    p("Radiation is the central mechanic of NuclearCraft. It accumulates from exposure to plutonium "
      "ore, radioactive items, irradiated zones, mobs, and PvP combat. As radiation increases, "
      "the player progresses through four stages — each with escalating debuffs.", BODY_STYLE),
    sp(4),
    p("Radiation Stages", H2_STYLE),
    simple_tbl([
        ["Stage", "Threshold", "Debuffs", "Visual Effect"],
        ["0 — Clean",     "0",       "None",                                    "Normal HUD"],
        ["1 — Irradiated","1–499",   "Nausea, mild slowness",                   "Green tint, subtle HUD glow"],
        ["2 — Contaminated","500–999","Poison, slowness II, weakness",           "Screen distortion, green particles"],
        ["3 — Critical",  "1000+",   "Wither, blindness, severe slowness, hunger","Heavy screen effects, constant damage"],
    ], [20*mm, 22*mm, 50*mm, CW-92*mm]),
    sp(4),
    p("Radiation Sources", H2_STYLE),
    simple_tbl([
        ["Source", "Radiation Gain", "Notes"],
        ["Standing near Plutonium Ore", "Configurable per tick", "Range-based; blocked by full hazmat suit"],
        ["Mining Plutonium Ore (no drill)","Large burst",         "Use Radiation Drill to mine safely"],
        ["Irradiated Zombie proximity", "Moderate per second",   "Alpha Zombies emit more than standard"],
        ["Radiation Cloud",             "High while inside",     "Created by Alpha Zombie deaths"],
        ["Radiation Night event",       "Passive area-wide",     "Triggered at configurable intervals"],
        ["Plutonium Arrow hit",         "Injected directly",     "PvP — Phase 9"],
        ["Radiation Aura (PvP)",        "Per aura tick",         "From MK-III/IV equipment — Phase 9"],
        ["Radioactive Core (held)",     "Minor passive",         "Configurable"],
    ], [50*mm, 30*mm, CW-80*mm]),
    sp(4),
    p("Reducing Radiation", H2_STYLE),
    p("• Radiation decays naturally over time (configurable rate).<br/>"
      "• <b>Radiation Antidote</b> — instantly clears all radiation.<br/>"
      "• <b>Radiation Serum</b> — clears radiation and grants 10 minutes of immunity.<br/>"
      "• <b>Hazmat Suit</b> — reduces radiation gain by up to 60% when wearing all 4 pieces.<br/>"
      "• <b>Plutonium Armor MK-III+</b> — built-in radiation resistance that scales with upgrade tier.", BODY_STYLE),
    sp(6),
    PageBreak(),
]

# ─── SECTION 3: NUCLEAR SMELTER ───────────────────────────────────────────
story += [
    p("3. Nuclear Smelter", H1_STYLE), hr(),
    p("The Nuclear Smelter (CMD 1201, base: Blast Furnace) is an early-game machine that converts "
      "Raw Plutonium Fragments into Refined Plutonium Ingots. It is required to progress into gear crafting.", BODY_STYLE),
    sp(4),
    p("Obtaining", H2_STYLE),
    p("Give command: <font name='Courier'>/nc give &lt;player&gt; nuclear-smelter</font><br/>"
      "Crafting: Configured in <font name='Courier'>smelter.yml</font> (default recipe uses Blast Furnace + Radioactive Cores).", BODY_STYLE),
    sp(4),
    p("Operation", H2_STYLE),
    p("• Right-click the Nuclear Smelter item in the world (it functions as a placed block alternative).<br/>"
      "• Add <b>Raw Plutonium Fragments</b> to the input slot.<br/>"
      "• Add fuel: <b>Coal, Blaze Rod, or Lava Bucket</b>.<br/>"
      "• Each fragment smelts into one <b>Refined Plutonium Ingot</b>.<br/>"
      "• Smelting speed and fuel efficiency are configurable.", BODY_STYLE),
    sp(4),
    simple_tbl([
        ["Fuel", "Burn Time (ticks)", "Ingots per Fuel"],
        ["Coal",       "1600", "~8"],
        ["Blaze Rod",  "2400", "~12"],
        ["Lava Bucket","20000","~100 (bucket returned)"],
    ], [40*mm, 40*mm, CW-80*mm]),
    sp(6),
    PageBreak(),
]

# ─── SECTION 4: MOBS ──────────────────────────────────────────────────────
story += [
    p("4. Irradiated Zombies &amp; Mob Events", H1_STYLE), hr(),
    p("NuclearCraft adds two zombie variants and two server-wide mob events.", BODY_STYLE),
    sp(4),
    p("Zombie Types", H2_STYLE),
    simple_tbl([
        ["Type", "Health", "Damage", "Radiation Aura", "Special Drops", "Spawn Condition"],
        ["Irradiated Zombie",  "30 HP", "+4 atk", "Moderate",  "Irradiated Heart (chance)", "Natural + /nc zombie spawn irradiated"],
        ["Alpha Zombie",       "60 HP", "+8 atk", "High",       "Radioactive Core, Irradiated Heart", "Low chance near ore, /nc zombie spawn alpha"],
    ], [30*mm, 16*mm, 16*mm, 22*mm, 44*mm, CW-128*mm]),
    sp(4),
    p("Events", H2_STYLE),
    simple_tbl([
        ["Event", "Trigger", "Effect", "Duration"],
        ["Radiation Night",    "Configurable interval (default nightly)", "Passive radiation gain for all players on surface", "Full night cycle"],
        ["Zombie Surge",       "Configurable interval or /nc zombie surge start", "Mass irradiated zombie spawns near players", "Configurable ticks"],
        ["Radiation Cloud",    "Alpha Zombie death",      "Lingering radiation cloud at death location", "Configurable ticks"],
    ], [30*mm, 50*mm, 50*mm, CW-130*mm]),
    sp(6),
    PageBreak(),
]

# ─── SECTION 5: ORE ───────────────────────────────────────────────────────
story += [
    p("5. Plutonium Ore &amp; Mining", H1_STYLE), hr(),
    p("Plutonium Ore is the foundation of all progression. It generates underground and emits "
      "a radiation field that damages nearby unprotected players.", BODY_STYLE),
    sp(4),
    simple_tbl([
        ["Property", "Value"],
        ["Block type",           "Custom ore (configurable stone host)"],
        ["Generation depth",     "Y -64 to Y 16 (configurable)"],
        ["Vein size",            "1–4 blocks (configurable)"],
        ["Radiation radius",     "5 blocks (configurable)"],
        ["Safe mining tool",     "Radiation Drill only — any other tool triggers radiation burst"],
        ["Drop",                 "Raw Plutonium Fragment (1–3, configurable Fortune bonuses)"],
        ["Fortune interaction",  "Fortune I = +1, Fortune II = +2, Fortune III = +3 max"],
    ], [50*mm, CW-50*mm]),
    sp(6),
    PageBreak(),
]

# ─── SECTION 6: EQUIPMENT ─────────────────────────────────────────────────
story += [
    p("6. Equipment — Tools, Weapons &amp; Armor", H1_STYLE), hr(),
    p("All equipment is crafted from <b>Refined Plutonium Ingots</b> and <b>Industrial Fabric</b> "
      "(for armor). Equipment can be upgraded via the Nuclear Forge (see Section 8).", BODY_STYLE),
    sp(4),
    p("Hazmat Suit (Radiation Protection)", H2_STYLE),
    simple_tbl([
        ["Piece", "CMD", "Base Item", "Armor", "Radiation Reduction"],
        ["Hazmat Helmet",     "1306", "Leather Helmet",     "3", "15% per piece"],
        ["Hazmat Chestplate", "1307", "Leather Chestplate", "8", "15% per piece"],
        ["Hazmat Leggings",   "1308", "Leather Leggings",   "6", "15% per piece"],
        ["Hazmat Boots",      "1309", "Leather Boots",      "3", "15% per piece"],
        ["Full set bonus", "", "", "", "Total: 60% radiation reduction (max configurable)"],
    ], [34*mm, 14*mm, 32*mm, 14*mm, CW-94*mm]),
    sp(4),
    p("Plutonium Armor (Combat + Resistance)", H2_STYLE),
    simple_tbl([
        ["Piece", "CMD", "Base Item", "Armor", "Special"],
        ["Plutonium Helmet",     "1310", "Netherite Helmet",     "3", "Radiation resistance scales with MK tier"],
        ["Plutonium Chestplate", "1311", "Netherite Chestplate", "8", "Radiation resistance scales with MK tier"],
        ["Plutonium Leggings",   "1312", "Netherite Leggings",   "6", "Radiation resistance scales with MK tier"],
        ["Plutonium Boots",      "1313", "Netherite Boots",      "3", "Radiation resistance scales with MK tier"],
        ["Full set bonus", "", "", "", "Radiation Aura at MK-III/IV (damages nearby players in PvP)"],
    ], [38*mm, 14*mm, 36*mm, 14*mm, CW-102*mm]),
    sp(4),
    p("Weapon Stats (Base / Before Upgrades)", H2_STYLE),
    simple_tbl([
        ["Weapon", "CMD", "Damage", "Attack Speed", "Special"],
        ["Plutonium Sword",   "1301", "+7 atk dmg", "1.6/s", "Radiation-on-hit, combo system, sword mastery"],
        ["Plutonium Axe",     "1302", "+9 atk dmg", "0.9/s", "Radiation-on-hit, axe mastery, critical surge"],
        ["Plutonium Arrow",   "1314", "Bow base", "—",       "Injects radiation on hit; bow mastery XP on kills"],
        ["Radiation Drill",   "1108", "Diamond equiv", "—",  "Safe plutonium ore mining; no mining radiation burst"],
    ], [36*mm, 14*mm, 22*mm, 22*mm, CW-94*mm]),
    sp(6),
    PageBreak(),
]

# ─── SECTION 7: FARMING ───────────────────────────────────────────────────
story += [
    p("7. Farming — Mutated Crops &amp; Cure Crafting", H1_STYLE), hr(),
    p("NuclearCraft adds a radiation-based farming system. Mutated Seeds grow only on Radioactive "
      "Farmland (created by right-clicking dirt with a Plutonium Shovel) and produce Healing Petals — "
      "the core ingredient for radiation cure items.", BODY_STYLE),
    sp(4),
    p("Farming Steps", H2_STYLE),
    p("1. Obtain <b>Mutated Seeds</b> — dropped by Irradiated Zombies or given via <font name='Courier'>/nc farming give mutated-seed</font>.<br/>"
      "2. Create <b>Radioactive Farmland</b> — right-click any dirt/grass block with the <b>Plutonium Shovel</b>.<br/>"
      "3. Plant the seed on farmland using the <b>Plutonium Hoe</b>.<br/>"
      "4. Water as normal. Crops grow faster in higher radiation zones.<br/>"
      "5. Harvest to obtain <b>Healing Petals</b>.", BODY_STYLE),
    sp(4),
    p("Cure Crafting Recipes", H2_STYLE),
    simple_tbl([
        ["Item", "Ingredients", "Crafting Type", "Effect"],
        ["Radiation Antidote", "3× Healing Petal + 1× Honey Bottle + 1× Gold Ingot", "Crafting Table (3×3)", "Clears all radiation &amp; debuffs"],
        ["Radiation Serum",    "2× Radiation Antidote + 1× Refined Plutonium Ingot + 1× Radioactive Core", "Crafting Table (3×3)", "Clears radiation + 10 min immunity"],
    ], [34*mm, 72*mm, 24*mm, CW-130*mm]),
    sp(4),
    p("Admin commands: <font name='Courier'>/nc farming give &lt;player&gt; mutated-seed|healing-petal|radiation-antidote|radiation-serum [amount]</font>", CODE_STYLE),
    p("<font name='Courier'>/nc farming growall</font> — instantly grows all mutated crops on the server.", CODE_STYLE),
    sp(6),
    PageBreak(),
]

# ─── SECTION 8: FORGE ──────────────────────────────────────────────────────
story += [
    p("8. Nuclear Forge &amp; Equipment Upgrades", H1_STYLE), hr(),
    p("The Nuclear Forge (CMD 1401) upgrades Plutonium and Hazmat equipment from <b>MK-I</b> through "
      "<b>MK-IV</b>. Each tier increases damage, armor, radiation resistance, and unlocks new passive "
      "abilities such as the Radiation Aura.", BODY_STYLE),
    sp(4),
    p("Upgrade Tiers", H2_STYLE),
    simple_tbl([
        ["Tier", "Radioactive Cores Required", "Damage Bonus", "Armor Bonus", "Special"],
        ["MK-I",   "Base (no forge needed)", "+0",  "+0",  "Base equipment"],
        ["MK-II",  "5 Radioactive Cores",    "+15%","+5%", "Slight radiation resistance bonus"],
        ["MK-III", "15 Radioactive Cores",   "+30%","+10%","Radiation resistance +25%; Radiation Aura unlocked for armor"],
        ["MK-IV",  "40 Radioactive Cores",   "+50%","+20%","Maximum resistance; Radiation Aura at full strength; set bonus"],
    ], [14*mm, 38*mm, 22*mm, 22*mm, CW-96*mm]),
    sp(4),
    p("Radiation Aura (MK-III/IV Armor)", H2_STYLE),
    p("When wearing MK-III or MK-IV Plutonium Armor, the player emits a <b>Radiation Aura</b> that "
      "passively irradiates nearby players in PvP. Aura range and damage scale with tier. "
      "The Aura replaces the old passive aura system in Phase 9 (AdvancedAuraManager).", BODY_STYLE),
    sp(4),
    p("Forge Commands", H2_STYLE),
    p("<font name='Courier'>/nc forge give &lt;player&gt; nuclear-forge</font> — give a Nuclear Forge.<br/>"
      "<font name='Courier'>/nc forge energy set|add|clear &lt;player&gt; [amount]</font> — manage forge energy.<br/>"
      "<font name='Courier'>/nc forge upgrade mk2|mk3|mk4 &lt;player&gt;</font> — force-upgrade held item.<br/>"
      "<font name='Courier'>/nc forge stats [player]</font> — view upgrade statistics.", CODE_STYLE),
    sp(6),
    PageBreak(),
]

# ─── SECTION 9: COMBAT ─────────────────────────────────────────────────────
story += [
    p("9. Advanced Combat &amp; PvP Radiation Mechanics", H1_STYLE), hr(),
    p("Phase 9 transforms radiation from a passive hazard into an active combat weapon. "
      "Hitting players with Plutonium weapons injects radiation. Combo chains amplify radiation bursts. "
      "Weapon mastery improves damage over time. The Radiation Aura damages nearby enemies passively.", BODY_STYLE),
    sp(4),
    p("Core Combat Systems", H2_STYLE),
    simple_tbl([
        ["System", "Description"],
        ["Radiation Injection",  "Plutonium Sword/Axe hits inject radiation into the victim. Amount scales with attacker's weapon tier."],
        ["Combo System",         "Landing consecutive hits builds a combo. Higher combos amplify radiation injection (configurable multiplier)."],
        ["Critical Surge",       "After a combo threshold, a radiation surge fires — dealing bonus radiation to all nearby enemies."],
        ["Weapon Mastery",       "Each weapon type (sword, axe, bow) has its own XP track. Higher mastery = higher base radiation injection."],
        ["Plutonium Arrow",      "Fires from any bow. Injects radiation on impact. Bow mastery XP gained on kills."],
        ["Radiation Aura (PvP)", "MK-III/IV armor emits aura that irradiates nearby players continuously. Managed by AdvancedAuraManager."],
        ["Infection System",     "High-radiation hits can apply a lingering infection debuff that causes radiation to spread/linger."],
    ], [40*mm, CW-40*mm]),
    sp(4),
    p("PvP Radiation Kill Types", H2_STYLE),
    simple_tbl([
        ["Kill Type", "Tracked Stat", "Advancement"],
        ["Standard PvP kill",     "pvpKills",          "—"],
        ["Radiation kill",        "pvpRadiationKills", "RADIOACTIVE_WARRIOR (first), RADIATION_MASTER (10,000 total rad inflicted)"],
        ["Plutonium Arrow kill",  "pvpArrowKills",     "NUCLEAR_ARCHER"],
        ["Aura kill",             "pvpAuraKills",      "—"],
        ["Contamination (stage↑)","—",                 "CONTAMINATOR"],
    ], [44*mm, 34*mm, CW-78*mm]),
    sp(4),
    p("Mastery Levels", H2_STYLE),
    simple_tbl([
        ["Mastery XP", "Level", "Bonus"],
        ["0",    "Novice",   "+0% radiation injection"],
        ["500",  "Adept",    "+10% radiation injection"],
        ["1500", "Expert",   "+25% radiation injection"],
        ["3000", "Master",   "+50% radiation injection"],
        ["6000", "Grandmaster","   +80% radiation injection + Surge damage boost"],
    ], [20*mm, 28*mm, CW-48*mm]),
    sp(4),
    p("Combat Commands", H2_STYLE),
    p("<font name='Courier'>/nc combat stats [player]</font> — view PvP kill statistics.<br/>"
      "<font name='Courier'>/nc combat mastery [player]</font> — view weapon mastery levels.<br/>"
      "<font name='Courier'>/nc combat radiation [player]</font> — view radiation combat data (arrow hits, aura damage).", CODE_STYLE),
    sp(6),
    PageBreak(),
]

# ─── SECTION 10: COMMANDS ──────────────────────────────────────────────────
story += [
    p("10. Commands Reference", H1_STYLE), hr(),
    p("All commands use the <font name='Courier'>/nuclearcraft</font> or <font name='Courier'>/nc</font> alias.", BODY_STYLE),
    sp(4),
    p("General", H2_STYLE),
    simple_tbl([
        ["Command", "Permission", "Description"],
        ["/nc help",            "—",                       "Show help menu"],
        ["/nc version",         "—",                       "Display plugin version"],
        ["/nc info",            "—",                       "Show runtime info"],
        ["/nc reload",          "nuclearcraft.reload",     "Reload all configuration files"],
        ["/nc debug",           "nuclearcraft.debug",      "Toggle debug mode"],
        ["/nc radiation check", "—",                       "View your own radiation status"],
    ], [52*mm, 44*mm, CW-96*mm]),
    sp(4),
    p("Admin Subcommands", H2_STYLE),
    simple_tbl([
        ["Subcommand", "Key Arguments", "Description"],
        ["/nc give",           "&lt;player&gt; &lt;item-id&gt; [amount]",     "Give any custom item"],
        ["/nc radiation",      "add|remove|set|clear &lt;player&gt; [amount]","Manage player radiation"],
        ["/nc zombie",         "spawn irradiated|alpha &lt;player&gt;",        "Spawn zombie near player"],
        ["/nc zombie surge",   "start|stop",                                   "Toggle zombie surge event"],
        ["/nc ore spawn",      "&lt;player&gt;",                               "Spawn plutonium ore near player"],
        ["/nc ore give",       "&lt;player&gt; fragment|drill [amount]",       "Give ore-related items"],
        ["/nc smelter give",   "&lt;player&gt; [amount]",                      "Give a Nuclear Smelter"],
        ["/nc equipment give", "&lt;player&gt; &lt;slot&gt; [mk]",             "Give equipment piece"],
        ["/nc farming give",   "&lt;player&gt; &lt;item&gt; [amount]",         "Give farming items"],
        ["/nc farming growall","—",                                             "Instantly grow all crops"],
        ["/nc forge give",     "&lt;player&gt; [amount]",                      "Give a Nuclear Forge"],
        ["/nc forge energy",   "set|add|clear &lt;player&gt; [amount]",        "Manage forge energy"],
        ["/nc forge upgrade",  "mk2|mk3|mk4 &lt;player&gt;",                   "Force upgrade held item"],
        ["/nc combat stats",   "[player]",                                      "View PvP kill stats"],
        ["/nc combat mastery", "[player]",                                      "View weapon mastery"],
    ], [44*mm, 58*mm, CW-102*mm]),
    sp(6),
    PageBreak(),
]

# ─── SECTION 11: PERMISSIONS ───────────────────────────────────────────────
story += [
    p("11. Permissions", H1_STYLE), hr(),
    simple_tbl([
        ["Permission Node", "Default", "Description"],
        ["nuclearcraft.admin",            "op", "Parent node — grants all admin permissions"],
        ["nuclearcraft.admin.radiation",  "op", "Manage player radiation"],
        ["nuclearcraft.admin.zombie",     "op", "Spawn zombies, trigger surge"],
        ["nuclearcraft.admin.ore",        "op", "Spawn ore, give ore items"],
        ["nuclearcraft.admin.smelter",    "op", "Give smelters, debug smelter"],
        ["nuclearcraft.admin.equipment",  "op", "Give equipment, manage gear"],
        ["nuclearcraft.admin.farming",    "op", "Give seeds/cures, growall"],
        ["nuclearcraft.admin.forge",      "op", "Give forge, manage upgrades"],
        ["nuclearcraft.admin.combat",     "op", "View combat stats, mastery data"],
        ["nuclearcraft.give",             "op", "Use /nc give command"],
        ["nuclearcraft.reload",           "op", "Use /nc reload command"],
        ["nuclearcraft.debug",            "op", "Use /nc debug command"],
    ], [60*mm, 18*mm, CW-78*mm]),
    sp(6),
    PageBreak(),
]

# ─── SECTION 12: CONFIG FILES ─────────────────────────────────────────────
story += [
    p("12. Configuration Files", H1_STYLE), hr(),
    p("All configuration files are located in <font name='Courier'>plugins/NuclearCraft/</font>. "
      "Use <font name='Courier'>/nc reload</font> to apply changes without restarting the server.", BODY_STYLE),
    sp(4),
    simple_tbl([
        ["File", "Controls"],
        ["config.yml",    "General settings: language, debug mode, global enable flags"],
        ["radiation.yml", "Radiation gains, decay rate, stage thresholds, armor reduction, progression intervals"],
        ["zombies.yml",   "Zombie health/damage/aura, spawn weights, drop tables"],
        ["boss.yml",      "Plutonium Titan stats, arena settings, spawn conditions"],
        ["ore.yml",       "Ore generation: depth range, vein size, radiation radius, drop amounts"],
        ["smelter.yml",   "Fuel burn times, smelt speed, recipe"],
        ["toolstats.yml", "Tool/weapon damage, attack speed, durability, mining speed"],
        ["armors.yml",    "Armor values, radiation resistance per piece, set bonuses"],
        ["equipment.yml", "Equipment upgrade multipliers, aura range/damage, repair costs"],
        ["forge.yml",     "Forge energy capacity, core cost per tier, overload mechanics, upgrade recipes"],
        ["farming.yml",   "Crop growth speed, farmland creation, harvest yield, cure recipes"],
        ["combat.yml",    "PvP radiation injection amounts, combo multipliers, mastery XP thresholds, surge config, aura tick rate"],
        ["messages.yml",  "All player-facing messages (supports MiniMessage formatting)"],
    ], [34*mm, CW-34*mm]),
    sp(8),
    hr(GREEN),
    p("NuclearCraft: Plutonium Age  ·  Plugin Guide 1.0.1  ·  PaperMC 1.21+ / Java 21", SMALL_STYLE),
    p("☢  All radiation thresholds, damage values, and drop rates are fully configurable.", SMALL_STYLE),
]

# ─── BUILD ─────────────────────────────────────────────────────────────────
def on_page(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(BG_DARK)
    canvas.rect(0, 0, W, H, fill=1, stroke=0)
    # footer
    canvas.setFont("Helvetica", 7)
    canvas.setFillColor(colors.HexColor("#555555"))
    canvas.drawCentredString(W/2, 8*mm, f"NuclearCraft: Plutonium Age — Plugin Guide 1.0.1   |   Page {doc.page}")
    canvas.setStrokeColor(BORDER)
    canvas.setLineWidth(0.4)
    canvas.line(18*mm, 11*mm, W-18*mm, 11*mm)
    canvas.restoreState()

for old in ["NuclearCraft_Recipes.pdf", PDF_NAME]:
    if os.path.exists(old):
        os.remove(old)

doc.build(story, onFirstPage=on_page, onLaterPages=on_page)
print(f"✅  {PDF_NAME} created")
