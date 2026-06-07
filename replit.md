# NuclearCraft: Plutonium Age

A Paper 1.21.4 (Java 21) Minecraft plugin for a nuclear-themed survival SMP. Players mine plutonium, craft radioactive gear, fight irradiated zombies and a Titan boss, manage radiation exposure, and farm mutated crops.

---

## Build

```bash
export JAVA_HOME=/tmp/jdk-21.0.7+6
export PATH=$JAVA_HOME/bin:$PATH
cd nuclearcraft-plugin
mvn clean package -B
# Output: nuclearcraft-plugin/target/NuclearCraft-1.0.1.jar
```

JDK 16 is in the Nix environment — **do not use it**. Download Adoptium Temurin 21 to `/tmp/jdk-21.0.7+6` if missing:
```bash
curl -L "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.7%2B6/OpenJDK21U-jdk_x64_linux_hotspot_21.0.7_6.tar.gz" | tar -xz -C /tmp
```

---

## Deliverables

| File | Contents |
|---|---|
| `NuclearCraft-PlutoniumAge-1.0.1.zip` | Plugin JAR (place in `plugins/`) |
| `plutonium smp resource pack.zip` | Resource pack (host and set `resource-pack=` in `server.properties`) |

---

## Resource Pack

Path: `nuclearcraft-plugin/src/main/resources/resourcepack/`

### Item Textures (`assets/nuclearcraft/textures/item/`)
All 43 custom items have 16×16 solid pixel-art textures with opaque fills (256/256 pixels). Applied via Custom Model Data (CMD) overrides on base items.

| Category | Items |
|---|---|
| Core materials | `radioactive_core`, `raw_plutonium_fragment`, `refined_plutonium_ingot`, `mutated_seed`, `healing_petal`, `irradiated_heart`, `titan_core`, `radiation_drill`, `reactor_heart`, `ancient_reactor_blueprint`, `mutated_crystal`, `titan_fragment`, `industrial_fabric` |
| Machines | `nuclear_smelter`, `nuclear_forge`, `titan_reactor_forge` |
| Consumables | `radiation_antidote`, `radiation_serum` |
| Plutonium tools | `plutonium_sword/axe/pickaxe/shovel/hoe`, `plutonium_arrow` |
| Hazmat armor | `hazmat_helmet/chestplate/leggings/boots` |
| Plutonium armor | `plutonium_helmet/chestplate/leggings/boots` |
| Titan tech | `titan_sword/axe/pickaxe/shovel/hoe/bow/arrow`, `titan_helmet/chestplate/leggings/boots` |

### Custom Model Data (CMD) Mapping
| CMD | Item | Base material |
|---|---|---|
| 1301 | Plutonium Sword | NETHERITE_SWORD |
| 1302 | Plutonium Axe | NETHERITE_AXE |
| 1303 | Plutonium Pickaxe | NETHERITE_PICKAXE |
| 1304 | Plutonium Shovel | NETHERITE_SHOVEL |
| 1305 | Plutonium Hoe | NETHERITE_HOE |
| 1306 | Hazmat Helmet | LEATHER_HELMET |
| 1307 | Hazmat Chestplate | LEATHER_CHESTPLATE |
| 1308 | Hazmat Leggings | LEATHER_LEGGINGS |
| 1309 | Hazmat Boots | LEATHER_BOOTS |
| 1310 | Plutonium Helmet | NETHERITE_HELMET |
| 1311 | Plutonium Chestplate | NETHERITE_CHESTPLATE |
| 1312 | Plutonium Leggings | NETHERITE_LEGGINGS |
| 1313 | Plutonium Boots | NETHERITE_BOOTS |
| 1314 | Plutonium Arrow | ARROW |

### Block Textures (`assets/minecraft/textures/block/`)
- `farmland_moist_top.png` — Light green radioactive soil (overrides vanilla moist farmland top)
- `farmland_moist.png` — Light green radioactive soil sides

### Entity Textures (`assets/minecraft/textures/entity/zombie/`)
- `zombie.png` — Green-tinted irradiated zombie skin (overrides all zombies server-wide — intentional for nuclear SMP)
- `husk.png` — Toxic green husk variant

---

## Feature Implementation Status

### Radiation System
| Feature | Status | Implementation |
|---|---|---|
| Radiation stages (0–4) | ✅ Working | `RadiationManager` — 0-99 clean, 100-249 stage 1, 250-499 stage 2, 500-749 stage 3, 750-1000 stage 4 |
| Potion effects per stage | ✅ Fixed | Stage 1: Weakness+Nausea, Stage 2: +Slowness, Stage 3: stronger+Hunger+damage, Stage 4: max effects+damage |
| Glitch screen (NAUSEA) | ✅ Fixed | Was broken: re-applied every 40 ticks which reset the 300-tick warmup. Now uses 800-tick duration, only refreshed when <100 ticks remain |
| Random teleportation | ✅ Added | Stage 3: 5% chance/sec to teleport 2-5 blocks. Stage 4: 15% chance/sec to teleport 3-8 blocks. Portal particles + enderman sound |
| Skin infection glow | ✅ Added | Infected players (stage≥1) added to scoreboard team `nc_infected` (color GREEN) + `setGlowing(true)`. Visible to all players as green outline |
| Radiation actionbar | ✅ Added | Persistent HUD above hotbar: `☢ IRRADIATED ██████░░░░ [600/1000]`, color-coded by stage |
| Particle effects | ✅ Working | Stage-appropriate particles in `RadiationVisualManager` |
| Geiger counter sounds | ✅ Working | Frequency/pitch scales with stage |
| Radiation decay | ✅ Working | Slow recovery after 10 min clean |
| Radiation progression | ✅ Working | Radiation increases passively per stage |
| Armor resistance | ✅ Working | Hazmat/Plutonium armor reduces radiation intake |

