---
name: Paper 1.21 Attribute names
description: Correct Attribute enum constant names for Paper/Bukkit 1.21+ — the GENERIC_ prefix was removed.
---

In Paper 1.21+, the `Attribute` enum no longer uses the `GENERIC_` prefix.
Using `GENERIC_ATTACK_DAMAGE` etc. causes **"cannot find symbol"** compile errors.

**Correct names (verified via existing Phase 6 code):**

| Old (pre-1.21)                   | New (1.21+)               |
|----------------------------------|---------------------------|
| `Attribute.GENERIC_ATTACK_DAMAGE`| `Attribute.ATTACK_DAMAGE` |
| `Attribute.GENERIC_ATTACK_SPEED` | `Attribute.ATTACK_SPEED`  |
| `Attribute.GENERIC_ARMOR`        | `Attribute.ARMOR`         |
| `Attribute.GENERIC_ARMOR_TOUGHNESS` | `Attribute.ARMOR_TOUGHNESS` |
| `Attribute.GENERIC_MAX_HEALTH`   | `Attribute.MAX_HEALTH`    |
| `Attribute.GENERIC_MOVEMENT_SPEED`| `Attribute.MOVEMENT_SPEED`|
| `Attribute.GENERIC_KNOCKBACK_RESISTANCE` | `Attribute.KNOCKBACK_RESISTANCE` |

**Why:** Paper 1.21 refactored the Attribute API to remove the redundant GENERIC_ prefix.
**How to apply:** Any time attribute modifiers are set on ItemMeta in this project, use the new names.
