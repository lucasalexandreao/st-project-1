package st.project;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.Graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class ProjectileTest {


    // DOMÍNIO
    @Test
    void shouldIdentifyPlayerProjectile() {
        Projectile playerProjectile = new Projectile(0, 0, 0, 0, true);
        Projectile enemyProjectile = new Projectile(0, 0, 0, 0, false);

        assertTrue(playerProjectile.isPlayerOwned());
        assertFalse(enemyProjectile.isPlayerOwned());
    }

    // DOMÍNIO
    @Test
    void shouldHandleNegativeVelocity() {
        Projectile projectile = new Projectile(50, 50, -5, -10, true);

        projectile.update();

        assertEquals(45, projectile.getX());
        assertEquals(40, projectile.getY());
    }

    // ESTRUTURAL
    @Test
    void shouldDrawPlayerProjectileWithCyanColor() {
        Graphics mockGraphics = mock(Graphics.class);
        Projectile playerProjectile = new Projectile(50, 75, 2, 3, true);

        playerProjectile.draw(mockGraphics);

        verify(mockGraphics).setColor(Color.CYAN);
        verify(mockGraphics).fillOval(45, 70, 10, 10);
    }

    // ESTRUTURAL
    @Test
    void shouldDrawEnemyProjectileWithMagentaColor() {
        Graphics mockGraphics = mock(Graphics.class);
        Projectile enemyProjectile = new Projectile(100, 150, -1, -2, false);

        enemyProjectile.draw(mockGraphics);

        verify(mockGraphics).setColor(Color.MAGENTA);
        verify(mockGraphics).fillOval(95, 145, 10, 10);
    }
}
