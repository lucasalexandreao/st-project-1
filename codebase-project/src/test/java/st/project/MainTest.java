package st.project;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    // ESTRUTURAL
    @Test
    void shouldInstantiateMainClass() {
        Main main = new Main();

        assertNotNull(main);
    }

    // DOMÍNIO E ESTRUTURAL
    @Test
    void shouldCreateWindowWithExpectedConfiguration() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Main.createMainWindow(null));

        assertEquals("contentPanel não pode ser nulo", exception.getMessage());
    }

    // GERADO POR IA (PARA ATINGIR 100% DE COBERTURA)
    // ESTRUTURAL
    @Test
    void shouldScheduleActionWhenNotInEventDispatchThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicBoolean executedOnEdt = new AtomicBoolean(false);

        boolean executedImmediately = Main.runOnEventDispatchThread(() -> {
            executed.set(true);
            executedOnEdt.set(EventQueue.isDispatchThread());
            latch.countDown();
        });

        assertFalse(executedImmediately);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(executed.get());
        assertTrue(executedOnEdt.get());
    }

    // GERADO POR IA (PARA ATINGIR 100% DE COBERTURA)
    // ESTRUTURAL
    @Test
    void shouldExecuteImmediatelyWhenAlreadyInEventDispatchThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executedImmediately = new AtomicBoolean(false);
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        AtomicReference<Throwable> failure = new AtomicReference<>(null);

        SwingUtilities.invokeAndWait(() -> {
            try {
                boolean result = Main.runOnEventDispatchThread(() -> {
                    actionExecuted.set(true);
                    assertTrue(EventQueue.isDispatchThread());
                });
                executedImmediately.set(result);
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }

        assertTrue(executedImmediately.get());
        assertTrue(actionExecuted.get());
    }

    // GERADO POR IA (PARA ATINGIR 100% DE COBERTURA)
    // ESTRUTURAL E FRONTEIRA
    @Test
    void shouldCallStartupActionWhenMainIsInvoked() {
        AtomicBoolean started = new AtomicBoolean(false);
        Runnable original = Main.startupAction;

        try {
            Main.startupAction = () -> started.set(true);

            Main.main(new String[0]);

            long timeoutMs = System.currentTimeMillis() + 2000;
            while (!started.get() && System.currentTimeMillis() < timeoutMs) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            assertTrue(started.get());
        } finally {
            Main.startupAction = original;
        }
    }

    // ESTRUTURAL
    @Test
    void shouldRunStartGameWithInjectedDependencies() {
        class TestPanel extends JPanel {
            private boolean focusRequested;

            @Override
            public boolean requestFocusInWindow() {
                this.focusRequested = true;
                return true;
            }

            boolean wasFocusRequested() {
                return focusRequested;
            }
        }

        Supplier<JPanel> originalSupplier = Main.gamePanelSupplier;
        Consumer<JPanel> originalDisplayer = Main.windowDisplayer;
        AtomicBoolean displayed = new AtomicBoolean(false);
        TestPanel panel = new TestPanel();

        try {
            Main.gamePanelSupplier = () -> panel;
            Main.windowDisplayer = p -> {
                assertSame(panel, p);
                displayed.set(true);
            };

            Main.startGame();

            assertTrue(displayed.get());
            assertTrue(panel.wasFocusRequested());
        } finally {
            Main.gamePanelSupplier = originalSupplier;
            Main.windowDisplayer = originalDisplayer;
        }
    }

    // DOMÍNIO E ESTRUTURAL
    @Test
    void shouldDisplayWindowUsingDefaultWindowDisplayer() {
        JPanel panel = new JPanel();

        Main.windowDisplayer.accept(panel);

        JFrame createdWindow = null;
        for (Frame frame : Frame.getFrames()) {
            if (frame instanceof JFrame) {
                JFrame candidate = (JFrame) frame;
                if (candidate.getContentPane().getComponentCount() > 0
                        && candidate.getContentPane().getComponent(0) == panel) {
                    createdWindow = candidate;
                    break;
                }
            }
        }

        assertNotNull(createdWindow);
        assertTrue(createdWindow.isVisible());
        createdWindow.dispose();
    }
}
