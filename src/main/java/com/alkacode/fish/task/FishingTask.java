package com.alkacode.fish.task;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import com.alkacode.fish.model.PlayerFishStats;
import com.alkacode.fish.util.FishUtil;
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
 * <p>O contador de peixes exibido (actionbar + chat) é o mesmo mantido por
 * {@link com.alkacode.fish.manager.FishingAreaManager} para a visita à área inteira -
 * NÃO reseta a cada lance de vara, só quando o jogador realmente entra/sai da área
 * (ver {@link com.alkacode.fish.listener.FishingAreaTrackerListener}).
 *
 * <p>O AFK é encerrado automaticamente (recolhendo a linha) se:
 * <ul>
 *   <li>o jogador sair da região principal de pesca;</li>
 *   <li>a entidade do hook deixar de existir de verdade (não uma leitura de posição -
 *       ver {@code anchor} no Session, congelado no lance pra ignorar bobbing/correnteza);</li>
 *   <li>o jogador se afastar mais de {@code max-fishing-distance} blocos do ponto do lance.</li>
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
        // ActionBar tem vida curta (~3s) no client - um heartbeat próprio mantém ela
        // visível o ciclo inteiro, já que o delay da vara costuma ser bem maior que isso.
        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickActionBar(player), 0L, 20L);
        // Âncora congelada no local/instante do lance - o bobber vanilla tem física própria
        // (bobbing, correnteza) que fica derivando a posição real do hook aos poucos; usar
        // hook.getLocation() ao vivo em todo ciclo fazia a linha "recolher sozinha" depois de
        // um tempo mesmo com o jogador parado e a vara no nível máximo. As checagens de
        // área/água passam a validar contra esse ponto fixo, não a posição física atual.
        Location anchor = hook.getLocation().clone();
        sessions.put(uuid, new Session(task, actionBarTask, finalRod, hook, anchor));

        FishUtil.showTitle(player, plugin.getMessages(), "fish.started_title", "fish.started_subtitle");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        // Linha vazia antes: separa visualmente o começo da sessão de pesca do resto do chat.
        player.sendMessage(net.kyori.adventure.text.Component.empty());
        player.sendMessage(plugin.getMessages().parse("fish.started"));
    }

    private void tickActionBar(Player player) {
        if (!sessions.containsKey(player.getUniqueId())) return;
        int count = plugin.getFishingAreaManager().getFishCount(player);
        String name = "peixe" + (count == 1 ? "" : "s");
        player.sendActionBar(plugin.getMessages().parse("fish.afk_fishing",
                Map.of("count", String.valueOf(count), "name", name)));
    }

    private void cycle(Player player, FishingRod rod, FishHook hook) {
        UUID uuid = player.getUniqueId();
        Session session = sessions.get(uuid);
        if (session == null) return;

        if (!player.isOnline()) {
            retractAndStop(player, hook);
            return;
        }

        // A entidade do hook precisa continuar existindo (senão a linha foi recolhida de
        // verdade por algum motivo real), mas a checagem de área/água usa a ÂNCORA
        // congelada no lance, não a posição física ao vivo - o bobber vanilla desliza um
        // pouco sozinho (bobbing/correnteza) e isso derrubava a sessão depois de um tempo
        // mesmo com o jogador parado e a vara no nível máximo.
        if (!hook.isValid()) {
            if (session.misses().incrementAndGet() < 2) return;
            retractAndStop(player, hook);
            return;
        }
        session.misses().set(0);
        Location anchor = session.anchor();

        // Jogador saindo da região é sempre imediato (intencional).
        if (!plugin.getFishingAreaManager().isPlayerInFishingArea(player)) {
            retractAndStop(player, hook);
            return;
        }
        double maxDist = plugin.getConfig().getDouble("fishing-area.max-fishing-distance", 50.0);
        if (maxDist >= 0 && player.getLocation().distance(anchor) > maxDist) {
            retractAndStop(player, hook);
            return;
        }

        var stats = plugin.getPlayerDataManager().getStats(uuid);
        if (stats.isRodBroken() || !plugin.getRodManager().isHoldingRod(player)) {
            retractAndStop(player, hook);
            return;
        }

        boolean caught = plugin.getFishingListener().afkCatch(player, anchor);
        if (!caught) return;

        // Flash de captura - o heartbeat volta a mostrar o status genérico no próximo tick.
        int count = plugin.getFishingAreaManager().getFishCount(player);
        String name = "peixe" + (count == 1 ? "" : "s");
        player.sendActionBar(plugin.getMessages().parse("fish.afk_count",
            Map.of("count", String.valueOf(count), "name", name,
                    "bonus", plugin.getFishingClassManager().getBonusSuffix(player))));

        // Checagem de quebra
        if (!rod.isUnbreakable()) {
            double chance = rod.getBreakChance();
            if (chance > 0 && Math.random() * 100 < chance) {
                stats.setRodBroken(true);
                plugin.getPlayerDataManager().save(uuid);
                plugin.getRodManager().removeRodItem(player);
                retractAndStop(player, hook);
                // Vara quebrada encerra a pesca atual de verdade (precisa reparar pra continuar) -
                // a proxima pesca deve contar do zero, nao herdar o total de antes de quebrar.
                plugin.getFishingAreaManager().markEnter(player);
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
        session.actionBarTask().cancel();
        FishUtil.showTitle(player, plugin.getMessages(), "fish.stopped_title", "fish.stopped_subtitle");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_CAT_AMBIENT, 1f, 1f);
    }

    /** Para o modo AFK SEM mostrar title (usado quando o jogador recolhe manualmente). */
    public void stopQuietly(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        session.task().cancel();
        session.actionBarTask().cancel();
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

    private record Session(BukkitTask task, BukkitTask actionBarTask, FishingRod rod, FishHook hook, Location anchor,
                            java.util.concurrent.atomic.AtomicInteger misses) {
        Session(BukkitTask task, BukkitTask actionBarTask, FishingRod rod, FishHook hook, Location anchor) {
            this(task, actionBarTask, rod, hook, anchor, new java.util.concurrent.atomic.AtomicInteger(0));
        }
    }
}
