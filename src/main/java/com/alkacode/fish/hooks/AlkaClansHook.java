package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Integração com AlkaClans via AlkaClansProvider (torneios em equipe / tag). */
public final class AlkaClansHook extends HookBase {

    private Method getAPI;
    private Object api;

    public AlkaClansHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("AlkaClans")) return;
            Class<?> providerClass = Class.forName("com.alkacode.clans.api.AlkaClansProvider");
            this.getAPI = providerClass.getMethod("getAPI");
            Object resolved = getAPI.invoke(null);
            if (resolved != null) {
                this.api = resolved;
                this.getClan = api.getClass().getMethod("getClan", UUID.class);
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("AlkaClans hook falhou: " + e.getMessage());
        }
    }

    private Method getClan;

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    /** Nome do clan do jogador, ou null se não tiver. */
    public String getClanName(UUID uuid) {
        try {
            if (api != null && getClan != null) {
                Object result = getClan.invoke(api, uuid);
                if (result instanceof Optional<?> opt && opt.isPresent()) {
                    Object clan = opt.get();
                    Method name = clan.getClass().getMethod("getName");
                    Object n = name.invoke(clan);
                    return n != null ? n.toString() : null;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
