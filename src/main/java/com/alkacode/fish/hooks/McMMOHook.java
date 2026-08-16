package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/** Integração com mcMMO via reflection (XP de pesca). */
public final class McMMOHook extends HookBase {

    private Method addXpMethod;
    private Method getLevelMethod;
    private boolean available = false;

    public McMMOHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!plugin.getServer().getPluginManager().isPluginEnabled("mcMMO")) return;
            Class<?> expApiClass = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
            this.addXpMethod = expApiClass.getMethod("addRawXP", Player.class, String.class, float.class);
            this.getLevelMethod = expApiClass.getMethod("getLevel", Player.class, String.class);
            this.available = true;
        } catch (Throwable e) {
            plugin.getLogger().warning("mcMMO hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public void addFishingXp(Player player, double xp) {
        if (!available || addXpMethod == null) return;
        try {
            String skillName = plugin.getConfig().getString("mcmmo.skill-name", "FISHING");
            addXpMethod.invoke(null, player, skillName, (float) xp);
            if (plugin.getConfig().getBoolean("mcmmo.enabled", true)) {
                player.sendMessage(plugin.getMessages().parse("mcmmo.xp-gained",
                        java.util.Map.of("xp", String.format("%.1f", xp))));
            }
        } catch (Throwable ignored) {}
    }

    public int getFishingLevel(Player player) {
        if (!available || getLevelMethod == null) return 0;
        try {
            String skillName = plugin.getConfig().getString("mcmmo.skill-name", "FISHING");
            Object result = getLevelMethod.invoke(null, player, skillName);
            if (result instanceof Number n) return n.intValue();
        } catch (Throwable ignored) {}
        return 0;
    }
}
