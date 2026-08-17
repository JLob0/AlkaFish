package com.alkacode.fish.task;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import com.alkacode.fish.model.PlayerFishStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modo AFK de pesca: quando a linha toca a água dentro da área, um ciclo automático
 * pesca a cada `delay` da vara. Mostra contador de peixes na hotbar e no chat, e para
 * quando a vara (se quebrável) quebra.
 */
public final class FishingTask {

    private final AlkaFishPlugin plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public FishingTask(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    /** Inicia o modo AFK para o jogador (no-op se já estiver pescando). */
    public void start(Player player, Location hookLoc) {
        UUID uuid = player.getUniqueId();
        if (sessions.containsKey(uuid)) return;

        var stats = plugin.getPlayerDataManager().getStats(uuid);
        if (stats.isRodBroken()) {
            player.sendMessage(plugin.getMessages().parse("rod.broken"));
            return;
        }
        FishingRod rod = plugin.getRodManager().getRodById(stats.getRodId());
        if (rod == null) rod = plugin.getRodManager().getDefaultRod();
        if (rod == null) return;
        final FishingRod finalRod = rod;

        long delayTicks = Math.max(20L, rod.getDelaySeconds() * 20L);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> cycle(player, finalRod, hookLoc), delayTicks, delayTicks);
        sessions.put(uuid, new Session(task, finalRod, hookLoc));

        plugin.getFishingAreaManager().markEnter(player);
        showTitle(player, true);
    }

    private void cycle(Player player, FishingRod rod, Location hookLoc) {
        UUID uuid = player.getUniqueId();
        if (!player.isOnline() || !plugin.getFishingAreaManager().isInArea(player.getLocation())) {
            stop(player);
            return;
        }
        var stats = plugin.getPlayerDataManager().getStats(uuid);
        if (stats.isRodBroken() || !plugin.getRodManager().isHoldingRod(player)) {
            stop(player);
            return;
        }

        Session session = sessions.get(uuid);
        int countBefore = session != null ? session.fishCaught() : 0;
        // hookLoc, NUNCA player.getLocation() - a profundidade (calculateDepth) usa esse Y
        // pra filtrar peixe por min-depth, e todo peixe do fish.yml padrao exige min-depth
        // >= 1. A posicao do JOGADOR (em pe no deck, acima da agua) sempre da profundidade
        // ~0, entao nenhum peixe nunca passava no filtro - a vara nunca pescava nada.
        boolean caught = plugin.getFishingListener().afkCatch(player, hookLoc);
        if (!caught) return;

        int newCount = countBefore + 1;
        sessions.put(uuid, session.withFishCaught(newCount));

        // Mensagem na hotbar + chat com contador
        String name = "peixe" + (newCount == 1 ? "" : "s");
        player.sendActionBar(plugin.getMessages().parse("fishing.afk_count",
                java.util.Map.of("count", String.valueOf(newCount), "name", name)));

        // Checagem de quebra (só se a vara for quebrável)
        if (!rod.isUnbreakable()) {
            double chance = rod.getBreakChance();
            if (chance > 0 && Math.random() * 100 < chance) {
                stats.setRodBroken(true);
                plugin.getPlayerDataManager().save(uuid);
                stop(player);
                player.sendMessage(plugin.getMessages().parse("rod.broke_afk"));
            }
        }
    }

    /** Para o modo AFK. */
    public void stop(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        session.task().cancel();
        plugin.getFishingAreaManager().markLeave(player);
        showTitle(player, false);
    }

    public boolean isFishing(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void stopAll() {
        for (UUID uuid : sessions.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) stop(p);
        }
        sessions.clear();
    }

    private void showTitle(Player player, boolean started) {
        var msg = plugin.getMessages().parse(started ? "fishing.started_title" : "fishing.stopped_title");
        player.showTitle(net.kyori.adventure.title.Title.title(
                msg, net.kyori.adventure.text.Component.empty(),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(300), java.time.Duration.ofMillis(1500), java.time.Duration.ofMillis(300))));
    }

    private record Session(BukkitTask task, FishingRod rod, Location hookLoc, int fishCaught) {
        Session(BukkitTask task, FishingRod rod, Location hookLoc) {
            this(task, rod, hookLoc, 0);
        }
        Session withFishCaught(int n) {
            return new Session(task, rod, hookLoc, n);
        }
    }
}
