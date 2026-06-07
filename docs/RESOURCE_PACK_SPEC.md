# NuclearCraft Resource Pack Specification
Version 1.0.0 | Phase 12

## Overview

The NuclearCraft resource pack provides custom models, textures, and sounds for all plugin items. The plugin works without the pack (using vanilla fallback sounds and base item textures), but the full experience requires the pack.

---

## Pack Structure

```
NuclearCraft-ResourcePack.zip
├── pack.mcmeta
├── pack.png
└── assets/
    └── nuclearcraft/
        ├── models/
        │   ├── item/
        │   │   ├── radioactive_core.json
        │   │   ├── raw_plutonium_fragment.json
        │   │   ├── refined_plutonium_ingot.json
        │   │   ├── ...
        │   └── armor/
        │       ├── plutonium_layer_1.json
        │       ├── plutonium_layer_2.json
        │       ├── hazmat_layer_1.json
        │       ├── hazmat_layer_2.json
        │       ├── titan_layer_1.json
        │       └── titan_layer_2.json
        ├── textures/
        │   ├── item/
        │   │   ├── radioactive_core.png
        │   │   ├── raw_plutonium_fragment.png
        │   │   └── ...
        │   ├── models/
        │   │   └── armor/
        │   │       ├── plutonium_layer_1.png
        │   │       ├── plutonium_layer_2.png
        │   │       └── ...
        │   └── entity/
        │       ├── irradiated_zombie.png
        │       ├── alpha_zombie.png
        │       └── plutonium_titan.png
        └── sounds/
            ├── sounds.json
            ├── radiation/
            │   ├── gain.ogg
            │   ├── stage_change.ogg
            │   ├── cure.ogg
            │   ├── surge.ogg
            │   └── death.ogg
            ├── ore/
            │   ├── discover.ogg
            │   ├── mine.ogg
            │   └── drill.ogg
            ├── machine/
            │   ├── smelter_start.ogg
            │   ├── smelter_complete.ogg
            │   ├── smelter_overheat.ogg
            │   ├── forge_start.ogg
            │   ├── forge_complete.ogg
            │   ├── forge_overload.ogg
            │   ├── titan_forge_start.ogg
            │   └── titan_forge_complete.ogg
            ├── upgrade/
            │   ├── success.ogg
            │   └── fail.ogg
            ├── combat/
            │   ├── radiation_hit.ogg
            │   └── aura_pulse.ogg
            └── titan/
                ├── spawn.ogg
                ├── death.ogg
                ├── phase_change.ogg
                ├── roar.ogg
                └── ability/
                    ├── radiation.ogg
                    ├── slam.ogg
                    ├── summon.ogg
                    └── beam.ogg
```

---

## CustomModelData Registry

All custom items use the `CustomModelData` field to override their base material texture.

| Item ID | Base Material | CustomModelData |
|---|---|---|
| radioactive-core | MAGMA_CREAM | 1101 |
| raw-plutonium-fragment | PRISMARINE_CRYSTALS | 1102 |
| refined-plutonium-ingot | ECHO_SHARD | 1103 |
| mutated-seed | WHEAT_SEEDS | 1104 |
| healing-petal | PINK_PETALS | 1105 |
| irradiated-heart | HEART_OF_THE_SEA | 1106 |
| titan-core | NETHER_STAR | 1107 |
| radiation-drill | DIAMOND_PICKAXE | 1108 |
| reactor-heart | NETHER_STAR | 1109 |
| ancient-reactor-blueprint | PAPER | 1110 |
| mutated-crystal | AMETHYST_SHARD | 1111 |
| titan-fragment | AMETHYST_SHARD | **1112** |
| industrial-fabric | YELLOW_WOOL | 1113 |
| nuclear-smelter | BLAST_FURNACE | 1201 |
| radiation-antidote | HONEY_BOTTLE | 1301 |
| radiation-serum | GLASS_BOTTLE | 1302 |
| nuclear-forge | SMITHING_TABLE | 1401 |
| titan-reactor-forge | CRYING_OBSIDIAN | 1501 |
| titan-helmet | NETHERITE_HELMET | 1502 |
| titan-chestplate | NETHERITE_CHESTPLATE | 1503 |
| titan-leggings | NETHERITE_LEGGINGS | 1504 |
| titan-boots | NETHERITE_BOOTS | 1505 |
| titan-sword | NETHERITE_SWORD | 1506 |
| titan-axe | NETHERITE_AXE | 1507 |
| titan-pickaxe | NETHERITE_PICKAXE | 1508 |
| titan-shovel | NETHERITE_SHOVEL | 1509 |
| titan-hoe | NETHERITE_HOE | 1510 |
| titan-bow | BOW | 1511 |
| titan-arrow | SPECTRAL_ARROW | 1512 |
| plutonium-sword | *(via EquipmentManager)* | 1601 |
| plutonium-axe | *(via EquipmentManager)* | 1602 |
| plutonium-pickaxe | *(via EquipmentManager)* | 1603 |
| plutonium-shovel | *(via EquipmentManager)* | 1604 |
| plutonium-hoe | *(via EquipmentManager)* | 1605 |
| plutonium-arrow | *(via EquipmentManager)* | 1606 |
| hazmat-helmet | *(via EquipmentManager)* | 1701 |
| hazmat-chestplate | *(via EquipmentManager)* | 1702 |
| hazmat-leggings | *(via EquipmentManager)* | 1703 |
| hazmat-boots | *(via EquipmentManager)* | 1704 |
| plutonium-helmet | *(via EquipmentManager)* | 1801 |
| plutonium-chestplate | *(via EquipmentManager)* | 1802 |
| plutonium-leggings | *(via EquipmentManager)* | 1803 |
| plutonium-boots | *(via EquipmentManager)* | 1804 |

