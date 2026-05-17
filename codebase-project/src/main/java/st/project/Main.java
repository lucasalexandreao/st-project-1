package st.project;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Main {

    static Runnable startupAction = Main::showMainMenu;
    static Supplier<String> playerNameSupplier = Main::promptForRequiredPlayerName;
    static Function<String, JPanel> gamePanelSupplier = GamePanel::new;
    static Consumer<JPanel> windowDisplayer = panel -> {
        JFrame gameWindow = createMainWindow(panel);
        gameWindow.setVisible(true);
        gameWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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

    static void showMainMenu() {
        JFrame menuFrame = new JFrame("Labirinto - Menu");
        menuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        menuFrame.setResizable(false);
        menuFrame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Bem-vindo ao Labirinto");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(20));

        JButton playButton = new JButton("Jogar");
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.setPreferredSize(new Dimension(200, 40));
        playButton.addActionListener(e -> {
            menuFrame.dispose();
            startGame();
        });
        panel.add(playButton);

        panel.add(Box.createVerticalStrut(10));

        JButton exitButton = new JButton("Sair");
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.setPreferredSize(new Dimension(200, 40));
        exitButton.addActionListener(e -> System.exit(0));
        panel.add(exitButton);

        menuFrame.add(panel);
        menuFrame.pack();
        menuFrame.setVisible(true);
    }

    static void startGame() {
        String playerName = requirePlayerName(playerNameSupplier.get());
        if (playerName == null) {
            showMainMenu();
            return;
        }

        JPanel gamePanel = gamePanelSupplier.apply(playerName);
        windowDisplayer.accept(gamePanel);
        gamePanel.requestFocusInWindow();
    }

    static String promptForRequiredPlayerName() {
        LeaderboardRepository repository = createDefaultLeaderboardRepository();
        return promptForLogin(repository);
    }

    static String promptForLogin(LeaderboardRepository repository) {
        JFrame tempFrame = new JFrame();
        tempFrame.setVisible(false);
        LoginDialog loginDialog = new LoginDialog(repository);
        String result = loginDialog.show(tempFrame);
        tempFrame.dispose();
        return result;
    }

    static String requirePlayerName(String playerName) {
        if (playerName == null) {
            return null;
        }

        String trimmed = playerName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static LeaderboardRepository createDefaultLeaderboardRepository() {
        try {
            return new JdbcLeaderboardRepository("leaderboard.db");
        } catch (RuntimeException e) {
            return new NoOpLeaderboardRepository();
        }
    }


    public static void main(String[] args){
        runOnEventDispatchThread(startupAction);
    }
}