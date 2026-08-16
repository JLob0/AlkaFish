package com.alkacode.fish.database.entity;

import java.sql.Timestamp;
import java.util.UUID;

/** Registro de resultado de torneio para histórico/leaderboard. */
public record TournamentRecordEntity(
    int id,
    UUID playerUuid,
    String tournamentType,
    double score,
    int position,
    double reward,
    Timestamp endedAt
) {}
