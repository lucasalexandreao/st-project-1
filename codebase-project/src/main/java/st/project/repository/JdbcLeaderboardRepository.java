package st.project.repository;

import st.project.model.user.User;
import st.project.model.user.LeaderboardEntry;
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
        String normalizedName = playerName == null ? null : playerName.trim();
        String sql = "INSERT INTO leaderboard (player_name, completion_millis, played_at) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, normalizedName);
                statement.setLong(2, completionMillis);
                statement.setString(3, Instant.now().toString());
                statement.executeUpdate();
            }

            // Update aggregate user stats (total_score) for a finished win.
            String updateSql = "UPDATE players SET total_score = COALESCE(total_score,0) + ?, last_login = ? WHERE player_name = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                updateStmt.setLong(1, completionMillis);
                updateStmt.setString(2, Instant.now().toString());
                updateStmt.setString(3, normalizedName);
                int updated = updateStmt.executeUpdate();
                if (updated == 0) {
                    String insertUser = "INSERT INTO players(player_name, last_login, total_score, session_count) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertUser)) {
                        insertStmt.setString(1, normalizedName);
                        insertStmt.setString(2, Instant.now().toString());
                        insertStmt.setLong(3, completionMillis);
                        insertStmt.setInt(4, 1);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao salvar score no leaderboard", e);
        }
    }

    @Override
    public void saveCurrentPlayer(String playerName) {
        String normalizedName = playerName == null ? null : playerName.trim();
        String sql = "INSERT INTO players (player_name, last_login, session_count) VALUES (?, ?, 1) "
            + "ON CONFLICT(player_name) DO UPDATE SET last_login = excluded.last_login, session_count = COALESCE(players.session_count, 0) + 1";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedName);
            statement.setString(2, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao salvar jogador atual", e);
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
        String leaderboardSql = "CREATE TABLE IF NOT EXISTS leaderboard ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "player_name TEXT NOT NULL,"
                + "completion_millis INTEGER NOT NULL CHECK(completion_millis >= 0),"
                + "played_at TEXT NOT NULL"
                + ")";
        String playersSql = "CREATE TABLE IF NOT EXISTS players (" +
            "player_name TEXT PRIMARY KEY," +
            "last_login TEXT NOT NULL," +
            "password TEXT," +
            "total_score INTEGER DEFAULT 0," +
            "session_count INTEGER DEFAULT 0," +
            "is_superuser INTEGER DEFAULT 0" +
            ")";

        String usersIndex = "CREATE INDEX IF NOT EXISTS idx_users_score ON players(total_score DESC)";
        String usersSessionsIndex = "CREATE INDEX IF NOT EXISTS idx_users_sessions ON players(session_count DESC)";

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute(leaderboardSql);
            statement.execute(playersSql);
            statement.execute(usersIndex);
            statement.execute(usersSessionsIndex);
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao criar schema do leaderboard", e);
        }
    }

    @Override
    public void createUser(String playerName, String passwordHash, boolean isSuperuser) {
        String normalizedName = playerName == null ? null : playerName.trim();
        String sql = "INSERT OR REPLACE INTO players(player_name, password, total_score, session_count, is_superuser, last_login) VALUES (?, ?, COALESCE((SELECT total_score FROM players WHERE player_name = ?), 0), COALESCE((SELECT session_count FROM players WHERE player_name = ?), 0), ?, ?)";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, normalizedName);
            stmt.setString(2, passwordHash);
            stmt.setString(3, normalizedName);
            stmt.setString(4, normalizedName);
            stmt.setInt(5, isSuperuser ? 1 : 0);
            stmt.setString(6, Instant.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao criar/atualizar usuário", e);
        }
    }

    @Override
    public void deleteUser(String playerName) {
        String normalizedName = playerName == null ? null : playerName.trim();
        String sql = "DELETE FROM players WHERE player_name = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, normalizedName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao deletar usuário", e);
        }
    }

    @Override
    public User getUser(String playerName) {
        String normalizedName = playerName.toLowerCase();
        String sql = "SELECT player_name, password, total_score, session_count, is_superuser FROM players WHERE player_name = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, normalizedName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("player_name"),
                            rs.getString("password"),
                            rs.getLong("total_score"),
                            rs.getInt("session_count"),
                            rs.getInt("is_superuser") != 0
                    );
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao buscar usuário", e);
        }
        return null;
    }

    @Override
    public List<User> getTopUsersByScore(int limit) {
        String sql = "SELECT player_name, password, total_score, session_count, is_superuser FROM players ORDER BY total_score DESC, player_name ASC LIMIT ?";
        List<User> users = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(
                            rs.getString("player_name"),
                            rs.getString("password"),
                            rs.getLong("total_score"),
                            rs.getInt("session_count"),
                            rs.getInt("is_superuser") != 0
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao consultar top users por score", e);
        }
        return users;
    }

    @Override
    public List<User> getTopUsersBySessions(int limit) {
        String sql = "SELECT player_name, password, total_score, session_count, is_superuser FROM players ORDER BY session_count DESC, player_name ASC LIMIT ?";
        List<User> users = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(
                            rs.getString("player_name"),
                            rs.getString("password"),
                            rs.getLong("total_score"),
                            rs.getInt("session_count"),
                            rs.getInt("is_superuser") != 0
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao consultar top users por sessions", e);
        }
        return users;
    }
}
