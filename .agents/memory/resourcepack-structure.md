---
name: Resource pack full structure
description: Complete file layout, CMD IDs, namespace conventions, and dispatch format for NuclearCraft-ResourcePack.zip (the authoritative 7.29 MB pack in src/main/resources/).
---

## Location
`nuclearcraft-plugin/src/main/resources/NuclearCraft-ResourcePack.zip`
- Compressed: 7,648,549 bytes (7.29 MB) — user refers to this as "the 8.73 MB one"
- 167 files total
- pack_format: 46, supported_formats: [34, 46]

## Top-level folder layout
```
pack.mcmeta
assets/
  minecraft/
    items/           ← MC 1.21 dispatch JSONs (range_dispatch)
    models/item/     ← legacy model JSONs (mirrors items/ but old format)
    textures/
      block/         ← farmland_moist.png, farmland_moist_top.png
      entity/zombie/ ← husk.png, zombie.png
      misc/          ← nausea.png
  nuclearcraft/
    equipment/       ← hazmat.json, plutonium.json, titan.json (worn armor layers)
    models/item/     ← 40+ custom item model JSONs
    textures/
      block/         ← plutonium_ore.png, radioactive_debris.png
      entity/equipment/humanoid/          ← hazmat.png, plutonium.png, titan.png
      entity/equipment/humanoid_leggings/ ← hazmat.png, plutonium.png, titan.png
      item/          ← all custom item PNGs (see list below)
```

## Item dispatch format (MC 1.21 — assets/minecraft/items/)
Uses `minecraft:range_dispatch` + `minecraft:custom_model_data`, NOT the old `overrides` array:
```json
{
  "model": {
    "type": "minecraft:range_dispatch",
    "property": "minecraft:custom_model_data",
    "index": 0,
    "fallback": { "type": "minecraft:model", "model": "minecraft:item/<base>" },
    "entries": [
      { "threshold": 1101.0, "model": { "type": "minecraft:model", "model": "nuclearcraft:item/<custom>" } }
    ]
  }
}
```

## Complete CMD ID → base item → custom model mapping

### Tier 1 Materials (CMD 1101–1115)
| CMD  | Base vanilla item       | Custom model           |
|------|-------------------------|------------------------|
| 1101 | magma_cream             | radioactive_core       |
| 1102 | prismarine_crystals     | raw_plutonium_fragment |
| 1103 | echo_shard              | refined_plutonium_ingot|
| 1104 | wheat_seeds             | mutated_seed           |
| 1105 | pink_petals             | healing_petal          |
| 1106 | heart_of_the_sea        | irradiated_heart       |
| 1107 | nether_star             | titan_core             |
| 1108 | diamond_pickaxe         | radiation_drill        |
| 1109 | nether_star             | reactor_heart          |
| 1110 | paper                   | ancient_reactor_blueprint |
| 1111 | amethyst_shard          | mutated_crystal        |
| 1112 | amethyst_shard          | titan_fragment         |
| 1113 | yellow_wool             | industrial_fabric      |

### Machines (CMD 1201, 1401)
| CMD  | Base vanilla item | Custom model   |
|------|-------------------|----------------|
| 1201 | blast_furnace     | nuclear_smelter|
| 1401 | smithing_table    | nuclear_forge  |

### Consumables (CMD 1301–1302 on different bases)
| CMD  | Base vanilla item | Custom model        |
|------|-------------------|---------------------|
| 1301 | honey_bottle      | radiation_antidote  |
| 1302 | glass_bottle      | radiation_serum     |

### Plutonium Tools (CMD 1301–1305)
| CMD  | Base vanilla item  | Custom model      |
|------|--------------------|-------------------|
| 1301 | netherite_sword    | plutonium_sword   |
| 1302 | netherite_axe      | plutonium_axe     |
| 1303 | netherite_pickaxe  | plutonium_pickaxe |
| 1304 | netherite_shovel   | plutonium_shovel  |
| 1305 | netherite_hoe      | plutonium_hoe     |
| 1314 | arrow              | plutonium_arrow   |

