package st.project.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import st.project.controller.GameController;
import st.project.model.GameConfig;
import st.project.model.game.GameState;
import st.project.model.game.Player;
import st.project.repository.LeaderboardRepository;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GamePanelNewTest {

    private GameController gameController;
    private GameState gameState;
    private Player player;
    private LeaderboardRepository repository;

    @BeforeEach
    void setUp() {
        gameController = mock(GameController.class);
        gameState = mock(GameState.class);
        player = mock(Player.class);
        repository = mock(LeaderboardRepository.class);

        when(gameController.getGameState()).thenReturn(gameState);
        when(gameState.getPlayer()).thenReturn(player);
    }

    // ---------------------------------------------------------
    // UTILITÁRIO DE REFLECTION
    // ---------------------------------------------------------

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ---------------------------------------------------------
    // TELA E RENDERIZAÇÃO
    // ---------------------------------------------------------

    // [TIPO: INTEGRAÇÃO GRÁFICA] Garante que o painel chama o motor de desenho corretamente.
    @Test
    void shouldRenderGameCorrectly() {
        try (MockedConstruction<Timer> timerMock = mockConstruction(Timer.class);
             MockedConstruction<GameRenderer> rendererMock = mockConstruction(GameRenderer.class)) {

            GamePanelNew panel = new GamePanelNew(gameController, repository, "Ana");

            Graphics mockGraphics = mock(Graphics.class);
            // O SEGREDO: Ensina o Pincel Falso a sobreviver ao g.create() interno do Swing!
            when(mockGraphics.create()).thenReturn(mockGraphics);

            // Chamamos a pintura manualmente
            panel.paintComponent(mockGraphics);

            // Verificamos se o Pincel Falso foi repassado para o nosso GameRenderer
            GameRenderer renderer = rendererMock.constructed().get(0);
            verify(renderer).render(eq(mockGraphics), eq(gameState), eq(player), any(), any(), any(), anyInt(), anyBoolean(), anyBoolean());
        }
    }

    // ---------------------------------------------------------
    // GAME LOOP (O CORAÇÃO DO JOGO)
    // ---------------------------------------------------------

    // [TIPO: ESTRUTURAL EXTREMO] Dispara o Timer em modo manual e cobre as branches do estado do jogo.
    @Test
    void shouldExecuteGameLoopLambdaAndHandleFinishCallback() throws Exception {
        ActionListener[] capturedListener = new ActionListener[1];

        try (MockedConstruction<Timer> timerMock = mockConstruction(Timer.class, (mock, context) -> {
            capturedListener[0] = (ActionListener) context.arguments().get(1);
        });
             MockedConstruction<GameRenderer> rendererMock = mockConstruction(GameRenderer.class)) {

            // Intercepta a chamada para não bugar o Swing
            GamePanelNew panel = new GamePanelNew(gameController, repository, "Ana") {
                @Override public void repaint() {}
            };

            Timer mockTimer = timerMock.constructed().get(0);

            Runnable mockCallback = mock(Runnable.class);
            panel.setOnGameFinished(mockCallback);

            // Cenário 1: Jogo rodando (isGameFinished = false)
            when(gameState.isGameFinished()).thenReturn(false);
            capturedListener[0].actionPerformed(null);

            verify(gameController, times(1)).updateGameLogic();
            verify(mockTimer, never()).stop();
            verify(mockCallback, never()).run();

            // Cenário 2: Jogo acabou (isGameFinished = true)
            when(gameState.isGameFinished()).thenReturn(true);
            capturedListener[0].actionPerformed(null);

            verify(mockTimer, times(1)).stop();
            verify(mockCallback, times(1)).run();
        }
    }

    // [TIPO: FRONTEIRA] Cobre o if oculto de callback Nulo e Timer Nulo
    @Test
    void shouldHandleGameFinishWithNullCallbackAndNullTimer() throws Exception {
        ActionListener[] capturedListener = new ActionListener[1];

        try (MockedConstruction<Timer> timerMock = mockConstruction(Timer.class, (mock, context) -> {
            capturedListener[0] = (ActionListener) context.arguments().get(1);
        });
             MockedConstruction<GameRenderer> rendererMock = mockConstruction(GameRenderer.class)) {

            GamePanelNew panel = new GamePanelNew(gameController, repository, "Bia");

            when(gameState.isGameFinished()).thenReturn(true);

            // Callback é nulo por padrão, não deve quebrar
            capturedListener[0].actionPerformed(null);

            // Força gameLoop = null para cobrir o if defensivo
            setField(panel, "gameLoop", null);
            panel.stopGameLoop();
        }
    }

    // ---------------------------------------------------------
    // EVENTOS DE TECLADO (KeyAdapter)
    // ---------------------------------------------------------

    // [TIPO: FRONTEIRA E LÓGICA] Testa todas as combinações complexas do botão SPACE e as direções do jogador.
    @Test
    void shouldHandleKeyboardInputsCorrectly() {
        try (MockedConstruction<Timer> timerMock = mockConstruction(Timer.class);
             MockedConstruction<GameRenderer> rendererMock = mockConstruction(GameRenderer.class)) {

            GamePanelNew panel = new GamePanelNew(gameController, repository, "Cris");
            KeyListener keyListener = panel.getKeyListeners()[0];

            // Jogo finalizado: ignora comandos
            when(gameState.isGameFinished()).thenReturn(true);
            keyListener.keyPressed(createKeyEvent(panel, KeyEvent.VK_UP));
            verify(gameController, never()).handlePlayerMovement(anyInt(), anyInt());

            // Jogo em andamento
            when(gameState.isGameFinished()).thenReturn(false);
            when(player.getGridX()).thenReturn(5);
            when(player.getGridY()).thenReturn(5);

            // ---------------------------------------------
            // Matriz do Botão de TIRO (Espaço)
            // ---------------------------------------------

            when(player.canShoot()).thenReturn(false);
            keyListener.keyPressed(createKeyEvent(panel, KeyEvent.VK_SPACE));
            verify(gameController, never()).addPlayerProjectile(anyInt(), anyInt(), anyInt(), anyInt());
            verify(gameController, times(1)).handlePlayerMovement(5, 5);

            when(player.canShoot()).thenReturn(true);
            when(player.hasAmmo()).thenReturn(false);
            keyListener.keyPressed(createKeyEvent(panel, KeyEvent.VK_SPACE));
            verify(gameController, never()).addPlayerProjectile(anyInt(), anyInt(), anyInt(), anyInt());
            verify(gameController, times(2)).handlePlayerMovement(5, 5);

            when(player.hasAmmo()).thenReturn(true);
            when(player.getLastDirX()).thenReturn(1);
            when(player.getLastDirY()).thenReturn(0);

            keyListener.keyPressed(createKeyEvent(panel, KeyEvent.VK_SPACE));

            int expectedPx = 5 * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
            int expectedPy = 5 * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
            verify(gameController, times(1)).addPlayerProjectile(expectedPx, expectedPy, 12, 0);

            // ---------------------------------------------
            // Matriz de Movimentação (Setinhas)
            // ---------------------------------------------

            keyListener.keyPressed(createKeyEvent(panel, KeyEvent.VK_UP));
            verify(gameController).handlePlayerMovement(5, 4);

            keyListener.keyPressed(createKeyEvent(panel, KeyEvent.VK_DOWN));
            verify(gameController).handlePlayerMovement(5, 6);

            keyListener.keyPressed(createKeyEvent(panel, KeyEvent.VK_LEFT));
            verify(gameController).handlePlayerMovement(4, 5);

            keyListener.keyPressed(createKeyEvent(panel, KeyEvent.VK_RIGHT));
            verify(gameController).handlePlayerMovement(6, 5);

            keyListener.keyPressed(createKeyEvent(panel, KeyEvent.VK_X)); // Tecla inútil

            // 2 pulos perdidos do Espaço + 4 setinhas + 1 X vazio = 7 chamadas totais de movimento
            verify(gameController, times(7)).handlePlayerMovement(anyInt(), anyInt());
        }
    }

    private KeyEvent createKeyEvent(Component source, int keyCode) {
        return new KeyEvent(source, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, ' ');
    }
}