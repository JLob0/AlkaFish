package com.alkacode.fish.placeholder;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.PlayerFishStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Expansão %alkafish_*% para o PlaceholderAPI. */
public final class AlkaFishExpansion extends PlaceholderExpansion {

    private final AlkaFishPlugin plugin;

    public AlkaFishExpansion(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "alkafish"; }
    @Override public @NotNull String getAuthor() { return "AlkaCode"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        return switch (params) {
            case "level" -> String.valueOf(stats.getLevel());
            case "xp" -> String.format(java.util.Locale.US, "%.1f", stats.getXp());
            case "xp_needed" -> String.format(java.util.Locale.US, "%.1f", PlayerFishStats.getXpForLevel(stats.getLevel() + 1));
            case "total_caught" -> String.valueOf(stats.getTotalCaught());
            case "biggest_length" -> String.format(java.util.Locale.US, "%.1f", stats.getBiggestLength());
            case "total_weight" -> String.format(java.util.Locale.US, "%.2f", stats.getTotalWeight());
            case "bag_weight" -> String.format(java.util.Locale.US, "%.2f", stats.getCurrentBagWeight());
            case "bag_capacity" -> String.format(java.util.Locale.US, "%.2f", stats.getBagCapacity());
            case "tournament_active" -> String.valueOf(plugin.getTournamentManager().getActiveTournament() != null);
            case "tournament_position" -> String.valueOf(plugin.getTournamentManager().getPlayerPosition(player));
            case "tournament_score" -> String.format(java.util.Locale.US, "%.2f", plugin.getTournamentManager().getPlayerScore(player));
            case "rod_level" -> String.valueOf(stats.getRodLevel());
            case "rod_name" -> {
                var rod = plugin.getRodManager().getRodById(stats.getRodId());
                yield rod != null ? rod.getDisplayName() : "Nenhuma";
            }
            case "rod_broken" -> stats.isRodBroken() ? "<red>Quebrada" : "<green>OK";
            case "nacar" -> String.format("%.0f", stats.getNacar());
            case "nacar_next" -> String.format("%.0f", stats.getNacarNext());
            case "class" -> {
                var fc = plugin.getFishingClassManager().getClass(stats.getActiveClassId());
                yield fc != null ? fc.getDisplayName() : "Nenhuma";
            }
            case "fished_count" -> String.valueOf(plugin.getFishingAreaManager().getFishCount(player));
            case "fishing_time" -> plugin.getFishingAreaManager().getFishingTime(player);
            case "status" -> plugin.getFishingTask().isFishing(player)
                    ? "<green>Pescando... " + plugin.getFishingAreaManager().getFishingTime(player)
                    : "<gray>Inativo";
            case "in_area" -> plugin.getFishingAreaManager().isInArea(player) ? "Sim" : "Não";
            case "mcmmo_level" -> {
                if (plugin.getMcMMOHook() != null && plugin.getMcMMOHook().isAvailable()) {
                    yield String.valueOf(plugin.getMcMMOHook().getFishingLevel(player));
                }
                yield "0";
            }
            case "booster_active" -> plugin.getBoosterService().isActive(player) ? "Sim" : "Não";
            case "booster_type" -> {
                var b = plugin.getBoosterService().getBooster(player);
                yield b != null ? b.getType() : "";
            }
            case "booster_time" -> {
                var b = plugin.getBoosterService().getBooster(player);
                yield b != null ? String.valueOf(b.getRemainingSeconds()) : "0";
            }
            case "booster_multiplier" -> {
                var b = plugin.getBoosterService().getBooster(player);
                yield b != null ? String.format("%.0f", b.getMultiplier()) : "0";
            }
            case "total_fished" -> String.valueOf(stats.getTotalCaught());
            case "rod_weight" -> {
                var rod = plugin.getRodManager().getRodById(stats.getRodId());
                yield rod != null ? String.format("%.0f", rod.getSupportedWeight()) : "0";
            }
            default -> null;
        };
    }
}
