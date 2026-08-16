package com.alkacode.fish.database.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.fish.database.entity.FishCaughtEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Log de capturas de peixes. */
public final class FishCaughtRepository extends AbstractRepository {

    public FishCaughtRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS alkafish_caught_log ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "fish_id VARCHAR(64) NOT NULL, "
                + "length DOUBLE NOT NULL, "
                + "weight DOUBLE NOT NULL, "
                + "caught_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "biome VARCHAR(64), "
                + "world VARCHAR(64))";
        execute(sql, ps -> {});
    }

    public void insert(FishCaughtEntity e) throws SQLException {
        execute("INSERT INTO alkafish_caught_log (player_uuid, fish_id, length, weight, biome, world) VALUES (?, ?, ?, ?, ?, ?)",
                ps -> {
                    ps.setString(1, e.playerUuid().toString());
                    ps.setString(2, e.fishId());
                    ps.setDouble(3, e.length());
                    ps.setDouble(4, e.weight());
                    ps.setString(5, e.biome());
                    ps.setString(6, e.world());
                });
    }

    public List<FishCaughtEntity> findByPlayer(UUID playerUuid, int limit) throws SQLException {
        List<FishCaughtEntity> out = new ArrayList<>();
        try (var conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM alkafish_caught_log WHERE player_uuid = ? ORDER BY caught_at DESC LIMIT ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public int countByPlayerAndFish(UUID playerUuid, String fishId) throws SQLException {
        try (var conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM alkafish_caught_log WHERE player_uuid = ? AND fish_id = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, fishId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private FishCaughtEntity map(ResultSet rs) throws SQLException {
        return new FishCaughtEntity(
                rs.getInt("id"),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("fish_id"),
                rs.getDouble("length"),
                rs.getDouble("weight"),
                rs.getTimestamp("caught_at"),
                rs.getString("biome"),
                rs.getString("world"));
    }
}
