package st.project.view;

import st.project.model.GameConfig;
import st.project.model.game.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

public class GameRenderer {
    private final int TILE_SIZE = GameConfig.TILE_SIZE;

    private final SpriteSheet playerSheet;
    private final SpriteSheet enemySheet;
    private final BufferedImage balaImage;
    private final BufferedImage madeiraImage;
    private static final int ENEMY_ROW   = 2;
    private static final int ENEMY_FRAME = 0;
    private static final int BALA_SIZE   = 24;
    private static final int MADEIRA_H   = 14;  // altura; largura calculada pela proporção (243:81 ≈ 3:1)
    private int animFrame = 1;
    private int animTick = 0;
    private int lastPlayerX = -1;
    private int lastPlayerY = -1;
    private int idleTicks = 0;
    private static final int ANIM_SPEED =2 ; 
    private static final int IDLE_DELAY = 15;  
    private static final int WALK_ROW_PHASE1 = 0;
    private static final int WALK_FRAMES_PHASE1 = 5;
    private static final int IDLE_ROW = 0;
    private static final int IDLE_FRAME = 6;

    public GameRenderer() {
        playerSheet = new SpriteSheet("/sprites/picapau.png", 8, 6);
        enemySheet   = new SpriteSheet("/sprites/canhao.png", 12, 5);
        balaImage    = loadImage("/sprites/bala.png");
        madeiraImage = loadImage("/sprites/madeira.png");
    }

