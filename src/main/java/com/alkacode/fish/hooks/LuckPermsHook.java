package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LuckPermsHook extends HookBase {

    private Method getUserManager;
    private Object api;

    public LuckPermsHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return;
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Method get = providerClass.getMethod("get");
            this.api = get.invoke(null);
            this.getUserManager = api.getClass().getMethod("getUserManager");
        } catch (Throwable e) {
            plugin.getLogger().warning("LuckPerms hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    public CompletableFuture<String> getPrimaryGroup(UUID uuid) {
        try {
            if (api != null && getUserManager != null) {
                Object userManager = getUserManager.invoke(api);
                if (userManager == null) return CompletableFuture.completedFuture("");
                Method getUser = userManager.getClass().getMethod("getUser", UUID.class);
                Object user = getUser.invoke(userManager, uuid);
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
