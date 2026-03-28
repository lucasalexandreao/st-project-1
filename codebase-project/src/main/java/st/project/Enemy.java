package st.project;

import java.awt.Color;
import java.awt.Graphics;

public class Enemy {
    private int gridX;
    private int gridY;
    private int shootCooldown;

    public Enemy(int gridX, int gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.shootCooldown = 60; // 60 frames (2 segundos a 30 FPS)
    }

    public void updateCooldown() {
        if (shootCooldown > 0) shootCooldown--;
    }

    public boolean canShoot() {
        if (shootCooldown <= 0) {
            shootCooldown = 60; // Reseta para mais 2 segundos
            return true;
        }
        return false;
    }

    public void draw(Graphics g, int tileSize) {
        g.setColor(Color.ORANGE);
        int px = gridX * tileSize;
        int py = gridY * tileSize;
        // Desenha um triângulo
        int[] xPoints = {px + tileSize / 2, px, px + tileSize};
        int[] yPoints = {py, py + tileSize, py + tileSize};
        g.fillPolygon(xPoints, yPoints, 3);
    }

    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }
}