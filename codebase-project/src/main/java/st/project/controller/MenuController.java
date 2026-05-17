package st.project.controller;

import javax.swing.*;
import java.awt.*;

public class MenuController {
    private final Runnable onPlayClick;
    private final Runnable onExitClick;

    public MenuController(Runnable onPlayClick, Runnable onExitClick) {
        this.onPlayClick = onPlayClick;
        this.onExitClick = onExitClick;
    }

    public void showMainMenu() {
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
            onPlayClick.run();
        });
        panel.add(playButton);

        panel.add(Box.createVerticalStrut(10));

        JButton exitButton = new JButton("Sair");
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.setPreferredSize(new Dimension(200, 40));
        exitButton.addActionListener(e -> onExitClick.run());
        panel.add(exitButton);

        menuFrame.add(panel);
        menuFrame.pack();
        menuFrame.setVisible(true);
    }
}
