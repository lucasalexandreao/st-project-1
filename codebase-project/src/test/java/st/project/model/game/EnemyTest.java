package st.project.model.game;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.Graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

public class EnemyTest {

    // DOMÍNIO
    @Test
    void shouldInitializeAtCorrectPosition() {
        Enemy enemy = new Enemy(5, 8);
        assertEquals(5, enemy.getGridX());
        assertEquals(8, enemy.getGridY());
    }

    // PBT (Teste Baseado em Propriedades) (DOMÍNIO)
    @Property
    void propertyBased_CooldownCyclesCorrectlyOverLongPeriods(
            @ForAll @IntRange(min = 0, max = 20000) int simulatedFrames
    ) {
        Enemy enemy = new Enemy(0, 0);

        // PRÉ-CONDIÇÃO
        assertFalse(enemy.canShoot(), "O inimigo não deve poder atirar no frame 0");

        int totalShotsFired = 0;

        // AÇÃO
        for(int i = 1; i <= simulatedFrames; i++) {
            enemy.updateCooldown();
            if (enemy.canShoot()) {
                totalShotsFired++;
                // PÓS-CONDIÇÃO IMEDIATA: Uma vez que ele atira, é proibido atirar no próximo acesso (cooldown resetou)
                assertFalse(enemy.canShoot(), "O cooldown não resetou imediatamente após o tiro!");
            }
        }

        // PÓS-CONDIÇÃO ESTATÍSTICA (A Propriedade)
        // Independentemente de quantos frames rodarem, a proporção matemática deve ser inviolável
        assertEquals(simulatedFrames / 60, totalShotsFired, "O ciclo de tiro dessincronizou do tempo (60 frames)!");
    }

    // ESTRUTURAL
    @Test
    void shouldDrawOrangeTriangle() {
        Graphics mockGraphics = mock(Graphics.class);
        Enemy enemy = new Enemy(2, 2);
        int tileSize = 40;

        enemy.draw(mockGraphics, tileSize);

        verify(mockGraphics).setColor(Color.ORANGE);
        verify(mockGraphics).fillPolygon(any(int[].class), any(int[].class), eq(3));
    }

    // FRONTEIRA E ESTRUTURAL
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