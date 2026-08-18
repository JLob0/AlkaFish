package com.alkacode.fish.service;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.database.entity.FishBagEntryEntity;
import com.alkacode.fish.model.Fish;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Sacola de peixes: adiciona capturas, agrupa por tipo, vende individual/total. */
public final class FishBagService {

    private final AlkaFishPlugin plugin;
    // Cache em memória (mesmo padrão do PlayerDataManager) - evita query síncrona
    // no banco toda vez que a Fish Bag GUI abre ou é vendida (R7).
    private final Map<UUID, List<FishBagEntryEntity>> cache = new ConcurrentHashMap<>();

    public FishBagService(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    /** Carrega a sacola de todos os jogadores online pro cache (chamado no enable/reload). */
    public void loadAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadAsync(player.getUniqueId());
        }
    }

    /** Carrega a sacola do jogador do banco pro cache (chamado no join). */
    public void loadAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                cache.put(uuid, new ArrayList<>(plugin.getFishBagRepository().findBag(uuid)));
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao carregar Fish Bag de " + uuid + ": " + e.getMessage());
                cache.put(uuid, new ArrayList<>());
            }
        });
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    /** Adiciona um peixe à sacola: atualiza o cache na hora, persiste async. */
    public void add(Player player, Fish fish, double weight) {
        UUID uuid = player.getUniqueId();
        List<FishBagEntryEntity> bag = cache.computeIfAbsent(uuid, id -> new ArrayList<>());
        boolean merged = false;
        for (int i = 0; i < bag.size(); i++) {
            FishBagEntryEntity e = bag.get(i);
            if (e.fishId().equals(fish.getId())) {
                bag.set(i, new FishBagEntryEntity(e.id(), uuid, e.fishId(),
                        e.amount() + 1, e.totalWeight() + weight, System.currentTimeMillis()));
                merged = true;
                break;
            }
        }
        if (!merged) {
            bag.add(0, new FishBagEntryEntity(0, uuid, fish.getId(), 1, weight, System.currentTimeMillis()));
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getFishBagRepository().add(uuid, fish.getId(), weight);
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao adicionar à sacola: " + e.getMessage());
            }
        });
    }

    /** Lê a sacola do cache (populado no join) - nunca bloqueia a thread chamadora.
     * Retorna uma cópia: quem chama nunca deve iterar direto sobre a lista viva do cache,
     * porque sellEntries() muta essa lista (removeIf) enquanto vende - iterar a referência
     * original nesse meio tempo estoura ConcurrentModificationException. */
    public List<FishBagEntryEntity> getBag(Player player) {
        return new ArrayList<>(cache.getOrDefault(player.getUniqueId(), List.of()));
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

    /** Linha extra (estilo mcMMO: bônus separado da frase principal, fonte entre
     * parênteses) pra colar depois da mensagem de venda - vazia se não há bônus ativo. */
    public String getSellBonusLine(Player player) {
        List<String> parts = new ArrayList<>();
        double classBonus = plugin.getFishingClassManager().getSellBonus(player);
        if (classBonus > 0) {
            var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
            var fc = plugin.getFishingClassManager().getClass(stats.getActiveClassId());
            String className = fc != null ? fc.getDisplayName() : "Classe";
            parts.add("<green>+" + String.format("%.0f", classBonus) + "% <gray>(" + className + "<gray>)");
        }
        double boosterBonus = plugin.getBoosterService().getSellBonus(player);
        if (boosterBonus > 0) {
            parts.add("<green>+" + String.format("%.0f", boosterBonus) + "% <gray>(Booster<gray>)");
        }
        return String.join(" ", parts);
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
        UUID uuid = player.getUniqueId();
        List<FishBagEntryEntity> bag = cache.get(uuid);
        for (FishBagEntryEntity entry : entries) {
            Fish fish = plugin.getFishManager().getFishById(entry.fishId());
            if (fish == null) continue;
            double unitWeight = entry.amount() > 0 ? entry.totalWeight() / entry.amount() : fish.getMinWeight();
            total += unitPrice(player, fish, unitWeight) * entry.amount();
            final String fid = entry.fishId();
            if (bag != null) bag.removeIf(e -> e.fishId().equals(fid));
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

    /** Liga/desliga o auto-venda ao encher a sacola (gated por perk VIP fora daqui, mesmo
     * padrão do RodManager#toggleAutoUpgrade). */
    public void toggleAutoSellOnFull(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        stats.setAutoSellOnFull(!stats.isAutoSellOnFull());
        plugin.getPlayerDataManager().save(player.getUniqueId());
    }

    public double totalWeight(Player player) {
        double total = 0;
        for (FishBagEntryEntity e : getBag(player)) total += e.totalWeight();
        return total;
    }

    /** Quantos upgrades de limite o jogador já comprou (deriva do limite atual, não
     * precisa de um contador separado). */
    private int bagUpgradeLevel(com.alkacode.fish.model.PlayerFishStats stats) {
        double initial = plugin.getConfig().getInt("fishing-area.max-bag-items", 500);
        double perLevel = plugin.getConfig().getDouble("fishing-area.bag-upgrade.limit-per-level", 100);
        if (perLevel <= 0) return 0;
        return Math.max(0, (int) Math.round((stats.getBagCapacity() - initial) / perLevel));
    }

    /** Custo em coins do PRÓXIMO upgrade de limite (cresce exponencialmente por nível já comprado). */
    public double bagUpgradeCost(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        double base = plugin.getConfig().getDouble("fishing-area.bag-upgrade.base-cost-coins", 5000.0);
        double growth = plugin.getConfig().getDouble("fishing-area.bag-upgrade.cost-growth", 1.2);
        return base * Math.pow(growth, bagUpgradeLevel(stats));
    }

    public boolean canUpgradeBagLimit(Player player) {
        return plugin.getEconomyBridge().getBalance(player.getUniqueId(), "coins") >= bagUpgradeCost(player);
    }

    /** Compra +limit-per-level no limite da sacola, descontando coins. */
    public boolean upgradeBagLimit(Player player) {
        if (!canUpgradeBagLimit(player)) return false;
        double cost = bagUpgradeCost(player);
        double perLevel = plugin.getConfig().getDouble("fishing-area.bag-upgrade.limit-per-level", 100);
        if (!plugin.getEconomyBridge().withdraw(player.getUniqueId(), "coins", cost)) return false;
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        stats.setBagCapacity(stats.getBagCapacity() + perLevel);
        plugin.getPlayerDataManager().save(player.getUniqueId());
        return true;
    }
}
