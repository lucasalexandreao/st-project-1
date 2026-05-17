package st.project;

public class User {
    private final String playerName;
    private final String passwordHash;
    private final long totalScore;
    private final int sessionCount;
    private final boolean superuser;

    public User(String playerName, String passwordHash, long totalScore, int sessionCount, boolean superuser) {
        this.playerName = playerName;
        this.passwordHash = passwordHash;
        this.totalScore = totalScore;
        this.sessionCount = sessionCount;
        this.superuser = superuser;
    }

    public String getPlayerName() { return playerName; }
    public String getPasswordHash() { return passwordHash; }
    public long getTotalScore() { return totalScore; }
    public int getSessionCount() { return sessionCount; }
    public boolean isSuperuser() { return superuser; }
}
