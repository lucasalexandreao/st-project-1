package st.project;

import java.util.Collections;
import java.util.List;

public class NoOpLeaderboardRepository implements LeaderboardRepository {

    @Override
    public void saveScore(String playerName, long completionMillis) {
        // Intencionalmente vazio: fallback quando o banco não estiver disponível.
    }

    @Override
    public List<LeaderboardEntry> getTopScores(int limit) {
        return Collections.emptyList();
    }
}
