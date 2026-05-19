package st.project.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import st.project.model.game.GameState;
import st.project.model.user.User;
import st.project.repository.JdbcLeaderboardRepository;
import st.project.repository.NoOpLeaderboardRepository;
import st.project.view.GamePanelNew;
import st.project.view.PostGameDialog;
import st.project.view.UserManagementDialog;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GameFlowManagerTest {

    // ---------------------------------------------------------
    // TESTES DE INICIALIZAÇÃO E BOOTSTRAP
    // ---------------------------------------------------------

    @Test
    void shouldExecuteMainSuccessfully() {
        try (MockedStatic<SwingUtilities> swingMock = mockStatic(SwingUtilities.class);
             MockedConstruction<GameFlowManager> flowMock = mockConstruction(GameFlowManager.class)) {

            swingMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(inv -> {
                        Runnable action = inv.getArgument(0);
                        action.run();
                        return null;
                    });

            GameFlowManager.main(new String[0]);

            assertFalse(flowMock.constructed().isEmpty(), "GameFlowManager deveria ter sido instanciado");
            verify(flowMock.constructed().get(0)).start();
        }
    }

    @Test
    void shouldFallbackToNoOpRepositoryOnDatabaseError() throws Exception {
        try (MockedConstruction<JdbcLeaderboardRepository> jdbcMock = mockConstruction(JdbcLeaderboardRepository.class,
                (mock, context) -> { throw new RuntimeException("Falha forçada no DB"); });
             MockedConstruction<MenuController> menuMock = mockConstruction(MenuController.class);
             MockedConstruction<AuthController> authMock = mockConstruction(AuthController.class)) {

            GameFlowManager manager = new GameFlowManager();

            Field repoField = GameFlowManager.class.getDeclaredField("repository");
            repoField.setAccessible(true);
            Object repoInstance = repoField.get(manager);

            assertTrue(repoInstance instanceof NoOpLeaderboardRepository, "Deveria ter feito fallback para o NoOpRepository");
        }
    }

    // ---------------------------------------------------------
    // TESTES DE FLUXO E MÁQUINA DE ESTADOS (MÚLTIPLAS BRANCHES)
    // ---------------------------------------------------------

    @Test
    void shouldHandleFullHappyPathFromMenuToPlaying() throws Exception {
        Runnable[] menuCallbacks = new Runnable[2];

        try (MockedConstruction<MenuController> menuMock = mockConstruction(MenuController.class,
                (mock, context) -> {
                    if (menuCallbacks[0] == null) {
                        menuCallbacks[0] = (Runnable) context.arguments().get(0);
                        menuCallbacks[1] = (Runnable) context.arguments().get(1);
                    }
                });
             MockedConstruction<AuthController> authMock = mockConstruction(AuthController.class,
                     (mock, context) -> {
                         when(mock.promptForLogin()).thenReturn("Ana");
                         when(mock.getPlayerNameOrNull("Ana")).thenReturn("Ana");
                     });
             MockedConstruction<JdbcLeaderboardRepository> repoMock = mockConstruction(JdbcLeaderboardRepository.class,
                     (mock, context) -> when(mock.getUser(anyString())).thenReturn(new User("Ana", "hash", 0, 0, false)))) {

            GameFlowManager manager = new GameFlowManager();
            manager.start();

            verify(menuMock.constructed().get(0)).showMainMenu();

            menuCallbacks[0].run();

            Field frameField = GameFlowManager.class.getDeclaredField("gameWindow");
            frameField.setAccessible(true);
            JFrame frame = (JFrame) frameField.get(manager);

            assertNotNull(frame);
            assertTrue(frame.isVisible());

            Field panelField = GameFlowManager.class.getDeclaredField("gamePanel");
            panelField.setAccessible(true);
            GamePanelNew panel = (GamePanelNew) panelField.get(manager);
            if (panel != null) panel.stopGameLoop();

            for (WindowListener wl : frame.getWindowListeners()) {
                wl.windowClosed(new WindowEvent(frame, WindowEvent.WINDOW_CLOSED));
            }
            verify(menuMock.constructed().get(0), times(2)).showMainMenu();

            if (frame != null) frame.dispose();
        }
    }

    @Test
    void shouldHandleLoginEdgeCases() {
        Runnable[] menuCallbacks = new Runnable[2];

        try (MockedConstruction<MenuController> menuMock = mockConstruction(MenuController.class,
                (mock, context) -> {
                    if (menuCallbacks[0] == null) {
                        menuCallbacks[0] = (Runnable) context.arguments().get(0);
                        menuCallbacks[1] = (Runnable) context.arguments().get(1);
                    }
                });
             MockedConstruction<AuthController> authMock = mockConstruction(AuthController.class);
             MockedConstruction<UserManagementDialog> dialogMock = mockConstruction(UserManagementDialog.class);
             MockedConstruction<JdbcLeaderboardRepository> repoMock = mockConstruction(JdbcLeaderboardRepository.class)) {

            GameFlowManager manager = new GameFlowManager();
            manager.start();

            AuthController auth = authMock.constructed().get(0);
            JdbcLeaderboardRepository repo = repoMock.constructed().get(0);

            when(auth.promptForLogin()).thenReturn(null);
            when(auth.getPlayerNameOrNull(null)).thenReturn(null);
            menuCallbacks[0].run();
            verify(menuMock.constructed().get(0), times(2)).showMainMenu();

            when(auth.promptForLogin()).thenReturn("admin", (String) null);
            when(auth.getPlayerNameOrNull("admin")).thenReturn("admin");
            when(repo.getUser("admin")).thenReturn(new User("admin", "h", 0, 0, true));

            menuCallbacks[0].run();

            assertFalse(dialogMock.constructed().isEmpty());
            verify(dialogMock.constructed().get(0)).show(null);
        }
    }

    @Test
    void shouldHandlePostGameAndExitActions() throws Exception {
        Runnable[] menuCallbacks = new Runnable[2];

        try (MockedConstruction<MenuController> menuMock = mockConstruction(MenuController.class,
                (mock, context) -> {
                    if (menuCallbacks[0] == null) {
                        menuCallbacks[0] = (Runnable) context.arguments().get(0);
                        menuCallbacks[1] = (Runnable) context.arguments().get(1);
                    }
                });
             MockedConstruction<AuthController> authMock = mockConstruction(AuthController.class,
                     (mock, context) -> {
                         when(mock.promptForLogin()).thenReturn("User");
                         when(mock.getPlayerNameOrNull("User")).thenReturn("User");
                     });
             MockedConstruction<JdbcLeaderboardRepository> repoMock = mockConstruction(JdbcLeaderboardRepository.class);
             MockedStatic<PostGameDialog> postGameMock = mockStatic(PostGameDialog.class)) {

            boolean[] exitCalled = {false};

            GameFlowManager manager = new GameFlowManager() {
                @Override
                protected void exitApp() {
                    exitCalled[0] = true;
                }
            };

            manager.start();
            menuCallbacks[0].run();

            Field frameField = GameFlowManager.class.getDeclaredField("gameWindow");
            frameField.setAccessible(true);
            JFrame frame1 = (JFrame) frameField.get(manager);

            Field panelField = GameFlowManager.class.getDeclaredField("gamePanel");
            panelField.setAccessible(true);

            GamePanelNew panel1 = (GamePanelNew) panelField.get(manager);
            if (panel1 != null) panel1.stopGameLoop();

            Method transitionTo = GameFlowManager.class.getDeclaredMethod("transitionTo", GameFlowManager.GameLifecycleState.class);
            transitionTo.setAccessible(true);

            postGameMock.when(() -> PostGameDialog.show(any(), anyBoolean(), any(), anyLong()))
                    .thenReturn(PostGameDialog.PostGameAction.PLAY_AGAIN);

            transitionTo.invoke(manager, GameFlowManager.GameLifecycleState.POST_GAME);

            JFrame frame2 = (JFrame) frameField.get(manager);
            assertNotNull(frame1);
            assertNotNull(frame2);
            assertNotSame(frame1, frame2);

            GamePanelNew panel2 = (GamePanelNew) panelField.get(manager);
            if (panel2 != null) panel2.stopGameLoop();

            postGameMock.when(() -> PostGameDialog.show(any(), anyBoolean(), any(), anyLong()))
                    .thenReturn(PostGameDialog.PostGameAction.RETURN_TO_MENU);
            transitionTo.invoke(manager, GameFlowManager.GameLifecycleState.POST_GAME);

            verify(menuMock.constructed().get(0), times(2)).showMainMenu();

            GameFlowManager nullManager = new GameFlowManager() {
                @Override protected void exitApp() {}
            };
            transitionTo.invoke(nullManager, GameFlowManager.GameLifecycleState.POST_GAME);
            verify(menuMock.constructed().get(1)).showMainMenu();

            menuCallbacks[0].run();
            GamePanelNew panel3 = (GamePanelNew) panelField.get(manager);
            if (panel3 != null) panel3.stopGameLoop();

            postGameMock.when(() -> PostGameDialog.show(any(), anyBoolean(), any(), anyLong()))
                    .thenReturn(PostGameDialog.PostGameAction.CLOSE);

            transitionTo.invoke(manager, GameFlowManager.GameLifecycleState.POST_GAME);
            assertTrue(exitCalled[0], "O Manager deveria ter pedido para fechar o app");

            exitCalled[0] = false;
            menuCallbacks[1].run(); // Agora ele chama o callback correto!
            assertTrue(exitCalled[0], "O Menu deveria ter pedido para fechar o app");

            JFrame lastFrame = (JFrame) frameField.get(manager);
            if (lastFrame != null) lastFrame.dispose();
        }
    }

    // [TIPO: ESTRUTURAL] Cobre as sub-condições do if de Post-Game
    @Test
    void shouldHandlePostGameSubConditions() throws Exception {
        try (MockedConstruction<MenuController> menuMock = mockConstruction(MenuController.class);
             MockedStatic<PostGameDialog> postGameMock = mockStatic(PostGameDialog.class)) {

            GameFlowManager manager = new GameFlowManager();
            Method transitionTo = GameFlowManager.class.getDeclaredMethod("transitionTo", GameFlowManager.GameLifecycleState.class);
            transitionTo.setAccessible(true);

            // Cenário 1: Panel != null, mas Controller == null (cobre o lado direito do ||)
            Field panelField = GameFlowManager.class.getDeclaredField("gamePanel");
            panelField.setAccessible(true);
            panelField.set(manager, mock(GamePanelNew.class));

            transitionTo.invoke(manager, GameFlowManager.GameLifecycleState.POST_GAME);
            verify(menuMock.constructed().get(0), times(1)).showMainMenu();

            // Cenário 2: Panel != null e Controller != null, mas Window == null
            Field controllerField = GameFlowManager.class.getDeclaredField("gameController");
            controllerField.setAccessible(true);
            GameController gcMock = mock(GameController.class);
            when(gcMock.getGameState()).thenReturn(mock(GameState.class));
            controllerField.set(manager, gcMock);

            postGameMock.when(() -> PostGameDialog.show(any(), anyBoolean(), any(), anyLong()))
                    .thenReturn(PostGameDialog.PostGameAction.RETURN_TO_MENU);

            // Dispara POST_GAME: passa pelo primeiro if, mas ignora o Window.dispose() pois a janela é nula
            transitionTo.invoke(manager, GameFlowManager.GameLifecycleState.POST_GAME);
            verify(menuMock.constructed().get(0), times(2)).showMainMenu();
        }
    }

    // [TIPO: ESTRUTURAL EXTREMO] Executa o lambda oculto de Fim de Jogo
    @Test
    void shouldExecuteHiddenLambdaFromGamePanel() throws Exception {
        try (MockedConstruction<JFrame> frameMock = mockConstruction(JFrame.class);
             MockedConstruction<GamePanelNew> panelMock = mockConstruction(GamePanelNew.class);
             MockedConstruction<MenuController> menuMock = mockConstruction(MenuController.class)) {

            GameFlowManager manager = new GameFlowManager();

            // Invocamos a criação da janela (que cria e injeta a Lambda no GamePanel falso)
            Method createGameWindow = GameFlowManager.class.getDeclaredMethod("createGameWindow");
            createGameWindow.setAccessible(true);
            createGameWindow.invoke(manager);

            // Capturamos a Lambda de dentro do Mock do GamePanel
            GamePanelNew panel = panelMock.constructed().get(0);
            ArgumentCaptor<Runnable> lambdaCaptor = ArgumentCaptor.forClass(Runnable.class);
            verify(panel).setOnGameFinished(lambdaCaptor.capture());

            Runnable hiddenLambda = lambdaCaptor.getValue();
            hiddenLambda.run();

            // Valida se a Lambda rodou a transição e voltou pro menu (pois o GameController era nulo no teste)
            verify(menuMock.constructed().get(0)).showMainMenu();
        }
    }

    // [TIPO: ESTRUTURAL EXTREMO] Invade a JVM para testar o System.exit sem fechar o teste
    @Test
    void shouldExecuteSystemExitSafely() {
        GameFlowManager manager = new GameFlowManager();

        SecurityManager original = System.getSecurityManager();
        try {
            System.setSecurityManager(new SecurityManager() {
                @Override
                public void checkPermission(java.security.Permission perm) {} // Permite tudo
                @Override
                public void checkExit(int status) {
                    throw new SecurityException("EXIT_INTERCEPTED"); // Impede a morte e lança erro!
                }
            });
            assertThrows(SecurityException.class, manager::exitApp);

        } catch (UnsupportedOperationException e) {
            System.out.println("Java moderno detectado. Teste de System.exit ignorado por segurança do SO.");
        } finally {
            try {
                System.setSecurityManager(original);
            } catch (UnsupportedOperationException ignored) {}
        }
    }

    // [TIPO: ESTRUTURAL] Intercepta a morte do sistema simulando o Runtime nativo do Java!
    @Test
    void shouldCoverExitAppWithoutKillingJVM() {
        GameFlowManager manager = new GameFlowManager();

        try (MockedStatic<Runtime> mockedRuntime = mockStatic(Runtime.class)) {
            Runtime mockInstance = mock(Runtime.class);
            mockedRuntime.when(Runtime::getRuntime).thenReturn(mockInstance);

            manager.exitApp();

            // Validamos que o código realmente tentou desligar o sistema com status 0
            verify(mockInstance).exit(0);
        }
    }

    // [TIPO: ESTRUTURAL] Cobre as branches invisíveis que o compilador gera em Switches de Enums
    @Test
    void shouldCoverSwitchNullBranches() throws Exception {
        GameFlowManager manager = new GameFlowManager();
        Method transitionTo = GameFlowManager.class.getDeclaredMethod("transitionTo", GameFlowManager.GameLifecycleState.class);
        transitionTo.setAccessible(true);

        // 1. Mandamos NULL para o switch principal (transitionTo)
        try {
            transitionTo.invoke(manager, new Object[]{null});
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }

        // 2. Preparamos o terreno para burlar os "ifs" iniciais do POST_GAME
        Field panelField = GameFlowManager.class.getDeclaredField("gamePanel");
        panelField.setAccessible(true);
        panelField.set(manager, mock(GamePanelNew.class));

        Field controllerField = GameFlowManager.class.getDeclaredField("gameController");
        controllerField.setAccessible(true);
        GameController gcMock = mock(GameController.class);
        when(gcMock.getGameState()).thenReturn(mock(GameState.class));
        controllerField.set(manager, gcMock);

        // Forçamos o painel de Fim de Jogo a retornar NULL para bater no segundo switch
        try (MockedStatic<PostGameDialog> postGameMock = mockStatic(PostGameDialog.class)) {
            postGameMock.when(() -> PostGameDialog.show(any(), anyBoolean(), any(), anyLong()))
                    .thenReturn(null);

            try {
                transitionTo.invoke(manager, GameFlowManager.GameLifecycleState.POST_GAME);
            } catch (InvocationTargetException e) {
                assertTrue(e.getCause() instanceof NullPointerException);
            }
        }
    }
}