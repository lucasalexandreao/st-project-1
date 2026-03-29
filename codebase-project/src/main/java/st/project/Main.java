package st.project;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Main {

    static Runnable startupAction = Main::startGame;
    static Supplier<JPanel> gamePanelSupplier = GamePanel::new;
    static Consumer<JPanel> windowDisplayer = new Consumer<JPanel>() {
        @Override
        public void accept(JPanel panel) {
            JFrame window = createMainWindow(panel);
            window.setVisible(true);
        }
    };

    static JFrame createMainWindow(JPanel contentPanel) {
        if (contentPanel == null) {
            throw new IllegalArgumentException("contentPanel não pode ser nulo");
        }

        // Configuração da Janela (JFrame)
        JFrame window = new JFrame("Labirinto");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Adiciona o nosso GamePanel visual
        window.add(contentPanel);

        // Define o tamanho da janela baseado no tamanho do mapa (TILE_SIZE * grade)
        window.pack(); // Faz a janela se ajustar ao painel interno
        window.setSize(620, 480); // Ajuste manual para caber o mapa de teste

        window.setResizable(false); // Não permite redimensionar
        window.setLocationRelativeTo(null); // Centraliza na tela
        return window;
    }

    static boolean runOnEventDispatchThread(Runnable action) {
        if (!EventQueue.isDispatchThread()) {
            SwingUtilities.invokeLater(action);
            return false;
        }

        action.run();
        return true;
    }

    static void startGame() {
        JPanel gamePanel = gamePanelSupplier.get();
        windowDisplayer.accept(gamePanel);
        gamePanel.requestFocusInWindow();
    }

    public static void main(String[] args){
        runOnEventDispatchThread(startupAction);
    }
}