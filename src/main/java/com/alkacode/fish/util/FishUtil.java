package com.alkacode.fish.util;

import com.alkacode.fish.FishMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

/** Helpers de texto MiniMessage e PDC de peixes. */
public final class FishUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Title.Times TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(300), Duration.ofMillis(1500), Duration.ofMillis(300));

    private FishUtil() {}

    public static Component parse(String miniMessage) {
        return MM.deserialize("<!i>" + miniMessage);
    }

    /** Mostra title+subtitle padrão (300ms fade-in, 1.5s hold, 300ms fade-out).
     * Chaves ausentes no messages.yml viram componente vazio em vez de texto cru. */
    public static void showTitle(Player player, FishMessages messages, String titleKey, String subtitleKey) {
        player.showTitle(Title.title(parseOrEmpty(messages, titleKey), parseOrEmpty(messages, subtitleKey), TITLE_TIMES));
    }

    private static Component parseOrEmpty(FishMessages messages, String key) {
        Component msg = messages.parse(key);
        if (PlainTextComponentSerializer.plainText().serialize(msg).equals(key)) return Component.empty();
        return msg;
    }

    private static final Title.Times CELEBRATION_TIMES = Title.Times.times(
            Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(500));

    /** Title+subtitle a partir de texto MiniMessage cru (não é chave de messages.yml) -
     * usado por tiers.yml, onde o texto já vem pronto do config. Fica mais tempo na tela
     * que o showTitle padrão (é um momento de destaque, não um status rotineiro). */
    public static void showRawTitle(Player player, String titleText, String subtitleText) {
        Component title = titleText != null && !titleText.isEmpty() ? parse(titleText) : Component.empty();
        Component subtitle = subtitleText != null && !subtitleText.isEmpty() ? parse(subtitleText) : Component.empty();
        player.showTitle(Title.title(title, subtitle, CELEBRATION_TIMES));
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
