package st.project;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import org.mockito.ArgumentCaptor;

class EnemyTest {

    @Test
    void shouldStoreGridPositionFromConstructor() {
        Enemy enemy = new Enemy(3, 4);

        assertEquals(3, enemy.getGridX());
        assertEquals(4, enemy.getGridY());
    }

    @Test
    void shouldNotShootBeforeCooldownReachesZero() {
        Enemy enemy = new Enemy(1, 1);

        assertFalse(enemy.canShoot());
    }

    @Test
    void shouldShootWhenCooldownReachesZeroAndResetCooldown() {
        Enemy enemy = new Enemy(1, 1);

        for (int i = 0; i < 60; i++) {
            enemy.updateCooldown();
        }

        assertTrue(enemy.canShoot());
        assertFalse(enemy.canShoot());
    }

    @Test
    void shouldNotDecrementCooldownBelowZero() {
        Enemy enemy = new Enemy(1, 1);

        for (int i = 0; i < 60; i++) {
            enemy.updateCooldown();
        }

        enemy.updateCooldown();

        assertTrue(enemy.canShoot());
    }

    @Test
    void shouldDrawOrangeTriangleAtExpectedCoordinates() {
        Graphics graphics = mock(Graphics.class);
        Enemy enemy = new Enemy(2, 3);
        int tileSize = 20;

        enemy.draw(graphics, tileSize);

        verify(graphics, times(1)).setColor(Color.ORANGE);

        ArgumentCaptor<int[]> xPointsCaptor = ArgumentCaptor.forClass(int[].class);
        ArgumentCaptor<int[]> yPointsCaptor = ArgumentCaptor.forClass(int[].class);

        verify(graphics).fillPolygon(xPointsCaptor.capture(), yPointsCaptor.capture(), eq(3));
        assertArrayEquals(new int[]{50, 40, 60}, xPointsCaptor.getValue());
        assertArrayEquals(new int[]{60, 80, 80}, yPointsCaptor.getValue());
    }
}