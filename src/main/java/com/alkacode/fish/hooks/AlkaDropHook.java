package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;

/** Integração com AlkaDrop via ServicesManager (entrega de drops). */
public final class AlkaDropHook extends HookBase {

    private Object api;
    private Method deliverDrops;

    public AlkaDropHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("AlkaDrop")) return;
            Class<?> apiClass = Class.forName("com.alkacode.drop.api.AlkaDropAPI");
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) return;
            this.api = registration.getProvider();
            this.deliverDrops = apiClass.getMethod("deliverDrops", Player.class, List.class, Location.class);
        } catch (Throwable e) {
            plugin.getLogger().warning("AlkaDrop hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    /** Entrega drops respeitando a preferência de coleta do jogador no AlkaDrop. */
    public void deliverDrops(Player player, List<ItemStack> drops, Location location) {
        try {
            if (api != null && deliverDrops != null) {
                deliverDrops.invoke(api, player, drops, location);
            }
        } catch (Throwable ignored) {}
    }
}
