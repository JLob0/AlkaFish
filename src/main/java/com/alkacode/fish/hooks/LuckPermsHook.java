package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Integração com LuckPerms via reflection (grupo do jogador p/ boost de sorte). */
public final class LuckPermsHook extends HookBase {

    private Method getUser;
    private Method getPrimaryGroup;
    private Object api;

    public LuckPermsHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return;
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Method get = providerClass.getMethod("get");
            this.api = get.invoke(null);
            Class<?> userManagerClass = Class.forName("net.luckperms.api.user.UserManager");
            this.getUser = userManagerClass.getMethod("getUser", UUID.class);
        } catch (Throwable e) {
            plugin.getLogger().warning("LuckPerms hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    /** Grupo primário do jogador (via async user). Completa com "" se indisponível. */
    public CompletableFuture<String> getPrimaryGroup(UUID uuid) {
        try {
            if (api != null && getUser != null) {
                Object userManager = getUser.invoke(api);
                Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
                if (user != null) {
                    Method primaryGroup = user.getClass().getMethod("getPrimaryGroup");
                    Object group = primaryGroup.invoke(user);
                    return CompletableFuture.completedFuture(group != null ? group.toString() : "");
                }
            }
        } catch (Throwable ignored) {}
        return CompletableFuture.completedFuture("");
    }
}
