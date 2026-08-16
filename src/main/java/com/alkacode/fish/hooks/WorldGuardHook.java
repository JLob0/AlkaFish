package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.Method;

/** Integração com WorldGuard via reflection (restrição de zonas de pesca). */
public final class WorldGuardHook extends HookBase {

    private Object regionQuery;
    private boolean available;

    public WorldGuardHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null
                    || !Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) return;
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wg = wgClass.getMethod("getInstance").invoke(null);
            Object platform = wg.getClass().getMethod("getPlatform").invoke(wg);
            Object rgc = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            this.regionQuery = rgc.getClass().getMethod("createQuery").invoke(rgc);
            this.available = true;
        } catch (Throwable e) {
            plugin.getLogger().warning("WorldGuard hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /** true se a localização está dentro de alguma região protegida. */
    public boolean isInRegion(Location location) {
        if (!available) return false;
        try {
            Object result = regionQuery.getClass()
                    .getMethod("getApplicableRegions", Location.class)
                    .invoke(regionQuery, location);
            int count = (int) result.getClass().getMethod("size").invoke(result);
            return count > 0;
        } catch (Throwable t) {
            return false;
        }
    }
}
