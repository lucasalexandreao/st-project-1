package st.project;

import st.project.model.game.Projectile; // Import atualizado
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

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

    // PBT (Teste Baseado em Propriedades) - NOVO!
    @Test
    void propertyBased_ProjectileMovesExactlyAccordingToVelocity() {
        Random rand = new Random();

        // Vamos gerar 1000 tiros com posições e velocidades completamente aleatórias
        for(int i = 0; i < 1000; i++) {
            // PRÉ-CONDIÇÕES (Geradas aleatoriamente)
            int startX = rand.nextInt(2000) - 1000; // Valores entre -1000 e +1000
            int startY = rand.nextInt(2000) - 1000;
            int dx = rand.nextInt(100) - 50;
            int dy = rand.nextInt(100) - 50;

            Projectile p = new Projectile(startX, startY, dx, dy, true);

            // AÇÃO
            p.update();

            // PÓS-CONDIÇÃO (A Propriedade/Lei da Física)
            // Não importa os números loucos que geramos, a posição final DEVE OBRIGATORIAMENTE ser: Posição Atual + Velocidade
            assertEquals(startX + dx, p.getX(), "O X deve sempre respeitar a lei do movimento uniforme");
            assertEquals(startY + dy, p.getY(), "O Y deve sempre respeitar a lei do movimento uniforme");
        }
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