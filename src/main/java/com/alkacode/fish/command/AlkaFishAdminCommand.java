package com.alkacode.fish.command;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRegion;
import com.alkacode.fish.model.TournamentType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** Comando /alkafish (admin). */
public final class AlkaFishAdminCommand implements CommandExecutor {

    private final AlkaFishPlugin plugin;

    public AlkaFishAdminCommand(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("alkafish.admin")) {
            send(sender, "errors.no-permission");
            return true;
        }

        if (args.length < 1) {
            sendAdminHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getFishManager().reload();
                plugin.getBaitManager().reload();
                plugin.getRodManager().reload();
                plugin.getEnchantmentManager().reload();
                plugin.getFishingClassManager().reload();
                plugin.getRewardManager().reload();
                plugin.getTierManager().reload();
                plugin.getMessages().reload();
                plugin.getMenuConfig().reload();
                send(sender, "admin.reloaded");
            }
            case "tournament", "torneio" -> handleTournament(sender, args);
            case "givexp" -> handleGiveXp(sender, args);
            case "npc" -> handleNpc(sender, args);
            case "setrod" -> handleSetRod(sender, args);
            case "area" -> handleArea(sender, args);
            case "givebooster" -> handleGiveBooster(sender, args);
            case "givenacar" -> handleGiveNacar(sender, args, true);
            case "removenacar" -> handleGiveNacar(sender, args, false);
            case "repairrod" -> handleRepairRod(sender, args);
            case "breakrod" -> handleBreakRod(sender, args);
            case "resetdata", "reset" -> handleResetData(sender, args);
            default -> sendAdminHelp(sender);
        }
        return true;
    }

    private void handleArea(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "admin.area-usage");
            return;
        }
        if (!(sender instanceof Player player)) {
            send(sender, "admin.players-only");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "pos1" -> {
                plugin.getFishingAreaManager().setPos1(player.getLocation());
                send(sender, "admin.pos1-set", Map.of(
                        "x", String.valueOf(player.getLocation().getBlockX()),
                        "y", String.valueOf(player.getLocation().getBlockY()),
                        "z", String.valueOf(player.getLocation().getBlockZ())));
            }
            case "pos2" -> {
                plugin.getFishingAreaManager().setPos2(player.getLocation());
                send(sender, "admin.pos2-set", Map.of(
                        "x", String.valueOf(player.getLocation().getBlockX()),
                        "y", String.valueOf(player.getLocation().getBlockY()),
                        "z", String.valueOf(player.getLocation().getBlockZ())));
            }
            case "set" -> {
                // Usa pos1/pos2 do plugin se disponível; senão, tenta a seleção do WorldEdit.
                FishingRegion region = plugin.getFishingAreaManager().getSelectionRegion();
                if (region == null && plugin.getWorldEditHook() != null) {
                    java.util.Optional<FishingRegion> sel = plugin.getWorldEditHook().getSelection(player);
                    if (sel.isPresent()) region = sel.get();
                }
                if (region == null) {
                    send(sender, "admin.area-mark-first");
                    return;
                }
                plugin.getFishingAreaManager().setArea("pesca", "<aqua>Área de Pesca", region);
                send(sender, "admin.area-set");
            }
            case "spawn" -> {
                plugin.getFishingAreaManager().setSpawn(player.getLocation());
                send(sender, "admin.area-spawn-set");
            }
            case "lobby" -> {
                FishingRegion lobbyRegion = plugin.getFishingAreaManager().getSelectionRegion();
                if (lobbyRegion == null && plugin.getWorldEditHook() != null) {
                    java.util.Optional<FishingRegion> sel = plugin.getWorldEditHook().getSelection(player);
                    if (sel.isPresent()) lobbyRegion = sel.get();
                }
                if (lobbyRegion == null) {
                    send(sender, "admin.area-mark-lobby-first");
                    return;
                }
                plugin.getFishingAreaManager().setLobby(lobbyRegion);
                send(sender, "admin.area-lobby-set");
            }
            case "saida" -> {
                plugin.getFishingAreaManager().setExit(player.getLocation());
                send(sender, "admin.area-exit-set");
            }
            case "info" -> {
                var area = plugin.getFishingAreaManager().getArea();
                if (area.isEmpty()) {
                    send(sender, "admin.area-not-configured");
                    return;
                }
                var r = area.get().getRegion();
                send(sender, "admin.area-info-header");
                send(sender, "admin.area-info-id", Map.of("id", area.get().getId()));
                send(sender, "admin.area-info-region", Map.of(
                        "world", String.valueOf(r.getWorld()),
                        "x1", String.valueOf(r.getX1()), "y1", String.valueOf(r.getY1()), "z1", String.valueOf(r.getZ1()),
                        "x2", String.valueOf(r.getX2()), "y2", String.valueOf(r.getY2()), "z2", String.valueOf(r.getZ2())));
                send(sender, "admin.area-info-spawn", Map.of(
                        "spawn", area.get().getSpawn() != null ? "definido" : "centro da região"));
            }
            default -> send(sender, "admin.area-usage");
        }
    }

    private void handleNpc(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "admin.npc-usage");
            return;
        }
        if (args[1].equalsIgnoreCase("spawn")) {
            plugin.getNpcManager().spawnNpc();
            send(sender, "npc.spawned");
        } else if (args[1].equalsIgnoreCase("remove")) {
            plugin.getNpcManager().removeNpc();
            send(sender, "npc.removed");
        }
    }

    private void handleSetRod(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, "admin.setrod-usage");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            send(sender, "errors.player-not-found");
            return;
        }
        var rod = plugin.getRodManager().getRodById(args[2]);
        if (rod == null) {
            send(sender, "admin.rod-not-found");
            return;
        }
        var s = plugin.getPlayerDataManager().getStats(target.getUniqueId());
        s.setRodId(rod.getId());
        s.setRodLevel(rod.getLevel());
        plugin.getPlayerDataManager().save(target.getUniqueId());
        plugin.getRodManager().giveRodItem(target, rod);
        send(sender, "admin.rod-set");
    }

    private void handleTournament(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "admin.tournament-usage");
            return;
        }
        if (args[1].equalsIgnoreCase("start")) {
            TournamentType type = TournamentType.BIGGEST_FISH;
            int duration = plugin.getConfig().getInt("tournaments.default-duration-minutes", 15);
            if (args.length > 2) {
                try {
                    type = TournamentType.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    send(sender, "admin.tournament-invalid-type");
                    return;
                }
            }
            if (args.length > 3) {
                try {
                    duration = Integer.parseInt(args[3]);
                } catch (NumberFormatException ignored) {}
            }
            plugin.getTournamentManager().startTournament(type, duration);
            send(sender, "admin.tournament-started");
        } else if (args[1].equalsIgnoreCase("stop")) {
            plugin.getTournamentManager().endTournament();
            send(sender, "admin.tournament-stopped");
        }
    }

    private void handleGiveXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, "admin.givexp-usage");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            send(sender, "errors.player-not-found");
            return;
        }
        double xp;
        try {
            xp = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            send(sender, "admin.invalid-xp");
            return;
        }
        var stats = plugin.getPlayerDataManager().getStats(target.getUniqueId());
        plugin.getLevelManager().addXp(target, stats, xp);
        plugin.getPlayerDataManager().save(target.getUniqueId());
        send(sender, "admin.xp-given", Map.of("player", target.getName(), "xp", String.format("%.1f", xp)));
    }

    private void handleGiveBooster(CommandSender sender, String[] args) {
        if (args.length < 5) {
            send(sender, "admin.givebooster-usage");
            send(sender, "admin.givebooster-types");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            send(sender, "errors.player-not-found");
            return;
        }
        String type = args[2].toUpperCase();
        double multiplier;
        int duration;
        try {
            multiplier = Double.parseDouble(args[3]);
            duration = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            send(sender, "admin.invalid-booster-values");
            return;
        }
        if (!java.util.Set.of("FISH_CHANCE", "NACAR_MULTIPLIER", "SELL_BONUS").contains(type)) {
            send(sender, "admin.invalid-booster-type");
            return;
        }
        plugin.getBoosterService().activate(target, type, multiplier, duration);
        send(sender, "admin.booster-applied", Map.of("player", target.getName()));
    }

    private void handleGiveNacar(CommandSender sender, String[] args, boolean give) {
        if (args.length < 3) {
            send(sender, "admin.givenacar-usage", Map.of("command", give ? "givenacar" : "removenacar"));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            send(sender, "errors.player-not-found");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            send(sender, "admin.invalid-amount");
            return;
        }
        if (give) {
            plugin.getEconomyBridge().deposit(target.getUniqueId(), "nacar", amount);
        } else if (!plugin.getEconomyBridge().withdraw(target.getUniqueId(), "nacar", amount)) {
            send(sender, "admin.not-enough-nacar-target");
            return;
        }
        send(sender, give ? "admin.nacar-given" : "admin.nacar-removed",
                Map.of("amount", String.format("%.0f", amount), "player", target.getName()));
    }

    private void handleRepairRod(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "admin.repairrod-usage");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            send(sender, "errors.player-not-found");
            return;
        }
        var stats = plugin.getPlayerDataManager().getStats(target.getUniqueId());
        stats.setRodBroken(false);
        var rod = plugin.getRodManager().getRodById(stats.getRodId());
        if (rod == null) rod = plugin.getRodManager().getDefaultRod();
        if (rod != null) plugin.getRodManager().giveRodItem(target, rod);
        plugin.getPlayerDataManager().save(target.getUniqueId());
        send(sender, "admin.rod-repaired-admin", Map.of("player", target.getName()));
    }

    private void handleBreakRod(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "admin.breakrod-usage");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            send(sender, "errors.player-not-found");
            return;
        }
        var stats = plugin.getPlayerDataManager().getStats(target.getUniqueId());
        stats.setRodBroken(true);
        plugin.getPlayerDataManager().save(target.getUniqueId());
        plugin.getRodManager().removeRodItem(target);
        send(sender, "admin.rod-broken-admin", Map.of("player", target.getName()));
    }

    private void handleResetData(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "admin.resetdata-usage");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            send(sender, "errors.player-not-found");
            return;
        }
        plugin.getPlayerDataManager().reset(target.getUniqueId());
        plugin.getRodManager().removeRodItem(target);
        send(sender, "admin.data-reset", Map.of("player", target.getName()));
    }

    private void sendAdminHelp(CommandSender sender) {
        send(sender, "admin.help-header");
        send(sender, "admin.help-reload");
        send(sender, "admin.help-tournament");
        send(sender, "admin.help-givexp");
        send(sender, "admin.help-npc");
        send(sender, "admin.help-setrod");
        send(sender, "admin.help-area");
        send(sender, "admin.help-repairrod");
        send(sender, "admin.help-resetdata");
    }

    private void send(CommandSender sender, String key) {
        sender.sendMessage(plugin.getMessages().parse(key));
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(plugin.getMessages().parse(key, placeholders));
    }
}
