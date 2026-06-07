# NuclearCraft: Plutonium Age — Admin Guide
Version 1.0.0 | Phase 12

## Overview

NuclearCraft is a nuclear-themed PaperMC 1.21+ plugin featuring radiation mechanics, custom mobs, industry machines, endgame boss content, and Titan-tier equipment across 12 development phases.

---

## Installation

1. Place `NuclearCraft-1.0.0.jar` in your server's `plugins/` folder.
2. Start the server once to generate all configuration files in `plugins/NuclearCraft/`.
3. Configure `config.yml` (database type, world restrictions).
4. Restart the server.

See `INSTALLATION_GUIDE.md` for full setup instructions.

---

## Command Reference

### Root Command
```
/nuclearcraft   (aliases: /nc, /nuclear)
```

### Admin Subcommands

| Command | Permission | Description |
|---|---|---|
| `/nc reload` | `nuclearcraft.reload` | Hot-reload all config files |
| `/nc debug` | `nuclearcraft.debug` | Toggle debug mode |
| `/nc debug <system>` | `nuclearcraft.debug` | Diagnose a specific system |
| `/nc give <player> <item>` | `nuclearcraft.give` | Give a custom item |
| `/nc radiation set <player> <amount>` | `nuclearcraft.admin.radiation` | Set player radiation |
| `/nc radiation add <player> <amount>` | `nuclearcraft.admin.radiation` | Add radiation |
| `/nc radiation clear <player>` | `nuclearcraft.admin.radiation` | Clear all radiation |
| `/nc zombie spawn irradiated\|alpha` | `nuclearcraft.admin.zombies` | Spawn a zombie |
| `/nc zombie surge start\|stop` | `nuclearcraft.admin.zombies` | Control Radiation Night |
| `/nc ore spawn plutonium` | `nuclearcraft.admin.ore` | Spawn ore at location |
| `/nc ore give fragment [n]` | `nuclearcraft.admin.ore` | Give fragments |
| `/nc smelter give [player]` | `nuclearcraft.admin.smelter` | Give a Nuclear Smelter |
| `/nc smelter debug` | `nuclearcraft.admin.smelter` | List active machines |
| `/nc equipment give <type>` | `nuclearcraft.admin.equipment` | Give equipment item |
| `/nc farming give <type> [n]` | `nuclearcraft.admin.farming` | Give farming item |
| `/nc farming growall [radius]` | `nuclearcraft.admin.farming` | Force-grow crops |
| `/nc forge energy set <n>` | `nuclearcraft.admin.forge` | Set forge energy |
| `/nc titan spawn` | `nuclearcraft.admin.titan` | Spawn the Titan |
| `/nc titan kill` | `nuclearcraft.admin.titan` | Kill active Titan |
| `/nc titan phase <1-4>` | `nuclearcraft.admin.titan` | Force Titan phase |
| `/nc titantech give <type>` | `nuclearcraft.admin` | Give Titan tech item |

### Debug Subcommands
```
/nc debug radiation    — Radiation system state
/nc debug zombies      — Zombie manager state
/nc debug ore          — Ore tracking state
/nc debug smelter      — Active machines
/nc debug forge        — Active forges
/nc debug farming      — Farming system state
/nc debug combat       — Combat system state
/nc debug titan        — Titan boss state (health, phase, location)
/nc debug performance  — TPS, memory, particle budget
/nc debug all          — All of the above
```

---

## Configuration Files

| File | Purpose |
|---|---|
| `config.yml` | Database, performance, world restrictions |
| `balance.yml` | All gameplay balance values |
| `radiation.yml` | Stage thresholds, decay, potion effects |
| `zombies.yml` | Zombie spawn rates, AI settings |
| `ore.yml` | Ore generation parameters |
| `smelter.yml` | Machine behaviour |
| `equipment.yml` | Tool and armor attributes |
| `farming.yml` | Crop growth, bloom spread |
| `forge.yml` | Upgrade energy costs |
| `combat.yml` | PvP radiation scaling |
| `titan.yml` | Boss health, abilities, arena |
| `titan_items.yml` | Titan technology recipes |
| `resourcepack.yml` | Resource pack delivery and sound settings |
| `messages.yml` | All player-facing strings (MiniMessage format) |

---

## Resource Pack Setup

1. Build or obtain the NuclearCraft resource pack (`.zip`).
2. Host it at a public HTTPS URL (e.g. GitHub Releases, your own CDN).
3. Generate the SHA-1 hash: `sha1sum NuclearCraft-ResourcePack.zip`
4. Set `resource-pack.enabled: true`, `url`, `hash` in `resourcepack.yml`.
5. Run `/nc reload`.

Players will be prompted to download the pack on join. Set `required: true` to kick players who decline.

---

## Performance Tuning

Edit `config.yml` under the `performance` section:

```yaml
performance:
  task-interval-ticks: 20           # How often the radiation task ticks
  data-save-interval-minutes: 5     # How often player data is saved
  max-particles-per-player: 50      # Particle budget per player
  particle-throttle-percent: 50     # Particle reduction % at low TPS
  monitor-interval-seconds: 30      # How often PerformanceManager logs stats
  log-stats: false                  # Enable to log TPS/memory to console
```

NuclearCraft automatically reduces particles when server TPS drops below 16.

---

## Economy Protection

Configure in `balance.yml` under `economy`:

- `boss-respawn-cooldown-minutes` — prevents Titan farming
- `afk-farming-timeout-seconds` — AFK crop farm protection
- `max-machines-per-player` — limits machine spam

---

## Permissions Summary

```
nuclearcraft.use              — Basic command access (default: true)
nuclearcraft.admin            — All admin permissions (default: op)
nuclearcraft.reload           — /nc reload
nuclearcraft.debug            — /nc debug toggle and subcommands
nuclearcraft.give             — /nc give
nuclearcraft.admin.radiation  — Radiation admin commands
nuclearcraft.admin.zombies    — Zombie admin commands
nuclearcraft.admin.ore        — Ore admin commands
nuclearcraft.admin.smelter    — Smelter admin commands
nuclearcraft.admin.equipment  — Equipment admin commands
nuclearcraft.admin.farming    — Farming admin commands
nuclearcraft.admin.forge      — Forge admin commands
nuclearcraft.admin.combat     — Combat admin commands
nuclearcraft.admin.titan      — Titan boss admin commands
nuclearcraft.admin.debug      — Debug diagnostic subcommands
```

---

## Troubleshooting

See `TROUBLESHOOTING.md` for common issues and solutions.
