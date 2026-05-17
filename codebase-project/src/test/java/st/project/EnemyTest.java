package st.project;

import st.project.model.game.Enemy; // Import atualizado
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.Graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class EnemyTest {

    // DOMÍNIO
    @Test
    void shouldInitializeAtCorrectPosition() {
        Enemy enemy = new Enemy(5, 8);
        assertEquals(5, enemy.getGridX());
        assertEquals(8, enemy.getGridY());
    }

    // PBT (Teste Baseado em Propriedades) - NOVO!
    @Test
    void propertyBased_CooldownCyclesCorrectlyOverLongPeriods() {
        Enemy enemy = new Enemy(0, 0);

        // PRÉ-CONDIÇÃO
        assertFalse(enemy.canShoot());

        int totalShotsFired = 0;
        int simulatedFrames = 10000; // Simulando mais de 5 minutos de jogo contínuo

        // AÇÃO
        for(int i = 1; i <= simulatedFrames; i++) {
            enemy.updateCooldown();
            if (enemy.canShoot()) {
                totalShotsFired++;
                // PÓS-CONDIÇÃO IMEDIATA: Uma vez que ele atira, é proibido atirar no próximo milissegundo (cooldown resetou)
                assertFalse(enemy.canShoot());
            }
        }

        // PÓS-CONDIÇÃO ESTATÍSTICA (A Propriedade)
        // Se atiramos a cada 60 frames exatos, em 10.000 frames devemos ter atirado exatamente 166 vezes (10000 / 60)
        assertEquals(simulatedFrames / 60, totalShotsFired, "O ciclo de tiro não pode dessincronizar com o tempo");
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