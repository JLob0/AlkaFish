package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Fish;
import com.alkacode.fish.util.TensionBarUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Mini-game de tensão com BossBar ao puxar o peixe. */
public final class TensionGameManager {

    private final AlkaFishPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, TensionGame> activeGames = new HashMap<>();

    public TensionGameManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    public void startGame(Player player, Fish fish, double length, double weight, Location loc, Consumer<Boolean> onComplete) {
        if (activeGames.containsKey(player.getUniqueId())) {
            onComplete.accept(false);
            return;
        }
        TensionGame game = new TensionGame(player, fish, onComplete);
        activeGames.put(player.getUniqueId(), game);
        game.start();
    }

    public void stopGame(Player player) {
        TensionGame game = activeGames.remove(player.getUniqueId());
        if (game != null) game.cleanup();
    }

    public boolean isInGame(Player player) {
        return activeGames.containsKey(player.getUniqueId());
    }

    private final class TensionGame {
        private final Player player;
        private final Fish fish;
        private final Consumer<Boolean> onComplete;
        private final BossBar bossBar;
        private BukkitTask task;

        private double tension = 50.0;
        private double targetZone = 50.0;
        private final double zoneSize;
        private double tensionVelocity = 0;
        private int ticks = 0;
        private final int maxTicks;
        private final double difficulty;
        private int outOfZoneCounter = 0;

        TensionGame(Player player, Fish fish, Consumer<Boolean> onComplete) {
            this.player = player;
            this.fish = fish;
            this.onComplete = onComplete;
            this.difficulty = calculateDifficulty(fish);
            this.zoneSize = Math.max(10, 35 - (difficulty * 5));
            this.maxTicks = (int) (100 + (difficulty * 50));
            this.bossBar = BossBar.bossBar(
                    mm.deserialize("<gray>Peixe na linha! Mantenha a tensão!"),
                    0.5f, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_20);
        }

        void start() {
            player.showBossBar(bossBar);
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    tick();
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }

        void tick() {
            ticks++;
            double noise = (Math.random() - 0.5) * difficulty * 4;
            tensionVelocity += noise;
            tensionVelocity *= 0.85;
            tension += tensionVelocity;
            tension = Math.max(0, Math.min(100, tension));

            targetZone += Math.sin(ticks * 0.1) * difficulty;
            targetZone = Math.max(zoneSize / 2, Math.min(100 - zoneSize / 2, targetZone));

            double lower = targetZone - zoneSize / 2;
            double upper = targetZone + zoneSize / 2;
            boolean inZone = tension >= lower && tension <= upper;

            bossBar.progress((float) (tension / 100.0));
            bossBar.color(TensionBarUtil.colorFor(tension, lower, upper));
            bossBar.name(mm.deserialize(TensionBarUtil.nameFor(tension, lower, upper)));

            if (tension <= 0) { fail("O peixe escapou..."); return; }
            if (tension >= 100) { fail("A linha arrebentou!"); return; }
            if (ticks >= maxTicks && inZone) { success(); return; }

            if (inZone) {
                outOfZoneCounter = Math.max(0, outOfZoneCounter - 1);
            } else {
                outOfZoneCounter++;
                if (outOfZoneCounter > 30) { fail("O peixe escapou..."); }
            }
        }

        void success() {
            cleanup();
            onComplete.accept(true);
        }

        void fail(String reason) {
            player.sendMessage(mm.deserialize("<red>" + reason));
            cleanup();
            onComplete.accept(false);
        }

        void cleanup() {
            if (task != null && !task.isCancelled()) task.cancel();
            player.hideBossBar(bossBar);
            activeGames.remove(player.getUniqueId());
        }

        private double calculateDifficulty(Fish fish) {
            return 1.0 + (fish.getRarity().ordinal() * 0.8);
        }
    }
}