    private BufferedImage loadImage(String path) {
        try {
            InputStream is = GameRenderer.class.getResourceAsStream(path);
            return is != null ? ImageIO.read(is) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public void render(Graphics g, GameState state, Player player, Room room,
                       List<Enemy> enemies, List<Projectile> projectiles,
                       int framesLeft, boolean gameWon, boolean gameOver) {
        drawTiles(g, room);
        drawEnemies(g, enemies, player);
        drawProjectiles(g, projectiles, state.getCurrentLevel());
        drawPlayer(g, player, gameOver, state.getCurrentLevel());
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
            case Room.TILE_FLOOR:
                g.setColor(new Color(139, 90, 43));
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                break;
            case Room.TILE_WALL:
                drawWallTile(g, x, y);
                break;
            case Room.TILE_KEY:
                drawKeyTile(g, x, y);
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

    private void drawKeyTile(Graphics g, int tx, int ty) {
        int px = tx * TILE_SIZE;
        int py = ty * TILE_SIZE;

        // fundo terra (igual ao floor)
        g.setColor(new Color(139, 90, 43));
        g.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        // sombra da chave
        g.setColor(new Color(160, 115, 0));
        g.fillOval(px + 4,  py + 9,  20, 20);   // cabeça
        g.fillRect(px + 22, py + 17, 15,  7);   // haste
        g.fillRect(px + 27, py + 24,  4,  6);   // dente 1
        g.fillRect(px + 32, py + 24,  4,  5);   // dente 2

        // chave principal (ouro)
        g.setColor(new Color(255, 200, 50));
        g.fillOval(px + 4,  py + 8,  20, 20);
        g.fillRect(px + 22, py + 16, 15,  7);
        g.fillRect(px + 27, py + 23,  4,  6);
        g.fillRect(px + 32, py + 23,  4,  5);

        // buraco da cabeça
        g.setColor(new Color(110, 65, 30));
        g.fillOval(px + 9, py + 13, 10, 10);
    }

    private void drawWallTile(Graphics g, int tx, int ty) {
        int variant = Math.abs(tx * 73 + ty * 37) % 3;
        int px = tx * TILE_SIZE;
        int py = ty * TILE_SIZE;
        switch (variant) {
            case 0: drawTree(g, px, py);  break;
            case 1: drawBush(g, px, py);  break;
            default: drawGrass(g, px, py); break;
        }
    }

    private void drawTree(Graphics g, int px, int py) {
        g.setColor(new Color(55, 120, 35));
        g.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        int trunkW = TILE_SIZE / 4;
        int trunkH = TILE_SIZE / 3;
        int trunkX = px + (TILE_SIZE - trunkW) / 2;
        int trunkY = py + TILE_SIZE - trunkH;

        g.setColor(new Color(101, 67, 33));
        g.fillRect(trunkX, trunkY, trunkW, trunkH);

        g.setColor(new Color(20, 90, 20));
        g.fillOval(px + 3, py + 5, TILE_SIZE - 6, TILE_SIZE - trunkH - 2);

        g.setColor(new Color(50, 160, 50));
        g.fillOval(px + 5, py + 2, TILE_SIZE - 10, TILE_SIZE - trunkH - 4);

        g.setColor(new Color(100, 200, 80));
        g.fillOval(px + 10, py + 4, TILE_SIZE / 3, TILE_SIZE / 5);
    }

    private void drawBush(Graphics g, int px, int py) {
        // fundo grama
        g.setColor(new Color(55, 120, 35));
        g.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        // matinhos — três tufos
        g.setColor(new Color(70, 150, 45));
        g.fillOval(px + 2,  py + 18, 10, 18);
        g.fillOval(px + 14, py + 15, 12, 20);
        g.fillOval(px + 27, py + 18, 10, 18);

        // pontas mais claras
        g.setColor(new Color(110, 195, 65));
        g.fillOval(px + 4,  py + 16, 6, 10);
        g.fillOval(px + 16, py + 13, 7, 10);
        g.fillOval(px + 29, py + 17, 5,  9);
    }

    private void drawGrass(Graphics g, int px, int py) {
        // grama lisa
        g.setColor(new Color(55, 120, 35));
        g.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        // manchas sutis para textura
        g.setColor(new Color(50, 110, 35));
        g.fillOval(px + 5,  py + 8,  14, 8);
        g.fillOval(px + 22, py + 20, 12, 7);
    }


    private void drawEnemies(Graphics g, List<Enemy> enemies, Player player) {
        for (Enemy e : enemies) {
            if (enemySheet.isLoaded()) {
                boolean faceLeft = player.getGridX() > e.getGridX();
                BufferedImage frame = faceLeft
                    ? enemySheet.getFrameFlipped(ENEMY_ROW, ENEMY_FRAME)
                    : enemySheet.getFrame(ENEMY_ROW, ENEMY_FRAME);
                int drawH = TILE_SIZE;
                int drawW = drawH * frame.getWidth() / frame.getHeight();
                int drawX = e.getGridX() * TILE_SIZE - (drawW - TILE_SIZE) / 2;
                int drawY = e.getGridY() * TILE_SIZE;
                g.drawImage(frame, drawX, drawY, drawW, drawH, null);
            } else {
                e.draw(g, TILE_SIZE);
            }
        }
    }

    private void drawProjectiles(Graphics g, List<Projectile> projectiles, int currentLevel) {
        for (Projectile p : projectiles) {
            if (!p.isPlayerOwned() && balaImage != null) {
                g.drawImage(balaImage, p.getX() - BALA_SIZE / 2, p.getY() - BALA_SIZE / 2, BALA_SIZE, BALA_SIZE, null);
            } else if (p.isPlayerOwned() && currentLevel >= 2 && madeiraImage != null) {
                int drawW = MADEIRA_H * madeiraImage.getWidth() / madeiraImage.getHeight();
                g.drawImage(madeiraImage, p.getX() - drawW / 2, p.getY() - MADEIRA_H / 2, drawW, MADEIRA_H, null);
            } else {
                p.draw(g);
            }
        }
    }

    private void drawPlayer(Graphics g, Player player, boolean gameOver, int currentLevel) {
        if (gameOver) return;
        int px = player.getGridX() * TILE_SIZE;
        int py = player.getGridY() * TILE_SIZE;

        boolean isMoving = (player.getGridX() != lastPlayerX || player.getGridY() != lastPlayerY);
        lastPlayerX = player.getGridX();
        lastPlayerY = player.getGridY();

        if (isMoving) idleTicks = 0;
        else if (idleTicks < IDLE_DELAY) idleTicks++;
        boolean isIdle = idleTicks >= IDLE_DELAY;

        int walkRow    = WALK_ROW_PHASE1;
        int frameCount =WALK_FRAMES_PHASE1;

        if (!isIdle) {
            animTick++;
            if (animTick >= ANIM_SPEED) {
                animTick = 0;
                animFrame = (animFrame % (frameCount - 1)) + 1;
            }
        }

        if (playerSheet.isLoaded()) {
            boolean facingLeft = player.getLastDirX() < 0;
            int drawRow   = isIdle ? IDLE_ROW   : walkRow;
            int drawFrame = isIdle ? IDLE_FRAME : animFrame;
            BufferedImage frame = facingLeft
                ? playerSheet.getFrameFlipped(drawRow, drawFrame)
                : playerSheet.getFrame(drawRow, drawFrame);
            g.drawImage(frame, px, py, TILE_SIZE, TILE_SIZE, null);
        } else {
            g.setColor(Color.GREEN);
            g.fillRect(px, py, TILE_SIZE, TILE_SIZE);
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
