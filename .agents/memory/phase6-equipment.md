---
name: Phase 6 equipment architecture
description: Key patterns and wiring decisions for the Phase 6 Plutonium Equipment system
---

# Phase 6 Equipment System

## Wiring pattern
- `EquipmentManager` is created and initialized in `NuclearCraftPlugin.initializeManagers()` after Phase 5.
- Its `initialize()` call internally wires `RadiationResistanceManager` into `RadiationManager` via `setResistanceManager()`.
- This means the resistance pipeline is activated automatically — no separate call in NuclearCraftPlugin needed.

## Plugin injection for PDC reads
- `WeaponEffectManager` and `EquipmentListener` both need a `JavaPlugin` instance to construct the `NamespacedKey` used for PDC lookups.
- Pass the plugin at construction time; do NOT use `Bukkit.getPluginManager().getPlugin("NuclearCraft")` — that's fragile and breaks the dependency graph.
- The PDC constant is `CustomItem.PDC_KEY_ID` (static field on CustomItem).

## Recipe validation pattern
- Plutonium tools/armor use `ECHO_SHARD` as the crafting ingredient material (because `refined-plutonium-ingot` item IS an ECHO_SHARD).
- `PrepareItemCraftEvent` in `EquipmentListener` scans matrix slots for ECHO_SHARD and validates each via PDC — if any ECHO_SHARD slot is NOT a refined-plutonium-ingot, the result is cleared to AIR.

## PlayerData Phase 6 fields
Six new stats fields (all int, default 0): swordHits, radiationDamageInflicted, blocksConverted, farmlandCreated, debrisGenerated, arrowsFired.
The full constructor appended these after bossKills/unlockedUpgrades — DatabaseManager loadPlayerData() and savePlayerData() must match this order exactly.

## DB migration
Phase 6 columns are added via `addColumnIfMissing()` in `runMigrations()` — safe for existing databases.
The UPSERT SQL uses 35 bound parameters (including all 6 Phase 6 fields).

**Why:** Keeping migration additive prevents data loss when updating from Phase 5 databases.
