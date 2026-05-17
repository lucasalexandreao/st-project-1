package st.project;

import java.util.List;

public interface LeaderboardRepository {
    void saveScore(String playerName, long completionMillis);
    void saveCurrentPlayer(String playerName);
    List<LeaderboardEntry> getTopScores(int limit);
    // User management
    void createUser(String playerName, String passwordHash, boolean isSuperuser);
    void deleteUser(String playerName);
    User getUser(String playerName);
    List<User> getTopUsersByScore(int limit);
    List<User> getTopUsersBySessions(int limit);
}
