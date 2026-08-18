package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** TOP de pescaria com seletor de métrica (peixes pescados / nácar / peso / tempo).
 * As consultas batem direto no banco (jogador offline também entra no ranking) - por
 * isso SEMPRE carrega os dados numa task assíncrona antes de abrir (ver openAsync),
 * nunca faz a query síncrona dentro de render(). */
public final class TopGui extends FishGui {

    private static final List<String> METRICS = List.of("fished", "nacar", "weight", "time");

    private final String metric;
    private final List<Entry> entries;

    private TopGui(AlkaFishPlugin plugin, Player player, String metric, List<Entry> entries) {
        super(plugin, player, "🏆 TOP Pescaria", Category.HIGHLIGHT, 4, "alkafish-top");
        this.metric = metric;
        this.entries = entries;
    }

    /** Ponto de entrada - busca os dados de forma assíncrona e só então abre a GUI
     * (na main thread, como qualquer interação de inventário exige). */
    public static void openAsync(AlkaFishPlugin plugin, Player player, String metric) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Entry> entries = fetch(plugin, metric);
            Bukkit.getScheduler().runTask(plugin, () -> new TopGui(plugin, player, metric, entries).open());
        });
    }

    private static List<Entry> fetch(AlkaFishPlugin plugin, String metric) {
        try {
            return switch (metric) {
                case "nacar" -> plugin.getEconomyBridge().getTopBalances("nacar", 10).stream()
                        .map(e -> new Entry(e.uuid(), e.balance())).toList();
                case "weight" -> plugin.getPlayerFishDataRepository().findTop("total_weight", 10).stream()
                        .map(e -> new Entry(e.uuid(), e.value())).toList();
                case "time" -> plugin.getPlayerFishDataRepository().findTop("total_fishing_seconds", 10).stream()
                        .map(e -> new Entry(e.uuid(), e.value())).toList();
                default -> plugin.getPlayerFishDataRepository().findTop("total_caught", 10).stream()
                        .map(e -> new Entry(e.uuid(), e.value())).toList();
            };
        } catch (Exception ex) {
            plugin.getLogger().warning("Falha ao carregar TOP (" + metric + "): " + ex.getMessage());
            return List.of();
        }
    }

    @Override
    public void render() {
        fillBlack();

        setItem(4, createItem(Material.COMPASS, "<gold>🏆 TOP: " + metricLabel(metric),
                        "<gray>Ranking por " + metricLabel(metric).toLowerCase(),
                        "",
                        "<yellow>Clique para trocar a métrica"),
                e -> openAsync(plugin, player, nextMetric(metric)));

        if (entries.isEmpty()) {
            setItem(13, createItem(Material.BARRIER, "<red>Ninguém no ranking ainda",
                    "<gray>Volte depois de pescar um pouco!"), e -> {});
        }

        int slot = 10;
        int position = 1;
        for (Entry entry : entries) {
            if (slot > 25) break;
            if (slot % 9 == 8) slot += 2;
            if (slot > 25) break;

            OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.uuid());
            String name = offline.getName() != null ? offline.getName() : "Desconhecido";
            setItem(slot, head(name, "<yellow>#" + position + " <white>" + name,
                    "",
                    "<gray>" + metricLabel(metric) + ": <aqua>" + formatValue(metric, entry.value())), e -> {});
            slot++;
            position++;
        }

        setItem(31, createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new FishingAreaGui(plugin, player).open());
    }

    private static String metricLabel(String metric) {
        return switch (metric) {
            case "nacar" -> "Nácar";
            case "weight" -> "Peso Total";
            case "time" -> "Tempo Pescando";
            default -> "Peixes Pescados";
        };
    }

    private static String formatValue(String metric, double value) {
        return switch (metric) {
            case "nacar" -> String.format("%.0f", value);
            case "weight" -> String.format("%.2f kg", value);
            case "time" -> {
                long seconds = (long) value;
                yield String.format("%dh%02dm", seconds / 3600, (seconds % 3600) / 60);
            }
            default -> String.format("%.0f", value);
        };
    }

    private static String nextMetric(String current) {
        int idx = METRICS.indexOf(current);
        return METRICS.get((idx + 1) % METRICS.size());
    }

    private record Entry(UUID uuid, double value) {}
}
