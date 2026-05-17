package st.project.view;

import st.project.controller.GameController;
import st.project.model.GameConfig;
import st.project.model.game.GameState;
import st.project.repository.LeaderboardRepository;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class GamePanelNew extends JPanel {
    private final GameController gameController;
    private final GameState gameState;
    private final GameRenderer renderer;

    private Timer gameLoop;
    private Runnable onGameFinished;

    public GamePanelNew(GameController gameController, LeaderboardRepository repository, String playerName) {
        this.gameController = gameController;
        this.gameState = gameController.getGameState();
        this.renderer = new GameRenderer();


        initializePanel();
        startGameLoop();
    }

    public void setOnGameFinished(Runnable callback) {
        this.onGameFinished = callback;
    }

    private void initializePanel() {
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setBackground(Color.BLACK);
        this.setPreferredSize(new Dimension(620, 480));
        this.addKeyListener(new GameInputAdapter());
    }

    private void startGameLoop() {
        gameLoop = new Timer(1000 / GameConfig.FPS, e -> {
            gameController.updateGameLogic();

            if (gameState.isGameFinished()) {
                stopGameLoop();
                if (onGameFinished != null) {
                    onGameFinished.run();
                }
            }

            repaint();
        });
        gameLoop.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render(g, gameState, gameState.getPlayer(), gameState.getCurrentRoom(),
                       gameState.getEnemies(), gameState.getProjectiles(),
                       gameState.getFramesLeft(), gameState.isGameWon(), gameState.isGameOver());
    }

    private class GameInputAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (gameState.isGameFinished()) return;

            int key = e.getKeyCode();

            if (key == KeyEvent.VK_SPACE && gameState.getPlayer().canShoot() && gameState.getPlayer().hasAmmo()) {
                int px = gameState.getPlayer().getGridX() * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
                int py = gameState.getPlayer().getGridY() * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
                int velX = gameState.getPlayer().getLastDirX() * 12;
                int velY = gameState.getPlayer().getLastDirY() * 12;
                gameController.addPlayerProjectile(px, py, velX, velY);
                return;
            }

            int nextX = gameState.getPlayer().getGridX();
            int nextY = gameState.getPlayer().getGridY();

            if (key == KeyEvent.VK_UP) nextY--;
            if (key == KeyEvent.VK_DOWN) nextY++;
            if (key == KeyEvent.VK_LEFT) nextX--;
            if (key == KeyEvent.VK_RIGHT) nextX++;

            gameController.handlePlayerMovement(nextX, nextY);
        }
    }

    public void stopGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }
}
