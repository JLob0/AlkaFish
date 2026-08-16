package com.alkacode.fish.database.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.fish.database.entity.PlayerFishDataEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** Persistência do estado de pesca dos jogadores. */
public final class PlayerFishDataRepository extends AbstractRepository {

    public PlayerFishDataRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS alkafish_player_data ("
                + "player_uuid VARCHAR(36) PRIMARY KEY, "
                + "level INT DEFAULT 1, "
                + "xp DOUBLE DEFAULT 0, "
                + "total_caught INT DEFAULT 0, "
                + "biggest_length DOUBLE DEFAULT 0, "
                + "biggest_fish_id VARCHAR(64) DEFAULT '', "
                + "total_weight DOUBLE DEFAULT 0, "
                + "bag_capacity DOUBLE DEFAULT 50.0, "
                + "current_bag_weight DOUBLE DEFAULT 0, "
                + "rod_id VARCHAR(32) DEFAULT 'wooden', "
                + "rod_level INT DEFAULT 1, "
                + "rod_broken BOOLEAN DEFAULT FALSE, "
                + "rod_enchants TEXT DEFAULT '', "
                + "active_class_id VARCHAR(32) DEFAULT '', "
                + "unlocked_classes TEXT DEFAULT '', "
                + "nacar DOUBLE DEFAULT 0, "
                + "nacar_next DOUBLE DEFAULT 100)";
        execute(sql, ps -> {});
    }

    public void save(PlayerFishDataEntity e) throws SQLException {
        String[] columns = {"player_uuid", "level", "xp", "total_caught", "biggest_length",
                "biggest_fish_id", "total_weight", "bag_capacity", "current_bag_weight",
                "rod_id", "rod_level", "rod_broken", "rod_enchants",
                "active_class_id", "unlocked_classes", "nacar", "nacar_next"};
        String sql = upsert("alkafish_player_data", columns, new String[]{"player_uuid"});
        execute(sql, ps -> bind(ps, e));
    }

    private void bind(PreparedStatement ps, PlayerFishDataEntity e) throws SQLException {
        ps.setString(1, e.playerUuid().toString());
        ps.setInt(2, e.level());
        ps.setDouble(3, e.xp());
        ps.setInt(4, e.totalCaught());
        ps.setDouble(5, e.biggestLength());
        ps.setString(6, e.biggestFishId());
        ps.setDouble(7, e.totalWeight());
        ps.setDouble(8, e.bagCapacity());
        ps.setDouble(9, e.currentBagWeight());
        ps.setString(10, e.rodId());
        ps.setInt(11, e.rodLevel());
        ps.setBoolean(12, e.rodBroken());
        ps.setString(13, e.rodEnchants());
        ps.setString(14, e.activeClassId());
        ps.setString(15, e.unlockedClasses());
        ps.setDouble(16, e.nacar());
        ps.setDouble(17, e.nacarNext());
    }

    public Optional<PlayerFishDataEntity> findByUuid(UUID uuid) throws SQLException {
        try (var conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM alkafish_player_data WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private PlayerFishDataEntity map(ResultSet rs) throws SQLException {
        return new PlayerFishDataEntity(
                UUID.fromString(rs.getString("player_uuid")),
                rs.getInt("level"),
                rs.getDouble("xp"),
                rs.getInt("total_caught"),
                rs.getDouble("biggest_length"),
                rs.getString("biggest_fish_id"),
                rs.getDouble("total_weight"),
                rs.getDouble("bag_capacity"),
                rs.getDouble("current_bag_weight"),
                rs.getString("rod_id"),
                rs.getInt("rod_level"),
                rs.getBoolean("rod_broken"),
                rs.getString("rod_enchants"),
                rs.getString("active_class_id"),
                rs.getString("unlocked_classes"),
                rs.getDouble("nacar"),
                rs.getDouble("nacar_next"));
    }
}
