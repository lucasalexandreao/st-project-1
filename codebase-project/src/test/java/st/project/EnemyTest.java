package st.project;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.Graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class EnemyTest {

    @Test
    void shouldInitializeAtCorrectPosition() {
        Enemy enemy = new Enemy(5, 8);

        assertEquals(5, enemy.getGridX());
        assertEquals(8, enemy.getGridY());
    }

    @Test
    void shouldWaitCooldownToShoot() {
        Enemy enemy = new Enemy(1, 1);

        assertFalse(enemy.canShoot());

        for (int i = 0; i < 60; i++) {
            enemy.updateCooldown();
        }

        assertTrue(enemy.canShoot());

        assertFalse(enemy.canShoot());
    }

    @Test
    void shouldDrawOrangeTriangle() {
        // Ferramenta de desenho usando Mockito
        Graphics mockGraphics = mock(Graphics.class);
        Enemy enemy = new Enemy(2, 2);
        int tileSize = 40;

        enemy.draw(mockGraphics, tileSize);

        verify(mockGraphics).setColor(Color.ORANGE);

        verify(mockGraphics).fillPolygon(any(int[].class), any(int[].class), eq(3));
    }

    @Test
    void shouldNotDecreaseCooldownBelowZero() {
        Enemy enemy = new Enemy(1, 1);

        for (int i = 0; i < 60; i++) {
            enemy.updateCooldown();
        }

        enemy.updateCooldown();

        assertTrue(enemy.canShoot());
    }
}