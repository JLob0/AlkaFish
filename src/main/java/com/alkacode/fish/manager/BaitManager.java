package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Bait;
import com.alkacode.fish.model.FishRarity;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Gerencia iscas ativas por jogador e carrega as definições de baits.yml. */
public final class BaitManager {

    private final AlkaFishPlugin plugin;
    private final Map<String, Bait> baitsById = new HashMap<>();
    private final Map<UUID, ActiveBait> activeBaits = new ConcurrentHashMap<>();

    public BaitManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        baitsById.clear();
        File file = new File(plugin.getDataFolder(), "baits.yml");
        if (!file.exists()) {
            plugin.saveResource("baits.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection baitSection = config.getConfigurationSection("baits");
        if (baitSection == null) return;

        for (String id : baitSection.getKeys(false)) {
            ConfigurationSection section = baitSection.getConfigurationSection(id);
            if (section == null) continue;
            try {
                Map<FishRarity, Double> rarityBonus = new HashMap<>();
                ConfigurationSection bonus = section.getConfigurationSection("rarity-bonus");
                if (bonus != null) {
                    for (String rarity : bonus.getKeys(false)) {
                        try {
                            rarityBonus.put(FishRarity.valueOf(rarity.toUpperCase()), bonus.getDouble(rarity, 0.0));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                double total = rarityBonus.values().stream().mapToDouble(Double::doubleValue).sum();
                ItemStack baseItem = new ItemStack(org.bukkit.Material.valueOf(
                        section.getString("material", "WHEAT_SEEDS").toUpperCase()));
                if (section.getInt("custom-model-data", 0) > 0) {
                    int cmd = section.getInt("custom-model-data");
                    baseItem.editMeta(meta -> meta.setCustomModelData(cmd));
                }
                Bait bait = new Bait(
                        id,
                        section.getString("display-name", id),
                        baseItem,
                        section.getInt("duration-seconds", 60),
                        section.getInt("radius", 5),
                        rarityBonus,
                        total);
                baitsById.put(id, bait);
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar isca '" + id + "': " + e.getMessage());
            }
        }
    }

    public Bait getBaitById(String id) {
        return baitsById.get(id);
    }

    public boolean hasActiveBait(UUID playerUuid, Location hookLoc) {
        ActiveBait bait = activeBaits.get(playerUuid);
        if (bait == null) return false;
        if (System.currentTimeMillis() > bait.expiry) {
            activeBaits.remove(playerUuid);
            return false;
        }
        return bait.location.distance(hookLoc) <= bait.radius;
    }

    public double getBaitLuckBonus(UUID playerUuid) {
        ActiveBait bait = activeBaits.get(playerUuid);
        return bait != null ? bait.luckBonus : 0.0;
    }

    public void activateBait(Player player, Bait bait, Location location) {
        activeBaits.put(player.getUniqueId(), new ActiveBait(
                bait.getId(),
                System.currentTimeMillis() + (bait.getDurationSeconds() * 1000L),
                location,
                bait.getRadius(),
                bait.getTotalLuckBonus()));
        player.sendMessage(com.alkacode.fish.util.FishUtil.parse("<green>Isca ativada! Duração: <yellow>" + bait.getDurationSeconds() + "s"));
    }

    public boolean isBaitItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "alkafish_bait_id"));
    }

    public String getBaitIdFromItem(ItemStack item) {
        if (!isBaitItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "alkafish_bait_id"),
                org.bukkit.persistence.PersistentDataType.STRING);
    }

    private record ActiveBait(String baitId, long expiry, Location location, double radius, double luckBonus) {}
}
