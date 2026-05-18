package st.project.model.user;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LeaderboardEntryTest {
    // ESTRUTURAL
    @Test
    void shouldStoreLeaderboardData() {
        LeaderboardEntry entry = new LeaderboardEntry("Heroi", 15000L, "2023-10-27");

        assertEquals("Heroi", entry.getPlayerName());
        assertEquals(15000L, entry.getCompletionMillis());
        assertEquals("2023-10-27", entry.getPlayedAt());
    }
}