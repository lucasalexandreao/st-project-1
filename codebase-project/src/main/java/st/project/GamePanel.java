package st.project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GamePanel extends JPanel {
    private final int TILE_SIZE = 40;

    private Room currentRoom;
    private Player player;
    private int currentLevel = 1;

    // Status do jogo
    private boolean gameWon = false;
    private boolean gameOver = false;
    private int framesLeft; // Tempo limite (30 FPS * 30 seg = 900 frames)

    // Entidades
    private List<Enemy> enemies;
    private List<Projectile> projectiles;

    private Timer gameLoop;

    public GamePanel() {
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();

        loadLevel1();

        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setBackground(Color.BLACK);
        this.addKeyListener(new GameInputAdapter());

        gameLoop = new Timer(1000/30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGameLogic();
                repaint();
            }
        });
        gameLoop.start();
    }

    private void loadLevel1() {
        currentLevel = 1;
        framesLeft = 30 * 30; // 30 Segundos
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
        currentRoom = new Room("Fase 1", layout);
        player = new Player(1, 1);
        enemies.clear();
        projectiles.clear();
    }

    private void loadLevel2() {
        currentLevel = 2;
        framesLeft = 30 * 30; // Reseta os 30 Segundos
        player.unlockShooting(); // DESBLOQUEIA O TIRO!
        player.resetKeys();
        player.setPosition(1, 1);

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
        currentRoom = new Room("Fase 2", layout);

        enemies.clear();
        projectiles.clear();
        enemies.add(new Enemy(7, 3)); // Adiciona monstro 1
        enemies.add(new Enemy(7, 7)); // Adiciona monstro 2
    }

    private void updateGameLogic() {
        if (gameWon || gameOver) return;

        // Limite de Tempo
        framesLeft--;
        if (framesLeft <= 0) {
            gameOver = true;
        }

        for (Enemy e : enemies) {
            if (player.getGridX() == e.getGridX() && player.getGridY() == e.getGridY()) {
                gameOver = true; // O jogador morre se as posições forem iguais
            }
        }

        // Atualiza Inimigos
        for (Enemy e : enemies) {
            e.updateCooldown();
            if (e.canShoot()) {
                // Calcula direção até o jogador
                double angle = Math.atan2(player.getGridY() - e.getGridY(), player.getGridX() - e.getGridX());
                int speed = 8;
                int dx = (int)(Math.cos(angle) * speed);
                int dy = (int)(Math.sin(angle) * speed);

                int startX = e.getGridX() * TILE_SIZE + TILE_SIZE / 2;
                int startY = e.getGridY() * TILE_SIZE + TILE_SIZE / 2;
                projectiles.add(new Projectile(startX, startY, dx, dy, false));
            }
        }

        // Atualiza Projetéis e Colisões
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.update();

            // Verifica se bateu na parede
            int gridX = p.getX() / TILE_SIZE;
            int gridY = p.getY() / TILE_SIZE;
            if (gridX >= 0 && gridX < currentRoom.getWidth() && gridY >= 0 && gridY < currentRoom.getHeight()) {
                if (currentRoom.getMapLayout()[gridY][gridX] == Room.TILE_WALL) {
                    it.remove();
                    continue;
                }
            } else {
                it.remove(); // Saiu do mapa
                continue;
            }

            // Colisões de Tiros
            if (p.isPlayerOwned()) {
                // Tiro do jogador acerta inimigo
                Iterator<Enemy> eIt = enemies.iterator();
                while (eIt.hasNext()) {
                    Enemy e = eIt.next();
                    // Checagem simples de caixa (Bounding box)
                    if (Math.abs(p.getX() - (e.getGridX() * TILE_SIZE + 20)) < 20 &&
                            Math.abs(p.getY() - (e.getGridY() * TILE_SIZE + 20)) < 20) {
                        eIt.remove(); // Inimigo morre
                        it.remove();  // Tiro some
                        break;
                    }
                }
            } else {
                // Tiro do inimigo acerta jogador
                if (Math.abs(p.getX() - (player.getGridX() * TILE_SIZE + 20)) < 20 &&
                        Math.abs(p.getY() - (player.getGridY() * TILE_SIZE + 20)) < 20) {
                    gameOver = true; // Jogador morre
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Desenha Mapa
        int[][] layout = currentRoom.getMapLayout();
        for (int y = 0; y < currentRoom.getHeight(); y++) {
            for (int x = 0; x < currentRoom.getWidth(); x++) {
                drawTile(g, x, y, layout[y][x]);
            }
        }

        // Desenha Inimigos
        for (Enemy e : enemies) {
            e.draw(g, TILE_SIZE);
        }

        // Desenha Tiros
        for (Projectile p : projectiles) {
            p.draw(g);
        }

        // Desenha Personagem (Se não estiver morto)
        if (!gameOver) {
            g.setColor(Color.GREEN);
            g.fillRect(player.getGridX() * TILE_SIZE, player.getGridY() * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        // UI (Tempo e Chaves)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        int segundos = framesLeft / 30;
        g.drawString("Tempo: " + segundos + "s | Chaves: " + player.getKeyCount() + "/" + Player.FRAGMENTS_NEEDED, 10, 20);
        if (player.canShoot()) {
            g.drawString("Munição: " + player.getAmmo() + " (Espaço)", 350, 20);
        }

        // Telas de Fim
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

            // Atirar (Espaço)
            if (key == KeyEvent.VK_SPACE && player.canShoot() && player.hasAmmo()) {
                player.decreaseAmmo(); // Gasta 1 tiro da mochila

                int px = player.getGridX() * TILE_SIZE + TILE_SIZE / 2;
                int py = player.getGridY() * TILE_SIZE + TILE_SIZE / 2;
                int velX = player.getLastDirX() * 12; // Velocidade do tiro do player
                int velY = player.getLastDirY() * 12;
                projectiles.add(new Projectile(px, py, velX, velY, true));
                return;
            }

            int nextX = player.getGridX();
            int nextY = player.getGridY();
            if (key == KeyEvent.VK_UP)    nextY--;
            if (key == KeyEvent.VK_DOWN)  nextY++;
            if (key == KeyEvent.VK_LEFT)  nextX--;
            if (key == KeyEvent.VK_RIGHT) nextX++;

            int[][] layout = currentRoom.getMapLayout();
            if (layout[nextY][nextX] == Room.TILE_WALL || layout[nextY][nextX] == Room.TILE_EXIT_LOCKED) return;

            if (key == KeyEvent.VK_UP)    player.moveUp();
            if (key == KeyEvent.VK_DOWN)  player.moveDown();
            if (key == KeyEvent.VK_LEFT)  player.moveLeft();
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
                }
            }
        }
    }}