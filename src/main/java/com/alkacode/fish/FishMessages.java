package com.alkacode.fish;

import com.alkacode.core.api.MessageProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

/** Provider de mensagens do AlkaFish, carregado de messages.yml. */
public final class FishMessages {

    private final AlkaFishPlugin plugin;
    private final MessageProvider messageProvider;
    private FileConfiguration config;

    public FishMessages(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        this.messageProvider = plugin.getAlkaAPI().getMessages();
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private String prefix() {
        return config.getString("prefix", "<dark_aqua>[AlkaFish] ");
    }

    public Component parse(String key, Map<String, String> placeholders) {
        String raw = config.getString(key, key);
        raw = raw.replace("{prefix}", prefix());
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return messageProvider.parse(raw);
    }

    public Component parse(String key) {
        return parse(key, null);
    }

    public void send(CommandSender target, String key) {
        target.sendMessage(parse(key));
    }

    public void send(CommandSender target, String key, Map<String, String> placeholders) {
        target.sendMessage(parse(key, placeholders));
    }
}
