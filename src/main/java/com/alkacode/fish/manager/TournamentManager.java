package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.database.entity.TournamentRecordEntity;
import com.alkacode.fish.model.Fish;
import com.alkacode.fish.model.Tournament;
import com.alkacode.fish.model.TournamentType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Gerencia torneios de pesca (auto e manuais). */
public final class TournamentManager {

    private final AlkaFishPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private Tournament activeTournament;
    private BukkitTask tournamentTask;
    private BossBar tournamentBar;
    private final Map<UUID, Double> scores = new ConcurrentHashMap<>();
    private double activeSellMultiplier = 1.0;

    public TournamentManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        scheduleNextTournament();
    }

    public void startTournament(TournamentType type, int durationMinutes) {
        if (activeTournament != null) return;
        activeTournament = new Tournament(type, durationMinutes * 60);
        scores.clear();
        activeSellMultiplier = plugin.getConfig().getDouble("tournaments.sell-multiplier-during-tournament", 1.5);

        tournamentBar = BossBar.bossBar(
                mm.deserialize("<gold>🏆 Torneio de Pesca: " + type.getDisplayName()),
                1.0f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_20);
        Bukkit.getOnlinePlayers().forEach(p -> p.showBossBar(tournamentBar));

        Bukkit.broadcast(mm.deserialize("<gold><bold>🏆 TORNEIO DE PESCA INICIADO!"));
        Bukkit.broadcast(mm.deserialize("<yellow>Tipo: <gold>" + type.getDisplayName()
                + " <yellow>| Duração: <gold>" + durationMinutes + " min"));

        tournamentTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void tick() {
        if (activeTournament == null) {
            tournamentTask.cancel();
            return;
        }
        activeTournament.tick();
        float progress = (float) activeTournament.getRemainingSeconds() / activeTournament.getTotalSeconds();
        tournamentBar.progress(progress);
        tournamentBar.name(mm.deserialize("<gold>🏆 " + activeTournament.getType().getDisplayName()
                + " <yellow>" + activeTournament.getTimeLeftFormatted()
                + " <gray>| Líder: " + getTopDisplay()));
        if (activeTournament.isFinished()) {
            endTournament();
        }
    }

    public void endTournament() {
        if (activeTournament == null) return;
        tournamentTask.cancel();
        Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(tournamentBar));

        List<Map.Entry<UUID, Double>> sorted = getSortedScores();
        Bukkit.broadcast(mm.deserialize("<gold><bold>🏆 TORNEIO ENCERRADO!"));

        List<String[]> rewards = loadRewardCommands();
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Map.Entry<UUID, Double> entry = sorted.get(i);
            Player p = Bukkit.getPlayer(entry.getKey());
            String name = p != null ? p.getName() : "?";
            Bukkit.broadcast(mm.deserialize("<yellow>" + (i + 1) + "º <gold>" + name
                    + " <gray>- <green>" + String.format("%.2f", entry.getValue())));
            giveRewards(entry.getKey(), i + 1, rewards);
            recordResult(entry.getKey(), i + 1, entry.getValue());
        }

        activeTournament = null;
        activeSellMultiplier = 1.0;
        scores.clear();
        scheduleNextTournament();
    }

    private void giveRewards(UUID uuid, int position, List<String[]> rewards) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        for (String[] cmd : rewards) {
            if (cmd.length == 2 && cmd[0].equals(String.valueOf(position))) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        cmd[1].replace("{player}", player.getName()));
            }
        }
    }

    private void recordResult(UUID uuid, int position, double score) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                double reward = position == 1
                        ? plugin.getConfig().getDouble("tournaments.rewards.first-prize", 5000.0) : 0.0;
                plugin.getTournamentRecordRepository().insert(new TournamentRecordEntity(
                        0, uuid, activeTournament != null ? activeTournament.getType().name() : "BIGGEST_FISH",
                        score, position, reward, new Timestamp(System.currentTimeMillis())));
            } catch (Exception ignored) {}
        });
    }

    private List<String[]> loadRewardCommands() {
        List<String[]> out = new ArrayList<>();
        var section = plugin.getConfig().getConfigurationSection("tournaments.rewards");
        if (section == null) return out;
        for (String pos : section.getKeys(false)) {
            List<String> cmds = section.getStringList(pos);
            if (cmds.isEmpty()) continue;
            for (String cmd : cmds) {
                out.add(new String[]{pos, cmd});
            }
        }
        return out;
    }

    public void registerCatch(Player player, Fish fish, double length, double weight) {
        if (activeTournament == null) return;
        double score = switch (activeTournament.getType()) {
            case BIGGEST_FISH -> length;
            case MOST_FISH -> 1;
            case TOTAL_WEIGHT -> weight;
            case FIRST_LEGENDARY -> fish.getRarity().ordinal() >= 4 ? 1 : 0;
            case RANDOM_RARITY_FIRST -> fish.getRarity().ordinal() >= activeTournament.getTargetRarity() ? 1 : 0;
        };
        scores.merge(player.getUniqueId(), score, Double::sum);
    }

    public int getPlayerPosition(Player player) {
        List<Map.Entry<UUID, Double>> sorted = getSortedScores();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getKey().equals(player.getUniqueId())) return i + 1;
        }
        return -1;
    }

    public double getPlayerScore(Player player) {
        return scores.getOrDefault(player.getUniqueId(), 0.0);
    }

    public double getActiveSellMultiplier() { return activeSellMultiplier; }
    public Tournament getActiveTournament() { return activeTournament; }

    private List<Map.Entry<UUID, Double>> getSortedScores() {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list;
    }

    private String getTopDisplay() {
        List<Map.Entry<UUID, Double>> sorted = getSortedScores();
        if (sorted.isEmpty()) return "Nenhum";
        Map.Entry<UUID, Double> first = sorted.get(0);
        Player p = Bukkit.getPlayer(first.getKey());
        return (p != null ? p.getName() : "?") + " (" + String.format("%.1f", first.getValue()) + ")";
    }

    private void scheduleNextTournament() {
        if (!plugin.getConfig().getBoolean("tournaments.auto-start-enabled", true)) return;
        int intervalMinutes = plugin.getConfig().getInt("tournaments.auto-start-interval-minutes", 120);
        long delay = intervalMinutes * 60L * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeTournament == null) {
                TournamentType[] types = TournamentType.values();
                TournamentType randomType = types[new Random().nextInt(types.length)];
                startTournament(randomType, plugin.getConfig().getInt("tournaments.default-duration-minutes", 15));
            }
        }, delay);
    }

    public void shutdown() {
        if (tournamentTask != null) tournamentTask.cancel();
        if (tournamentBar != null) {
            Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(tournamentBar));
        }
    }
}
