package st.project.model.game;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class ProjectileTest {

    // DOMÍNIO
    @Test
    void shouldIdentifyPlayerProjectile() {
        Projectile playerProjectile = new Projectile(0, 0, 0, 0, true);
        Projectile enemyProjectile = new Projectile(0, 0, 0, 0, false);

        assertTrue(playerProjectile.isPlayerOwned());
        assertFalse(enemyProjectile.isPlayerOwned());
    }

    // PBT (Teste Baseado em Propriedade) (DOMÍNIO)
    @Property
    void propertyBased_ProjectileMovesExactlyAccordingToVelocity(
            @ForAll @IntRange(min = -1000, max = 1000) int startX,
            @ForAll @IntRange(min = -1000, max = 1000) int startY,
            @ForAll @IntRange(min = -50, max = 50) int dx,
            @ForAll @IntRange(min = -50, max = 50) int dy
    ) {

        // PRÉ-CONDIÇÕES (Geradas automaticamente pelo framework)
        Projectile p = new Projectile(startX, startY, dx, dy, true);

        // AÇÃO
        p.update();

        // PÓS-CONDIÇÃO (A Propriedade)
        // A posição final deve ser: Posição Atual + Velocidade
        assertEquals(startX + dx, p.getX(), "O X deve sempre respeitar a lei do movimento uniforme");
        assertEquals(startY + dy, p.getY(), "O Y deve sempre respeitar a lei do movimento uniforme");
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