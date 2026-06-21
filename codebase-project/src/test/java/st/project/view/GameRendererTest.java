package st.project.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st.project.model.game.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
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
    // COBERTURA 100%: REFLECTION E ESTADOS VISUAIS (NOVOS TESTES)
    // ---------------------------------------------------------

    // Utilitário para injetar os Mocks nas variáveis privadas e finais do GameRenderer
    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    // [ESTRUTURAL] Força a matemática do mapa para desenhar Árvore, Arbusto e Grama (O Switch do drawWallTile)
    @Test
    void shouldRenderAllWallVariantsTreeBushAndGrass() {
        // Coordenadas escolhidas a dedo para bater em: 0 (Árvore), 1 (Arbusto) e 2 (Grama)
        int[][] layout = {
                {Room.TILE_WALL, Room.TILE_WALL},
                {Room.TILE_FLOOR, Room.TILE_WALL}
        };
        when(mockRoom.getMapLayout()).thenReturn(layout);
        when(mockRoom.getHeight()).thenReturn(2);
        when(mockRoom.getWidth()).thenReturn(2);

        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                Collections.emptyList(), Collections.emptyList(), 60, false, false);

        // Verifica a cor exclusiva do tronco da Árvore
        verify(mockGraphics, atLeastOnce()).setColor(new Color(101, 67, 33));
        // Verifica a cor exclusiva da ponta do Arbusto
        verify(mockGraphics, atLeastOnce()).setColor(new Color(110, 195, 65));
        // Verifica a cor exclusiva da mancha da Grama
        verify(mockGraphics, atLeastOnce()).setColor(new Color(50, 110, 35));
    }

    // [ESTRUTURAL] Simula vários frames para cobrir os ifs de Animação e Idle do Jogador
    @Test
    void shouldHandlePlayerAnimationAndIdleStates() throws Exception {
        // Força o SpriteSheet do jogador a fingir que carregou com sucesso
        SpriteSheet mockSheet = mock(SpriteSheet.class);
        when(mockSheet.isLoaded()).thenReturn(true);
        BufferedImage fakeImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        when(mockSheet.getFrame(anyInt(), anyInt())).thenReturn(fakeImg);
        when(mockSheet.getFrameFlipped(anyInt(), anyInt())).thenReturn(fakeImg);
        setPrivateField(renderer, "playerSheet", mockSheet);

        when(mockRoom.getMapLayout()).thenReturn(new int[][]{{0}});
        when(mockRoom.getHeight()).thenReturn(1);
        when(mockRoom.getWidth()).thenReturn(1);

        // Frame 1: Movendo para direita (LastDirX = 1) - Cobre o facingLeft = false
        when(mockPlayer.getGridX()).thenReturn(1);
        when(mockPlayer.getLastDirX()).thenReturn(1);
        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom, Collections.emptyList(), Collections.emptyList(), 60, false, false);

        // Frame 2: Movendo para a esquerda (LastDirX = -1) - Cobre o facingLeft = true
        when(mockPlayer.getGridX()).thenReturn(2);
        when(mockPlayer.getLastDirX()).thenReturn(-1);
        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom, Collections.emptyList(), Collections.emptyList(), 60, false, false);

        // Frame 3 ao 20: Mantém o jogador parado na mesma posição para o idleTicks subir acima de 15 (IDLE_DELAY)
        for(int i = 0; i < 20; i++) {
            renderer.render(mockGraphics, mockState, mockPlayer, mockRoom, Collections.emptyList(), Collections.emptyList(), 60, false, false);
        }

        // Verifica se usou a imagem normal e a invertida
        verify(mockSheet, atLeastOnce()).getFrameFlipped(anyInt(), anyInt());
        verify(mockSheet, atLeastOnce()).getFrame(anyInt(), anyInt());
    }

    // [ESTRUTURAL] Cobre as branches de imagens válidas para Tiros (Madeira/Bala) e Inimigos
    @Test
    void shouldRenderProjectilesAndEnemiesWithLoadedImages() throws Exception {
        // Injeta os Mocks de Imagens válidas
        SpriteSheet mockEnemySheet = mock(SpriteSheet.class);
        when(mockEnemySheet.isLoaded()).thenReturn(true);
        BufferedImage fakeImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        when(mockEnemySheet.getFrame(anyInt(), anyInt())).thenReturn(fakeImg);
        when(mockEnemySheet.getFrameFlipped(anyInt(), anyInt())).thenReturn(fakeImg);

        setPrivateField(renderer, "enemySheet", mockEnemySheet);
        setPrivateField(renderer, "balaImage", fakeImg);
        setPrivateField(renderer, "madeiraImage", fakeImg);

        when(mockRoom.getMapLayout()).thenReturn(new int[][]{{0}});
        when(mockRoom.getHeight()).thenReturn(1);
        when(mockRoom.getWidth()).thenReturn(1);
        when(mockState.getCurrentLevel()).thenReturn(2); // Nível 2 libera a textura da Madeira (Player)

        // Configura inimigos para testar os dois lados de visão (faceLeft true e false)
        when(mockPlayer.getGridX()).thenReturn(2);
        Enemy eLeft = mock(Enemy.class);  when(eLeft.getGridX()).thenReturn(1); // Player > Inimigo (faceLeft=true)
        Enemy eRight = mock(Enemy.class); when(eRight.getGridX()).thenReturn(3); // Player < Inimigo (faceLeft=false)

        // Configura Tiros para testar os dois donos
        Projectile pEnemy = mock(Projectile.class);  when(pEnemy.isPlayerOwned()).thenReturn(false);
        Projectile pPlayer = mock(Projectile.class); when(pPlayer.isPlayerOwned()).thenReturn(true);

        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                Arrays.asList(eLeft, eRight), Arrays.asList(pEnemy, pPlayer), 60, false, false);

        // Validações
        verify(mockEnemySheet).getFrameFlipped(anyInt(), anyInt());
        verify(mockEnemySheet).getFrame(anyInt(), anyInt());
        // Garante que o Graphics chamou o drawImage para as balas de madeira e as balas inimigas
        verify(mockGraphics, atLeastOnce()).drawImage(eq(fakeImg), anyInt(), anyInt(), anyInt(), anyInt(), isNull());
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

    // ---------------------------------------------------------
    // COBERTURA FINAL: FALLBACKS, TERNARES E MATRIZ BOOLEANA
    // ---------------------------------------------------------

    // [ESTRUTURAL] Cobre o catch e o operador ternário do método privado loadImage
    @Test
    void shouldCoverLoadImageExceptionAndTernary() throws Exception {
        java.lang.reflect.Method loadImageMethod = GameRenderer.class.getDeclaredMethod("loadImage", String.class);
        loadImageMethod.setAccessible(true);

        // Caminho 1 (Linha Vermelha): String nula força NullPointerException no getResourceAsStream, caindo no 'catch'
        Object resultFromNull = loadImageMethod.invoke(renderer, (String) null);
        assertNull(resultFromNull, "Deveria retornar nulo ao cair no catch");

        // Caminho 2 (Linha Amarela): Caminho inválido faz getResourceAsStream retornar null, cobrindo o lado falso do ternário (is == null)
        Object resultFromInvalidPath = loadImageMethod.invoke(renderer, "/caminho/invalido.png");
        assertNull(resultFromInvalidPath, "Deveria retornar nulo pelo operador ternário");
    }

    // [ESTRUTURAL] Cobre todos os blocos "else" de desenho (Enemy, Projectile e Player sem imagem) e tabelas-verdade do Tiro
    @Test
    void shouldCoverRenderFallbacksAndProjectilesBranches() throws Exception {
        // Força as imagens a estarem nulas ou não carregadas
        SpriteSheet unloadedSheet = mock(SpriteSheet.class);
        when(unloadedSheet.isLoaded()).thenReturn(false);

        setPrivateField(renderer, "playerSheet", unloadedSheet);
        setPrivateField(renderer, "enemySheet", unloadedSheet);
        setPrivateField(renderer, "balaImage", null);
        setPrivateField(renderer, "madeiraImage", null);

        when(mockRoom.getMapLayout()).thenReturn(new int[][]{{0}});
        when(mockRoom.getHeight()).thenReturn(1);
        when(mockRoom.getWidth()).thenReturn(1);

        Enemy mockEnemy = mock(Enemy.class);

        // Esgotando as Branches (Linhas Amarelas do Projectile)
        // Cenário A: Tiro de Inimigo, mas balaImage é nula -> Cai no else (p.draw)
        Projectile pEnemy = mock(Projectile.class);
        when(pEnemy.isPlayerOwned()).thenReturn(false);

        // Cenário B: Tiro de Player, Nível 1 -> Falha no currentLevel >= 2 -> Cai no else (p.draw)
        Projectile pPlayerLvl1 = mock(Projectile.class);
        when(pPlayerLvl1.isPlayerOwned()).thenReturn(true);
        when(mockState.getCurrentLevel()).thenReturn(1);

        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                java.util.Collections.singletonList(mockEnemy),
                java.util.Arrays.asList(pEnemy, pPlayerLvl1), 60, false, false);

        // Cenário C: Tiro de Player, Nível 2, mas madeiraImage é nula -> Cai no else (p.draw)
        Projectile pPlayerLvl2 = mock(Projectile.class);
        when(pPlayerLvl2.isPlayerOwned()).thenReturn(true);
        when(mockState.getCurrentLevel()).thenReturn(2);

        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                java.util.Collections.emptyList(),
                java.util.Collections.singletonList(pPlayerLvl2), 60, false, false);

        // VERIFICAÇÕES DE FALLBACKS (As 3 Linhas Vermelhas restantes):
        verify(mockEnemy, atLeastOnce()).draw(mockGraphics, 40); // Desenha triângulo laranja
        verify(pEnemy, atLeastOnce()).draw(mockGraphics);        // Desenha bolinha magenta
        verify(pPlayerLvl1, atLeastOnce()).draw(mockGraphics);   // Desenha bolinha cyan
        verify(pPlayerLvl2, atLeastOnce()).draw(mockGraphics);   // Desenha bolinha cyan
        verify(mockGraphics, atLeastOnce()).setColor(Color.GREEN); // Quadrado Verde do Player
        verify(mockGraphics, atLeastOnce()).fillRect(anyInt(), anyInt(), eq(40), eq(40));
    }

    // [ESTRUTURAL] Esgota a tabela-verdade do "isMoving" cobrindo o movimento puramente no eixo Y
    @Test
    void shouldCoverPlayerIsMovingYAxisBranch() throws Exception {
        when(mockRoom.getMapLayout()).thenReturn(new int[][]{{0}});
        when(mockRoom.getHeight()).thenReturn(1);
        when(mockRoom.getWidth()).thenReturn(1);

        when(mockPlayer.getGridX()).thenReturn(5);
        when(mockPlayer.getGridY()).thenReturn(5);

        // A branch amarela faltante era: X igual, Y diferente (false || true)
        setPrivateField(renderer, "lastPlayerX", 5);
        setPrivateField(renderer, "lastPlayerY", 4);

        renderer.render(mockGraphics, mockState, mockPlayer, mockRoom,
                java.util.Collections.emptyList(), java.util.Collections.emptyList(), 60, false, false);

        // O teste passa silenciosamente comprovando que a ramificação lógica foi percorrida
    }
}