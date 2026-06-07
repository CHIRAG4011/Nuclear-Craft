package com.nuclearcraft.resourcepack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of every custom sound event used by NuclearCraft.
 *
 * Sound names follow the format: nuclearcraft.<category>.<event>
 * These match the keys that must be declared in the resource pack's sounds.json.
 *
 * If a resource pack is not active the SoundManager falls back to a vanilla sound.
 */
public final class SoundRegistry {

    private SoundRegistry() {}

    // ── Radiation ─────────────────────────────────────────────────────────────
    public static final String RADIATION_GAIN            = "nuclearcraft.radiation.gain";
    public static final String RADIATION_STAGE_CHANGE    = "nuclearcraft.radiation.stage_change";
    public static final String RADIATION_CURE            = "nuclearcraft.radiation.cure";
    public static final String RADIATION_SURGE           = "nuclearcraft.radiation.surge";
    public static final String RADIATION_DEATH           = "nuclearcraft.radiation.death";

    // ── Items / Consumables ───────────────────────────────────────────────────
    public static final String ANTIDOTE_DRINK            = "nuclearcraft.item.antidote_drink";
    public static final String SERUM_DRINK               = "nuclearcraft.item.serum_drink";

    // ── Ore ───────────────────────────────────────────────────────────────────
    public static final String ORE_DISCOVER              = "nuclearcraft.ore.discover";
    public static final String ORE_MINE                  = "nuclearcraft.ore.mine";
    public static final String ORE_DRILL                 = "nuclearcraft.ore.drill";

    // ── Machines ──────────────────────────────────────────────────────────────
    public static final String SMELTER_START             = "nuclearcraft.machine.smelter_start";
    public static final String SMELTER_COMPLETE          = "nuclearcraft.machine.smelter_complete";
    public static final String SMELTER_OVERHEAT          = "nuclearcraft.machine.smelter_overheat";
    public static final String FORGE_START               = "nuclearcraft.machine.forge_start";
    public static final String FORGE_COMPLETE            = "nuclearcraft.machine.forge_complete";
    public static final String FORGE_OVERLOAD            = "nuclearcraft.machine.forge_overload";
    public static final String TITAN_FORGE_START         = "nuclearcraft.machine.titan_forge_start";
    public static final String TITAN_FORGE_COMPLETE      = "nuclearcraft.machine.titan_forge_complete";

    // ── Upgrades ─────────────────────────────────────────────────────────────
    public static final String UPGRADE_SUCCESS           = "nuclearcraft.upgrade.success";
    public static final String UPGRADE_FAIL              = "nuclearcraft.upgrade.fail";

    // ── Combat ───────────────────────────────────────────────────────────────
    public static final String WEAPON_RADIATION_HIT      = "nuclearcraft.combat.radiation_hit";
    public static final String WEAPON_AURA_PULSE         = "nuclearcraft.combat.aura_pulse";

    // ── Boss — Plutonium Titan ────────────────────────────────────────────────
    public static final String TITAN_SPAWN               = "nuclearcraft.titan.spawn";
    public static final String TITAN_DEATH               = "nuclearcraft.titan.death";
    public static final String TITAN_PHASE_CHANGE        = "nuclearcraft.titan.phase_change";
    public static final String TITAN_ABILITY_RADIATION   = "nuclearcraft.titan.ability.radiation";
    public static final String TITAN_ABILITY_SLAM        = "nuclearcraft.titan.ability.slam";
    public static final String TITAN_ABILITY_SUMMON      = "nuclearcraft.titan.ability.summon";
    public static final String TITAN_ABILITY_BEAM        = "nuclearcraft.titan.ability.beam";
    public static final String TITAN_ROAR                = "nuclearcraft.titan.roar";

    // ── Environment ───────────────────────────────────────────────────────────
    public static final String RADIATION_NIGHT_START     = "nuclearcraft.environment.radiation_night_start";
    public static final String RADIATION_NIGHT_END       = "nuclearcraft.environment.radiation_night_end";
    public static final String RADIATION_CLOUD_AMBIENT   = "nuclearcraft.environment.radiation_cloud";

