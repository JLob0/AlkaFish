package com.alkacode.fish.command;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.gui.ClassGui;
import com.alkacode.fish.gui.CodexGui;
import com.alkacode.fish.gui.EnchantGui;
import com.alkacode.fish.gui.FishBagGui;
import com.alkacode.fish.gui.FishingAreaGui;
import com.alkacode.fish.gui.RodGui;
import com.alkacode.fish.gui.SellGui;
import com.alkacode.fish.gui.TournamentGui;
import com.alkacode.fish.model.PlayerFishStats;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Comando /fish. */
public final class FishCommand implements CommandExecutor {

    private final AlkaFishPlugin plugin;

    public FishCommand(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores!");
            return true;
        }

        if (args.length == 0) {
            // /fish, /pesca ou /pescaria sem argumentos abre o menu principal da área
            // de pesca (com teleporte + funções premium), igual ao /minas.
            new FishingAreaGui(plugin, player).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sair", "exit", "voltar" -> plugin.getFishingAreaManager().teleportExit(player);
            case "bag", "mochila" -> new FishBagGui(plugin, player).open();
            case "sell", "vender" -> new SellGui(plugin, player).open();
            case "codex", "coleção" -> new CodexGui(plugin, player).open();
            case "tournament", "torneio" -> new TournamentGui(plugin, player).open();
            case "stats", "estatísticas" -> showStats(player);
            case "rod", "vara" -> new RodGui(plugin, player).open();
            case "enchant", "encantar" -> new EnchantGui(plugin, player).open();
            case "class", "classe" -> new ClassGui(plugin, player).open();
            case "invisibility", "invis" -> plugin.getFishingAreaManager().toggleInvisibility(player);
            case "autosell" -> {
                if (!player.hasPermission("alkafish.autosell")) {
                    player.sendMessage(plugin.getMessages().parse("errors.no-permission"));
                    return true;
                }
                player.sendMessage(plugin.getMessages().parse("sell.autosell-toggle"));
            }
            default -> showHelp(player);
        }
        return true;
    }

    private void showStats(Player player) {
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        player.sendMessage(plugin.getMessages().parse("stats.header"));
        player.sendMessage(plugin.getMessages().parse("stats.level", java.util.Map.of("level", String.valueOf(stats.getLevel()))));
        player.sendMessage(plugin.getMessages().parse("stats.xp", java.util.Map.of("xp", String.format("%.1f", stats.getXp()))));
        player.sendMessage(plugin.getMessages().parse("stats.total", java.util.Map.of("total", String.valueOf(stats.getTotalCaught()))));
        player.sendMessage(plugin.getMessages().parse("stats.biggest", java.util.Map.of("length", String.format("%.1f cm", stats.getBiggestLength()))));
    }

    private void showHelp(Player player) {
        player.sendMessage(plugin.getMessages().parse("help.header"));
        player.sendMessage(plugin.getMessages().parse("help.bag"));
        player.sendMessage(plugin.getMessages().parse("help.sell"));
        player.sendMessage(plugin.getMessages().parse("help.codex"));
        player.sendMessage(plugin.getMessages().parse("help.tournament"));
        player.sendMessage(plugin.getMessages().parse("help.stats"));
        player.sendMessage(plugin.getMessages().parse("help.rod"));
        player.sendMessage(plugin.getMessages().parse("help.enchant"));
        player.sendMessage(plugin.getMessages().parse("help.class"));
        player.sendMessage(plugin.getMessages().parse("help.invis"));
    }
}
