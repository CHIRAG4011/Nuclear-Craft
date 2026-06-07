# NuclearCraft: Plutonium Age — Balance Report
Version 1.0.0 | Phase 12

## Overview

All balance values are centralized in `balance.yml` and read at runtime via `BalanceManager`. No recompilation is needed to adjust values — use `/nc reload` after editing.

---

## Radiation System

| Value | Default | Notes |
|---|---|---|
| Decay rate / tick | 0.05 | At 20 TPS = 1 point/second decay |
| Ore mining gain | 25 pts | Per break without Radiation Drill |
| Zombie hit gain | 15 pts | Per irradiated zombie hit |
| Contagion rate | 5 pts/tick | Spreads to nearby unprotected players |
| Stage 1 threshold | 100 pts | ~100 ore breaks to reach |
| Stage 5 threshold | 950 pts | Near-lethal zone |

**Decay analysis:** At stage 3 (500 pts) with no new exposure, decay takes ~10,000 ticks (8.3 minutes). An Antidote clears instantly.

**Progression feel:** A new player mining ore bare-handed hits Stage 1 after ~4 breaks, Stage 2 after ~12, and Stage 3 after ~20. This is intentionally punishing to incentivize crafting the Radiation Drill early.

---

## Zombie Difficulty

| Zombie Type | HP | Damage | Notes |
|---|---|---|---|
| Irradiated | 30 HP | 6 dmg | ~1.5x standard zombie |
| Alpha | 80 HP | 12 dmg | Mini-boss level |

**Spawn chance:** 10% of natural zombie spawns are replaced. This gives a consistent but not overwhelming presence.

**Radiation Night multiplier:** 3x spawn chance = ~30% of spawns are irradiated. Combined with Alpha Zombies appearing, nights become genuinely dangerous.

---

## Ore Economy

| Value | Default | Notes |
|---|---|---|
| Vein size | 3 blocks | Small and rare |
| Veins per chunk | 1 | Very sparse |
| Depth range | Y -64 to -20 | Deep mining required |

**Intent:** Plutonium should feel like a premium material. Players need to invest in proper tools (Radiation Drill) before they can farm efficiently.

---

## Machine Costs

| Machine | Process Time | Fuel: Coal | Fuel: Blaze Rod | Fuel: Lava |
|---|---|---|---|---|
| Nuclear Smelter | 10s (200 ticks) | 800 ticks | 1200 ticks | 2000 ticks |

**Forge Energy:**

| Tier | Energy Cost | Success % | Expected Cores per Success |
|---|---|---|---|
| MK-I | 100 | 90% | ~1.1 |
| MK-II | 250 | 75% | ~1.3 |
| MK-III | 500 | 60% | ~1.7 |
| MK-IV | 1000 | 40% | ~2.5 |

The decreasing success chance creates a meaningful risk/reward curve. MK-IV items require significant investment.

---

## Equipment Balance

| Set | Radiation Resistance | Combat Role |
|---|---|---|
| No armor | 0% | None |
| Hazmat | 75% | Exploration / machine work |
| Plutonium | 50% | Combat focus (more combat stats) |
| Titan (full set) | 100% | Endgame — total immunity |

Hazmat has higher radiation resistance but lower combat stats. Plutonium armor sacrifices some protection for stronger offense. This creates a meaningful choice.

---

## Combat

| Value | Default | Notes |
|---|---|---|
| Radiation damage multiplier | 0.5 | 500 rad → +250 bonus PvP damage |
| Infection chance per hit | 10% | 1 in 10 hits spreads radiation |
| AOE radius | 4 blocks | Titan weapon AOE |

At max radiation (1000 pts), a player deals +500 bonus damage — effectively a one-shot multiplier. This rewards the high-risk high-radiation playstyle.

---

## Boss — Plutonium Titan

| Value | Default | Notes |
|---|---|---|
| Base health | 800 HP | ~40x iron golem |
| Radiation aura radius | 8 blocks | Constant exposure in melee |
| Radiation aura gain | 10 pts/tick | Stage 3 in ~50 ticks near Titan |
| Ability cooldown | 100 ticks | ~5 seconds |
| Max summons | 6 | Alpha zombies |
| Respawn cooldown | 60 minutes | Economy protection |

**Phase transitions:** 75% / 50% / 25% HP — each phase adds new abilities, making the later phases significantly more dangerous.

**Required preparation:** Players should arrive with Hazmat or Plutonium armor, a supply of Antidotes or a Serum, and maximum weapon mastery for efficient damage output.

---

## Economy Guards

| Guard | Default | Purpose |
|---|---|---|
| Boss respawn cooldown | 60 min | Prevents Titan loot farming |
| AFK farming timeout | 300 sec | Prevents AFK crop automation |
| Max machines per player | 5 | Prevents machine TPS abuse |

---

## Balancing Recommendations for Large SMPs

For 50+ concurrent players, consider:
- Increase `ore.veins-per-chunk` to 2–3 (more supply for more players)
- Increase `boss.titan.base-health` to 1200–1600 (requires larger raid groups)
- Decrease `zombies.spawn-chance` to 0.05 (reduce mob pressure)
- Increase `economy.boss-respawn-cooldown-minutes` to 120–180

For hardcore/competitive servers:
- Decrease `radiation.decay-rate-per-tick` to 0.01 (radiation lingers much longer)
- Increase `machines.forge.energy-cost.mk4` to 2000
- Decrease `machines.forge.success-chance.mk4` to 0.25
