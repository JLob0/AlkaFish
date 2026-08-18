package com.alkacode.fish.listener;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Área de pesca é zona segura por padrão: um jogador só dá dano em outro lá dentro se
 * AMBOS tiverem ativado o toggle de PvP no menu (opt-in mútuo, não é "flag e já era"). */
public final class AreaPvpListener implements Listener {

    private final AlkaFishPlugin plugin;

    public AreaPvpListener(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!plugin.getFishingAreaManager().isInArea(victim.getLocation())) return;
        if (plugin.getFishingAreaManager().isPvpEnabled(victim)
                && plugin.getFishingAreaManager().isPvpEnabled(attacker)) {
            return;
        }
        event.setCancelled(true);
        attacker.sendMessage(plugin.getMessages().parse("area.pvp-disabled"));
    }
}
