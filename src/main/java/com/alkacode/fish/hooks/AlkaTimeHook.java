package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Integração com AlkaTime via ServicesManager (tempo online). */
public final class AlkaTimeHook extends HookBase {

    private Object api;
    private Method getOnlineSeconds;

    public AlkaTimeHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("AlkaTime")) return;
            Class<?> apiClass = Class.forName("com.alkacode.time.api.AlkaTimeAPI");
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) return;
            this.api = registration.getProvider();
            this.getOnlineSeconds = apiClass.getMethod("getOnlineSeconds", UUID.class);
        } catch (Throwable e) {
            plugin.getLogger().warning("AlkaTime hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    public CompletableFuture<Long> getOnlineSeconds(UUID uuid) {
        try {
            if (api != null && getOnlineSeconds != null) {
                Object result = getOnlineSeconds.invoke(api, uuid);
                if (result instanceof CompletableFuture<?> cf) {
                    @SuppressWarnings("unchecked")
                    CompletableFuture<Long> casted = (CompletableFuture<Long>) cf;
                    return casted;
                }
            }
        } catch (Throwable ignored) {}
        return CompletableFuture.completedFuture(0L);
    }
}
