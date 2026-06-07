# NuclearCraft: Plutonium Age — Sound Design Specification
Version 1.0.0 | Phase 12

## Overview

NuclearCraft has 31 custom sound events across 8 categories. All sounds are optional — the plugin falls back to vanilla sounds automatically when the resource pack is not present.

---

## Sound Design Philosophy

**Theme:** Industrial nuclear decay, reactor energy, ancient radioactive technology

**Key characteristics:**
- Radiation sounds: low-frequency Geiger crackle, eerie resonance
- Machine sounds: heavy mechanical, steam, industrial buzz
- Titan sounds: deep, booming, otherworldly — inspired by reactor meltdown audio
- Healing sounds: clean, crystalline, hopeful contrast to the darkness

---

## Sound Events

### Radiation (5 sounds)

| Event Key | Trigger | Design Brief |
|---|---|---|
| `nuclearcraft.radiation.gain` | Player gains radiation | Soft Geiger counter click burst. 0.5–1s. Subtle, doesn't alarm. |
| `nuclearcraft.radiation.stage_change` | Radiation stage increases | Low resonant alarm tone, distorted. 1–2s. Unsettling. |
| `nuclearcraft.radiation.cure` | Radiation cured via Antidote/Serum | Clean crystalline chime, rising pitch. 1.5s. Relief. |
| `nuclearcraft.radiation.surge` | Radiation surge event starts | Deep nuclear alarm, reverb-heavy. 3–4s. Ominous. |
| `nuclearcraft.radiation.death` | Player dies from radiation | Slow electronic flatline tone, fading. 2–3s. Melancholy. |

### Consumables (2 sounds)

| Event Key | Trigger | Design Brief |
|---|---|---|
| `nuclearcraft.item.antidote_drink` | Antidote consumed | Liquid gulp + soft green bubble pop. 1s. |
| `nuclearcraft.item.serum_drink` | Serum consumed | Thick liquid + electric crackle + rush. 1.5s. Intense. |

### Ore (3 sounds)

| Event Key | Trigger | Design Brief |
|---|---|---|
| `nuclearcraft.ore.discover` | Plutonium ore mined first time | Crystal ping + Geiger burst. 1s. Discovery feel. |
| `nuclearcraft.ore.mine` | Ore block broken | Heavy stone crack + hissing radiation release. 0.8s. |
| `nuclearcraft.ore.drill` | Radiation Drill in use | Mechanical drill whir + grind. Loopable 0.5s chunk. |

### Machines (8 sounds)

| Event Key | Trigger | Design Brief |
|---|---|---|
| `nuclearcraft.machine.smelter_start` | Smelter begins processing | Heavy furnace ignite + industrial hum startup. 1.5s. |
| `nuclearcraft.machine.smelter_complete` | Smelter finishes | Mechanical clunk + success chime. 1s. |
| `nuclearcraft.machine.smelter_overheat` | Smelter overheats | Steam explosion burst + hissing. 2s. |
| `nuclearcraft.machine.forge_start` | Forge upgrade begins | Heavy anvil land + energy charge buildup. 2s. |
| `nuclearcraft.machine.forge_complete` | Forge upgrade succeeds | Metallic success ring + energy release whoosh. 1.5s. |
| `nuclearcraft.machine.forge_overload` | Forge overloads | Explosion + electrical short. 2.5s. Alarming. |
| `nuclearcraft.machine.titan_forge_start` | Titan Forge begins crafting | Deep reactor ignition + violet energy hum. 3s. Legendary feel. |
| `nuclearcraft.machine.titan_forge_complete` | Titan Forge completes | Grand victory chord + reactor pulse + crystalline ring. 3s. Epic. |

### Upgrades (2 sounds)

| Event Key | Trigger | Design Brief |
|---|---|---|
| `nuclearcraft.upgrade.success` | Item upgraded successfully | Enchantment shimmer + power-up chime. 1.5s. Satisfying. |
| `nuclearcraft.upgrade.fail` | Upgrade fails | Brittle crack + disappointment thud. 0.8s. |

### Combat (2 sounds)

| Event Key | Trigger | Design Brief |
|---|---|---|
| `nuclearcraft.combat.radiation_hit` | Radiation weapon hits | Sizzling nuclear burn + impact distort. 0.5s. |
| `nuclearcraft.combat.aura_pulse` | Radiation aura pulses | Soft energy pulse wave, rhythmic. 0.3s. Loopable. |

### Titan Boss (10 sounds)

| Event Key | Trigger | Design Brief |
|---|---|---|
| `nuclearcraft.titan.spawn` | Titan spawns | Earth-shaking boom + nuclear alarm + roar buildup. 4–5s. Event moment. |
| `nuclearcraft.titan.death` | Titan killed | Massive implosion → silence → crystalline fallout. 5s. Epic. |
| `nuclearcraft.titan.phase_change` | Phase transition | Reactor overload crackle + pitch shift down. 3s. |
| `nuclearcraft.titan.roar` | Titan roar ability | Deep reverberant roar with metallic overtones. 3s. |
| `nuclearcraft.titan.ability.radiation` | Radiation burst ability | Geiger peak + energy shockwave. 2s. |
| `nuclearcraft.titan.ability.slam` | Ground slam | Massive impact + tremor rumble. 1.5s. |
| `nuclearcraft.titan.ability.summon` | Zombie summon | Eerie zombie chorus + reactor pulse. 2s. |
| `nuclearcraft.titan.ability.beam` | Beam attack | Laser charge up + sustained beam hum. 2.5s. |

### Environment (3 sounds)

| Event Key | Trigger | Design Brief |
|---|---|---|
| `nuclearcraft.environment.radiation_night_start` | Radiation Night begins | Slow Geiger crescendo + ominous drone. 4s. |
| `nuclearcraft.environment.radiation_night_end` | Radiation Night ends | Drone fades + brief relief tone. 2s. |
| `nuclearcraft.environment.radiation_cloud` | Radiation cloud ambient | Soft hiss + crackle loop. 1s loopable. |

---

## Audio Production Notes

- **Format:** OGG Vorbis, Mono preferred for 3D spatialization
- **Sample rate:** 44100 Hz
- **Bit depth:** 16-bit
- **Max file size:** 500 KB per sound (compressed OGG)
- **Loopable sounds:** Ensure seamless loop points (drill, aura pulse, cloud ambient)
- **Titan sounds:** Record/synthesize at 2x the normal volume — the code applies volume 2.0 multiplier at source. Do not pre-amplify.

---

## Vanilla Fallback Sounds

When no resource pack is active, these vanilla sounds play instead:

| Custom Sound | Vanilla Fallback |
|---|---|
| radiation.gain | `block.beacon.power_select` |
| radiation.stage_change | `entity.elder_guardian.curse` |
| radiation.cure | `entity.player.levelup` |
| radiation.surge | `entity.warden.sonic_boom` |
| titan.spawn | `entity.ender_dragon.growl` |
| titan.death | `entity.ender_dragon.death` |
| machine.smelter_start | `block.blastfurnace.fire_crackle` |
| upgrade.success | `block.enchantment_table.use` |

Full fallback list: see `SoundRegistry.java`.
