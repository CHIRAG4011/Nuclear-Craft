package com.nuclearcraft.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Utility for MiniMessage color parsing and text manipulation.
 */
public final class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ColorUtil() {}

    /**
     * Parses a MiniMessage string into a Component.
     */
    public static Component parse(String text) {
        if (text == null) return Component.empty();
        return MINI_MESSAGE.deserialize(text);
    }

    /**
     * Sends a MiniMessage-formatted message to a player.
     */
    public static void send(Player player, String text) {
        player.sendMessage(parse(text));
    }

    /**
     * Strips all MiniMessage tags and returns plain text.
     */
    public static String stripFormatting(String text) {
        return PlainTextComponentSerializer.plainText().serialize(parse(text));
    }

    /**
     * Serializes a Component back into a MiniMessage string.
     */
    public static String serialize(Component component) {
        return MINI_MESSAGE.serialize(component);
    }

    /**
     * Replaces placeholders in a MiniMessage string and parses the result.
     */
    public static Component parsePlaceholders(String text, String... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("replacements must be provided as key-value pairs");
        }
        for (int i = 0; i < replacements.length; i += 2) {
            text = text.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return parse(text);
    }
}
