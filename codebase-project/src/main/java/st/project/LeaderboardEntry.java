package st.project;

public class LeaderboardEntry {
    private final String playerName;
    private final long completionMillis;
    private final String playedAt;

    public LeaderboardEntry(String playerName, long completionMillis, String playedAt) {
        this.playerName = playerName;
        this.completionMillis = completionMillis;
        this.playedAt = playedAt;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getCompletionMillis() {
        return completionMillis;
    }

    public String getPlayedAt() {
        return playedAt;
    }
}
