package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Carrega e executa as recompensas de pesca (rewards.yml). */
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
            Reward reward = new Reward(
                    r.getString("display-name", id),
                    r.getStringList("commands"),
                    r.getString("messages.title", ""),
                    r.getString("messages.actionbar", ""));
            rewards.put(id, reward);
        }
    }

    public void executeReward(Player player, String rewardId) {
        Reward reward = rewards.get(rewardId);
        if (reward == null) return;
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

    public record Reward(String displayName, List<String> commands, String title, String actionbar) {}
}
