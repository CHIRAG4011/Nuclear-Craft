# NuclearCraft: Plutonium Age — Installation Guide
Version 1.0.0

## Requirements

| Requirement | Minimum | Recommended |
|---|---|---|
| Server Software | Paper 1.21 | Paper 1.21.4+ |
| Java | Java 21 | Java 21 |
| RAM | 2 GB | 4 GB+ |
| Storage | 500 MB | 1 GB+ |

> **Purpur** is also supported. Spigot is **not** supported.

---

## Step 1: Install the Plugin

1. Download `NuclearCraft-1.0.0.jar`.
2. Copy it to your server's `plugins/` directory.
3. Start the server once to generate configs:

```
plugins/
  NuclearCraft/
    config.yml
    balance.yml
    radiation.yml
    zombies.yml
    ore.yml
    smelter.yml
    equipment.yml
    farming.yml
    forge.yml
    combat.yml
    titan.yml
    titan_items.yml
    resourcepack.yml
    messages.yml
    toolstats.yml
    armors.yml
    boss.yml
```

---

## Step 2: Configure the Database

Open `plugins/NuclearCraft/config.yml`:

### SQLite (default — no setup required)
```yaml
database:
  type: SQLITE
  sqlite:
    file: nuclearcraft.db
```

### MySQL
```yaml
database:
  type: MYSQL
  mysql:
    host: localhost
    port: 3306
    database: nuclearcraft
    username: your_user
    password: your_password
    pool-size: 10
```

Create the MySQL database before starting:
```sql
CREATE DATABASE nuclearcraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

NuclearCraft handles all table creation and migrations automatically.

---

## Step 3: Configure Worlds

By default NuclearCraft is active in all worlds except `world_the_end`.

Edit `config.yml`:
```yaml
world:
  allowed-worlds: []       # Empty = all worlds
  blocked-worlds:
    - world_the_end
    - your_creative_world
```

---

## Step 4: Resource Pack (Optional)

If you want custom models and sounds, set up the resource pack:

1. Host the NuclearCraft resource pack at a public HTTPS URL.
2. Get its SHA-1 hash: `sha1sum NuclearCraft-ResourcePack.zip`
3. Edit `plugins/NuclearCraft/resourcepack.yml`:

```yaml
resource-pack:
  enabled: true
  url: "https://your-server.com/NuclearCraft-ResourcePack.zip"
  hash: "abc123...40hexchars"
  required: false
  prompt: "Download the NuclearCraft resource pack for the best experience!"
```

---

## Step 5: Adjust Balance (Optional)

`balance.yml` contains every numerical gameplay value. Review and adjust to your server's preferences before opening to players. Key values to consider:

- `radiation.decay-rate-per-tick` — How quickly radiation clears (default: 0.05/tick)
- `zombies.spawn-chance` — How often a zombie is replaced with an irradiated variant (default: 10%)
- `boss.titan.base-health` — Titan difficulty (default: 800 HP)
- `economy.boss-respawn-cooldown-minutes` — Prevent Titan farming (default: 60 min)

---

## Step 6: Verify Installation

After restarting, check your server console for:

```
╔══════════════════════════════════════════╗
║        NuclearCraft: Plutonium Age       ║
╠══════════════════════════════════════════╣
║  Version : 1.0.0                         ║
...
╚══════════════════════════════════════════╝
[ReleaseManager] Java version: 21 ✔
[ReleaseManager] Max heap: XXXX MB ✔
[ReleaseManager] Server version: ... ✔
[TestingManager] All N startup checks PASSED.
[NuclearCraft] NuclearCraft enabled successfully in XXXms.
```

If you see `FAILED` in the testing output, check `TROUBLESHOOTING.md`.

---

## Updating from a Previous Version

NuclearCraft's `MigrationManager` handles config updates automatically. Simply replace the `.jar` file and restart. Existing player data and configs are preserved.

Always **back up** your database file before updating.

---

## Uninstalling

1. Remove `NuclearCraft-1.0.0.jar` from `plugins/`.
2. Optionally delete `plugins/NuclearCraft/` (this removes all configs and the SQLite database).
3. If using MySQL, the `nuclearcraft` database can be dropped manually.
