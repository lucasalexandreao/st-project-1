package st.project;

import st.project.model.user.LeaderboardEntry;
import st.project.repository.JdbcLeaderboardRepository;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcLeaderboardRepositoryTest {

    @Test
    void shouldPersistAndReturnOrderedBestTimes() throws Exception {
        Path dbFile = Files.createTempFile("leaderboard-test", ".db");
        try {
            JdbcLeaderboardRepository repository = new JdbcLeaderboardRepository(dbFile.toString());

            repository.saveCurrentPlayer("Ana");
            repository.saveScore("Ana", 4500);
            repository.saveScore("Bia", 2200);
            repository.saveScore("Cris", 3100);

            List<LeaderboardEntry> top2 = repository.getTopScores(2);

            assertEquals(2, top2.size());
            assertEquals("Bia", top2.get(0).getPlayerName());
            assertEquals(2200, top2.get(0).getCompletionMillis());
            assertEquals("Cris", top2.get(1).getPlayerName());
            assertEquals(3100, top2.get(1).getCompletionMillis());

            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
                 PreparedStatement statement = connection.prepareStatement("SELECT player_name FROM players WHERE player_name = ?")) {
                statement.setString(1, "Ana");
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertEquals(true, resultSet.next());
                    assertEquals("Ana", resultSet.getString("player_name"));
                    assertEquals(false, resultSet.next());
                }
            }

            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
                 PreparedStatement statement = connection.prepareStatement("SELECT session_count FROM players WHERE player_name = ?")) {
                statement.setString(1, "Ana");
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertEquals(true, resultSet.next());
                    assertEquals(1, resultSet.getInt("session_count"));
                    assertEquals(false, resultSet.next());
                }
            }
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    void shouldDeleteUserAndTheirRuns() throws Exception {
        Path dbFile = Files.createTempFile("leaderboard-test-delete", ".db");
        try {
            JdbcLeaderboardRepository repository = new JdbcLeaderboardRepository(dbFile.toString());

            repository.saveCurrentPlayer("Ana");
            repository.saveScore("Ana", 4500);
            repository.saveScore("Ana", 2200);

            repository.deleteUser("Ana");

            assertEquals(0, repository.getTopScores(10).size());

            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
                 PreparedStatement statement = connection.prepareStatement("SELECT player_name FROM players WHERE player_name = ?")) {
                statement.setString(1, "Ana");
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertEquals(false, resultSet.next());
                }
            }
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }
}
