package com.alkacode.fish.database.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.fish.database.entity.TournamentRecordEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Histórico de resultados de torneios. */
public final class TournamentRecordRepository extends AbstractRepository {

    public TournamentRecordRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS alkafish_tournament_records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "tournament_type VARCHAR(32) NOT NULL, "
                + "score DOUBLE NOT NULL, "
                + "position INT NOT NULL, "
                + "reward DOUBLE NOT NULL, "
                + "ended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        execute(sql, ps -> {});
    }

    public void insert(TournamentRecordEntity e) throws SQLException {
        execute("INSERT INTO alkafish_tournament_records (player_uuid, tournament_type, score, position, reward) VALUES (?, ?, ?, ?, ?)",
                ps -> {
                    ps.setString(1, e.playerUuid().toString());
                    ps.setString(2, e.tournamentType());
                    ps.setDouble(3, e.score());
                    ps.setInt(4, e.position());
                    ps.setDouble(5, e.reward());
                });
    }

    public List<TournamentRecordEntity> findWinsByPlayer(UUID playerUuid, int limit) throws SQLException {
        List<TournamentRecordEntity> out = new ArrayList<>();
        try (var conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM alkafish_tournament_records WHERE player_uuid = ? ORDER BY ended_at DESC LIMIT ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private TournamentRecordEntity map(ResultSet rs) throws SQLException {
        return new TournamentRecordEntity(
                rs.getInt("id"),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("tournament_type"),
                rs.getDouble("score"),
                rs.getInt("position"),
                rs.getDouble("reward"),
                rs.getTimestamp("ended_at"));
    }
}
