# NuclearCraft: Plutonium Age — Developer API Guide
Version 1.0.0 | Phase 12

## Overview

NuclearCraft exposes its systems through the main plugin class and a set of custom Bukkit events. Other plugins can integrate with NuclearCraft by depending on it as a soft dependency.

---

## Soft Dependency Setup

In your `plugin.yml`:
```yaml
softdepend: [NuclearCraft]
```

In your plugin code:
```java
import com.nuclearcraft.core.NuclearCraftPlugin;

Plugin raw = Bukkit.getPluginManager().getPlugin("NuclearCraft");
if (raw instanceof NuclearCraftPlugin nc) {
    // NuclearCraft is active — use its API
}
```

---

## Radiation API

```java
NuclearCraftPlugin nc = ...;
RadiationManager rm = nc.getRadiationManager();

// Get a player's current radiation level (0.0 – 1000.0)
double rad = rm.getRadiation(player);

// Add radiation
rm.addRadiation(player, 50.0, RadiationSource.EXTERNAL);

// Remove radiation
rm.removeRadiation(player, 25.0);

// Set radiation
rm.setRadiation(player, 0.0);

// Get radiation stage (1–5, or 0 for safe)
int stage = rm.getRadiationStage(player);
```

---

## Custom Events

All events are in the `com.nuclearcraft.events` and `com.nuclearcraft.boss.events` packages.

### Radiation Events

```java
@EventHandler
public void onRadiationGain(RadiationGainEvent event) {
    Player player = event.getPlayer();
    double amount = event.getAmount();
    RadiationSource source = event.getSource();
    event.setCancelled(true); // prevent the gain
}

@EventHandler
public void onRadiationStageChange(RadiationStageChangeEvent event) {
    int oldStage = event.getOldStage();
    int newStage = event.getNewStage();
}

@EventHandler
public void onRadiationCure(RadiationCureEvent event) {
    Player player = event.getPlayer();
    double amountCured = event.getAmountCured();
}

@EventHandler
public void onRadiationDeath(RadiationDeathEvent event) {
    // Player died from radiation exposure
}
```

### Machine Events

```java
@EventHandler
public void onSmelterStart(NuclearSmelterStartEvent event) {
    Location machineLoc = event.getMachineLocation();
}

@EventHandler
public void onSmelterComplete(NuclearSmelterCompleteEvent event) {
    ItemStack output = event.getOutput();
    event.setCancelled(true); // prevent output delivery
}

@EventHandler
public void onForgeUpgradeSuccess(ForgeUpgradeSuccessEvent event) {
    Player player = event.getPlayer();
    ItemStack upgraded = event.getResult();
    String tier = event.getTier(); // "mk1", "mk2", "mk3", "mk4"
}

@EventHandler
public void onForgeUpgradeFail(ForgeUpgradeFailEvent event) {
    // Upgrade rolled and failed
}
```

### Farming Events

```java
@EventHandler
public void onCropMutation(CropMutationEvent event) {
    Location cropLoc = event.getLocation();
    event.setCancelled(true); // prevent mutation
}
```

### Boss Events

```java
@EventHandler
public void onTitanSpawn(TitanSpawnEvent event) {
    Giant titan = event.getTitan();
    Player summoner = event.getSummoner();
    event.setCancelled(true); // prevent spawn
}

@EventHandler
public void onTitanPhaseChange(TitanPhaseChangeEvent event) {
    TitanPhase newPhase = event.getNewPhase();
    Giant titan = event.getTitan();
}

@EventHandler
public void onTitanDeath(TitanDeathEvent event) {
    Giant titan = event.getTitan();
    List<Player> participants = event.getParticipants();
}

@EventHandler
public void onTitanReward(TitanRewardEvent event) {
    Player player = event.getPlayer();
    List<ItemStack> rewards = event.getRewards();
    event.getRewards().add(new ItemStack(Material.DIAMOND, 5)); // add custom reward
}
```

---

## Item API

```java
ItemManager im = nc.getItemManager();

// Get a custom item by ID
Optional<CustomItem> item = im.getItem("radioactive-core");
if (item.isPresent()) {
    ItemStack stack = item.get().build(1);
    player.getInventory().addItem(stack);
}

// Check if an ItemStack is a NuclearCraft custom item
// Uses PersistentDataContainer under the hood
boolean isCustom = item.get().matches(someItemStack);
```

---

## Balance API (Phase 12)

```java
BalanceManager bm = nc.getBalanceManager();

double decayRate    = bm.getRadiationDecayRate();
double titanHealth  = bm.getTitanBaseHealth();
double hazmatResist = bm.getHazmatResistancePercent();
```

---

## Sound API (Phase 12)

```java
SoundManager sm = nc.getSoundManager();

// Play a named sound for a player
sm.play(player, SoundRegistry.RADIATION_CURE);

// Play at a location (all nearby players hear it)
sm.playAtLocation(location, SoundRegistry.TITAN_SPAWN, 2.0f, 0.6f);

// Convenience methods
sm.playRadiationGain(player);
sm.playTitanSpawn(titanLocation);
```

---

## Particle API (Phase 12)

```java
ParticleManager pm = nc.getParticleManager();

pm.spawnRadiationGain(player);
pm.spawnTitanSpawn(titanLocation);
pm.spawnUpgradeSuccess(player);
pm.spawnAntidoteEffect(player);
```

---

## ModelRegistry (Phase 12)

```java
// Get the CustomModelData ID for any item
int id = ModelRegistry.getId("titan-sword"); // returns 1506

// Get item ID from CustomModelData
String itemId = ModelRegistry.getItemId(1506); // returns "titan-sword"

// Get all mappings
Map<String, Integer> all = ModelRegistry.getAllMappings();
```

---

## Player Data API

```java
PlayerDataManager pdm = nc.getPlayerDataManager();

// Get player data (returns Optional)
pdm.getPlayerData(player.getUniqueId()).thenAccept(data -> {
    if (data != null) {
        long zombiesKilled = data.getZombiesKilled();
        int titanKills = data.getTitanKills();
    }
});
```

---

## Namespace

The NuclearCraft namespace is `nuclearcraft`. All NamespacedKeys follow:
```java
NamespacedKey key = new NamespacedKey(nc, "your-key");
```

Or using `AssetRegistry`:
```java
String ns = AssetRegistry.namespace(); // "nuclearcraft"
String fullKey = AssetRegistry.key("my-item"); // "nuclearcraft:my-item"
```
