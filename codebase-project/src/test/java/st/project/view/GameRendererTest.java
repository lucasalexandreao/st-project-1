package st.project.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st.project.model.game.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;

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
        // Criamos um mapa forjado contendo TODOS os tipos de blocos mapeados
        // e incluímos o "0" (ou 99) para forçar o código a passar direto pelo switch (Default oculto)
        int[][] fakeLayout = {
                {Room.TILE_WALL, Room.TILE_KEY, Room.TILE_EXIT_LOCKED},
                {Room.TILE_EXIT_OPEN, 0, 99} // 0 e 99 são "chão livre", não têm desenho específico
        };

        when(mockRoom.getMapLayout()).thenReturn(fakeLayout);
        when(mockRoom.getHeight()).thenReturn(2);
        when(mockRoom.getWidth()).thenReturn(3);

        // Simulamos o estado normal de jogo (rodando)
        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                Collections.emptyList(), Collections.emptyList(),
                60, false, false);

        // Verificações cirúrgicas para ver se o Graphics pintou as cores corretas do Switch
        verify(mockGraphics).setColor(Color.DARK_GRAY); // TILE_WALL
        verify(mockGraphics).setColor(Color.YELLOW);    // TILE_KEY (e VOCÊ ZEROU, mas aqui gameWon é false)
        verify(mockGraphics).setColor(Color.RED);       // TILE_EXIT_LOCKED (e GAME OVER, mas gameOver é false)
        verify(mockGraphics).setColor(Color.CYAN);      // TILE_EXIT_OPEN

        // Verifica se o jogador foi desenhado (verde)
        verify(mockGraphics).setColor(Color.GREEN);
    }

    // ---------------------------------------------------------
    // RENDERIZAÇÃO DE ENTIDADES (Inimigos e Tiros)
    // ---------------------------------------------------------

    // [TIPO: INTEGRAÇÃO E DOMÍNIO] Garante que as listas de entidades repassam o comando de desenho (Delegation)
    @Test
    void shouldRenderEnemiesAndProjectiles() {
        // Configuramos um mapa minúsculo apenas para não estourar erro de layout
        when(mockRoom.getMapLayout()).thenReturn(new int[][]{{0}});
        when(mockRoom.getHeight()).thenReturn(1);
        when(mockRoom.getWidth()).thenReturn(1);

        Enemy mockEnemy = mock(Enemy.class);
        Projectile mockProjectile = mock(Projectile.class);

        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                Arrays.asList(mockEnemy), Arrays.asList(mockProjectile),
                60, false, false);

        // Valida se o Renderer "terceirizou" a pintura corretamente para os próprios objetos
        verify(mockEnemy, times(1)).draw(eq(mockGraphics), anyInt());
        verify(mockProjectile, times(1)).draw(mockGraphics);
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

    // [TIPO: ESTRUTURAL] Cobre o estado de VITÓRIA (GameWon) e HUD sem arma
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

        // Se não tem gameOver, o jogador é desenhado
        verify(mockGraphics).setColor(Color.GREEN);

        // Valida que a munição NÃO foi desenhada
        verify(mockGraphics, never()).drawString(contains("Munição"), anyInt(), anyInt());

        // Valida a tela de Vitória
        verify(mockGraphics).setColor(Color.YELLOW);
        verify(mockGraphics).drawString("VOCÊ ZEROU O JOGO!", 120, 200);
    }
}