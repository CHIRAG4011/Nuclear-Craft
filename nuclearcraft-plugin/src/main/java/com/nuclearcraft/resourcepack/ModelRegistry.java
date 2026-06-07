package com.nuclearcraft.resourcepack;

import com.nuclearcraft.utils.NCLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Authoritative registry of every CustomModelData value used by NuclearCraft.
 *
 * ID ranges (by base material, since same CMD on different materials does not conflict)
 *  1101-1199  Core material items
 *  1200-1299  Machines
 *  1300-1302  Consumables (radiation-antidote, radiation-serum) — HONEY_BOTTLE / GLASS_BOTTLE
 *  1301-1305  Plutonium tools — NETHERITE_SWORD/AXE/PICKAXE/SHOVEL/HOE  (shared CMD range, different base)
 *  1306-1309  Hazmat armor — LEATHER_HELMET/CHESTPLATE/LEGGINGS/BOOTS
 *  1310-1313  Plutonium armor — NETHERITE_HELMET/CHESTPLATE/LEGGINGS/BOOTS
 *  1314       Plutonium arrow — ARROW
 *  1400-1499  Upgrade machines
 *  1500-1599  Titan Technology equipment
 *
 * Note: CMDs 1301-1302 are shared between cure items and plutonium tools because they
 *       sit on different base materials — no in-game conflict exists.
 * Note: titan-fragment was previously a duplicate of radiation-drill (both 1108).
 *       The canonical value for titan-fragment is now 1112.
 */
public final class ModelRegistry {

    private ModelRegistry() {}

    // ── Core / Material items ─────────────────────────────────────────────────
    public static final int RADIOACTIVE_CORE           = 1101;
    public static final int RAW_PLUTONIUM_FRAGMENT     = 1102;
    public static final int REFINED_PLUTONIUM_INGOT    = 1103;
    public static final int MUTATED_SEED               = 1104;
    public static final int HEALING_PETAL              = 1105;
    public static final int IRRADIATED_HEART           = 1106;
    public static final int TITAN_CORE                 = 1107;
    public static final int RADIATION_DRILL            = 1108;
    public static final int REACTOR_HEART              = 1109;
    public static final int ANCIENT_REACTOR_BLUEPRINT  = 1110;
    public static final int MUTATED_CRYSTAL            = 1111;
    public static final int TITAN_FRAGMENT             = 1112;
    public static final int INDUSTRIAL_FABRIC          = 1113;

    // ── Machines ──────────────────────────────────────────────────────────────
    public static final int NUCLEAR_SMELTER            = 1201;

    // ── Consumables ───────────────────────────────────────────────────────────
    public static final int RADIATION_ANTIDOTE         = 1301;
    public static final int RADIATION_SERUM            = 1302;

    // ── Upgrade machines ──────────────────────────────────────────────────────
    public static final int NUCLEAR_FORGE              = 1401;

    // ── Titan Technology ──────────────────────────────────────────────────────
    public static final int TITAN_REACTOR_FORGE        = 1501;
    public static final int TITAN_HELMET               = 1502;
    public static final int TITAN_CHESTPLATE           = 1503;
    public static final int TITAN_LEGGINGS             = 1504;
    public static final int TITAN_BOOTS                = 1505;
    public static final int TITAN_SWORD                = 1506;
    public static final int TITAN_AXE                  = 1507;
    public static final int TITAN_PICKAXE              = 1508;
    public static final int TITAN_SHOVEL               = 1509;
    public static final int TITAN_HOE                  = 1510;
    public static final int TITAN_BOW                  = 1511;
    public static final int TITAN_ARROW                = 1512;

    // ── Plutonium Tools (NETHERITE base, CMDs 1301-1305) ─────────────────────
    public static final int PLUTONIUM_SWORD            = 1301;
    public static final int PLUTONIUM_AXE              = 1302;
    public static final int PLUTONIUM_PICKAXE          = 1303;
    public static final int PLUTONIUM_SHOVEL           = 1304;
    public static final int PLUTONIUM_HOE              = 1305;

    // ── Hazmat Armor (LEATHER base, CMDs 1306-1309) ───────────────────────────
    public static final int HAZMAT_HELMET              = 1306;
    public static final int HAZMAT_CHESTPLATE          = 1307;
    public static final int HAZMAT_LEGGINGS            = 1308;
    public static final int HAZMAT_BOOTS               = 1309;

    // ── Plutonium Armor (NETHERITE base, CMDs 1310-1313) ─────────────────────
    public static final int PLUTONIUM_HELMET           = 1310;
    public static final int PLUTONIUM_CHESTPLATE       = 1311;
    public static final int PLUTONIUM_LEGGINGS         = 1312;
    public static final int PLUTONIUM_BOOTS            = 1313;

