package st.project.view;

import st.project.model.game.*;
import st.project.model.user.LeaderboardEntry;
import st.project.repository.LeaderboardRepository;
import st.project.repository.JdbcLeaderboardRepository;
import st.project.repository.NoOpLeaderboardRepository;
import st.project.Main;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.LongSupplier;

public class GamePanel extends JPanel {
    private final int TILE_SIZE = 40;

    private Room currentRoom;
    private Player player;
    private int currentLevel = 1;

    private boolean gameWon = false;
    private boolean gameOver = false;
    private int framesLeft;

    private List<Enemy> enemies;
    private List<Projectile> projectiles;

    private final LeaderboardRepository leaderboardRepository;
    private final LongSupplier clock;
    private final String currentPlayerName;
    private long runStartMillis;
    private boolean scoreSaved;
    private boolean postGameHandled;
    private List<LeaderboardEntry> leaderboard;

    private Timer gameLoop;

    public GamePanel() {
        this(createDefaultLeaderboardRepository(), System::currentTimeMillis, "Player");
    }

    public GamePanel(String playerName) {
        this(createDefaultLeaderboardRepository(), System::currentTimeMillis, playerName);
    }

    GamePanel(LeaderboardRepository leaderboardRepository, LongSupplier clock) {
        this(leaderboardRepository, clock, "Player");
    }

