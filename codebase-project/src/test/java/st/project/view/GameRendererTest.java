package st.project.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st.project.model.game.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GameRendererTest {

    private GameRenderer renderer;
    private Graphics mockGraphics;
    private GameState mockState;
    private Player mockPlayer;
    private Room mockRoom;

    @BeforeEach
    void setUp() {
        renderer = new GameRenderer();
        mockGraphics = mock(Graphics.class);
        mockState = mock(GameState.class);
        mockPlayer = mock(Player.class);
        mockRoom = mock(Room.class);

        // Configuração base do jogador
        when(mockPlayer.getGridX()).thenReturn(2);
        when(mockPlayer.getGridY()).thenReturn(2);
        when(mockPlayer.getKeyCount()).thenReturn(1);
        when(mockPlayer.getAmmo()).thenReturn(5);
    }

    // ---------------------------------------------------------
    // RENDERIZAÇÃO DE CENÁRIOS E MAPA
    // ---------------------------------------------------------

    // [TIPO: LÓGICA E ESTRUTURAL] Valida a renderização do mapa e esgota as branches do switch de Tiles
    @Test
    void shouldRenderAllTileTypesAndSkipUnknowns() {
        int[][] fakeLayout = {
                {Room.TILE_WALL, Room.TILE_KEY, Room.TILE_EXIT_LOCKED},
                {Room.TILE_EXIT_OPEN, 0, 99} // 0 e 99 são "chão livre", não têm desenho específico
        };

        when(mockRoom.getMapLayout()).thenReturn(fakeLayout);
        when(mockRoom.getHeight()).thenReturn(2);
        when(mockRoom.getWidth()).thenReturn(3);

        // Simulamos o estado normal de jogo
        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                Collections.emptyList(), Collections.emptyList(),
                60, false, false);

        // Verificações atualizadas para as novas cores RGB do GameRenderer
        verify(mockGraphics, atLeastOnce()).setColor(new Color(55, 120, 35)); // TILE_WALL (Base da grama/árvore/arbusto)
        verify(mockGraphics, atLeastOnce()).setColor(new Color(255, 200, 50)); // TILE_KEY (Chave de Ouro)
        verify(mockGraphics).setColor(Color.RED);       // TILE_EXIT_LOCKED
        verify(mockGraphics).setColor(Color.CYAN);      // TILE_EXIT_OPEN

        // Verifica se o jogador foi desenhado (sprite ou fallback verde)
        boolean drewSprite = !mockingDetails(mockGraphics).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("drawImage")).findAny().isEmpty();
        boolean drewFallback = !mockingDetails(mockGraphics).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("setColor")
                        && Color.GREEN.equals(i.getArguments()[0])).findAny().isEmpty();
        assertTrue(drewSprite || drewFallback, "Player deve ser desenhado (sprite ou fallback verde)");
    }

    // ---------------------------------------------------------
    // RENDERIZAÇÃO DE ENTIDADES (Inimigos e Tiros)
    // ---------------------------------------------------------
// [TIPO: INTEGRAÇÃO E DOMÍNIO] Garante que as listas de entidades repassam o comando de desenho (Delegation)
    @Test
    void shouldRenderEnemiesAndProjectiles() {
        when(mockRoom.getMapLayout()).thenReturn(new int[][]{{0}});
        when(mockRoom.getHeight()).thenReturn(1);
        when(mockRoom.getWidth()).thenReturn(1);
        when(mockState.getCurrentLevel()).thenReturn(1); // Evita problemas na lógica de projéteis

        Enemy mockEnemy = mock(Enemy.class);
        Projectile mockProjectile = mock(Projectile.class);

        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                Arrays.asList(mockEnemy), Arrays.asList(mockProjectile),
                60, false, false);

        // Verifica se o renderer usou sprites (drawImage)
        boolean usedSprites = !mockingDetails(mockGraphics).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("drawImage")).findAny().isEmpty();

        // Verifica se o renderer chamou o fallback do inimigo
        boolean usedEnemyFallback = !mockingDetails(mockEnemy).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("draw")).findAny().isEmpty();

        // Verifica se o renderer chamou o fallback do projétil
        boolean usedProjectileFallback = !mockingDetails(mockProjectile).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("draw")).findAny().isEmpty();

        // O teste passa se renderizou via Sprite OU chamou o método manual da entidade
        assertTrue(usedSprites || usedEnemyFallback, "Inimigo deve ser desenhado (via sprite ou fallback)");
        assertTrue(usedSprites || usedProjectileFallback, "Projétil deve ser desenhado (via sprite ou fallback)");
    }
    // ---------------------------------------------------------
    // HUD E ESTADOS DE FIM DE JOGO (GameOver e GameWon)
    // ---------------------------------------------------------

    // [TIPO: FRONTEIRA E LÓGICA] Cobre o estado de GAME OVER e HUD com arma ativada
    @Test
    void shouldRenderGameOverStatusAndFullHUD() {
        when(mockRoom.getMapLayout()).thenReturn(new int[][]{{0}});
        when(mockRoom.getHeight()).thenReturn(1);
        when(mockRoom.getWidth()).thenReturn(1);

        // Ativa o tiro (Entra no if do canShoot no HUD)
        when(mockPlayer.canShoot()).thenReturn(true);

        // Renderiza com gameOver = true
        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                Collections.emptyList(), Collections.emptyList(),
                120, false, true);

        // Se está Game Over, o jogador NÃO deve ser desenhado! (Entra na branch falsa do if (!gameOver))
        verify(mockGraphics, never()).setColor(Color.GREEN);

        // Valida o HUD de munição
        verify(mockGraphics).drawString(contains("Munição: 5"), eq(350), eq(20));

        // Valida a tela de Morte
        verify(mockGraphics).setColor(Color.RED);
        verify(mockGraphics).drawString("GAME OVER", 180, 200);
    }

    // [TIPO: ESTRUTURAL] Cobre o estado de GameWon e HUD sem arma
    @Test
    void shouldRenderGameWonStatusAndSimpleHUD() {
        when(mockRoom.getMapLayout()).thenReturn(new int[][]{{0}});
        when(mockRoom.getHeight()).thenReturn(1);
        when(mockRoom.getWidth()).thenReturn(1);

        // Desativa o tiro (Não entra no if da munição)
        when(mockPlayer.canShoot()).thenReturn(false);

        // Renderiza com gameWon = true e gameOver = false
        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                Collections.emptyList(), Collections.emptyList(),
                60, true, false);

        // Se não tem gameOver, o jogador é desenhado (sprite ou fallback verde)
        boolean drewSprite = !mockingDetails(mockGraphics).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("drawImage")).findAny().isEmpty();
        boolean drewFallback = !mockingDetails(mockGraphics).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("setColor")
                        && Color.GREEN.equals(i.getArguments()[0])).findAny().isEmpty();
        assertTrue(drewSprite || drewFallback, "Player deve ser desenhado (sprite ou fallback verde)");

        // Valida que a munição NÃO foi desenhada
        verify(mockGraphics, never()).drawString(contains("Munição"), anyInt(), anyInt());

        // Valida a tela de Vitória
        verify(mockGraphics).setColor(Color.YELLOW);
        verify(mockGraphics).drawString("VOCÊ ZEROU O JOGO!", 120, 200);
    }
}