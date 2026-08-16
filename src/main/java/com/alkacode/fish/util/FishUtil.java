package com.alkacode.fish.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/** Helpers de texto MiniMessage e PDC de peixes. */
public final class FishUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private FishUtil() {}

    public static Component parse(String miniMessage) {
        return MM.deserialize("<!i>" + miniMessage);
    }

    /** Converte um nome de bioma para o formato de exibição (OCEAN -> Ocean). */
    public static String biomeDisplay(String biome) {
        if (biome == null || biome.isEmpty()) return "Desconhecido";
        StringBuilder sb = new StringBuilder();
        String lower = biome.toLowerCase(java.util.Locale.ROOT);
        String[] parts = lower.split("_");
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    /** Formata um peso em kg com 2 casas. */
    public static String weight(double weight) {
        return String.format(java.util.Locale.US, "%.2f kg", weight);
    }

    /** Formata um comprimento em cm com 1 casa. */
    public static String length(double length) {
        return String.format(java.util.Locale.US, "%.1f cm", length);
    }
}
