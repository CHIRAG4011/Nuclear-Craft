---
name: Phase 8 Nuclear Forge architecture
description: Key design decisions for Phase 8 Nuclear Forge and MK upgrade system.
---

## Package layout
- `com.nuclearcraft.forge` — ForgeState, ForgeData, NuclearForgeManager, NuclearForgeGUI, ForgeRecipeManager
- `com.nuclearcraft.upgrade` — UpgradeTier, UpgradeManager
- `com.nuclearcraft.equipment` — RadiationAuraManager (added to existing package)
- `com.nuclearcraft.events` — ForgeUpgradeStartEvent, ForgeUpgradeSuccessEvent, ForgeUpgradeFailEvent, ForgeOverloadEvent
- `com.nuclearcraft.listeners` — ForgeListener (added)

## PDC key
- Upgrade tier stored as INTEGER under `nuclearcraft:upgrade_tier`
- Item identity: `nuclearcraft:nuclearcraft_item_id` = "nuclear-forge"
- Nuclear Forge item: base `Material.SMITHING_TABLE`, CMD 1401

## forge.yml location
- `src/main/resources/forge.yml` (auto-copied by saveResource)
- ConfigManager.ConfigFile.FORGE added; access via `configManager.getForge()`
- Tier config keys: `upgrades.mk1`, `upgrades.mk2`, `upgrades.mk3`, `upgrades.mk4`

## Overload cooldown Long cast
- `overloadCooldowns` is `Map<String, Long>`; `getCurrentTick()` returns `int`
- Must cast: `(long) plugin.getServer().getCurrentTick() + overloadShutdownTicks`
- Without the cast, compiler error: "int cannot be converted to java.lang.Long"

## RadiationSource additions
- `NUCLEAR_FORGE` — overload radiation burst
- `EQUIPMENT_AURA` — MK-IV aura PvP radiation

## GUI slots (54-slot, 6 rows)
- EQUIPMENT=10, MATERIAL=13, CATALYST=16
- PROGRESS=22, PREVIEW=28, STATUS=31, ENERGY=34
- FORGE_BTN=47, INFO=49, OUTPUT=51
- All others = black glass border

## NuclearCraftCommand wiring
- Added `nuclearForgeManager` and `upgradeManager` as final fields + constructor params
- New subcommand: `forge give|energy set/add/clear|upgrade <tier>|stats`
- Permission: `nuclearcraft.admin.forge`

## Advancement keys (Phase 8)
- MASTER_BLACKSMITH, ENHANCED_ARSENAL, NUCLEAR_ENGINEER, PERFECTED_TECHNOLOGY
