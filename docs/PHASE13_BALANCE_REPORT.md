# NuclearCraft: Plutonium Age — Phase 13 Balance Report
Version 1.1.0 | Economy, Balancing & Progression Pass

---

## Overview

Phase 13 is a dedicated balance pass. No new content was added. Six new balance sub-managers were created:

| Manager | Role |
|---|---|
| `LootBalanceManager` | Centralized loot drop rates for zombies, titan, and ore |
| `EconomyBalanceManager` | Boss cooldowns, AFK detection, machine limits, exploit prevention |
| `RadiationBalanceManager` | Radiation gain caps, contagion gating, post-cure grace |
| `CombatBalanceManager` | PvP radiation damage caps, infection stack limits |
| `BossBalanceManager` | Titan health scaling, contribution tiers, reward multipliers |
| `ProgressionBalanceManager` | Milestone tracking, progression timing, rush detection |

All values are in `balance.yml` and adjustable without recompilation.

---

## 1. Loot Economy

### Radioactive Core
**Target:** ~1 per 15–20 kills at Level 1

| Zombie Level | Core Chance | Expected Kills per Core |
|---|---|---|
| 1 (common) | 6% | ~17 |
| 2 (strong) | 10% | ~10 |
| 3 (alpha) | 15% | ~7 |

**Rationale:** The Radiation Drill requires Radioactive Cores. A player should need to kill approximately 20–30 zombies to craft their first drill — enough to make it feel earned without requiring hours of grinding.

### Mutated Seed
**Target:** ~1 per 12–18 kills at Level 1

| Zombie Level | Seed Chance | Expected Kills per Seed |
|---|---|---|
| 1 | 7% | ~14 |
| 2 | 9% | ~11 |
| 3 | 12% | ~8 |

**Rationale:** Seeds are needed for farming, which is mid-game. Slightly more common than cores to allow farms to be established without excessive grinding.

### Irradiated Heart
**Target:** ~1 per 100–150 kills

| Zombie Level | Heart Chance | Expected Kills per Heart |
|---|---|---|
| 1 | 0.8% | ~125 |
| 2 | 1.0% | ~100 |
| 3 | 1.5% | ~67 |

**Rationale:** Hearts are rare progression items. They should feel like genuine treasures. Alpha zombies reward persistent hunters.

### Radiation Night (Surge) Modifier
During a Radiation Night, all loot chances are doubled (capped at 100%). This makes Radiation Night genuinely rewarding for players who brave it, instead of just being a threat.

---

## 2. Titan Rewards

| Drop | Amount/Chance | Notes |
|---|---|---|
| Titan Core | Guaranteed | Summoning material |
| Reactor Heart | Guaranteed | Required for Titan Tech |
| Titan Fragments | 8–16 | Uniform random |
| Ancient Blueprint | 70% | Recipe unlock |
| Mutated Crystal | 50% | Crafting material |

### Contribution Tiers

| Tier | Damage Share Threshold | Loot Multiplier |
|---|---|---|
| Tier 1 (Top) | ≥ 40% of total damage | ×1.5 |
| Tier 2 (Mid) | ≥ 20% of total damage | ×1.2 |
| Tier 3 (Participant) | ≥ 5% of total damage | ×1.0 |

**Top contributor bonus:** +2 extra Titan Fragments per Tier 1/2 contributor.

### Multiplayer Health Scaling

| Players | Titan HP |
|---|---|
| 1 | 800 |
| 2 | 900 |
| 3 | 1,000 |
| 5 | 1,200 |
| 10 | 1,700 |

Health per extra player: **+100 HP** (configurable). Cap at 10 players.

**Solo viability:** Solo Titan at 800 HP is possible but very difficult. Full Plutonium MK-IV gear + Serum + Antidotes + Radiation Drill are essentially required.

---

## 3. Plutonium Ore

| Fortune Level | Fragments per Block |
|---|---|
| None | 2 |
| Fortune I | 3 |
| Fortune II | 4 |
| Fortune III | 5 |

**Ore rarity target:** More valuable than diamonds, less rare than Ancient Debris. One vein (3 blocks) with Fortune III yields ~15 fragments — enough for one Smelter processing run. This keeps the Smelter as a consistent midgame investment rather than a trivially-filled machine.

---

## 4. Radiation System

### Per-Event Cap
Maximum radiation from a single event: **100 pts**. Prevents instantaneous kills from radiation spikes (e.g. running through a dense ore field).

### Contagion Gating
Contagion spreads to nearby players every 40 ticks minimum. Without this gate, contagion could apply 20 times per second in close-quarters combat — unplayable.

### Post-Cure Grace Period
After consuming a cure, radiation cannot rise above **50 pts** for **600 ticks (30 seconds)**. This prevents the frustrating loop where a player cures themselves and is immediately re-infected to lethal levels.

### Stage Timing Analysis (with default decay 0.05/tick)

