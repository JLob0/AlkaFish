package com.alkacode.fish.database.entity;

import java.util.UUID;

/** Linha da tabela alkafish_pending_rewards - recompensa de pesca esperando ser
 * reivindicada na aba "Recompensas" da Sacola de Peixes. */
public record PendingRewardEntity(
    UUID id,
    UUID playerUuid,
    String type,        // "ITEM" ou "CRATE_KEY"
    String material,     // Material enum name - null se type=CRATE_KEY
    int amount,
    String crateId,       // id da crate no AlkaCrates - null se type=ITEM
    String displayName,
    long createdAt
) {}