    // ── Vanilla fallback map ───────────────────────────────────────────────────
    private static final Map<String, String> FALLBACKS;

    static {
        Map<String, String> fb = new HashMap<>();
        fb.put(RADIATION_GAIN,           "minecraft:block.beacon.power_select");
        fb.put(RADIATION_STAGE_CHANGE,   "minecraft:entity.elder_guardian.curse");
        fb.put(RADIATION_CURE,           "minecraft:entity.player.levelup");
        fb.put(RADIATION_SURGE,          "minecraft:entity.warden.sonic_boom");
        fb.put(RADIATION_DEATH,          "minecraft:entity.warden.death");
        fb.put(ANTIDOTE_DRINK,           "minecraft:entity.generic.drink");
        fb.put(SERUM_DRINK,              "minecraft:entity.generic.drink");
        fb.put(ORE_DISCOVER,             "minecraft:block.amethyst_cluster.break");
        fb.put(ORE_MINE,                 "minecraft:block.stone.break");
        fb.put(ORE_DRILL,                "minecraft:block.iron_trapdoor.open");
        fb.put(SMELTER_START,            "minecraft:block.blastfurnace.fire_crackle");
        fb.put(SMELTER_COMPLETE,         "minecraft:block.anvil.use");
        fb.put(SMELTER_OVERHEAT,         "minecraft:block.fire.extinguish");
        fb.put(FORGE_START,              "minecraft:block.anvil.land");
        fb.put(FORGE_COMPLETE,           "minecraft:entity.player.levelup");
        fb.put(FORGE_OVERLOAD,           "minecraft:entity.generic.explode");
        fb.put(TITAN_FORGE_START,        "minecraft:block.respawn_anchor.charge");
        fb.put(TITAN_FORGE_COMPLETE,     "minecraft:ui.toast.challenge_complete");
        fb.put(UPGRADE_SUCCESS,          "minecraft:block.enchantment_table.use");
        fb.put(UPGRADE_FAIL,             "minecraft:entity.item.break");
        fb.put(WEAPON_RADIATION_HIT,     "minecraft:entity.generic.hurt");
        fb.put(WEAPON_AURA_PULSE,        "minecraft:block.beacon.ambient");
        fb.put(TITAN_SPAWN,              "minecraft:entity.ender_dragon.growl");
        fb.put(TITAN_DEATH,              "minecraft:entity.ender_dragon.death");
        fb.put(TITAN_PHASE_CHANGE,       "minecraft:entity.warden.listening_angry");
        fb.put(TITAN_ABILITY_RADIATION,  "minecraft:entity.warden.sonic_boom");
        fb.put(TITAN_ABILITY_SLAM,       "minecraft:entity.warden.tendril_clicks_listening");
        fb.put(TITAN_ABILITY_SUMMON,     "minecraft:entity.warden.heartbeat");
        fb.put(TITAN_ABILITY_BEAM,       "minecraft:entity.guardian.attack");
        fb.put(TITAN_ROAR,               "minecraft:entity.ender_dragon.growl");
        fb.put(RADIATION_NIGHT_START,    "minecraft:entity.elder_guardian.curse");
        fb.put(RADIATION_NIGHT_END,      "minecraft:block.beacon.deactivate");
        fb.put(RADIATION_CLOUD_AMBIENT,  "minecraft:block.beacon.ambient");
        FALLBACKS = Collections.unmodifiableMap(fb);
    }

    /**
     * Returns the vanilla fallback sound for a custom sound key,
     * or the key itself if no fallback is registered.
     */
    public static String getFallback(String soundKey) {
        return FALLBACKS.getOrDefault(soundKey, soundKey);
    }

    public static Map<String, String> getAllFallbacks() {
        return FALLBACKS;
    }

    public static int totalSounds() {
        return FALLBACKS.size();
    }
}
