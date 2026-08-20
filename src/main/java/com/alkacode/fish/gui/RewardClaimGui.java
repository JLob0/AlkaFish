package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.database.entity.PendingRewardEntity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Recompensas de pesca pendentes (itens/keys de crate) esperando reivindicação. */
public final class RewardClaimGui extends FishGui {

    public RewardClaimGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🎁 Recompensas de Pesca", Category.HIGHLIGHT, 6, "alkafish-rewards");
    }

    @Override
    public void render() {
        var layout = applyBorder("reward-claim");
        List<PendingRewardEntity> pending = plugin.getPendingRewardService().getPending(player);

        setAt(layout, 'H', createItem(Material.CHEST, "<gold>🎁 Recompensas",
                        "<gray>Pendentes: <yellow>" + pending.size()));

        setAt(layout, 'R', createItem(Material.EMERALD_BLOCK, "<green>✔ Reivindicar Tudo",
                        "<gray>Recebe todas as recompensas pendentes"),
                e -> {
                    if (pending.isEmpty()) {
                        player.sendMessage(plugin.getMessages().parse("rewards.empty"));
                        return;
                    }
                    int count = plugin.getPendingRewardService().claimAll(player);
                    player.sendMessage(plugin.getMessages().parse("rewards.claimed-all",
                            java.util.Map.of("count", String.valueOf(count))));
                    new RewardClaimGui(plugin, player).open();
                });
        setAt(layout, 'V', createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new FishBagGui(plugin, player).open());
        setAt(layout, 'F', createItem(Material.BARRIER, "<red>❌ Fechar"), e -> player.closeInventory());

        var contentSlots = layout.findSlots('0');
        int idx = 0;
        for (PendingRewardEntity reward : pending) {
            if (idx >= contentSlots.size()) break;
            ItemStack icon = buildIcon(reward);
            int slot = contentSlots.get(idx++);
            setItem(slot, icon, e -> {
                if (plugin.getPendingRewardService().claim(player, reward.id())) {
                    player.sendMessage(plugin.getMessages().parse("rewards.claimed",
                            java.util.Map.of("reward", reward.displayName())));
                }
                new RewardClaimGui(plugin, player).open();
            });
        }

        if (pending.isEmpty()) {
            setItem(22, createItem(Material.GRAY_DYE, "<gray>Nenhuma recompensa pendente",
                    "<gray>Pesque pra ganhar recompensas bônus!"));
        }
    }

    private ItemStack buildIcon(PendingRewardEntity reward) {
        if ("CRATE_KEY".equals(reward.type())) {
            return createItem(Material.TRIPWIRE_HOOK, "<aqua>🔑 " + reward.displayName(),
                    "<gray>Crate: <aqua>" + reward.crateId(),
                    "<gray>Quantidade: <yellow>" + reward.amount() + "x",
                    "",
                    "<yellow>Clique para reivindicar");
        }
        Material material = Material.matchMaterial(String.valueOf(reward.material()));
        if (material == null) material = Material.CHEST;
        return createItem(material, "<yellow>" + reward.displayName(),
                "<gray>Quantidade: <yellow>" + reward.amount() + "x",
                "",
                "<yellow>Clique para reivindicar");
    }
}
