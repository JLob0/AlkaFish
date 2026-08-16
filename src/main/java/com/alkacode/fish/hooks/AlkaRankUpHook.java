package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;

/** Integração com AlkaRankUp via ServicesManager (getSellMultiplier por prestígio). */
public final class AlkaRankUpHook extends HookBase {

    private Object api;
    private Method getSellMultiplier;

    public AlkaRankUpHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("AlkaRankUp")) return;
            Class<?> apiClass = Class.forName("com.alkacode.rankup.api.AlkaRankUpAPI");
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) return;
            this.api = registration.getProvider();
            this.getSellMultiplier = apiClass.getMethod("getSellMultiplier", UUID.class);
        } catch (Throwable e) {
            plugin.getLogger().warning("AlkaRankUp hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    public double getSellMultiplier(UUID uuid) {
        try {
            if (api != null && getSellMultiplier != null) {
                Object result = getSellMultiplier.invoke(api, uuid);
                if (result instanceof Number n) return n.doubleValue();
            }
        } catch (Throwable ignored) {}
        return 1.0;
    }
}
