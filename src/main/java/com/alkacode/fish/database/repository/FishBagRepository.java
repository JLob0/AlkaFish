package com.alkacode.fish.database.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.fish.database.entity.FishBagEntryEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Sacola de peixes do jogador (agrupada por fish_id). */
public final class FishBagRepository extends AbstractRepository {

    public FishBagRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS alkafish_bag ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "fish_id VARCHAR(64) NOT NULL, "
                + "amount INTEGER DEFAULT 0, "
                + "total_weight DOUBLE DEFAULT 0, "
                + "caught_at BIGINT DEFAULT 0, "
                + "UNIQUE(player_uuid, fish_id))";
        execute(sql, ps -> {});
    }

    /** Adiciona/agrega um peixe (upsert por player+fish). */
    public void add(UUID playerUuid, String fishId, double weight) throws SQLException {
        execute("INSERT INTO alkafish_bag (player_uuid, fish_id, amount, total_weight, caught_at) VALUES (?, ?, 1, ?, ?) "
                        + "ON CONFLICT(player_uuid, fish_id) DO UPDATE SET "
                        + "amount = amount + 1, total_weight = total_weight + ?, caught_at = ?",
                ps -> {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, fishId);
                    ps.setDouble(3, weight);
                    ps.setLong(4, System.currentTimeMillis());
                    ps.setDouble(5, weight);
                    ps.setLong(6, System.currentTimeMillis());
                });
    }

    public List<FishBagEntryEntity> findBag(UUID playerUuid) throws SQLException {
        List<FishBagEntryEntity> out = new ArrayList<>();
        try (var conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM alkafish_bag WHERE player_uuid = ? ORDER BY caught_at DESC")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public void removeFish(UUID playerUuid, String fishId) throws SQLException {
        execute("DELETE FROM alkafish_bag WHERE player_uuid = ? AND fish_id = ?",
                ps -> { ps.setString(1, playerUuid.toString()); ps.setString(2, fishId); });
    }

    public void clear(UUID playerUuid) throws SQLException {
        execute("DELETE FROM alkafish_bag WHERE player_uuid = ?", ps -> ps.setString(1, playerUuid.toString()));
    }

    public double totalWeight(UUID playerUuid) throws SQLException {
        try (var conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(SUM(total_weight), 0) FROM alkafish_bag WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    private FishBagEntryEntity map(ResultSet rs) throws SQLException {
        return new FishBagEntryEntity(
                rs.getInt("id"),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("fish_id"),
                rs.getInt("amount"),
                rs.getDouble("total_weight"),
                rs.getLong("caught_at"));
    }
}
