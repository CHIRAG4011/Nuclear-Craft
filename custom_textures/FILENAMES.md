# Custom Texture File Names

Drop your PNG files into the matching folder. Filenames must match exactly.

---

## Item Icons — 16×16 PNG
Folder: `custom_textures/assets/nuclearcraft/textures/item/`

| File name                      | In-game item              |
|-------------------------------|---------------------------|
| radioactive_core.png           | Radioactive Core          |
| raw_plutonium_fragment.png     | Raw Plutonium Fragment    |
| refined_plutonium_ingot.png    | Refined Plutonium Ingot   |
| mutated_seed.png               | Mutated Seed              |
| healing_petal.png              | Healing Petal             |
| irradiated_heart.png           | Irradiated Heart          |
| titan_core.png                 | Titan Core                |
| titan_fragment.png             | Titan Fragment            |
| mutated_crystal.png            | Mutated Crystal           |
| reactor_heart.png              | Reactor Heart             |
| ancient_reactor_blueprint.png  | Ancient Reactor Blueprint |
| industrial_fabric.png          | Industrial Fabric         |
| radiation_drill.png            | Radiation Drill           |
| plutonium_sword.png            | Plutonium Sword           |
| plutonium_axe.png              | Plutonium Axe             |
| plutonium_pickaxe.png          | Plutonium Pickaxe         |
| plutonium_shovel.png           | Plutonium Shovel          |
| plutonium_hoe.png              | Plutonium Hoe             |
| plutonium_arrow.png            | Plutonium Arrow           |
| hazmat_helmet.png              | Hazmat Helmet             |
| hazmat_chestplate.png          | Hazmat Chestplate         |
| hazmat_leggings.png            | Hazmat Leggings           |
| hazmat_boots.png               | Hazmat Boots              |
| plutonium_helmet.png           | Plutonium Helmet          |
| plutonium_chestplate.png       | Plutonium Chestplate      |
| plutonium_leggings.png         | Plutonium Leggings        |
| plutonium_boots.png            | Plutonium Boots           |
| radiation_antidote.png         | Radiation Antidote        |
| radiation_serum.png            | Radiation Serum           |
| nuclear_smelter.png            | Nuclear Smelter (item)    |
| nuclear_forge.png              | Nuclear Forge (item)      |
| titan_reactor_forge.png        | Titan Reactor Forge       |
| titan_helmet.png               | Titan Helmet              |
| titan_chestplate.png           | Titan Chestplate          |
| titan_leggings.png             | Titan Leggings            |
| titan_boots.png                | Titan Boots               |
| titan_sword.png                | Titan Sword               |
| titan_axe.png                  | Titan Axe                 |
| titan_pickaxe.png              | Titan Pickaxe             |
| titan_shovel.png               | Titan Shovel              |
| titan_hoe.png                  | Titan Hoe                 |
| titan_bow.png                  | Titan Bow                 |
| titan_arrow.png                | Titan Arrow               |

---

## Worn Armor Skins — larger PNG (Minecraft armor sheet layout)
Folder: `custom_textures/assets/nuclearcraft/textures/entity/equipment/humanoid/`

| File name       | Covers                          |
|-----------------|---------------------------------|
| hazmat.png      | Hazmat helmet, chestplate, boots|
| plutonium.png   | Plutonium helmet, chestplate, boots|
| titan.png       | Titan helmet, chestplate, boots |

Folder: `custom_textures/assets/nuclearcraft/textures/entity/equipment/humanoid_leggings/`

| File name       | Covers           |
|-----------------|------------------|
| hazmat.png      | Hazmat leggings  |
| plutonium.png   | Plutonium leggings|
| titan.png       | Titan leggings   |

---

## Block Textures — 16×16 PNG
Folder: `custom_textures/assets/nuclearcraft/textures/block/`

| File name              | Block              |
|------------------------|--------------------|
| plutonium_ore.png      | Plutonium Ore      |
| radioactive_debris.png | Radioactive Debris |

---

## Zombie / Entity Skins
Folder: `custom_textures/assets/minecraft/textures/entity/zombie/`

| File name    | Entity              |
|--------------|---------------------|
| zombie.png   | Irradiated Zombie   |
| husk.png     | Irradiated Husk     |

---

## After uploading

Run this once to rebuild the zip:
```
python3 rebuild_resourcepack.py
```

Then re-host the zip and update `resourcepack.yml` with the new SHA-1 hash.
