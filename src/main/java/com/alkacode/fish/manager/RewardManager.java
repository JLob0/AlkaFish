package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrega e executa as recompensas de pesca (rewards.yml). Itens e keys de crate NUNCA
 * vão direto pro inventário/console - ficam pendentes (ver PendingRewardService) até o
 * jogador reivindicar na aba "Recompensas" da Sacola de Peixes. Coins/nacar são
 * depositados na hora (não faz sentido "reivindicar" dinheiro depois).
 */
public final class RewardManager {

    private final AlkaFishPlugin plugin;
    private final Map<String, Reward> rewards = new HashMap<>();

    public RewardManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        rewards.clear();
        File file = new File(plugin.getDataFolder(), "rewards.yml");
        if (!file.exists()) plugin.saveResource("rewards.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("rewards");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection r = section.getConfigurationSection(id);
            if (r == null) continue;
            try {
                List<ItemPayload> items = new ArrayList<>();
                List<?> itemsRaw = r.getList("items");
                if (itemsRaw != null) {
                    for (Object obj : itemsRaw) {
                        if (obj instanceof Map<?, ?> map) {
                            Object matObj = map.get("material");
                            Material material = matObj != null
                                    ? Material.matchMaterial(String.valueOf(matObj).toUpperCase()) : null;
                            if (material == null) continue;
                            int amount = map.get("amount") instanceof Number n ? n.intValue() : 1;
                            items.add(new ItemPayload(material, amount));
                        }
                    }
                }
                Reward reward = new Reward(
                        r.getString("display-name", id),
                        items,
                        r.getDouble("coins", 0),
                        r.getDouble("nacar", 0),
                        r.getString("crate-id", null),
                        r.getInt("crate-key-amount", 0),
                        r.getStringList("commands"),
                        r.getString("messages.title", ""),
                        r.getString("messages.actionbar", ""));
                rewards.put(id, reward);
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar reward '" + id + "': " + e.getMessage());
            }
        }
    }

    public void executeReward(Player player, String rewardId) {
        Reward reward = rewards.get(rewardId);
        if (reward == null) return;

        if (reward.coins() > 0) plugin.getEconomyBridge().deposit(player.getUniqueId(), "coins", reward.coins());
        if (reward.nacar() > 0) plugin.getEconomyBridge().deposit(player.getUniqueId(), "nacar", reward.nacar());

        for (ItemPayload item : reward.items()) {
            plugin.getPendingRewardService().addItem(player, item.material(), item.amount(), reward.displayName());
        }
        if (reward.crateId() != null && reward.crateKeyAmount() > 0) {
            plugin.getPendingRewardService().addCrateKey(player, reward.crateId(), reward.crateKeyAmount(), reward.displayName());
        }

        // Comandos legados (ex.: broadcasts, efeitos) - continuam disparando na hora.
        for (String cmd : reward.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
        }

        if (reward.actionbar() != null && !reward.actionbar().isEmpty()) {
            player.sendActionBar(com.alkacode.fish.util.FishUtil.parse(reward.actionbar()));
        }
        if (reward.title() != null && !reward.title().isEmpty()) {
            player.showTitle(net.kyori.adventure.title.Title.title(
                    com.alkacode.fish.util.FishUtil.parse(reward.title()),
                    net.kyori.adventure.text.Component.empty(),
                    net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(2000), java.time.Duration.ofMillis(500))));
        }
    }

    public record ItemPayload(Material material, int amount) {}

    public record Reward(String displayName, List<ItemPayload> items, double coins, double nacar,
                          String crateId, int crateKeyAmount, List<String> commands, String title, String actionbar) {}
}