### Hazmat Armor (CMD 1306–1309)
| CMD  | Base vanilla item   | Custom model       |
|------|---------------------|--------------------|
| 1306 | leather_helmet      | hazmat_helmet      |
| 1307 | leather_chestplate  | hazmat_chestplate  |
| 1308 | leather_leggings    | hazmat_leggings    |
| 1309 | leather_boots       | hazmat_boots       |

### Plutonium Armor (CMD 1310–1313)
| CMD  | Base vanilla item      | Custom model          |
|------|------------------------|-----------------------|
| 1310 | netherite_helmet       | plutonium_helmet      |
| 1311 | netherite_chestplate   | plutonium_chestplate  |
| 1312 | netherite_leggings     | plutonium_leggings    |
| 1313 | netherite_boots        | plutonium_boots       |

### Titan Tier (CMD 1501–1512)
| CMD  | Base vanilla item      | Custom model          |
|------|------------------------|-----------------------|
| 1501 | crying_obsidian        | titan_reactor_forge   |
| 1502 | netherite_helmet       | titan_helmet          |
| 1503 | netherite_chestplate   | titan_chestplate      |
| 1504 | netherite_leggings     | titan_leggings        |
| 1505 | netherite_boots        | titan_boots           |
| 1506 | netherite_sword        | titan_sword           |
| 1507 | netherite_axe          | titan_axe             |
| 1508 | netherite_pickaxe      | titan_pickaxe         |
| 1509 | netherite_shovel       | titan_shovel (inferred)|
| 1510 | netherite_hoe          | titan_hoe (inferred)  |
| 1511 | bow                    | titan_bow             |
| 1512 | spectral_arrow         | titan_arrow           |

## Custom model JSON conventions (assets/nuclearcraft/models/item/)
- Handheld tools/weapons: `{ "parent": "minecraft:item/handheld", "textures": { "layer0": "nuclearcraft:item/<name>" } }`
- Flat items/armor/misc: `{ "parent": "minecraft:item/generated", "textures": { "layer0": "nuclearcraft:item/<name>" } }`

## Equipment definitions (assets/nuclearcraft/equipment/)
Used for worn armor appearance in MC 1.21:
```json
{ "layers": [{ "texture": "nuclearcraft:hazmat" }] }
{ "layers": [{ "texture": "nuclearcraft:plutonium" }] }
{ "layers": [{ "texture": "nuclearcraft:titan" }] }
```
Corresponding layer PNGs in `assets/nuclearcraft/textures/entity/equipment/humanoid/` and `humanoid_leggings/`.

## All custom item texture PNGs (assets/nuclearcraft/textures/item/)
ancient_reactor_blueprint, hazmat_boots, hazmat_chestplate, hazmat_helmet, hazmat_leggings,
healing_petal, industrial_fabric, irradiated_heart, mutated_crystal, mutated_seed,
nuclear_forge, nuclear_smelter, plutonium_arrow, plutonium_axe, plutonium_boots,
plutonium_chestplate, plutonium_helmet, plutonium_hoe, plutonium_leggings, plutonium_pickaxe,
plutonium_shovel, plutonium_sword, radiation_antidote, radiation_drill, radiation_serum,
radioactive_core, raw_plutonium_fragment, reactor_heart, refined_plutonium_ingot,
titan_arrow, titan_axe, titan_boots, titan_bow, titan_chestplate, titan_core,
titan_fragment, titan_helmet, titan_hoe, titan_leggings, titan_pickaxe,
titan_reactor_forge, titan_shovel, titan_sword

## Texture sizes
- Item icons: 16×16 RGBA PNG (pixel art, generated by generate_resourcepack.py)
- Armor layer textures: larger PNGs (humanoid humanoid_leggings), ~400–650 KB each
- Block textures: farmland_moist ~806 KB each; plutonium_ore ~18 KB; radioactive_debris ~22 KB
- Entity zombie skins: ~9–10 KB

## Why items/ and models/item/ both exist
The pack contains BOTH the new MC 1.21 `assets/minecraft/items/` dispatch JSONs AND legacy `assets/minecraft/models/item/` JSONs. The `items/` folder is the authoritative path for 1.21 clients.
