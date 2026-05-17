package st.project;

import java.util.Collections;
import java.util.List;

public class NoOpLeaderboardRepository implements LeaderboardRepository {

    @Override
    public void saveScore(String playerName, long completionMillis) {
        // Intencionalmente vazio: fallback quando o banco não estiver disponível.
    }

    @Override
    public void saveCurrentPlayer(String playerName) {
        // Intencionalmente vazio: fallback quando o banco não estiver disponível.
    }

    @Override
    public List<LeaderboardEntry> getTopScores(int limit) {
        return Collections.emptyList();
    }

    @Override
    public void createUser(String playerName, String passwordHash, boolean isSuperuser) {
        // no-op
    }

    @Override
    public void deleteUser(String playerName) {
        // no-op
    }

    @Override
    public User getUser(String playerName) {
        return null;
    }

    @Override
    public List<User> getTopUsersByScore(int limit) {
        return Collections.emptyList();
    }

    @Override
    public List<User> getTopUsersBySessions(int limit) {
        return Collections.emptyList();
    }
}
