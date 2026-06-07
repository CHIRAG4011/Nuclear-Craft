package com.nuclearcraft.resourcepack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Texture design specification registry for NuclearCraft.
 *
 * Each entry describes the visual design intent for an item/block texture.
 * This class acts as the source-of-truth specification that an artist uses
 * when creating the physical texture files for the resource pack.
 *
 * All textures live under: assets/nuclearcraft/textures/
 */
public final class TextureRegistry {

    private TextureRegistry() {}

    public record TextureSpec(
            String itemId,
            String texturePath,
            String theme,
            String colorPalette,
            String shapeDesign,
            String glowLocations,
            String animationNotes,
            String modelNotes
    ) {}

    private static final Map<String, TextureSpec> TEXTURES;

    static {
        Map<String, TextureSpec> m = new LinkedHashMap<>();

        // ── Core material items ───────────────────────────────────────────────

        m.put("radioactive-core", new TextureSpec(
                "radioactive-core",
                "item/radioactive_core",
                "Unstable nuclear power source",
                "Neon green (#39FF14), electric yellow (#FFE000), dark black (#0A0A0A)",
                "Rounded crystal sphere with cracked energy fault lines radiating outward",
                "Crystal core center pulses; fault line edges glow",
                "Animated: pulsing glow cycle ~20 ticks; core brightens and dims",
                "Custom 3D model: sphere base with floating energy shards around it"
        ));

        m.put("raw-plutonium-fragment", new TextureSpec(
                "raw-plutonium-fragment",
                "item/raw_plutonium_fragment",
                "Unstable mined crystal fragment",
                "Toxic green (#39FF14), dark forest green (#1A4A00), charcoal (#1C1C1C)",
                "Jagged irregular shard shape, sharp edges, inner green glow core",
                "Inner crystal core glows; outer edges have subtle toxic sheen",
                "Static texture with optional emissive layer on the crystal core",
                "Flat item texture; no 3D model required"
        ));

        m.put("refined-plutonium-ingot", new TextureSpec(
                "refined-plutonium-ingot",
                "item/refined_plutonium_ingot",
                "Processed nuclear metal bar",
                "Metallic dark grey (#3A3A3A), neon green channel (#39FF14), chrome highlight (#C0C0C0)",
                "Standard ingot shape; single green energy channel running down the center",
                "Green channel glows; chrome highlights shimmer",
                "Static texture; energy channel uses emissive map",
                "Standard ingot flat item; no 3D model"
        ));

        m.put("mutated-seed", new TextureSpec(
                "mutated-seed",
                "item/mutated_seed",
                "Irradiated contaminated seed",
                "Dark brown (#2A1A00), sickly green veins (#7FFF00), pale yellow (#E8D5A3)",
                "Oval seed shape with green glowing vein network running through it",
                "Vein intersections glow brightest",
                "Subtle pulse on the veins; very slow cycle",
                "Flat item texture"
        ));

        m.put("healing-petal", new TextureSpec(
                "healing-petal",
                "item/healing_petal",
                "Medical radioactive flower petal",
                "Soft blue (#88CCFF), gentle cyan (#00FFEE), white (#FFFFFF)",
                "Single curved petal with delicate vein details and soft inner glow",
                "Petal center and tip glow softly",
                "Gentle breathing glow effect",
                "Flat item texture with emissive layer"
        ));

        m.put("irradiated-heart", new TextureSpec(
                "irradiated-heart",
                "item/irradiated_heart",
                "Corrupted zombie heart",
                "Dark red (#8B0000), pulsing green (#39FF14), black (#000000)",
                "Heart shape with green radiation corruption spreading from center",
                "Corruption tendrils glow green; heart body has dark pulsing red",
                "Pulsing beat animation on the heart shape",
                "3D model: heart shape with floating corruption particles optional"
        ));

        m.put("titan-core", new TextureSpec(
                "titan-core",
                "item/titan_core",
                "Crystallized Titan reactor essence",
                "Deep violet (#7700FF), neon green (#39FF14), pure white core (#FFFFFF)",
                "Multi-faceted crystal with reactor glow from within",
                "Crystal facets shimmer; inner white core pulses",
                "Rapid energy pulse; floating crystal shards orbit",
                "3D model: central crystal with orbiting energy shards"
        ));

        m.put("radiation-drill", new TextureSpec(
                "radiation-drill",
                "item/radiation_drill",
                "Industrial nuclear mining tool",
                "Gunmetal grey (#4A4A4A), neon green (#39FF14), orange heat (#FF6600)",
                "Drill head with rotating bit marks, green radiation safety stripe",
                "Drill tip glows orange-hot; safety stripe glows green",
                "Bit rotation idle animation; green stripe flickers",
                "3D model strongly recommended: drill head with spiral grooves"
        ));

        m.put("titan-fragment", new TextureSpec(
                "titan-fragment",
                "item/titan_fragment",
                "Titan reactor plating shard",
                "Deep purple (#7700FF), cracked gold (#FFD700), black void (#000000)",
                "Angular armor shard with energy cracks across the surface",
                "Energy cracks pulse purple-to-gold",
                "Crack glow pulses in and out",
                "Flat item texture; emissive cracks"
        ));

        m.put("reactor-heart", new TextureSpec(
                "reactor-heart",
                "item/reactor_heart",
                "Titan reactor core organ",
                "Deep red (#CC0000), nuclear green (#39FF14), void black (#000000)",
                "Stylized reactor sphere resembling a heart; glowing veins across surface",
                "Entire surface has low glow; vein intersections are bright",
                "Pulsing reactor beat; veins animate along their length",
                "3D sphere model with surface vein details"
        ));

        m.put("titan-fragment", new TextureSpec(
                "titan-fragment",
                "item/titan_fragment",
                "Reactor plating fragment",
                "Void black (#0A0A0A), electric violet (#7700FF), cracked gold (#FFD700)",
                "Jagged armor plate shard with glowing impact cracks",
                "Impact cracks glow violet-to-gold gradient",
                "Slow crack glow cycle",
                "Flat item"
        ));

        m.put("mutated-crystal", new TextureSpec(
                "mutated-crystal",
                "item/mutated_crystal",
                "Crystallized radioactive energy",
                "Aqua (#00FFCC), deep violet (#7700FF), clear white (#FFFFFF)",
                "Tall jagged crystal cluster, color shifts from base to tip",
                "Tips glow white; base glows violet",
                "Color gradient shift animation tip-to-base",
                "3D crystal cluster model preferred"
        ));

        // ── Machines ─────────────────────────────────────────────────────────

        m.put("nuclear-smelter", new TextureSpec(
                "nuclear-smelter",
                "item/nuclear_smelter",
                "Industrial plutonium refinery",
                "Dark iron (#2A2A2A), hazard yellow (#FFE000), neon green (#39FF14)",
                "Squat industrial furnace with reactor window and warning stripes",
                "Reactor window glows green; hazard stripes glow yellow",
                "Reactor window pulsing glow",
                "Block model matches blast furnace base shape with custom decals"
        ));

        m.put("nuclear-forge", new TextureSpec(
                "nuclear-forge",
                "item/nuclear_forge",
                "Equipment upgrade station",
                "Dark metal (#1A1A1A), energy orange (#FF8800), green power (#39FF14)",
                "Smithing table shape with reactor energy conduits along top",
                "Conduits pulse orange; power indicators glow green",
                "Energy conduit flow animation",
                "Block model based on smithing table with custom overlay"
        ));

        m.put("titan-reactor-forge", new TextureSpec(
                "titan-reactor-forge",
                "item/titan_reactor_forge",
                "Pinnacle nuclear engineering machine",
                "Crying obsidian purple (#6A0DAD), void black (#000000), reactor green (#39FF14)",
                "Dense cubic machine covered in rune-like energy circuits",
                "Energy circuits glow green; machine core pulses purple",
                "Circuit path animation: energy flows along circuit lines",
                "3D block model with animated circuit overlays"
        ));

        // ── Consumables ──────────────────────────────────────────────────────

        m.put("radiation-antidote", new TextureSpec(
                "radiation-antidote",
                "item/radiation_antidote",
                "Radiation cure bottle",
                "Honey amber (#FFB300), clear glass (#CCFFEE), green liquid (#39FF14)",
                "Honey bottle shape with glowing green cure liquid",
                "Liquid inside bottle glows softly",
                "Liquid shimmer animation",
                "Flat item using honey bottle base model"
        ));

        m.put("radiation-serum", new TextureSpec(
                "radiation-serum",
                "item/radiation_serum",
                "Advanced nuclear serum vial",
                "Crystal clear glass (#E8F8FF), vivid violet (#7700FF), neon green (#39FF14)",
                "Thin glass vial with layered liquid — violet bottom, green top",
                "Both liquid layers glow; stopper glows green",
                "Liquid ripple + gentle bubble animation",
                "Flat item using glass bottle base model"
        ));

        // ── Equipment — Plutonium Armor & Tools ───────────────────────────────

        m.put("plutonium-armor-set", new TextureSpec(
                "plutonium-armor-set",
                "models/armor/plutonium_layer",
                "Military nuclear technology",
                "Matte black alloy (#0A0A0A), nuclear green (#39FF14), reactor orange (#FF6600)",
                "Heavy segmented armor plates with green energy conduit channels between segments",
                "Conduit channels glow continuously; reactor ports on chestplate glow orange",
                "Conduit flow animation; reactor ports pulse",
                "Two-layer armor texture (layer_1, layer_2)"
        ));

        m.put("hazmat-armor-set", new TextureSpec(
                "hazmat-armor-set",
                "models/armor/hazmat_layer",
                "Radiation protection suit",
                "Safety yellow (#FFE000), white suit (#F0F0F0), dark visor (#1A1A1A)",
                "Full-body sealed hazmat suit with chemical-resistant outer shell and visor",
                "Visor has subtle green HUD-like inner glow",
                "Subtle visor glow flicker",
                "Two-layer armor texture; helmet has custom visor model"
        ));

        m.put("titan-armor-set", new TextureSpec(
                "titan-armor-set",
                "models/armor/titan_layer",
                "Ancient reactor civilization technology",
                "Void black (#000000), reactor violet (#7700FF), nuclear green (#39FF14), cracked gold (#FFD700)",
                "Legendary armor with floating energy panel inserts and runic circuit engravings",
                "Floating panels glow violet; runic circuits glow green; cracks glow gold",
                "Floating panel orbit animation; rune glow pulse",
                "Custom 3D armor model with floating element attach points"
        ));

        TEXTURES = Collections.unmodifiableMap(m);
    }

    public static TextureSpec get(String itemId) {
        return TEXTURES.get(itemId);
    }

    public static Map<String, TextureSpec> getAll() {
        return TEXTURES;
    }

    public static int totalTextures() {
        return TEXTURES.size();
    }
}
