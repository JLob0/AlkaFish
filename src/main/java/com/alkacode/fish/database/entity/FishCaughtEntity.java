package com.alkacode.fish.database.entity;

import java.sql.Timestamp;
import java.util.UUID;

/** Registro de um peixe capturado por um jogador. */
public record FishCaughtEntity(
    int id,
    UUID playerUuid,
    String fishId,
    double length,
    double weight,
    Timestamp caughtAt,
    String biome,
    String world
) {}
