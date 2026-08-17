package com.alkacode.fish.service;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.database.entity.FishBagEntryEntity;
import com.alkacode.fish.model.Fish;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** Sacola de peixes: adiciona capturas, agrupa por tipo, vende individual/total. */
public final class FishBagService {

    private final AlkaFishPlugin plugin;

    public FishBagService(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    /** Adiciona um peixe à sacola (async). */
    public void add(Player player, Fish fish, double weight) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getFishBagRepository().add(player.getUniqueId(), fish.getId(), weight);
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao adicionar à sacola: " + e.getMessage());
            }
        });
    }

    public List<FishBagEntryEntity> getBag(Player player) {
        try {
            return plugin.getFishBagRepository().findBag(player.getUniqueId());
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Preço unitário de um peixe (com multiplicadores de rank/classe/torneio/booster). */
    public double unitPrice(Player player, Fish fish, double unitWeight) {
        double price = fish.calculatePrice(unitWeight);
        if (plugin.getAlkaRankUpHook() != null && plugin.getAlkaRankUpHook().isAvailable()) {
            price *= plugin.getAlkaRankUpHook().getSellMultiplier(player.getUniqueId());
        }
        price *= plugin.getTournamentManager().getActiveSellMultiplier();
        price *= (1 + plugin.getFishingClassManager().getSellBonus(player) / 100.0);
        price *= (1 + plugin.getBoosterService().getSellBonus(player) / 100.0);
        return price;
    }

    /** Vende um tipo de peixe da sacola. Retorna total pago em coins. */
    public double sell(Player player, String fishId) {
        List<FishBagEntryEntity> bag = getBag(player);
        FishBagEntryEntity target = null;
        for (FishBagEntryEntity e : bag) {
            if (e.fishId().equals(fishId)) { target = e; break; }
        }
        if (target == null) return 0;
        return sellEntries(player, List.of(target));
    }

    /** Vende todos os peixes da sacola. Retorna total pago em coins. */
    public double sellAll(Player player) {
        return sellEntries(player, getBag(player));
    }

    private double sellEntries(Player player, List<FishBagEntryEntity> entries) {
        double total = 0;
        for (FishBagEntryEntity entry : entries) {
            Fish fish = plugin.getFishManager().getFishById(entry.fishId());
            if (fish == null) continue;
            double unitWeight = entry.amount() > 0 ? entry.totalWeight() / entry.amount() : fish.getMinWeight();
            total += unitPrice(player, fish, unitWeight) * entry.amount();
            final String fid = entry.fishId();
            final UUID uuid = player.getUniqueId();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getFishBagRepository().removeFish(uuid, fid);
                } catch (Exception ignored) {}
            });
        }
        plugin.getEconomyBridge().deposit(player.getUniqueId(), "coins", total);

        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        stats.removeFromBag(stats.getCurrentBagWeight());
        plugin.getPlayerDataManager().save(player.getUniqueId());
        return total;
    }

    public double totalWeight(Player player) {
        try {
            return plugin.getFishBagRepository().totalWeight(player.getUniqueId());
        } catch (Exception e) {
            return 0;
        }
    }
}
