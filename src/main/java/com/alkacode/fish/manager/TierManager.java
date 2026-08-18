package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishRarity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/** Carrega tiers.yml - quais raridades de peixe recebem aviso especial ao pescar
 * (título na tela + broadcast), configurável em vez de hardcoded no ordinal do enum. */
public final class TierManager {

    private final AlkaFishPlugin plugin;
    private final Map<FishRarity, TierAnnouncement> announcements = new EnumMap<>(FishRarity.class);

    public TierManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        announcements.clear();
        File file = new File(plugin.getDataFolder(), "tiers.yml");
        if (!file.exists()) plugin.saveResource("tiers.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("tiers");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection t = section.getConfigurationSection(key);
            if (t == null) continue;
            try {
                FishRarity rarity = FishRarity.valueOf(key.toUpperCase());
                announcements.put(rarity, new TierAnnouncement(
                        t.getString("title", ""),
                        t.getString("subtitle", ""),
                        t.getString("broadcast", "")));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Raridade inválida em tiers.yml: '" + key + "'");
            }
        }
    }

    /** null se essa raridade não tem aviso especial configurado. */
    public TierAnnouncement getAnnouncement(FishRarity rarity) {
        return announcements.get(rarity);
    }

    public record TierAnnouncement(String title, String subtitle, String broadcast) {}
}
