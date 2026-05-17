package st.project;

import st.project.controller.GameFlowManager;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Main {

    // injectable hooks for tests
    public static Runnable startupAction = () -> {
        SwingUtilities.invokeLater(() -> {
            GameFlowManager manager = new GameFlowManager();
            manager.start();
        });
    };

    public static Supplier<String> playerNameSupplier = () -> JOptionPane.showInputDialog(null, "Nome do jogador");
    public static Function<String, JPanel> gamePanelSupplier = playerName -> new JPanel();
    public static Consumer<JPanel> windowDisplayer = panel -> {
        JFrame w = createMainWindow(panel);
        w.setVisible(true);
    };

    public static void main(String[] args) {
        if (startupAction == null) {
            startupAction = () -> {
                SwingUtilities.invokeLater(() -> {
                    GameFlowManager manager = new GameFlowManager();
                    manager.start();
                });
            };
        }

        SwingUtilities.invokeLater(startupAction);
    }

    public static JFrame createMainWindow(JPanel contentPanel) {
        if (contentPanel == null) throw new IllegalArgumentException("contentPanel não pode ser nulo");

        JFrame window = new JFrame("Labirinto");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.add(contentPanel);
        window.pack();
        window.setSize(620, 480);
        window.setLocationRelativeTo(null);
        return window;
    }

    public static boolean runOnEventDispatchThread(Runnable action) {
        if (EventQueue.isDispatchThread()) {
            action.run();
            return true;
        }

        SwingUtilities.invokeLater(action);
        return false;
    }

    public static String requirePlayerName(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    public static void startGame() {
        String raw = playerNameSupplier.get();
        String name = requirePlayerName(raw);
        if (name == null) return;

        JPanel panel = gamePanelSupplier.apply(name);
        windowDisplayer.accept(panel);
        if (panel != null) panel.requestFocusInWindow();
    }

    public static void showMainMenu() {
        SwingUtilities.invokeLater(() -> {
            GameFlowManager manager = new GameFlowManager();
            manager.start();
        });
    }
}
