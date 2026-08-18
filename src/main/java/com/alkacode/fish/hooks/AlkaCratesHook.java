package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/** Integração com AlkaCrates via ServicesManager (chave de crate como recompensa de pesca). */
public final class AlkaCratesHook extends HookBase {

    private Object api;
    private Method giveKey;
    private Method crateExists;

    public AlkaCratesHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("AlkaCrates")) return;
            Class<?> apiClass = Class.forName("com.alkacode.crates.api.AlkaCratesAPI");
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) return;
            this.api = registration.getProvider();
            this.giveKey = apiClass.getMethod("giveKey", Player.class, String.class, int.class);
            this.crateExists = apiClass.getMethod("crateExists", String.class);
        } catch (Throwable e) {
            plugin.getLogger().warning("AlkaCrates hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    /** true se o crateId existir configurado no AlkaCrates. */
    public boolean crateExists(String crateId) {
        try {
            if (api != null && crateExists != null) {
                Object result = crateExists.invoke(api, crateId);
                return result instanceof Boolean b && b;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /** Dá `amount` keys físicas da crate `crateId` pro jogador. */
    public void giveKey(Player player, String crateId, int amount) {
        try {
            if (api != null && giveKey != null) {
                giveKey.invoke(api, player, crateId, amount);
            }
        } catch (Throwable ignored) {}
    }
}
