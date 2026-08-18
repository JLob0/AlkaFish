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
        addColumnIfMissing("auto_upgrade_enabled", "BOOLEAN DEFAULT FALSE");
        addColumnIfMissing("saved_inventory", "TEXT DEFAULT ''");
        addColumnIfMissing("total_fishing_seconds", "BIGINT DEFAULT 0");
        addColumnIfMissing("rod_skin_id", "VARCHAR(32) DEFAULT ''");
        addColumnIfMissing("auto_sell_on_full", "BOOLEAN DEFAULT FALSE");
    }

    /** Adiciona coluna nova numa tabela já existente sem apagar dados (ALTER TABLE
     * idempotente - ignora o erro de "coluna já existe" em restarts seguintes). */
    private void addColumnIfMissing(String column, String definition) {
        try {
            execute("ALTER TABLE alkafish_player_data ADD COLUMN " + column + " " + definition, ps -> {});
        } catch (SQLException ignored) {
            // coluna ja existe de uma execucao anterior
        }
    }

    public void save(PlayerFishDataEntity e) throws SQLException {
        String[] columns = {"player_uuid", "level", "xp", "total_caught", "biggest_length",
                "biggest_fish_id", "total_weight", "bag_capacity", "current_bag_weight",
                "rod_id", "rod_level", "rod_broken", "rod_enchants",
                "active_class_id", "unlocked_classes", "nacar", "nacar_next", "auto_upgrade_enabled", "saved_inventory",
                "total_fishing_seconds", "rod_skin_id", "auto_sell_on_full"};
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
        ps.setDouble(16, e.rodNacarEarned());
        ps.setDouble(17, e.nacarNext());
        ps.setBoolean(18, e.autoUpgradeEnabled());
        ps.setString(19, e.savedInventory());
        ps.setLong(20, e.totalFishingSeconds());
        ps.setString(21, e.rodSkinId());
        ps.setBoolean(22, e.autoSellOnFull());
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
                rs.getDouble("nacar_next"),
                rs.getBoolean("auto_upgrade_enabled"),
                rs.getString("saved_inventory"),
                rs.getLong("total_fishing_seconds"),
                rs.getString("rod_skin_id"),
                rs.getBoolean("auto_sell_on_full"));
    }

    /** Top N jogadores por uma coluna numérica (total_caught, total_weight ou
     * total_fishing_seconds) - consulta direta no banco (não no cache, que só tem
     * jogadores online), pensada pra tela de TOP, não pra hot path. */
    private static final java.util.Set<String> TOP_COLUMNS =
            java.util.Set.of("total_caught", "total_weight", "total_fishing_seconds");

    public java.util.List<TopEntry> findTop(String column, int limit) throws SQLException {
        if (!TOP_COLUMNS.contains(column)) {
            throw new IllegalArgumentException("Coluna de TOP não permitida: " + column);
        }
        java.util.List<TopEntry> out = new java.util.ArrayList<>();
        try (var conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT player_uuid, " + column + " AS value FROM alkafish_player_data "
                             + "ORDER BY " + column + " DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new TopEntry(UUID.fromString(rs.getString("player_uuid")), rs.getDouble("value")));
                }
            }
        }
        return out;
    }

    public record TopEntry(UUID uuid, double value) {}
}
