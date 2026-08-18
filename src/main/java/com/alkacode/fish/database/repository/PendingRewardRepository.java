package com.alkacode.fish.database.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.fish.database.entity.PendingRewardEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Recompensas de pesca pendentes de reivindicação (itens/keys de crate). */
public final class PendingRewardRepository extends AbstractRepository {

    public PendingRewardRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS alkafish_pending_rewards ("
                + "id VARCHAR(36) PRIMARY KEY, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "type VARCHAR(16) NOT NULL, "
                + "material VARCHAR(64), "
                + "amount INT DEFAULT 1, "
                + "crate_id VARCHAR(64), "
                + "display_name VARCHAR(128), "
                + "created_at BIGINT)";
        execute(sql, ps -> {});
    }

    public void insert(PendingRewardEntity e) throws SQLException {
        execute("INSERT INTO alkafish_pending_rewards (id, player_uuid, type, material, amount, crate_id, display_name, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                ps -> {
                    ps.setString(1, e.id().toString());
                    ps.setString(2, e.playerUuid().toString());
                    ps.setString(3, e.type());
                    ps.setString(4, e.material());
                    ps.setInt(5, e.amount());
                    ps.setString(6, e.crateId());
                    ps.setString(7, e.displayName());
                    ps.setLong(8, e.createdAt());
                });
    }

    public void updateAmount(UUID id, int amount) throws SQLException {
        execute("UPDATE alkafish_pending_rewards SET amount = ? WHERE id = ?", ps -> {
            ps.setInt(1, amount);
            ps.setString(2, id.toString());
        });
    }

    public void deleteById(UUID id) throws SQLException {
        execute("DELETE FROM alkafish_pending_rewards WHERE id = ?", ps -> ps.setString(1, id.toString()));
    }

    public List<PendingRewardEntity> findByPlayer(UUID playerUuid) throws SQLException {
        List<PendingRewardEntity> out = new ArrayList<>();
        try (var conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM alkafish_pending_rewards WHERE player_uuid = ? ORDER BY created_at ASC")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private PendingRewardEntity map(ResultSet rs) throws SQLException {
        return new PendingRewardEntity(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("type"),
                rs.getString("material"),
                rs.getInt("amount"),
                rs.getString("crate_id"),
                rs.getString("display_name"),
                rs.getLong("created_at"));
    }
}