    // ── Plutonium Arrow (ARROW base, CMD 1314) ────────────────────────────────
    public static final int PLUTONIUM_ARROW            = 1314;

    // ── Lookup maps ───────────────────────────────────────────────────────────

    private static final Map<String, Integer> ITEM_TO_ID;
    private static final Map<Integer, String> ID_TO_ITEM;

    static {
        Map<String, Integer> fwd = new HashMap<>();
        fwd.put("radioactive-core",          RADIOACTIVE_CORE);
        fwd.put("raw-plutonium-fragment",     RAW_PLUTONIUM_FRAGMENT);
        fwd.put("refined-plutonium-ingot",    REFINED_PLUTONIUM_INGOT);
        fwd.put("mutated-seed",              MUTATED_SEED);
        fwd.put("healing-petal",             HEALING_PETAL);
        fwd.put("irradiated-heart",          IRRADIATED_HEART);
        fwd.put("titan-core",                TITAN_CORE);
        fwd.put("radiation-drill",           RADIATION_DRILL);
        fwd.put("reactor-heart",             REACTOR_HEART);
        fwd.put("ancient-reactor-blueprint", ANCIENT_REACTOR_BLUEPRINT);
        fwd.put("mutated-crystal",           MUTATED_CRYSTAL);
        fwd.put("titan-fragment",            TITAN_FRAGMENT);
        fwd.put("industrial-fabric",         INDUSTRIAL_FABRIC);
        fwd.put("nuclear-smelter",           NUCLEAR_SMELTER);
        fwd.put("radiation-antidote",        RADIATION_ANTIDOTE);
        fwd.put("radiation-serum",           RADIATION_SERUM);
        fwd.put("nuclear-forge",             NUCLEAR_FORGE);
        fwd.put("titan-reactor-forge",       TITAN_REACTOR_FORGE);
        fwd.put("titan-helmet",              TITAN_HELMET);
        fwd.put("titan-chestplate",          TITAN_CHESTPLATE);
        fwd.put("titan-leggings",            TITAN_LEGGINGS);
        fwd.put("titan-boots",               TITAN_BOOTS);
        fwd.put("titan-sword",               TITAN_SWORD);
        fwd.put("titan-axe",                 TITAN_AXE);
        fwd.put("titan-pickaxe",             TITAN_PICKAXE);
        fwd.put("titan-shovel",              TITAN_SHOVEL);
        fwd.put("titan-hoe",                 TITAN_HOE);
        fwd.put("titan-bow",                 TITAN_BOW);
        fwd.put("titan-arrow",               TITAN_ARROW);
        fwd.put("plutonium-sword",           PLUTONIUM_SWORD);
        fwd.put("plutonium-axe",             PLUTONIUM_AXE);
        fwd.put("plutonium-pickaxe",         PLUTONIUM_PICKAXE);
        fwd.put("plutonium-shovel",          PLUTONIUM_SHOVEL);
        fwd.put("plutonium-hoe",             PLUTONIUM_HOE);
        fwd.put("plutonium-arrow",           PLUTONIUM_ARROW);
        fwd.put("hazmat-helmet",             HAZMAT_HELMET);
        fwd.put("hazmat-chestplate",         HAZMAT_CHESTPLATE);
        fwd.put("hazmat-leggings",           HAZMAT_LEGGINGS);
        fwd.put("hazmat-boots",              HAZMAT_BOOTS);
        fwd.put("plutonium-helmet",          PLUTONIUM_HELMET);
        fwd.put("plutonium-chestplate",      PLUTONIUM_CHESTPLATE);
        fwd.put("plutonium-leggings",        PLUTONIUM_LEGGINGS);
        fwd.put("plutonium-boots",           PLUTONIUM_BOOTS);
        ITEM_TO_ID = Collections.unmodifiableMap(fwd);

        Map<Integer, String> rev = new HashMap<>();
        fwd.forEach((name, id) -> {
            if (rev.containsKey(id)) {
                NCLogger.warn("[ModelRegistry] Duplicate CustomModelData " + id
                        + " — '" + name + "' conflicts with '" + rev.get(id) + "'");
            } else {
                rev.put(id, name);
            }
        });
        ID_TO_ITEM = Collections.unmodifiableMap(rev);
    }

    public static int getId(String itemId) {
        return ITEM_TO_ID.getOrDefault(itemId, -1);
    }

    public static String getItemId(int modelData) {
        return ID_TO_ITEM.get(modelData);
    }

    public static Map<String, Integer> getAllMappings() {
        return ITEM_TO_ID;
    }

    public static int totalItems() {
        return ITEM_TO_ID.size();
    }

    public static void validate() {
        NCLogger.info("[ModelRegistry] " + ITEM_TO_ID.size() + " items mapped across "
                + ID_TO_ITEM.size() + " unique CustomModelData IDs.");
    }
}
