package com.alkacode.fish.database.entity;

import java.util.UUID;

/** Linha da tabela alkafish_bag (peixes agrupados por tipo). */
public record FishBagEntryEntity(
    int id,
    UUID playerUuid,
    String fishId,
    int amount,
    double totalWeight,
    long caughtAt
) {}
