package st.project.controller;

import st.project.model.GameConfig;
import st.project.model.game.*;
import st.project.model.user.LeaderboardEntry;
import st.project.repository.LeaderboardRepository;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameController {
    private final GameState gameState;
    private final LeaderboardRepository leaderboardRepository;
    private final String playerName;
    private long runStartMillis;
    private boolean scoreSaved;
    private List<LeaderboardEntry> leaderboard;

    public GameController(GameState gameState, LeaderboardRepository repository, String playerName) {
        this.gameState = gameState;
        this.leaderboardRepository = repository;
        this.playerName = playerName;
        this.runStartMillis = System.currentTimeMillis();
        this.scoreSaved = false;
        this.leaderboard = new ArrayList<>();

        leaderboardRepository.saveCurrentPlayer(playerName);
        loadLeaderboard();
    }

    public void updateGameLogic() {
        if (gameState.isGameFinished()) return;

        gameState.decrementFrames();
        if (gameState.getFramesLeft() <= 0) {
            gameState.setGameOver(true);
        }

        checkEnemyCollisions();
        updateEnemyProjectiles();
        updatePlayerProjectiles();

        if (gameState.isGameOver()) {
            finishGame(false);
        }
    }

    private void checkEnemyCollisions() {
        for (Enemy e : gameState.getEnemies()) {
            if (gameState.getPlayer().getGridX() == e.getGridX() &&
                gameState.getPlayer().getGridY() == e.getGridY()) {
                gameState.setGameOver(true);
            }
        }
    }

    private void updateEnemyProjectiles() {
        for (Enemy e : gameState.getEnemies()) {
            e.updateCooldown();
            if (e.canShoot()) {
                fireEnemyProjectile(e);
            }
        }
    }

    private void fireEnemyProjectile(Enemy e) {
        double angle = Math.atan2(gameState.getPlayer().getGridY() - e.getGridY(),
                                  gameState.getPlayer().getGridX() - e.getGridX());
        int speed = 8;
        int dx = (int) (Math.cos(angle) * speed);
        int dy = (int) (Math.sin(angle) * speed);

        int startX = e.getGridX() * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
        int startY = e.getGridY() * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
        gameState.getProjectiles().add(new Projectile(startX, startY, dx, dy, false));
    }

    private void updatePlayerProjectiles() {
        Iterator<Projectile> it = gameState.getProjectiles().iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.update();

            if (isProjectileOutOfBounds(p)) {
                it.remove();
                continue;
            }

            if (isProjectileInWall(p)) {
                it.remove();
                continue;
            }

            if (p.isPlayerOwned()) {
                if (checkPlayerProjectileEnemyCollision(p, it)) {
                    continue;
                }
            } else {
                if (checkEnemyProjectilePlayerCollision(p)) {
                    gameState.setGameOver(true);
                }
            }
        }
    }

    private boolean isProjectileOutOfBounds(Projectile p) {
        int gridX = p.getX() / GameConfig.TILE_SIZE;
        int gridY = p.getY() / GameConfig.TILE_SIZE;
        return gridX < 0 || gridX >= gameState.getCurrentRoom().getWidth() ||
               gridY < 0 || gridY >= gameState.getCurrentRoom().getHeight();
    }

    private boolean isProjectileInWall(Projectile p) {
        int gridX = p.getX() / GameConfig.TILE_SIZE;
        int gridY = p.getY() / GameConfig.TILE_SIZE;
        if (gridX >= 0 && gridX < gameState.getCurrentRoom().getWidth() &&
            gridY >= 0 && gridY < gameState.getCurrentRoom().getHeight()) {
            return gameState.getCurrentRoom().getMapLayout()[gridY][gridX] == Room.TILE_WALL;
        }
        return false;
    }

    private boolean checkPlayerProjectileEnemyCollision(Projectile p, Iterator<Projectile> projectileIt) {
        Iterator<Enemy> eIt = gameState.getEnemies().iterator();
        while (eIt.hasNext()) {
            Enemy e = eIt.next();
            if (Math.abs(p.getX() - (e.getGridX() * GameConfig.TILE_SIZE + 20)) < 20 &&
                Math.abs(p.getY() - (e.getGridY() * GameConfig.TILE_SIZE + 20)) < 20) {
                eIt.remove();
                projectileIt.remove();
                return true;
            }
        }
        return false;
    }

    private boolean checkEnemyProjectilePlayerCollision(Projectile p) {
        Player player = gameState.getPlayer();
        return Math.abs(p.getX() - (player.getGridX() * GameConfig.TILE_SIZE + 20)) < 20 &&
               Math.abs(p.getY() - (player.getGridY() * GameConfig.TILE_SIZE + 20)) < 20;
    }

    public void handlePlayerMovement(int nextX, int nextY) {
        Player player = gameState.getPlayer();
        Room room = gameState.getCurrentRoom();

        if (gameState.isGameFinished()) return;

        int[][] layout = room.getMapLayout();
        if (layout[nextY][nextX] == Room.TILE_WALL || layout[nextY][nextX] == Room.TILE_EXIT_LOCKED) {
            return;
        }

        // Update position based on direction
        if (nextX < player.getGridX()) player.moveLeft();
        else if (nextX > player.getGridX()) player.moveRight();
        else if (nextY < player.getGridY()) player.moveUp();
        else if (nextY > player.getGridY()) player.moveDown();

        checkItemsAndPortals();
    }

    private void checkItemsAndPortals() {
        Player player = gameState.getPlayer();
        Room room = gameState.getCurrentRoom();

        if (room.takeItemAt(player.getGridX(), player.getGridY()) == Room.TILE_KEY) {
            player.collectKey();
            if (player.hasAllKeys()) {
                room.openExit(14, 5);
            }
        }

        if (room.getMapLayout()[player.getGridY()][player.getGridX()] == Room.TILE_EXIT_OPEN) {
            if (gameState.getCurrentLevel() == 1) {
                loadLevel2();
            } else {
                gameState.setGameWon(true);
                finishGame(true);
            }
        }
    }

    public void addPlayerProjectile(int px, int py, int velX, int velY) {
        Player player = gameState.getPlayer();
        if (gameState.isGameFinished() || !player.hasAmmo()) return;

        player.decreaseAmmo();
        gameState.getProjectiles().add(new Projectile(px, py, velX, velY, true));
    }

    public void loadLevel1() {
        Player player = new Player(1, 1);
        int[][] layout = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1},
                {1, 0, 1, 2, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1},
                {1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3},
                {1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1},
                {1, 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1},
                {1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };

        Room room = new Room(layout);
        player.setPosition(1, 1, room);

        gameState.setCurrentLevel(1);
        gameState.setPlayer(player);
        gameState.setCurrentRoom(room);
        gameState.setFramesLeft(GameConfig.GAME_DURATION_FRAMES);
        gameState.clearEnemies();
        gameState.clearProjectiles();
        gameState.setGameWon(false);
        gameState.setGameOver(false);

        this.runStartMillis = System.currentTimeMillis();
        this.scoreSaved = false;
    }

    public void loadLevel2() {
        Player player = gameState.getPlayer();
        player.unlockShooting();
        player.resetKeys();

        int[][] layout = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1},
                {1, 0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 2, 1, 0, 1},
                {1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3},
                {1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1},
                {1, 0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1},
                {1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };

        Room room = new Room(layout);
        player.setPosition(1, 1, room);

        gameState.setCurrentLevel(2);
        gameState.setCurrentRoom(room);
        gameState.setFramesLeft(GameConfig.GAME_DURATION_FRAMES);
        gameState.clearEnemies();
        gameState.clearProjectiles();
        gameState.getEnemies().add(new Enemy(7, 3));
        gameState.getEnemies().add(new Enemy(7, 7));
    }

    private void saveWinningScore() {
        if (scoreSaved) return;

        long elapsed = System.currentTimeMillis() - runStartMillis;
        leaderboardRepository.saveScore(playerName, elapsed);
        loadLeaderboard();
        scoreSaved = true;
    }

    private void finishGame(boolean won) {
        if (won) {
            saveWinningScore();
        }
    }

    private void loadLeaderboard() {
        leaderboard = leaderboardRepository.getTopScores(5);
    }

    public GameState getGameState() {
        return gameState;
    }

    public long getElapsedMillis() {
        return System.currentTimeMillis() - runStartMillis;
    }

    public List<LeaderboardEntry> getLeaderboard() {
        return leaderboard;
    }
}