### Irradiated Zombies
| Feature | Status | Implementation |
|---|---|---|
| Zombie conversion (60% natural spawns) | ✅ Working | `ZombieSpawnManager` — NATURAL, JOCKEY, MOUNT, REINFORCEMENTS, VILLAGE_DEFENSE, CHUNK_GEN |
| Zombie texture / appearance | ✅ Fixed | Green dyed leather armor per tier (Level 1: helmet, Level 2: +chest, Level 3-4: full set). All zombies also have `setGlowing(true)`. Resource pack overrides zombie.png with green-tinted skin |
| 4 zombie levels | ✅ Working | L1: basic, L2: yellow name, L3: gold name, L4 (Alpha): gradient name + neon green armor |
| Alpha zombie (Level 4) | ✅ Working | Bright toxic green armor (RGB 100,255,30), name visible above head |
| Radiation on hit | ✅ Working | `ZombieCombatManager` — amount scales with zombie level |
| Death radiation cloud | ✅ Working | `RadiationCloudManager` |
| Custom loot | ✅ Working | `ZombieLootManager` |
| Radiation Night / Surge | ✅ Working | `RadiationNightManager` + boss bar |

### Duplicate Recipe Reload Bug
| Feature | Status |
|---|---|
| Reload crash fix | ✅ Fixed | `plugin.getServer().removeRecipe(key)` called before `addRecipe()` in all 4 managers: `PlutoniumToolManager`, `PlutoniumArmorManager`, `PlutoniumArrowManager`, `ForgeRecipeManager` |

### Other Systems
| System | Status |
|---|---|
| Plutonium ore generation | ✅ Working |
| Nuclear Smelter | ✅ Working |
| Nuclear Forge | ✅ Working |
| Titan Boss fight | ✅ Working |
| Radioactive Farmland | ✅ Working (green soil texture) |
| Mutated crops & healing petal | ✅ Working |
| Radiation antidote / serum | ✅ Working |
| Titan Tech (second tier) | ✅ Working |
| Advancement system | ✅ Working |
| PvP radiation combo | ✅ Working |

---

## Known Limitations

- **Skin texture change per-player**: Minecraft's resource pack system does not support per-player skin overlays without a mod (OptiFine CIT is client-side only). The infection is shown via green scoreboard glow + action bar HUD instead.
- **Per-zombie texture**: Different zombie tiers cannot have different textures in vanilla without OptiFine. The green leather armor + glow provides visual tier differentiation instead. The resource pack overrides the global zombie.png.
- **Radioactive farmland block**: Uses `Material.FARMLAND` as base — all moist farmland on the server will have the green radioactive texture (intended behavior for nuclear SMP).

---

## Project Structure

```
nuclearcraft-plugin/
├── src/main/java/com/nuclearcraft/
│   ├── core/          NuclearCraftPlugin.java  (main entry point)
│   ├── radiation/     RadiationManager, RadiationVisualManager, ContagionManager
│   ├── zombies/       IrradiatedZombie, ZombieSpawnManager, ZombieCombatManager
│   ├── equipment/     EquipmentManager + all gear managers
│   ├── farming/       FarmingManager + mutated crop/seed/plant managers
│   ├── forge/         NuclearForgeManager, ForgeRecipeManager
│   ├── smelter/       NuclearSmelterManager
│   ├── boss/          TitanManager + combat/ability/phase managers
│   ├── titantech/     TitanTechManager + titan gear
│   ├── ore/           PlutoniumOreManager + generation/mining
│   ├── combat/        CombatManager + PvP radiation systems
│   ├── listeners/     All Bukkit event listeners
│   ├── data/          PlayerData persistence (SQLite via DatabaseManager)
│   ├── config/        ConfigManager (loads all .yml files)
│   └── resourcepack/  ModelRegistry, TextureRegistry, ResourcePackManager
├── src/main/resources/
│   ├── plugin.yml
│   ├── config.yml / radiation.yml / zombies.yml / equipment.yml / ...
│   └── resourcepack/  (complete resource pack, bundled with plugin)
└── pom.xml
```

---

## User Preferences

- Deliver both plugin JAR and resource pack as zip files
- Plugin name identifier: `NuclearCraft-PlutoniumAge` (no spaces/colons — required for plugin.yml)
- Java 21 at `/tmp/jdk-21.0.7+6` — always set JAVA_HOME before compiling
- NCLogger takes single String — use `String.format()` at call site, no varargs
- Paper 1.21 attributes: no `GENERIC_` prefix (use `Attribute.ATTACK_DAMAGE`, `MOVEMENT_SPEED`, etc.)
