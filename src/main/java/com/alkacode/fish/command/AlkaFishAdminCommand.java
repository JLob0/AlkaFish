package com.alkacode.fish.command;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRegion;
import com.alkacode.fish.model.TournamentType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
            sender.sendMessage(plugin.getMessages().parse("errors.no-permission"));
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
                plugin.getMessages().reload();
                sender.sendMessage(plugin.getMessages().parse("admin.reloaded"));
            }
            case "tournament", "torneio" -> handleTournament(sender, args);
            case "givexp" -> handleGiveXp(sender, args);
            case "npc" -> handleNpc(sender, args);
            case "setrod" -> handleSetRod(sender, args);
            case "area" -> handleArea(sender, args);
            default -> sendAdminHelp(sender);
        }
        return true;
    }

    private void handleArea(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("<red>Uso: /alkafish area set|spawn|lobby|saida|info");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("<red>Apenas jogadores!");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "set" -> {
                // Define a região da área usando a seleção do WorldEdit (//pos1///pos2)
                if (plugin.getWorldEditHook() == null) {
                    sender.sendMessage("<red>WorldEdit/FAWE não detectado!");
                    return;
                }
                java.util.Optional<FishingRegion> sel = plugin.getWorldEditHook().getSelection(player);
                if (sel.isEmpty()) {
                    sender.sendMessage("<red>Use a //wand do WorldEdit e marque //pos1 e //pos2!");
                    return;
                }
                plugin.getFishingAreaManager().setArea("pesca", "<aqua>Área de Pesca", sel.get());
                sender.sendMessage(plugin.getMessages().parse("admin.area-set"));
            }
            case "spawn" -> {
                plugin.getFishingAreaManager().setSpawn(player.getLocation());
                sender.sendMessage(plugin.getMessages().parse("admin.area-spawn-set"));
            }
            case "lobby" -> {
                if (plugin.getWorldEditHook() == null) {
                    sender.sendMessage("<red>WorldEdit/FAWE não detectado!");
                    return;
                }
                java.util.Optional<FishingRegion> sel = plugin.getWorldEditHook().getSelection(player);
                if (sel.isEmpty()) {
                    sender.sendMessage("<red>Use a //wand do WorldEdit e marque //pos1 e //pos2!");
                    return;
                }
                plugin.getFishingAreaManager().setLobby(sel.get());
                sender.sendMessage(plugin.getMessages().parse("admin.area-lobby-set"));
            }
            case "saida" -> {
                plugin.getFishingAreaManager().setExit(player.getLocation());
                sender.sendMessage(plugin.getMessages().parse("admin.area-exit-set"));
            }
            case "info" -> {
                var area = plugin.getFishingAreaManager().getArea();
                if (area.isEmpty()) {
                    sender.sendMessage("<red>Área de pesca não configurada.");
                    return;
                }
                var r = area.get().getRegion();
                sender.sendMessage("<gold><bold>🏝 Área de Pesca");
                sender.sendMessage("<gray>ID: <aqua>" + area.get().getId());
                sender.sendMessage("<gray>Região: <green>" + r.getWorld() + " <gray>(" + r.getX1() + "," + r.getY1() + "," + r.getZ1() + ") -> (" + r.getX2() + "," + r.getY2() + "," + r.getZ2() + ")");
                sender.sendMessage("<gray>Spawn: <green>" + (area.get().getSpawn() != null ? "definido" : "centro da região"));
            }
            default -> sender.sendMessage("<red>Uso: /alkafish area set|spawn|lobby|saida|info");
        }
    }

    private void handleNpc(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("<red>Use: /alkafish npc spawn | remove");
            return;
        }
        if (args[1].equalsIgnoreCase("spawn")) {
            plugin.getNpcManager().spawnNpc();
            sender.sendMessage(plugin.getMessages().parse("npc.spawned"));
        } else if (args[1].equalsIgnoreCase("remove")) {
            plugin.getNpcManager().removeNpc();
            sender.sendMessage(plugin.getMessages().parse("npc.removed"));
        }
    }

    private void handleSetRod(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("<red>Use: /alkafish setrod <player> <rod-id>");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().parse("errors.player-not-found"));
            return;
        }
        var rod = plugin.getRodManager().getRodById(args[2]);
        if (rod == null) {
            sender.sendMessage("<red>Vara não encontrada!");
            return;
        }
        var s = plugin.getPlayerDataManager().getStats(target.getUniqueId());
        s.setRodId(rod.getId());
        s.setRodLevel(rod.getLevel());
        plugin.getPlayerDataManager().save(target.getUniqueId());
        plugin.getRodManager().giveRodItem(target, rod);
        sender.sendMessage("<green>Vara setada!");
    }

    private void handleTournament(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("<red>Use: /alkafish tournament start <tipo> [minutos]");
            return;
        }
        if (args[1].equalsIgnoreCase("start")) {
            TournamentType type = TournamentType.BIGGEST_FISH;
            int duration = plugin.getConfig().getInt("tournaments.default-duration-minutes", 15);
            if (args.length > 2) {
                try {
                    type = TournamentType.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("<red>Tipo inválido!");
                    return;
                }
            }
            if (args.length > 3) {
                try {
                    duration = Integer.parseInt(args[3]);
                } catch (NumberFormatException ignored) {}
            }
            plugin.getTournamentManager().startTournament(type, duration);
            sender.sendMessage(plugin.getMessages().parse("admin.tournament-started"));
        } else if (args[1].equalsIgnoreCase("stop")) {
            plugin.getTournamentManager().endTournament();
            sender.sendMessage(plugin.getMessages().parse("admin.tournament-stopped"));
        }
    }

    private void handleGiveXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("<red>Use: /alkafish givexp <player> <amount>");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().parse("errors.player-not-found"));
            return;
        }
        double xp;
        try {
            xp = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("<red>Valor de XP inválido!");
            return;
        }
        var stats = plugin.getPlayerDataManager().getStats(target.getUniqueId());
        plugin.getLevelManager().addXp(target, stats, xp);
        plugin.getPlayerDataManager().save(target.getUniqueId());
        sender.sendMessage(plugin.getMessages().parse("admin.xp-given",
                java.util.Map.of("player", target.getName(), "xp", String.format("%.1f", xp))));
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessages().parse("admin.help-header"));
        sender.sendMessage(plugin.getMessages().parse("admin.help-reload"));
        sender.sendMessage(plugin.getMessages().parse("admin.help-tournament"));
        sender.sendMessage(plugin.getMessages().parse("admin.help-givexp"));
        sender.sendMessage(plugin.getMessages().parse("admin.help-npc"));
        sender.sendMessage(plugin.getMessages().parse("admin.help-setrod"));
        sender.sendMessage(plugin.getMessages().parse("admin.help-area"));
    }
}
