package com.alkacode.fish.service;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.database.entity.PendingRewardEntity;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Recompensas de pesca pendentes (itens/keys de crate) - fica na aba "Recompensas" da
 * Sacola de Peixes até o jogador reivindicar. Mesmo padrão de cache do FishBagService. */
public final class PendingRewardService {

    private final AlkaFishPlugin plugin;
    private final Map<UUID, List<PendingRewardEntity>> cache = new ConcurrentHashMap<>();

    public PendingRewardService(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadAsync(player.getUniqueId());
        }
    }

    public void loadAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                cache.put(uuid, new ArrayList<>(plugin.getPendingRewardRepository().findByPlayer(uuid)));
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao carregar rewards pendentes de " + uuid + ": " + e.getMessage());
                cache.put(uuid, new ArrayList<>());
            }
        });
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public void addItem(Player player, Material material, int amount, String displayName) {
        add(player, "ITEM", material.name(), amount, null, displayName);
    }

    public void addCrateKey(Player player, String crateId, int amount, String displayName) {
        add(player, "CRATE_KEY", null, amount, crateId, displayName);
    }

    private void add(Player player, String type, String material, int amount, String crateId, String displayName) {
        UUID uuid = player.getUniqueId();
        List<PendingRewardEntity> list = cache.computeIfAbsent(uuid, id -> new ArrayList<>());

        // Mesmo padrão de merge do FishBagService#add - sem isso cada peixe pescado criava
        // uma linha nova de 1x na tabela (340 recompensas pendentes = 340 linhas de "1x"
        // em vez de uma pilha só, mesmo a entrega final empilhando certo no inventário
        // graças ao addItem() do Bukkit).
        for (int i = 0; i < list.size(); i++) {
            PendingRewardEntity e = list.get(i);
            if (!e.type().equals(type) || !java.util.Objects.equals(e.material(), material)
                    || !java.util.Objects.equals(e.crateId(), crateId)) {
                continue;
            }
            PendingRewardEntity merged = new PendingRewardEntity(e.id(), e.playerUuid(), e.type(), e.material(),
                    e.amount() + amount, e.crateId(), e.displayName(), e.createdAt());
            list.set(i, merged);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getPendingRewardRepository().updateAmount(merged.id(), merged.amount());
                } catch (Exception ex) {
                    plugin.getLogger().warning("Falha ao atualizar reward pendente: " + ex.getMessage());
                }
            });
            return;
        }

        PendingRewardEntity entity = new PendingRewardEntity(
                UUID.randomUUID(), uuid, type, material, amount, crateId, displayName, System.currentTimeMillis());
        list.add(entity);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getPendingRewardRepository().insert(entity);
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao salvar reward pendente: " + e.getMessage());
            }
        });
    }

    /** Lê do cache (populado no join) - nunca bloqueia a thread chamadora. Cópia defensiva:
     * ver FishBagService#getBag para o motivo (evita ConcurrentModificationException
     * quando claimAll muta a lista viva do cache enquanto o chamador ainda itera). */
    public List<PendingRewardEntity> getPending(Player player) {
        return new ArrayList<>(cache.getOrDefault(player.getUniqueId(), List.of()));
    }

    /** Reivindica uma recompensa: entrega (item/key) e remove da lista. */
    public boolean claim(Player player, UUID rewardId) {
        List<PendingRewardEntity> list = cache.get(player.getUniqueId());
        if (list == null) return false;
        PendingRewardEntity found = null;
        for (PendingRewardEntity e : list) {
            if (e.id().equals(rewardId)) { found = e; break; }
        }
        if (found == null) return false;

        deliver(player, found);
        list.remove(found);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getPendingRewardRepository().deleteById(rewardId);
            } catch (Exception ignored) {}
        });
        return true;
    }

    /** Reivindica tudo de uma vez. Retorna quantas recompensas foram entregues. */
    public int claimAll(Player player) {
        int count = 0;
        for (PendingRewardEntity e : getPending(player)) {
            if (claim(player, e.id())) count++;
        }
        return count;
    }

    private void deliver(Player player, PendingRewardEntity e) {
        if ("CRATE_KEY".equals(e.type())) {
            if (plugin.getAlkaCratesHook() != null && plugin.getAlkaCratesHook().isAvailable()) {
                plugin.getAlkaCratesHook().giveKey(player, e.crateId(), e.amount());
            } else {
                player.sendMessage(plugin.getMessages().parse("rewards.crates-unavailable"));
            }
            return;
        }
        Material material = e.material() != null ? Material.matchMaterial(e.material()) : null;
        if (material == null) return;
        ItemStack item = new ItemStack(material, e.amount());
        player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }
}
