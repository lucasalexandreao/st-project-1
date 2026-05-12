package st.project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JdbcLeaderboardRepository implements LeaderboardRepository {
    private final String jdbcUrl;

    public JdbcLeaderboardRepository(String dbPath) {
        this(dbPath, false);
    }

    public JdbcLeaderboardRepository(String jdbcUrl, boolean rawUrl) {
        this.jdbcUrl = rawUrl ? jdbcUrl : "jdbc:sqlite:" + jdbcUrl;
        ensureSchema();
    }

    

    @Override
    public void saveScore(String playerName, long completionMillis) {
        String sql = "INSERT INTO leaderboard (player_name, completion_millis, played_at) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setLong(2, completionMillis);
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao salvar score no leaderboard", e);
        }
    }

    @Override
    public List<LeaderboardEntry> getTopScores(int limit) {
        String sql = "SELECT player_name, completion_millis, played_at FROM leaderboard ORDER BY completion_millis ASC, id ASC LIMIT ?";
        List<LeaderboardEntry> entries = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new LeaderboardEntry(
                            resultSet.getString("player_name"),
                            resultSet.getLong("completion_millis"),
                            resultSet.getString("played_at")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao consultar leaderboard", e);
        }

        return entries;
    }

    private void ensureSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS leaderboard ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "player_name TEXT NOT NULL,"
                + "completion_millis INTEGER NOT NULL CHECK(completion_millis >= 0),"
                + "played_at TEXT NOT NULL"
                + ")";

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao criar schema do leaderboard", e);
        }
    }
}
