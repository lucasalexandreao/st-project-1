package st.project.model.game;

import java.awt.Color;
import java.awt.Graphics;

public class Projectile {
    private int x, y; // Posição em pixels na tela
    private int dx, dy; // Velocidade/Direção
    private boolean isPlayerOwned; // Verdadeiro se for tiro do jogador

    public Projectile(int x, int y, int dx, int dy, boolean isPlayerOwned) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.isPlayerOwned = isPlayerOwned;
    }

    public void update() {
        x += dx;
        y += dy;
    }

    public void draw(Graphics g) {
        g.setColor(isPlayerOwned ? Color.CYAN : Color.MAGENTA);
        g.fillOval(x - 5, y - 5, 10, 10); // Bolinha de tamanho 10
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isPlayerOwned() { return isPlayerOwned; }
}