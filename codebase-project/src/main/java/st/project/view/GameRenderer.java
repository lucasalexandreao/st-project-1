package st.project.view;

import st.project.model.GameConfig;
import st.project.model.game.*;
import java.awt.*;
import java.util.List;

public class GameRenderer {
    private final int TILE_SIZE = GameConfig.TILE_SIZE;

    public void render(Graphics g, GameState state, Player player, Room room,
                       List<Enemy> enemies, List<Projectile> projectiles,
                       int framesLeft, boolean gameWon, boolean gameOver) {
        drawTiles(g, room);
        drawEnemies(g, enemies);
        drawProjectiles(g, projectiles);
        drawPlayer(g, player, gameOver);
        drawHUD(g, player, framesLeft);
        drawGameStatus(g, gameWon, gameOver);
    }

    private void drawTiles(Graphics g, Room room) {
        int[][] layout = room.getMapLayout();
        for (int y = 0; y < room.getHeight(); y++) {
            for (int x = 0; x < room.getWidth(); x++) {
                drawTile(g, x, y, layout[y][x]);
            }
        }
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

    private void drawEnemies(Graphics g, List<Enemy> enemies) {
        for (Enemy e : enemies) {
            e.draw(g, TILE_SIZE);
        }
    }

    private void drawProjectiles(Graphics g, List<Projectile> projectiles) {
        for (Projectile p : projectiles) {
            p.draw(g);
        }
    }

    private void drawPlayer(Graphics g, Player player, boolean gameOver) {
        if (!gameOver) {
            g.setColor(Color.GREEN);
            g.fillRect(player.getGridX() * TILE_SIZE, player.getGridY() * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
    }

    private void drawHUD(Graphics g, Player player, int framesLeft) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        int segundos = framesLeft / GameConfig.FPS;
        g.drawString("Tempo: " + segundos + "s | Chaves: " + player.getKeyCount() + "/" + GameConfig.FRAGMENTS_NEEDED, 10, 20);
        if (player.canShoot()) {
            g.drawString("Munição: " + player.getAmmo() + " (Espaço)", 350, 20);
        }
    }

    private void drawGameStatus(Graphics g, boolean gameWon, boolean gameOver) {
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
}
