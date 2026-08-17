package com.alkacode.fish.service;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Booster;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Gerencia boosters ativos (FISH_CHANCE / CORAL_MULTIPLIER / SELL_BONUS). */
public final class BoosterService {

    private final AlkaFishPlugin plugin;
    private final Map<UUID, Booster> boosters = new ConcurrentHashMap<>();

    public BoosterService(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        // Task periódica de expiração (1s)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            boosters.entrySet().removeIf(e -> e.getValue().isExpired());
        }, 20L, 20L);
    }

    public void activate(Player player, String type, double multiplier, int durationSeconds) {
        boosters.put(player.getUniqueId(), new Booster(type, multiplier, System.currentTimeMillis(), durationSeconds));
        player.sendMessage(plugin.getMessages().parse("booster.activated", java.util.Map.of(
                "type", type, "multiplier", String.format("%.0f", multiplier), "duration", String.valueOf(durationSeconds))));
    }

    public Booster getBooster(Player player) {
        Booster b = boosters.get(player.getUniqueId());
        if (b != null && b.isExpired()) {
            boosters.remove(player.getUniqueId());
            player.sendMessage(plugin.getMessages().parse("booster.expired"));
            return null;
        }
        return b;
    }

    public boolean isActive(Player player) {
        return getBooster(player) != null;
    }

    private double bonusFor(Player player, String type) {
        Booster b = getBooster(player);
        if (b == null || !b.getType().equalsIgnoreCase(type)) return 0;
        return b.getMultiplier();
    }

    /** +% de chance de peixe melhor (FISH_CHANCE). */
    public double getFishChance(Player player) {
        return bonusFor(player, "FISH_CHANCE");
    }

    /** +% de corais ganhos (CORAL_MULTIPLIER). */
    public double getCoralMultiplier(Player player) {
        return bonusFor(player, "CORAL_MULTIPLIER");
    }

    /** +% no preço de venda (SELL_BONUS). */
    public double getSellBonus(Player player) {
        return bonusFor(player, "SELL_BONUS");
    }

    public Map<UUID, Booster> getAll() {
        return boosters;
    }
}
