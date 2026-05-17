package st.project.model.game;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private int currentLevel;
    private boolean gameWon;
    private boolean gameOver;
    private int framesLeft;
    private Player player;
    private Room currentRoom;
    private List<Enemy> enemies;
    private List<Projectile> projectiles;
    private long runStartMillis;
    private long elapsedMillis;

    public GameState(int level, Player player, Room room) {
        this.currentLevel = level;
        this.gameWon = false;
        this.gameOver = false;
        this.framesLeft = 900; // Default: 30 seconds
        this.player = player;
        this.currentRoom = room;
        this.enemies = new ArrayList<>();
        this.projectiles = new ArrayList<>();
        this.runStartMillis = System.currentTimeMillis();
        this.elapsedMillis = 0;
    }

    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int level) { this.currentLevel = level; }

    public boolean isGameWon() { return gameWon; }
    public void setGameWon(boolean won) { this.gameWon = won; }

    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean over) { this.gameOver = over; }

    public int getFramesLeft() { return framesLeft; }
    public void setFramesLeft(int frames) { this.framesLeft = frames; }
    public void decrementFrames() { this.framesLeft--; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room room) { this.currentRoom = room; }

    public List<Enemy> getEnemies() { return enemies; }
    public void setEnemies(List<Enemy> enemies) { this.enemies = enemies; }

    public List<Projectile> getProjectiles() { return projectiles; }
    public void setProjectiles(List<Projectile> projectiles) { this.projectiles = projectiles; }

    public long getRunStartMillis() { return runStartMillis; }
    public void setRunStartMillis(long millis) { this.runStartMillis = millis; }

    public long getElapsedMillis() { return elapsedMillis; }
    public void setElapsedMillis(long millis) { this.elapsedMillis = millis; }

    public boolean isGameFinished() {
        return gameWon || gameOver;
    }

    public void clearEnemies() { enemies.clear(); }
    public void clearProjectiles() { projectiles.clear(); }
}