At 20 TPS, decay = 1 pt/second. From Stage 3 (500 pts) with no new exposure, full decay takes **~8.3 minutes**. An Antidote clears it instantly.

The decay rate is intentionally slow to create meaningful decisions. Players cannot simply "walk off" dangerous radiation.

---

## 5. PvP & Combat

### Radiation Damage Cap
Max radiation bonus per hit: **15 HP**. At 1000 rad with 0.5 multiplier, the raw bonus would be 500 — clearly game-breaking. The cap ensures radiation adds strategy without replacing combat skill.

### Infection Stack Limit
Max infections per fight per player: **5**. After 5 hits, the attacker cannot apply more infection to the same target until they die. Prevents radiation stacking as an unavoidable kill.

### Contagion Range
Contagion spreads within **5 blocks**. This requires meaningful proximity — a player can safely observe combat from a distance without being infected.

### Aura Pulse Cap
Maximum radiation per aura pulse: **20 pts**. At 40-tick intervals, that's a maximum of **10 pts/second** from aura weapons in sustained combat — dangerous, but giving the victim time to retreat.

---

## 6. Exploit Prevention

### Boss Farming
**Cooldown:** 60 minutes between Titan kills per player. Even if the server spawns a second Titan immediately, players on cooldown cannot collect rewards.

### AFK Farming
**Detection:** Players must interact with blocks every **300 seconds** or they're flagged as AFK. AFK players stop receiving loot from automatic farm systems.

**Mitigation:** Toxic Bloom spreading continues to affect AFK players — they are still at risk. Only automated loot granting is blocked, not natural game damage.

### Machine Abuse
- **Per-player cap:** Max 5 machines (configurable)
- **Placement cooldown:** 5 seconds between placements
- At 5 machines × 0.5 rad/tick each = 2.5 rad/tick total environmental output. With 10 max machines this would be 5 rad/tick — significant but not overwhelming.

### Duplication Prevention
NuclearCraft custom items carry a `NamespacedKey` PDC stamp. The `duplicate-detection` setting enables checking for suspicious item quantities. Server-side enforcement relies on the per-player machine cap and boss cooldown rather than client-side validation.

---

## 7. Progression Timing (Default Settings)

| Milestone | Min Days | Max Days | Notes |
|---|---|---|---|
| First Irradiated Zombie | 1 | 1 | Day 1 encounter |
| First Radioactive Core | 2 | 3 | From zombie kills |
| First Plutonium Ore | 4 | 7 | Requires Radiation Drill |
| First Radiation Drill | 4 | 7 | Requires ~20 cores |
| First Nuclear Smelter | 7 | 14 | Requires cores + materials |
| First Plutonium Equipment | 10 | 18 | Requires smelter output |
| First Radioactive Farm | 12 | 20 | Requires Mutated Seeds |
| First MK-I Upgrade | 15 | 25 | Requires Forge + energy |
| First Titan Summon | 20 | 35 | Requires Titan Core |
| First Titan Kill | 25 | 40 | Requires full preparation |
| First Titan Equipment | 35 | 50 | Requires Titan Forge |

"Days" = 8 active hours/day on a survival multiplayer server.

Rush detection alerts admins when a player reaches a milestone in less than 50% of the expected minimum time.

---

## 8. Recommended Settings by Server Type

### Small SMP (2–10 players)
Use defaults. The economy is balanced for this case.

### Large Public Server (50+ players)
```yaml
# balance.yml
ore:
  veins-per-chunk: 2          # More supply for more players

loot:
  zombie:
    level-1:
      radioactive-core-chance: 0.08   # Slightly more generous

boss:
  titan:
    base-health: 1200.0               # Requires larger groups
    boss-respawn-cooldown-minutes: 120
    health-per-extra-player: 80.0

economy:
  max-machines-per-player: 3          # Reduce TPS load
```

### Hardcore / Competitive
```yaml
radiation:
  decay-rate-per-tick: 0.02           # Radiation lingers much longer
  max-gain-per-event: 75.0            # Slightly lower cap — more punishing
  post-cure-grace-ticks: 200          # Shorter grace period

combat:
  max-radiation-damage-bonus: 20.0    # Slightly higher PvP lethality

loot:
  zombie:
    level-1:
      radioactive-core-chance: 0.04   # Rarer cores — longer grind
      irradiated-heart-chance: 0.005  # Rarer hearts

boss:
  titan:
    base-health: 1600.0
    ability-cooldown-ticks: 60        # More frequent abilities
```

### Casual / Beginner-Friendly
```yaml
radiation:
  decay-rate-per-tick: 0.10           # Faster recovery
  zombie-hit-gain: 8.0                # Less damage per hit
  post-cure-grace-ticks: 1200         # 1 minute grace period

loot:
  zombie:
    level-1:
      radioactive-core-chance: 0.10   # More frequent drops

economy:
  boss-respawn-cooldown-minutes: 30   # Faster boss respawn
```
