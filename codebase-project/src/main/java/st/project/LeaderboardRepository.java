package st.project;

import java.util.List;

public interface LeaderboardRepository {
    void saveScore(String playerName, long completionMillis);
    List<LeaderboardEntry> getTopScores(int limit);
}
