package st.project;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import st.project.controller.GameFlowManager;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainTest {

    // ESTRUTURAL
    @Test
    void shouldInstantiateMainClass() {
        assertNotNull(new Main());
    }

    // DOMÍNIO E ESTRUTURAL
    @Test
    void shouldCreateWindowWithExpectedConfiguration() {
        if (GraphicsEnvironment.isHeadless()) return;

        JPanel panel = new JPanel();
        JFrame window = Main.createMainWindow(panel);
        try {
            assertEquals("Labirinto", window.getTitle());
            assertEquals(JFrame.EXIT_ON_CLOSE, window.getDefaultCloseOperation());
            assertFalse(window.isResizable());
            assertEquals(620, window.getWidth());
            assertEquals(480, window.getHeight());
            assertSame(panel, window.getContentPane().getComponent(0));
        } finally {
            window.dispose();
        }
    }

    // FRONTEIRA
    @Test
    void shouldThrowWhenCreatingWindowWithNullPanel() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Main.createMainWindow(null));
        assertEquals("contentPanel não pode ser nulo", ex.getMessage());
    }

    // ESTRUTURAL
    @Test
    void shouldScheduleActionWhenNotInEventDispatchThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        boolean executedImmediately = Main.runOnEventDispatchThread(() -> {
            executed.set(true);
            latch.countDown();
        });

        assertFalse(executedImmediately);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(executed.get());
    }

    // ESTRUTURAL
    @Test
    void shouldExecuteImmediatelyWhenAlreadyInEventDispatchThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executedImmediately = new AtomicBoolean(false);
        AtomicReference<Throwable> failure = new AtomicReference<>(null);

        SwingUtilities.invokeAndWait(() -> {
            try {
                executedImmediately.set(Main.runOnEventDispatchThread(() -> {}));
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNull(failure.get());
        assertTrue(executedImmediately.get());
    }

    // ESTRUTURAL E FRONTEIRA
    @Test
    void shouldRequirePlayerNameCorrectly() {
        assertNull(Main.requirePlayerName(null));
        assertNull(Main.requirePlayerName("   "));
        assertEquals("Ana", Main.requirePlayerName("  Ana  "));
    }

    // ESTRUTURAL (Com Injeção de Dependência)
    @Test
    void shouldRunStartGameWithInjectedDependencies() {
        Supplier<String> originalNameSupplier = Main.playerNameSupplier;
        Function<String, JPanel> originalSupplier = Main.gamePanelSupplier;
        Consumer<JPanel> originalDisplayer = Main.windowDisplayer;
        AtomicBoolean displayed = new AtomicBoolean(false);
        JPanel fakePanel = new JPanel();

        try {
            Main.playerNameSupplier = () -> "  Bia  ";
            Main.gamePanelSupplier = playerName -> {
                assertEquals("Bia", playerName);
                return fakePanel;
            };
            Main.windowDisplayer = p -> {
                assertSame(fakePanel, p);
                displayed.set(true);
            };

            Main.startGame();

            assertTrue(displayed.get());
        } finally {
            Main.playerNameSupplier = originalNameSupplier;
            Main.gamePanelSupplier = originalSupplier;
            Main.windowDisplayer = originalDisplayer;
        }
    }

    // FRONTEIRA
    @Test
    void shouldNotStartGameWhenPlayerNameIsMissing() {
        Supplier<String> originalNameSupplier = Main.playerNameSupplier;
        AtomicBoolean gameCreated = new AtomicBoolean(false);

        try {
            Main.playerNameSupplier = () -> "   ";
            Main.gamePanelSupplier = playerName -> {
                gameCreated.set(true);
                return new JPanel();
            };

            Main.startGame();

            assertFalse(gameCreated.get(), "O jogo iniciou com nome em branco!");
        } finally {
            Main.playerNameSupplier = originalNameSupplier;
        }
    }

    // ESTRUTURAL
    @Test
    void shouldCallGameFlowManagerWhenMainMethodOrShowMainMenuIsInvoked() {
        Runnable originalAction = Main.startupAction;

        try (MockedStatic<SwingUtilities> swingMock = mockStatic(SwingUtilities.class);
             MockedConstruction<GameFlowManager> flowMock = mockConstruction(GameFlowManager.class)) {

            // Em vez de rodar dentro do Mock (o que cega o JaCoCo), apenas guardamos as tarefas num balde.
            java.util.List<Runnable> tasks = new java.util.ArrayList<>();
            swingMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        tasks.add(invocation.getArgument(0));
                        return null;
                    });

            Main.startupAction = null;

            // AÇÃO
            Main.main(new String[0]);
            Main.showMainMenu();

            for (int i = 0; i < tasks.size(); i++) {
                tasks.get(i).run();
            }

            // PÓS-CONDIÇÃO: O Mockito enxerga (mesma thread) e o JaCoCo enxerga (fora do thenAnswer)!
            assertTrue(flowMock.constructed().size() >= 2, "O GameFlowManager não foi instanciado!");
        } finally {
            Main.startupAction = originalAction;
        }
    }

    // ESTRUTURAL (Cobre a lambda vermelha original do startupAction)
    @Test
    void shouldExecuteDefaultStartupAction() {
        try (MockedStatic<SwingUtilities> swingMock = mockStatic(SwingUtilities.class);
             MockedConstruction<st.project.controller.GameFlowManager> mocked = mockConstruction(st.project.controller.GameFlowManager.class)) {

            java.util.List<Runnable> tasks = new java.util.ArrayList<>();
            swingMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        tasks.add(invocation.getArgument(0));
                        return null;
                    });

            Main.startupAction.run();

            for (int i = 0; i < tasks.size(); i++) {
                tasks.get(i).run();
            }

            // PÓS-CONDIÇÃO
            assertFalse(mocked.constructed().isEmpty(), "A lambda default não instanciou o Manager!");
        }
    }

    // ESTRUTURAL (Cobre a lambda original do windowDisplayer)
    @Test
    void shouldExecuteDefaultWindowDisplayer() {
        if (GraphicsEnvironment.isHeadless()) return;

        JPanel testPanel = new JPanel();

        // Chamamos a lambda default diretamente
        Main.windowDisplayer.accept(testPanel);

        // Procuramos a janela criada
        Frame[] frames = Frame.getFrames();
        JFrame createdFrame = null;
        for (Frame f : frames) {
            if (f instanceof JFrame && f.getTitle().equals("Labirinto") && f.isVisible()) {
                createdFrame = (JFrame) f;
                break;
            }
        }

        assertNotNull(createdFrame, "A lambda default não abriu a janela!");
        createdFrame.dispose(); // Limpeza
    }

    // ESTRUTURAL E FRONTEIRA (Cobre a linha amarela: if startupAction == null)
    @Test
    void shouldNotReassignStartupActionIfItIsNotNull() throws Exception {
        Runnable originalAction = Main.startupAction;
        AtomicBoolean customActionRan = new AtomicBoolean(false);

        Main.startupAction = () -> customActionRan.set(true);

        Main.main(new String[0]);

        SwingUtilities.invokeAndWait(() -> {});

        assertTrue(customActionRan.get());

        Main.startupAction = originalAction; // Limpeza
    }

    // ESTRUTURAL E FRONTEIRA (Cobre a linha amarela: if panel != null)
    @Test
    void shouldHandleNullPanelInStartGameQuietly() {
        Supplier<String> origName = Main.playerNameSupplier;
        Function<String, JPanel> origPanel = Main.gamePanelSupplier;
        Consumer<JPanel> origDisp = Main.windowDisplayer;

        try {
            Main.playerNameSupplier = () -> "Testador";
            Main.gamePanelSupplier = name -> null;

            AtomicBoolean displayerCalled = new AtomicBoolean(false);
            Main.windowDisplayer = p -> {
                assertNull(p);
                displayerCalled.set(true);
            };

            assertDoesNotThrow(Main::startGame);
            assertTrue(displayerCalled.get());
        } finally {
            Main.playerNameSupplier = origName;
            Main.gamePanelSupplier = origPanel;
            Main.windowDisplayer = origDisp;
        }
    }

    // ESTRUTURAL E UI (Substituindo a janela modal por um Mock estático)
    @Test
    void shouldExecuteDefaultPlayerNameSupplier() {
        if (GraphicsEnvironment.isHeadless()) return;

        try (MockedStatic<JOptionPane> optionPaneMock = mockStatic(JOptionPane.class)) {

            optionPaneMock.when(() -> JOptionPane.showInputDialog(any(), anyString()))
                    .thenReturn("HeroiMock");

            String result = Main.playerNameSupplier.get();

            assertEquals("HeroiMock", result);
        }
    }

    // ESTRUTURAL (Cobre o método oculto da lambda original gamePanelSupplier)
    @Test
    void shouldExecuteDefaultGamePanelSupplier() {
        JPanel panel = Main.gamePanelSupplier.apply("JogadorTeste");

        assertNotNull(panel, "A lambda falhou em retornar o painel do jogo!");
        assertTrue(panel instanceof javax.swing.JPanel, "O objeto retornado não é um JPanel!");
    }
}