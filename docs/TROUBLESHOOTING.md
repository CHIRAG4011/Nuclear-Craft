# NuclearCraft: Plutonium Age — Troubleshooting Guide
Version 1.0.0

---

## Startup Checks Failed

Run `/nc debug all` in-game (requires `nuclearcraft.debug` permission) or check the startup log.

### "Database reachable: FAIL"
- **SQLite:** Ensure the server has write permission to `plugins/NuclearCraft/`.
- **MySQL:** Check host, port, username, password, and that the database exists.
- Check `config.yml` for typos in the database section.

### "ModelRegistry has no duplicates: FAIL"
- This is an internal issue. File a bug report with your server version.

### "Config files present: FAIL"
- One or more YAML files failed to parse. Check for syntax errors.
- Delete the broken file; NuclearCraft will regenerate it with defaults on next start.

---

## Plugin Fails to Load

### "Cannot access class" / "Cannot find symbol" errors
- You are running Java < 21. NuclearCraft requires Java 21+.
- Check with `java -version` and upgrade your JRE.

### "NoSuchMethodError" or "ClassNotFoundException"
- You are not running Paper 1.21+. Spigot is not supported.
- Purpur 1.21+ is supported.

---

## Radiation Not Working

- Check that the world is not in `blocked-worlds` in `config.yml`.
- Verify `radiation.yml` stage thresholds are set correctly.
- Run `/nc debug radiation` to see the system state.
- Ensure `task-interval-ticks` in `config.yml` is not set to 0.

---

## Custom Sounds Not Playing

- Verify `resource-pack.enabled: true` in `resourcepack.yml`.
- Confirm the pack URL is publicly accessible (test in a browser).
- The SHA-1 hash must match exactly — regenerate it with `sha1sum NuclearCraft-ResourcePack.zip`.
- Players must accept the resource pack when prompted.
- If using `required: false`, players who decline will hear vanilla fallback sounds.

---

## Resource Pack Not Applying to Players

- The pack URL must use **HTTPS**, not HTTP.
- Test that the URL works from outside your network.
- Check `resourcepack.yml` for correct hash format (40 lowercase hex chars).
- Check console for `[ResourcePackManager]` warning messages.

---

## Titan Boss Not Spawning

- Confirm the summoner has a **Titan Core** in hand.
- Check `boss.titan.base-health` is > 0 in `balance.yml`.
- Run `/nc debug titan` to see if a Titan is already active.
- Confirm the spawn location has enough open space (the Titan is a Giant entity).
- Check `titan.yml` for arena size requirements.

---

## Machines (Smelter / Forge) Not Working

- The machine must be placed as a block (right-click the placed machine to open GUI).
- The machine must have fuel loaded.
- Check `/nc smelter debug` to see all tracked machines and their state.
- Run `/nc debug smelter` for system-level diagnostics.

---

## Performance Issues

Check `/nc debug performance` for:
- **TPS** — if below 16, particles are automatically throttled
- **Heap** — if above 85%, consider increasing server RAM or reducing tracked entities

Tune in `config.yml`:
```yaml
performance:
  max-particles-per-player: 20     # Reduce from 50
  task-interval-ticks: 40          # Check less frequently (every 2s instead of 1s)
  data-save-interval-minutes: 10   # Save less frequently
```

Tune in `balance.yml`:
```yaml
zombies:
  spawn-chance: 0.05               # Reduce irradiated zombie spawns
economy:
  max-machines-per-player: 3       # Limit machine count
```

---

## Player Data Lost After Update

- Player data is stored in the database (`nuclearcraft.db` for SQLite).
- As long as the database file is intact, data persists across updates.
- If you moved or deleted the `.db` file, data cannot be recovered.
- Always back up `plugins/NuclearCraft/nuclearcraft.db` before updates.

---

## Still Having Issues?

1. Enable debug mode: `/nc debug` (toggles verbose console logging)
2. Reproduce the issue and collect the relevant console output
3. Run `/nc debug all` and copy the output
4. Include your server version, Java version, and NuclearCraft version in any report
