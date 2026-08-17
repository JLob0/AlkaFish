package com.alkacode.fish.task;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import com.alkacode.fish.model.PlayerFishStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modo AFK de pesca: quando a linha toca a água dentro da área, um ciclo automático
 * pesca a cada `delay` da vara. Mostra contador de peixes na hotbar e no chat, e para
 * quando a vara (se quebrável) quebra.
 *
 * <p>O AFK é encerrado automaticamente (recolhendo a linha) se:
 * <ul>
 *   <li>o jogador sair da região principal de pesca;</li>
 *   <li>o hook sair da região principal ou da água;</li>
 *   <li>o jogador se afastar mais de {@code max-fishing-distance} blocos do hook.</li>
 * </ul>
 * Enquanto o jogador andar DENTRO da região, a linha não recolhe.
 */
public final class FishingTask {

    private final AlkaFishPlugin plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public FishingTask(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    /** Inicia o modo AFK para o jogador (no-op se já estiver pescando). */
    public void start(Player player, FishHook hook) {
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
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> cycle(player, finalRod, hook), delayTicks, delayTicks);
        sessions.put(uuid, new Session(task, finalRod, hook));

        plugin.getFishingAreaManager().markEnter(player);
        showTitle(player, true);
    }

    private void cycle(Player player, FishingRod rod, FishHook hook) {
        UUID uuid = player.getUniqueId();

        // Recolhe automaticamente se o jogador saiu da região, o hook saiu da região/água,
        // ou o jogador se afastou além do limite configurado.
        if (!player.isOnline()
                || !plugin.getFishingAreaManager().isPlayerInFishingArea(player)
                || !hook.isValid()
                || !plugin.getFishingAreaManager().isHookInFishingArea(hook)
                || !plugin.getFishingAreaManager().isWaterInArea(hook.getLocation())) {
            retractAndStop(player, hook);
            return;
        }
        double maxDist = plugin.getConfig().getDouble("fishing-area.max-fishing-distance", 50.0);
        if (maxDist >= 0 && player.getLocation().distance(hook.getLocation()) > maxDist) {
            retractAndStop(player, hook);
            return;
        }

        var stats = plugin.getPlayerDataManager().getStats(uuid);
        if (stats.isRodBroken() || !plugin.getRodManager().isHoldingRod(player)) {
            retractAndStop(player, hook);
            return;
        }

        Session session = sessions.get(uuid);
        int currentCount = session != null ? session.fishCaught() : 0;
        Location hookLoc = hook.getLocation();

        // ActionBar persistente — mostra em TODO ciclo
        String name = "peixe" + (currentCount == 1 ? "" : "s");
        player.sendActionBar(plugin.getMessages().parse("fish.afk_fishing",
            java.util.Map.of("count", String.valueOf(currentCount), "name", name)));

        boolean caught = plugin.getFishingListener().afkCatch(player, hookLoc);
        if (!caught) return;

        int newCount = currentCount + 1;
        sessions.put(uuid, session.withFishCaught(newCount));

        // ActionBar de captura
        player.sendActionBar(plugin.getMessages().parse("fish.afk_count",
            java.util.Map.of("count", String.valueOf(newCount), "name", name)));

        // Checagem de quebra
        if (!rod.isUnbreakable()) {
            double chance = rod.getBreakChance();
            if (chance > 0 && Math.random() * 100 < chance) {
                stats.setRodBroken(true);
                plugin.getPlayerDataManager().save(uuid);
                plugin.getRodManager().removeRodItem(player);
                retractAndStop(player, hook);
                player.sendMessage(plugin.getMessages().parse("rod.broke_afk"));
            }
        }
    }

    /** Encerra o AFK recolhendo a linha (auto, sem clique do jogador) e mostra o title de parada. */
    private void retractAndStop(Player player, FishHook hook) {
        if (hook != null && hook.isValid()) hook.remove();
        stop(player);
    }

    /** Para o modo AFK mostrando o title de parada. */
    public void stop(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        session.task().cancel();
        plugin.getFishingAreaManager().markLeave(player);
        showTitle(player, false);
    }

    /** Para o modo AFK SEM mostrar title (usado quando o jogador recolhe manualmente). */
    public void stopQuietly(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        session.task().cancel();
        plugin.getFishingAreaManager().markLeave(player);
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
        var title = plugin.getMessages().parse(started ? "fish.started_title" : "fish.stopped_title");
        var subtitle = parseOrEmpty(started ? "fish.started_subtitle" : "fish.stopped_subtitle");
        player.showTitle(net.kyori.adventure.title.Title.title(
                title, subtitle,
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(300), java.time.Duration.ofMillis(1500), java.time.Duration.ofMillis(300))));
    }

    /** Resolve a mensagem; se a key não existir no messages.yml (parse devolve a própria
     * key), retorna um componente vazio em vez de mostrar texto cru. */
    private net.kyori.adventure.text.Component parseOrEmpty(String key) {
        var msg = plugin.getMessages().parse(key);
        if (msg != null) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(msg);
            if (plain.equals(key)) return net.kyori.adventure.text.Component.empty();
        }
        return msg;
    }

    private record Session(BukkitTask task, FishingRod rod, FishHook hook, int fishCaught) {
        Session(BukkitTask task, FishingRod rod, FishHook hook) {
            this(task, rod, hook, 0);
        }
        Session withFishCaught(int n) {
            return new Session(task, rod, hook, n);
        }
    }
}
