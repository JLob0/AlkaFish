package com.alkacode.fish.database.entity;

import java.util.UUID;

/** Linha da tabela alkafish_player_data. */
public record PlayerFishDataEntity(
    UUID playerUuid,
    int level,
    double xp,
    int totalCaught,
    double biggestLength,
    String biggestFishId,
    double totalWeight,
    double bagCapacity,
    double currentBagWeight,
    String rodId,
    int rodLevel,
    boolean rodBroken,
    String rodEnchants,
    String activeClassId,
    String unlockedClasses,
    double nacar,
    double nacarNext
) {}