> **Note:** The titan-fragment ID was corrected from 1108 to 1112 in Phase 12 to fix a duplicate with radiation-drill.

---

## sounds.json Template

```json
{
  "nuclearcraft.radiation.gain": { "sounds": [{"name": "nuclearcraft:radiation/gain"}] },
  "nuclearcraft.radiation.stage_change": { "sounds": [{"name": "nuclearcraft:radiation/stage_change"}] },
  "nuclearcraft.radiation.cure": { "sounds": [{"name": "nuclearcraft:radiation/cure"}] },
  "nuclearcraft.radiation.surge": { "sounds": [{"name": "nuclearcraft:radiation/surge"}] },
  "nuclearcraft.radiation.death": { "sounds": [{"name": "nuclearcraft:radiation/death"}] },
  "nuclearcraft.item.antidote_drink": { "sounds": [{"name": "nuclearcraft:item/antidote_drink"}] },
  "nuclearcraft.item.serum_drink": { "sounds": [{"name": "nuclearcraft:item/serum_drink"}] },
  "nuclearcraft.ore.discover": { "sounds": [{"name": "nuclearcraft:ore/discover"}] },
  "nuclearcraft.ore.mine": { "sounds": [{"name": "nuclearcraft:ore/mine"}] },
  "nuclearcraft.ore.drill": { "sounds": [{"name": "nuclearcraft:ore/drill"}] },
  "nuclearcraft.machine.smelter_start": { "sounds": [{"name": "nuclearcraft:machine/smelter_start"}] },
  "nuclearcraft.machine.smelter_complete": { "sounds": [{"name": "nuclearcraft:machine/smelter_complete"}] },
  "nuclearcraft.machine.smelter_overheat": { "sounds": [{"name": "nuclearcraft:machine/smelter_overheat"}] },
  "nuclearcraft.machine.forge_start": { "sounds": [{"name": "nuclearcraft:machine/forge_start"}] },
  "nuclearcraft.machine.forge_complete": { "sounds": [{"name": "nuclearcraft:machine/forge_complete"}] },
  "nuclearcraft.machine.forge_overload": { "sounds": [{"name": "nuclearcraft:machine/forge_overload"}] },
  "nuclearcraft.machine.titan_forge_start": { "sounds": [{"name": "nuclearcraft:machine/titan_forge_start"}] },
  "nuclearcraft.machine.titan_forge_complete": { "sounds": [{"name": "nuclearcraft:machine/titan_forge_complete"}] },
  "nuclearcraft.upgrade.success": { "sounds": [{"name": "nuclearcraft:upgrade/success"}] },
  "nuclearcraft.upgrade.fail": { "sounds": [{"name": "nuclearcraft:upgrade/fail"}] },
  "nuclearcraft.combat.radiation_hit": { "sounds": [{"name": "nuclearcraft:combat/radiation_hit"}] },
  "nuclearcraft.combat.aura_pulse": { "sounds": [{"name": "nuclearcraft:combat/aura_pulse"}] },
  "nuclearcraft.titan.spawn": { "sounds": [{"name": "nuclearcraft:titan/spawn", "volume": 2.0}] },
  "nuclearcraft.titan.death": { "sounds": [{"name": "nuclearcraft:titan/death", "volume": 2.0}] },
  "nuclearcraft.titan.phase_change": { "sounds": [{"name": "nuclearcraft:titan/phase_change", "volume": 2.0}] },
  "nuclearcraft.titan.roar": { "sounds": [{"name": "nuclearcraft:titan/roar", "volume": 2.0}] },
  "nuclearcraft.titan.ability.radiation": { "sounds": [{"name": "nuclearcraft:titan/ability/radiation"}] },
  "nuclearcraft.titan.ability.slam": { "sounds": [{"name": "nuclearcraft:titan/ability/slam"}] },
  "nuclearcraft.titan.ability.summon": { "sounds": [{"name": "nuclearcraft:titan/ability/summon"}] },
  "nuclearcraft.titan.ability.beam": { "sounds": [{"name": "nuclearcraft:titan/ability/beam"}] },
  "nuclearcraft.environment.radiation_night_start": { "sounds": [{"name": "nuclearcraft:environment/radiation_night_start"}] },
  "nuclearcraft.environment.radiation_night_end": { "sounds": [{"name": "nuclearcraft:environment/radiation_night_end"}] },
  "nuclearcraft.environment.radiation_cloud": { "sounds": [{"name": "nuclearcraft:environment/radiation_cloud"}] }
}
```
