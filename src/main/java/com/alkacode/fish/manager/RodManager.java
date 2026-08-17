package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gerencia varas de pesca (carrega rods.yml, upa/repara, dá item). */
public final class RodManager {

    private final AlkaFishPlugin plugin;
    private final Map<String, FishingRod> rodsById = new HashMap<>();
    private final List<FishingRod> rodsByLevel = new ArrayList<>();

    public RodManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        rodsById.clear();
        rodsByLevel.clear();
        File file = new File(plugin.getDataFolder(), "rods.yml");
        if (!file.exists()) plugin.saveResource("rods.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection rodSection = config.getConfigurationSection("rods");
        if (rodSection == null) return;

        for (String id : rodSection.getKeys(false)) {
            ConfigurationSection section = rodSection.getConfigurationSection(id);
            if (section == null) continue;
            try {
                List<FishingRod.RodReward> rewards = new ArrayList<>();
                for (String r : section.getStringList("rewards")) {
                    String[] parts = r.split(",");
                    if (parts.length == 2) {
                        rewards.add(new FishingRod.RodReward(Double.parseDouble(parts[0]), parts[1]));
                    }
                }
                FishingRod rod = new FishingRod(
                        id,
                        section.getInt("level", 1),
                        section.getString("display-name", id),
                        org.bukkit.Material.valueOf(section.getString("material", "FISHING_ROD").toUpperCase()),
                        section.getInt("custom-model-data", 0),
                        section.getDouble("supported-weight", 5.0),
                        section.getInt("delay-seconds", 8),
                        section.getBoolean("break-on-heavy", true),
                        section.getBoolean("unbreakable", false),
                        section.getDouble("break-chance", 0.0),
                        section.getBoolean("allow-auto-sell", false),
                        section.getDouble("repair-cost.coins", 0),
                        section.getDouble("repair-cost.nacar", 0),
                        section.getDouble("upgrade-cost.coins", 0),
                        section.getDouble("upgrade-cost.nacar", 0),
                        section.getInt("enchant-base-cost", 10),
                        rewards,
                        section.getString("item.name", id),
                        section.getStringList("item.lore"));
                rodsById.put(id, rod);
                rodsByLevel.add(rod);
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar vara '" + id + "': " + e.getMessage());
            }
        }
        rodsByLevel.sort(Comparator.comparingInt(FishingRod::getLevel));
    }

    public FishingRod getRodById(String id) {
        return rodsById.get(id);
    }

    public FishingRod getDefaultRod() {
        return rodsByLevel.isEmpty() ? null : rodsByLevel.get(0);
    }

    public FishingRod getNextRod(FishingRod rod) {
        int idx = rodsByLevel.indexOf(rod);
        if (idx < 0 || idx + 1 >= rodsByLevel.size()) return null;
        return rodsByLevel.get(idx + 1);
    }

    public boolean canUpgrade(Player player, FishingRod current) {
        FishingRod next = getNextRod(current);
        if (next == null) return false;
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        double coins = plugin.getEconomyBridge().getBalance(player.getUniqueId(), "coins");
        if (coins < next.getUpgradeCostCoins()) return false;
        return stats.getNacar() >= next.getUpgradeCostNacar();
    }

    public boolean upgradeRod(Player player, FishingRod current) {
        FishingRod next = getNextRod(current);
        if (next == null) return false;
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        plugin.getEconomyBridge().withdraw(player.getUniqueId(), "coins", next.getUpgradeCostCoins());
        stats.setNacar(stats.getNacar() - next.getUpgradeCostNacar());
        stats.setRodId(next.getId());
        stats.setRodLevel(next.getLevel());
        giveRodItem(player, next);
        plugin.getPlayerDataManager().save(player.getUniqueId());
        player.sendMessage(plugin.getMessages().parse("rod.upgraded",
                java.util.Map.of("level", String.valueOf(next.getLevel()))));
        return true;
    }

    public boolean canRepair(Player player, FishingRod rod) {
        return rod != null;
    }

    public boolean repairRod(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        FishingRod rod = getRodById(stats.getRodId());
        if (rod == null) rod = getDefaultRod();
        if (rod == null) return false;
        // Reparo é gratuito e sempre funciona (não depende de coins/nacar/economia).
        stats.setRodBroken(false);
        giveRodItem(player, rod);
        plugin.getPlayerDataManager().save(player.getUniqueId());
        player.sendMessage(plugin.getMessages().parse("rod.repaired"));
        return true;
    }

    /** Dá a vara no slot configurado (rod-slot). */
    public void giveRodItem(Player player, FishingRod rod) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        ItemStack item = rod.toItemStack(plugin, stats.getRodEnchantLevels(), stats.getNacar(), stats.getNacarNext());
        int slot = getRodSlot();
        player.getInventory().setItem(slot, item);
        // Sem isso a vara ficava SO na hotbar, nao necessariamente na mao - se o jogador
        // nao estivesse ja no slot certo, PlayerFishEvent nunca disparava (vanilla exige
        // segurar um item de vara de verdade) e isHoldingRod() no ciclo AFK falhava,
        // derrubando a sessao no primeiro tick.
        player.getInventory().setHeldItemSlot(slot);
    }

    /** Remove a vara do slot configurado (rod-slot) se ela for do AlkaFish. */
    public void removeRodItem(Player player) {
        int slot = getRodSlot();
        ItemStack current = player.getInventory().getItem(slot);
        if (current == null || !current.hasItemMeta()) return;
        if (current.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "alkafish_rod_id"))) {
            player.getInventory().setItem(slot, null);
        }
    }

    private int getRodSlot() {
        int slot = plugin.getConfig().getInt("rods.rod-slot", 0);
        return (slot < 0 || slot >= 36) ? 0 : slot;
    }

    /** true se o jogador está segurando uma vara de pesca do AlkaFish na mão. */
    public boolean isHoldingRod(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "alkafish_rod_id"));
    }
}
