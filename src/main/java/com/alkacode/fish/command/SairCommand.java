package com.alkacode.fish.command;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /sair - sai da área de pesca (teleporta para o spawn/saída setada). */
public final class SairCommand implements CommandExecutor {

    private final AlkaFishPlugin plugin;

    public SairCommand(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores!");
            return true;
        }
        if (!plugin.getFishingAreaManager().isInArea(player)) {
            player.sendMessage(plugin.getMessages().parse("area.not-in-area"));
            return true;
        }
        plugin.getFishingAreaManager().teleportExit(player);
        return true;
    }
}
