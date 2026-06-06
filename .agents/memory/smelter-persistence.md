---
name: Smelter data persistence
description: How NuclearSmelterManager persists machine data to YAML without YAML path issues.
---

## Rule
Do NOT use the serialized location string (e.g. `world,100,-64,200`) as a YAML path segment. Commas aren't YAML path separators but dots are — any world name with a dot would break. Instead, use numbered keys.

## How to apply
**Save:**
```java
int index = 0;
for (SmelterData machine : machines.values()) {
    String path = "machines.m" + index++;
    cfg.set(path + ".location-key", machine.getLocationKey()); // world,x,y,z
    cfg.set(path + ".state", machine.getState().name());
    // ...
}
```

**Load:**
```java
for (String key : section.getKeys(false)) {  // iterates m0, m1, ...
    String locationKey = cfg.getString("machines." + key + ".location-key");
    Location loc = SmelterData.deserializeLocation(locationKey);
    // ...
}
```

`SmelterData.serializeLocation()` returns `world,x,y,z`.
`SmelterData.deserializeLocation()` splits by comma: `[world, x, y, z]`.

**Why:** Using the location as a YAML path key caused broken load logic in the original draft.
