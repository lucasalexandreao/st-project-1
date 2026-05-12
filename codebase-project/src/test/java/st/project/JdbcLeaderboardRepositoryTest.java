package st.project;

import org.junit.jupiter.api.Test;

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

            repository.saveScore("Ana", 4500);
            repository.saveScore("Bia", 2200);
            repository.saveScore("Cris", 3100);

            List<LeaderboardEntry> top2 = repository.getTopScores(2);

            assertEquals(2, top2.size());
            assertEquals("Bia", top2.get(0).getPlayerName());
            assertEquals(2200, top2.get(0).getCompletionMillis());
            assertEquals("Cris", top2.get(1).getPlayerName());
            assertEquals(3100, top2.get(1).getCompletionMillis());
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }
}
