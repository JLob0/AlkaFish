package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingClass;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gerencia as classes de armadura de pesca (carrega classes.yml). */
public final class FishingClassManager {

    private final AlkaFishPlugin plugin;
    private final Map<String, FishingClass> classes = new HashMap<>();
    private final Map<Integer, FishingClass> classesByOrder = new HashMap<>();

    public FishingClassManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        classes.clear();
        classesByOrder.clear();
        File file = new File(plugin.getDataFolder(), "classes.yml");
        if (!file.exists()) plugin.saveResource("classes.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("classes");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection c = section.getConfigurationSection(id);
            if (c == null) continue;
            try {
                Color color = parseColor(c.getString("color", "255:255:255"));
                List<FishingClass.ClassEffect> effects = new ArrayList<>();
                for (String eff : c.getStringList("effects")) {
                    String[] parts = eff.split(":");
                    PotionEffectType type = org.bukkit.Registry.EFFECT.get(org.bukkit.NamespacedKey.minecraft(parts[0].toLowerCase(java.util.Locale.ROOT)));
                    if (type != null) {
                        effects.add(new FishingClass.ClassEffect(type, parts.length > 1 ? Integer.parseInt(parts[1]) : 1));
                    }
                }
                Map<String, FishingClass.MenuItem> menuItems = new HashMap<>();
                ConfigurationSection menu = c.getConfigurationSection("menu");
                if (menu != null) {
                    for (String state : menu.getKeys(false)) {
                        ConfigurationSection mi = menu.getConfigurationSection(state);
                        if (mi == null) continue;
                        menuItems.put(state, new FishingClass.MenuItem(
                                org.bukkit.Material.valueOf(mi.getString("material", "LEATHER_CHESTPLATE").toUpperCase()),
                                mi.getString("name", id),
                                parseColor(mi.getString("color", "255:255:255")),
                                mi.getStringList("lore")));
                    }
                }
                ConfigurationSection bonus = c.getConfigurationSection("bonus");
                ConfigurationSection prices = c.getConfigurationSection("prices");
                FishingClass fc = new FishingClass(
                        id,
                        c.getInt("order", 0),
                        c.getString("display-name", id),
                        color,
                        c.getString("permission", ""),
                        effects,
                        bonus != null ? bonus.getDouble("fish-chance", 0) : 0,
                        bonus != null ? bonus.getDouble("coin-bonus", 0) : 0,
                        bonus != null ? bonus.getDouble("sell-bonus", 0) : 0,
                        prices != null ? prices.getDouble("coins", 0) : 0,
                        prices != null ? prices.getDouble("nacar", 0) : 0,
                        c.getString("activator.name", id),
                        c.getStringList("activator.lore"),
                        menuItems);
                classes.put(id, fc);
                classesByOrder.put(fc.getOrder(), fc);
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar classe '" + id + "': " + e.getMessage());
            }
        }
    }

    private Color parseColor(String s) {
        try {
            String[] parts = s.split(":");
            int r = Integer.parseInt(parts[0]);
            int g = Integer.parseInt(parts[1]);
            int b = Integer.parseInt(parts[2]);
            return Color.fromRGB(r, g, b);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    public Collection<FishingClass> getAllClasses() {
        return classes.values().stream().sorted(Comparator.comparingInt(FishingClass::getOrder)).toList();
    }

    public FishingClass getClass(String id) {
        return classes.get(id);
    }

    public boolean canUpgrade(Player player, String classId) {
        FishingClass fc = classes.get(classId);
        if (fc == null) return false;
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());

        if (plugin.getConfig().getBoolean("classes.ordered", true)) {
            if (fc.getOrder() > 0) {
                FishingClass prev = classesByOrder.get(fc.getOrder() - 1);
                if (prev != null && !stats.getUnlockedClasses().contains(prev.getId())) return false;
            }
        }
        if (stats.getUnlockedClasses().contains(classId)) return true;
        if (!fc.getPermission().isEmpty() && !player.hasPermission(fc.getPermission())) return false;
        double coins = plugin.getEconomyBridge().getBalance(player.getUniqueId(), "coins");
        if (coins < fc.getPriceCoins()) return false;
        return stats.getNacar() >= fc.getPriceNacar();
    }

    public void upgradeClass(Player player, String classId) {
        FishingClass fc = classes.get(classId);
        if (fc == null) return;
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());

        if (!stats.getUnlockedClasses().contains(classId)) {
            plugin.getEconomyBridge().withdraw(player.getUniqueId(), "coins", fc.getPriceCoins());
            stats.setNacar(stats.getNacar() - fc.getPriceNacar());
            stats.getUnlockedClasses().add(classId);
        }
        equipClass(player, classId);
        plugin.getPlayerDataManager().save(player.getUniqueId());
        player.sendMessage(plugin.getMessages().parse("class.upgraded",
                java.util.Map.of("class", fc.getDisplayName())));
    }

    public void equipClass(Player player, String classId) {
        FishingClass fc = classes.get(classId);
        if (fc == null) return;
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        clearClassEffects(player);
        stats.setActiveClassId(classId);

        for (FishingClass.ClassEffect effect : fc.getEffects()) {
            if (effect.type() != null) {
                player.addPotionEffect(new PotionEffect(effect.type(), Integer.MAX_VALUE, effect.level() - 1, false, false));
            }
        }
        plugin.getPlayerDataManager().save(player.getUniqueId());
        player.sendMessage(plugin.getMessages().parse("class.equipped",
                java.util.Map.of("class", fc.getDisplayName())));
    }

    public void clearClassEffects(Player player) {
        for (FishingClass fc : classes.values()) {
            for (FishingClass.ClassEffect effect : fc.getEffects()) {
                if (effect.type() != null) player.removePotionEffect(effect.type());
            }
        }
    }

    public double getFishChanceBonus(Player player) {
        FishingClass fc = classes.get(plugin.getPlayerDataManager().getStats(player.getUniqueId()).getActiveClassId());
        return fc != null ? fc.getFishChanceBonus() : 0;
    }

    public double getCoinBonus(Player player) {
        FishingClass fc = classes.get(plugin.getPlayerDataManager().getStats(player.getUniqueId()).getActiveClassId());
        return fc != null ? fc.getCoinBonus() : 0;
    }

    public double getSellBonus(Player player) {
        FishingClass fc = classes.get(plugin.getPlayerDataManager().getStats(player.getUniqueId()).getActiveClassId());
        return fc != null ? fc.getSellBonus() : 0;
    }
}