    GamePanel(LeaderboardRepository leaderboardRepository, LongSupplier clock, String playerName) {
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();
        this.leaderboardRepository = leaderboardRepository;
        this.clock = clock;
        this.currentPlayerName = normalizePlayerName(playerName);
        this.leaderboard = new ArrayList<>();
        this.runStartMillis = clock.getAsLong();
        this.scoreSaved = false;
        this.postGameHandled = false;

        this.leaderboardRepository.saveCurrentPlayer(this.currentPlayerName);
        loadLeaderboard();

        loadLevel1();

        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setBackground(Color.BLACK);
        this.addKeyListener(new GameInputAdapter());

        gameLoop = new Timer(1000 / 30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGameLogic();
                repaint();
            }
        });
        gameLoop.start();
    }

    private static LeaderboardRepository createDefaultLeaderboardRepository() {
        try {
            return new JdbcLeaderboardRepository("leaderboard.db");
        } catch (RuntimeException e) {
            return new NoOpLeaderboardRepository();
        }
    }

    private void loadLevel1() {
        currentLevel = 1;
        runStartMillis = clock.getAsLong();
        scoreSaved = false;
        postGameHandled = false;
        framesLeft = 30 * 30;
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
        currentRoom = new Room(layout);
        player = new Player(1, 1, currentRoom);
        enemies.clear();
        projectiles.clear();
    }

    private void loadLevel2() {
        currentLevel = 2;
        framesLeft = 30 * 30;
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
        currentRoom = new Room(layout);
        player.setPosition(1, 1, currentRoom);

        enemies.clear();
        projectiles.clear();
        enemies.add(new Enemy(7, 3));
        enemies.add(new Enemy(7, 7));
    }

    private void updateGameLogic() {
        if (gameWon || gameOver) return;

        framesLeft--;
        if (framesLeft <= 0) {
            gameOver = true;
        }

        for (Enemy e : enemies) {
            if (player.getGridX() == e.getGridX() && player.getGridY() == e.getGridY()) {
                gameOver = true;
            }
        }

        for (Enemy e : enemies) {
            e.updateCooldown();
            if (e.canShoot()) {
                double angle = Math.atan2(player.getGridY() - e.getGridY(), player.getGridX() - e.getGridX());
                int speed = 8;
                int dx = (int) (Math.cos(angle) * speed);
                int dy = (int) (Math.sin(angle) * speed);

                int startX = e.getGridX() * TILE_SIZE + TILE_SIZE / 2;
                int startY = e.getGridY() * TILE_SIZE + TILE_SIZE / 2;
                projectiles.add(new Projectile(startX, startY, dx, dy, false));
            }
        }

        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.update();

            int gridX = p.getX() / TILE_SIZE;
            int gridY = p.getY() / TILE_SIZE;
            if (gridX >= 0 && gridX < currentRoom.getWidth() && gridY >= 0 && gridY < currentRoom.getHeight()) {
                if (currentRoom.getMapLayout()[gridY][gridX] == Room.TILE_WALL) {
                    it.remove();
                    continue;
                }
            } else {
                it.remove();
                continue;
            }

            if (p.isPlayerOwned()) {
                Iterator<Enemy> eIt = enemies.iterator();
                while (eIt.hasNext()) {
                    Enemy e = eIt.next();
                    if (Math.abs(p.getX() - (e.getGridX() * TILE_SIZE + 20)) < 20 &&
                            Math.abs(p.getY() - (e.getGridY() * TILE_SIZE + 20)) < 20) {
                        eIt.remove();
                        it.remove();
                        break;
                    }
                }
            } else {
                if (Math.abs(p.getX() - (player.getGridX() * TILE_SIZE + 20)) < 20 &&
                        Math.abs(p.getY() - (player.getGridY() * TILE_SIZE + 20)) < 20) {
                    gameOver = true;
                }
            }
        }

        if (gameOver) {
            finishGameIfNeeded(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int[][] layout = currentRoom.getMapLayout();
        for (int y = 0; y < currentRoom.getHeight(); y++) {
            for (int x = 0; x < currentRoom.getWidth(); x++) {
                drawTile(g, x, y, layout[y][x]);
            }
        }

        for (Enemy e : enemies) {
            e.draw(g, TILE_SIZE);
        }

        for (Projectile p : projectiles) {
            p.draw(g);
        }

        if (!gameOver) {
            g.setColor(Color.GREEN);
            g.fillRect(player.getGridX() * TILE_SIZE, player.getGridY() * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        int segundos = framesLeft / 30;
        g.drawString("Tempo: " + segundos + "s | Chaves: " + player.getKeyCount() + "/" + Player.FRAGMENTS_NEEDED, 10, 20);
        if (player.canShoot()) {
            g.drawString("Munição: " + player.getAmmo() + " (Espaço)", 350, 20);
        }

        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("GAME OVER", 180, 200);
        } else if (gameWon) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("VOCÊ ZEROU O JOGO!", 120, 200);
        }
    }

    private void saveWinningScoreIfNeeded() {
        if (scoreSaved) {
            return;
        }

        long elapsed = Math.max(0L, clock.getAsLong() - runStartMillis);
        leaderboardRepository.saveScore(currentPlayerName, elapsed);
        loadLeaderboard();
        scoreSaved = true;
    }

    private void finishGameIfNeeded(boolean won) {
        if (postGameHandled) {
            return;
        }

        postGameHandled = true;
        long elapsed = Math.max(0L, clock.getAsLong() - runStartMillis);
        if (won) {
            saveWinningScoreIfNeeded();
            elapsed = Math.max(0L, clock.getAsLong() - runStartMillis);
        }

        if (gameLoop != null) {
            gameLoop.stop();
        }

        PostGameDialog.PostGameAction action = PostGameDialog.show(this, won, leaderboard, elapsed);
        if (action == PostGameDialog.PostGameAction.PLAY_AGAIN) {
            Main.startGame();
        } else if (action == PostGameDialog.PostGameAction.RETURN_TO_MENU) {
            Main.showMainMenu();
        }

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    private void loadLeaderboard() {
        leaderboard = leaderboardRepository.getTopScores(5);
    }

    private String normalizePlayerName(String playerName) {
        if (playerName == null) {
            return "Player";
        }

        String trimmed = playerName.trim();
        return trimmed.isEmpty() ? "Player" : trimmed;
    }

    private void drawTile(Graphics g, int x, int y, int type) {
        switch (type) {
            case Room.TILE_WALL:
                g.setColor(Color.DARK_GRAY);
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                break;
            case Room.TILE_KEY:
                g.setColor(Color.YELLOW);
                g.fillOval(x * TILE_SIZE + 10, y * TILE_SIZE + 10, TILE_SIZE - 20, TILE_SIZE - 20);
                break;
            case Room.TILE_EXIT_LOCKED:
                g.setColor(Color.RED);
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                break;
            case Room.TILE_EXIT_OPEN:
                g.setColor(Color.CYAN);
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                break;
        }
    }

    private class GameInputAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (gameWon || gameOver) return;
            int key = e.getKeyCode();

            if (key == KeyEvent.VK_SPACE && player.canShoot() && player.hasAmmo()) {
                player.decreaseAmmo();

                int px = player.getGridX() * TILE_SIZE + TILE_SIZE / 2;
                int py = player.getGridY() * TILE_SIZE + TILE_SIZE / 2;
                int velX = player.getLastDirX() * 12;
                int velY = player.getLastDirY() * 12;
                projectiles.add(new Projectile(px, py, velX, velY, true));
                return;
            }

            int nextX = player.getGridX();
            int nextY = player.getGridY();
            if (key == KeyEvent.VK_UP) nextY--;
            if (key == KeyEvent.VK_DOWN) nextY++;
            if (key == KeyEvent.VK_LEFT) nextX--;
            if (key == KeyEvent.VK_RIGHT) nextX++;

            int[][] layout = currentRoom.getMapLayout();
            if (layout[nextY][nextX] == Room.TILE_WALL || layout[nextY][nextX] == Room.TILE_EXIT_LOCKED) return;

            if (key == KeyEvent.VK_UP) player.moveUp();
            if (key == KeyEvent.VK_DOWN) player.moveDown();
            if (key == KeyEvent.VK_LEFT) player.moveLeft();
            if (key == KeyEvent.VK_RIGHT) player.moveRight();

            checkItemsAndPortals();
        }

        private void checkItemsAndPortals() {
            if (currentRoom.takeItemAt(player.getGridX(), player.getGridY()) == Room.TILE_KEY) {
                player.collectKey();
                if (player.hasAllKeys()) currentRoom.openExit(14, 5);
            }

            if (currentRoom.getMapLayout()[player.getGridY()][player.getGridX()] == Room.TILE_EXIT_OPEN) {
                if (currentLevel == 1) {
                    loadLevel2();
                } else {
                    gameWon = true;
                    finishGameIfNeeded(true);
                }
            }
        }
    }
}