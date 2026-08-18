package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.RodEnchantment;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Gerencia os encantamentos custom das varas (carrega enchantments.yml). */
public final class EnchantmentManager {

    private final AlkaFishPlugin plugin;
    private final Map<String, RodEnchantment> enchantments = new HashMap<>();

    public EnchantmentManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        enchantments.clear();
        File file = new File(plugin.getDataFolder(), "enchantments.yml");
        if (!file.exists()) plugin.saveResource("enchantments.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("enchantments");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection e = section.getConfigurationSection(id);
            if (e == null) continue;
            try {
                List<RodEnchantment.EnchantCommand> commands = new ArrayList<>();
                for (String c : e.getStringList("commands")) {
                    String[] parts = c.split(",");
                    if (parts.length == 2) {
                        commands.add(new RodEnchantment.EnchantCommand(Double.parseDouble(parts[0]), parts[1]));
                    }
                }
                RodEnchantment.EnchantMessages messages = null;
                ConfigurationSection msg = e.getConfigurationSection("messages");
                if (msg != null) {
                    messages = new RodEnchantment.EnchantMessages(
                            msg.getString("title", ""), msg.getString("actionbar", ""), msg.getStringList("chat"));
                }
                Map<String, RodEnchantment.MenuItem> menuItems = new HashMap<>();
                ConfigurationSection menu = e.getConfigurationSection("menu-item");
                if (menu != null) {
                    for (String state : menu.getKeys(false)) {
                        RodEnchantment.MenuItem mi = RodEnchantment.MenuItem.from(menu.getConfigurationSection(state));
                        if (mi != null) menuItems.put(state, mi);
                    }
                }
                RodEnchantment enc = new RodEnchantment(
                        id,
                        e.getString("display-name", id),
                        e.getString("description", ""),
                        e.getInt("default-level", 0),
                        e.getInt("max-level", 10),
                        e.getDouble("percentage-per-level", 0),
                        e.getDouble("multiplier-per-level", 0),
                        e.getInt("cost.base", 20),
                        e.getInt("cost.per-level", 15),
                        commands,
                        messages,
                        menuItems);
                enchantments.put(id, enc);
            } catch (Exception ex) {
                plugin.getLogger().warning("Erro ao carregar encantamento '" + id + "': " + ex.getMessage());
            }
        }
    }

    public Collection<RodEnchantment> getAllEnchantments() {
        return enchantments.values();
    }

    public RodEnchantment getEnchantment(String id) {
        return enchantments.get(id);
    }

    public int getTotalLuckBonus(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        RodEnchantment lucky = enchantments.get("lucky");
        if (lucky == null) return 0;
        return (int) lucky.percentBonus(stats.getRodEnchantLevel("lucky"));
    }

    public double getTotalMultiplierBonus(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        RodEnchantment mult = enchantments.get("multiplier");
        if (mult == null) return 0;
        return (mult.multiplierBonus(stats.getRodEnchantLevel("multiplier")) - 1.0);
    }

    public boolean canUpgrade(Player player, String enchantId) {
        RodEnchantment enc = enchantments.get(enchantId);
        if (enc == null) return false;
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        int current = stats.getRodEnchantLevel(enchantId);
        if (enc.getMaxLevel() != -1 && current >= enc.getMaxLevel()) return false;
        int cost = enc.costForLevel(current);
        return plugin.getEconomyBridge().getBalance(player.getUniqueId(), "nacar") >= cost;
    }

    public boolean upgradeEnchantment(Player player, String enchantId) {
        RodEnchantment enc = enchantments.get(enchantId);
        if (enc == null) return false;
        if (!canUpgrade(player, enchantId)) return false;
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        int current = stats.getRodEnchantLevel(enchantId);
        int cost = enc.costForLevel(current);
        plugin.getEconomyBridge().withdraw(player.getUniqueId(), "nacar", cost);
        stats.setRodEnchantLevel(enchantId, current + 1);
        plugin.getPlayerDataManager().save(player.getUniqueId());
        // Sem isso a vara na mão ficava com o lore de encantamento congelado até
        // sair/voltar da área - o mesmo tipo de bug já corrigido pro nacar por captura.
        plugin.getRodManager().refreshRodItem(player);
        player.sendMessage(plugin.getMessages().parse("rod.enchant-upgraded",
                java.util.Map.of("enchant", enc.getDisplayName(), "level", String.valueOf(current + 1))));
        return true;
    }

    /** Processa o Keychain (chance de drops/comandos ao pescar). */
    public void processKeychain(Player player) {
        RodEnchantment keychain = enchantments.get("keychain");
        if (keychain == null) return;
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        int level = stats.getRodEnchantLevel("keychain");
        if (level <= 0) return;
        double chance = keychain.percentBonus(level);
        if (ThreadLocalRandom.current().nextDouble(0, 100) > chance) return;

        for (RodEnchantment.EnchantCommand cmd : keychain.getCommands()) {
            if (ThreadLocalRandom.current().nextDouble(0, 100) <= cmd.chance()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.command().replace("{player}", player.getName()));
            }
        }
        if (keychain.getMessages() != null) {
            for (String chat : keychain.getMessages().chat()) {
                player.sendMessage(com.alkacode.fish.util.FishUtil.parse(chat));
            }
            if (keychain.getMessages().title() != null && !keychain.getMessages().title().isEmpty()) {
                player.showTitle(net.kyori.adventure.title.Title.title(
                        com.alkacode.fish.util.FishUtil.parse(keychain.getMessages().title()),
                        net.kyori.adventure.text.Component.empty(),
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(2000), java.time.Duration.ofMillis(500))));
            }
        }
    }

    /** Formata a linha de encantamentos para o lore da vara. */
    public String formatEnchants(Map<String, Integer> enchants) {
        if (enchants.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : enchants.entrySet()) {
            RodEnchantment enc = enchantments.get(e.getKey());
            if (enc == null || e.getValue() <= 0) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(enc.getDisplayName()).append(" <gray>Lvl.").append(e.getValue());
        }
        return sb.toString();
    }
}
