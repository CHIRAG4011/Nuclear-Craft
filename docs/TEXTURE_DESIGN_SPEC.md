# NuclearCraft: Plutonium Age — Texture Design Specification
Version 1.0.0 | Phase 12

## Overview

All NuclearCraft custom items require textures. This document provides the design brief for each item's artist. Technical texture spec is `16x16 px PNG` for items and `64x32 px PNG` for armor layers.

---

## Global Visual Language

| Element | Value |
|---|---|
| Primary Radiation Color | Neon Green `#39FF14` |
| Secondary Energy Color | Electric Violet `#7700FF` |
| Warning Color | Hazard Yellow `#FFE000` |
| Reactor Heat Color | Orange `#FF6600` |
| Cure / Heal Color | Cyan `#00FFEE` |
| Metal Base | Gunmetal Grey `#3A3A3A` |
| Dark Alloy | Near-Black `#0A0A0A` |

---

## Item Textures (16x16)

### Radioactive Core
- **Base:** Rounded crystal form, mostly dark
- **Colors:** Neon green (#39FF14), electric yellow (#FFE000), black (#0A0A0A)
- **Glow:** Core center glows brightest, fault lines radiate outward
- **Animation:** Pulsing brightness on inner core (use emissive layer)
- **Model:** 3D sphere with floating shard elements (optional but preferred)

### Raw Plutonium Fragment
- **Base:** Jagged irregular shard
- **Colors:** Toxic green (#39FF14), dark forest green (#1A4A00), charcoal (#1C1C1C)
- **Glow:** Inner crystal core
- **Animation:** None (static)

### Refined Plutonium Ingot
- **Base:** Standard ingot shape
- **Colors:** Metallic dark grey (#3A3A3A), neon green channel (#39FF14), chrome (#C0C0C0)
- **Glow:** Green channel running down center (emissive strip)
- **Animation:** None (channel is static glow)

### Mutated Seed
- **Base:** Oval seed shape
- **Colors:** Dark brown (#2A1A00), sickly green veins (#7FFF00), pale yellow (#E8D5A3)
- **Glow:** Vein intersections (subtle)
- **Animation:** Very slow vein pulse (optional)

### Healing Petal
- **Base:** Single curved petal with vein details
- **Colors:** Soft blue (#88CCFF), gentle cyan (#00FFEE), white (#FFFFFF)
- **Glow:** Petal center and tip
- **Animation:** Gentle breathing glow

### Irradiated Heart
- **Base:** Heart shape
- **Colors:** Dark red (#8B0000), pulsing green (#39FF14), black (#000000)
- **Glow:** Green corruption tendrils radiating from center
- **Animation:** Heartbeat pulse

### Titan Core
- **Base:** Multi-faceted crystal
- **Colors:** Deep violet (#7700FF), neon green (#39FF14), white core (#FFFFFF)
- **Glow:** White core pulses, facets shimmer
- **Animation:** Energy pulse + orbiting shards (3D model preferred)

### Radiation Drill
- **Base:** Drill head with spiral bit
- **Colors:** Gunmetal grey (#4A4A4A), neon green stripe (#39FF14), orange heat (#FF6600)
- **Glow:** Drill tip orange-hot, safety stripe green
- **Animation:** Idle bit rotation, stripe flicker

### Titan Fragment
- **Base:** Angular armor plate shard
- **Colors:** Void black (#0A0A0A), electric violet (#7700FF), cracked gold (#FFD700)
- **Glow:** Crack lines pulse violet-to-gold
- **Animation:** Crack glow cycle

### Reactor Heart
- **Base:** Stylized reactor sphere
- **Colors:** Deep red (#CC0000), nuclear green (#39FF14), black (#000000)
- **Glow:** Surface veins, entire body low-glow
- **Animation:** Reactor beat pulse

### Mutated Crystal
- **Base:** Tall jagged cluster
- **Colors:** Aqua (#00FFCC), deep violet (#7700FF), white (#FFFFFF)
- **Glow:** Tips glow white, base violet
- **Animation:** Color gradient shift tip-to-base

### Nuclear Smelter (Block Item)
- **Base:** Squat industrial furnace
- **Colors:** Dark iron (#2A2A2A), hazard yellow stripes (#FFE000), reactor window green (#39FF14)
- **Glow:** Reactor window, warning stripes
- **Animation:** Reactor window pulse

### Nuclear Forge (Block Item)
- **Base:** Smithing table with reactor conduits
- **Colors:** Dark metal (#1A1A1A), energy orange (#FF8800), power green (#39FF14)
- **Glow:** Conduit lines, power indicators
- **Animation:** Conduit flow

### Titan Reactor Forge (Block Item)
- **Base:** Dense cubic machine with energy circuits
- **Colors:** Crying obsidian purple (#6A0DAD), void black (#000000), reactor green (#39FF14)
- **Glow:** Circuit lines green, core purple pulse
- **Animation:** Circuit path energy flow

### Radiation Antidote
- **Base:** Honey bottle shape
- **Colors:** Honey amber (#FFB300), clear glass (#CCFFEE), green liquid (#39FF14)
- **Glow:** Liquid inside glows softly
- **Animation:** Liquid shimmer

### Radiation Serum
- **Base:** Thin glass vial
- **Colors:** Clear glass (#E8F8FF), vivid violet (#7700FF), neon green (#39FF14)
- **Glow:** Both liquid layers and stopper
- **Animation:** Liquid ripple + bubble

---

## Armor Textures (64x32)

### Hazmat Armor (layer_1, layer_2)
- **Theme:** Full-body sealed protective suit
- **Colors:** Safety yellow (#FFE000), white suit (#F0F0F0), dark visor (#1A1A1A)
- **Detail:** Chemical-resistant shell texture, helmet visor with green HUD glow
- **Animation:** Subtle visor glow flicker

### Plutonium Armor (layer_1, layer_2)
- **Theme:** Military nuclear technology
- **Colors:** Matte black alloy (#0A0A0A), nuclear green (#39FF14), reactor orange (#FF6600)
- **Detail:** Heavy segmented plates, green energy conduit channels between segments
- **Animation:** Conduit channels glow; reactor ports pulse orange

### Titan Armor (layer_1, layer_2)
- **Theme:** Ancient reactor civilization — legendary
- **Colors:** Void black (#000000), reactor violet (#7700FF), nuclear green (#39FF14), cracked gold (#FFD700)
- **Detail:** Floating energy panel inserts, runic circuit engravings across surface
- **Animation:** Floating panels orbit; rune glow pulse; cracks glow gold
- **Note:** This is the endgame prestige armor — it should look significantly more elaborate than all other sets

---

## Mob Textures

### Irradiated Zombie (64x64)
- **Base:** Standard zombie texture modified
- **Additions:** Green glowing eyes, radiation burn marks on skin, partial hazmat suit remnants, mutated flesh patches
- **Key detail:** Radiation burns visible on exposed skin areas

### Alpha Irradiated Zombie (64x64)
- **Base:** Larger zombie proportions
- **Additions:** Brighter green glow overall, more extensive mutations, exposed bone in areas, reactor fragment embedded in chest
- **Scale hint:** Use model scaling (larger), not just texture size change

### Plutonium Titan (custom entity model)
- **Theme:** Massive reactor creature, partially a walking reactor core
- **Key features:**
  - Exposed glowing reactor core in chest cavity
  - Broken ancient armor plating covering body segments
  - Radiation crystals growing from shoulders and back
  - Phase-dependent glow intensity (dim in Phase 1, blinding in Phase 4)
- **Colors:** Gunmetal armor (#4A4A4A), reactor green core (#39FF14), void black void (#000000), violet energy cracks
- **Note:** The Titan uses the Giant entity base — custom model requires a resource pack model override for giants

---

## Emissive Textures

For animated/glowing effects, provide separate emissive maps (suffix: `_e.png`) where:
- Full white `#FFFFFF` = maximum glow
- Black `#000000` = no glow
- Greyscale = partial glow

Required emissive maps: radioactive_core, refined_plutonium_ingot, titan_core, titan_fragment, reactor_heart, plutonium armor conduits, titan armor runes.
