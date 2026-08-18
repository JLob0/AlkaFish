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
        fillBlack();
        List<PendingRewardEntity> pending = plugin.getPendingRewardService().getPending(player);

        setItem(4, createItem(Material.CHEST, "<gold>🎁 Recompensas",
                        "<gray>Pendentes: <yellow>" + pending.size()),
                e -> {});

        setItem(49, createItem(Material.EMERALD_BLOCK, "<green>✔ Reivindicar Tudo",
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
        setItem(47, createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new FishBagGui(plugin, player).open());
        setItem(53, createItem(Material.BARRIER, "<red>❌ Fechar"), e -> player.closeInventory());

        int slot = 10;
        for (PendingRewardEntity reward : pending) {
            if (slot > 43) break;
            ItemStack icon = buildIcon(reward);
            setItem(slot, icon, e -> {
                if (plugin.getPendingRewardService().claim(player, reward.id())) {
                    player.sendMessage(plugin.getMessages().parse("rewards.claimed",
                            java.util.Map.of("reward", reward.displayName())));
                }
                new RewardClaimGui(plugin, player).open();
            });
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        if (pending.isEmpty()) {
            setItem(22, createItem(Material.GRAY_DYE, "<gray>Nenhuma recompensa pendente",
                    "<gray>Pesque pra ganhar recompensas bônus!"), e -> {});
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
