package st.project.view;

import st.project.repository.LeaderboardRepository;
import st.project.model.user.User;
import javax.swing.*;
import java.awt.*;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;

public class LoginDialog {
    private final LeaderboardRepository repository;
    private JDialog dialog;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton createButton;
    private JButton rankingButton;
    private String result;

    public LoginDialog(LeaderboardRepository repository) {
        this.repository = repository;
    }

    public String show(Component parent) {
        dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Login", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Usuário:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        loginButton = new JButton("Entrar");
        createButton = new JButton("Criar");
        rankingButton = new JButton("Ranking");

        loginButton.addActionListener(e -> handleLogin());
        createButton.addActionListener(e -> handleCreate());
        rankingButton.addActionListener(e -> handleRanking());

        buttonPanel.add(loginButton);
        buttonPanel.add(createButton);
        buttonPanel.add(rankingButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);

        return result;
    }

    private void handleLogin() {
        String username = normalizeUsername(usernameField.getText());
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Usuário e senha são obrigatórios.", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = repository.getUser(username);
        if (user == null) {
            JOptionPane.showMessageDialog(dialog, "Usuário não encontrado.", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passwordHash = hashPassword(password);
        if (!passwordHash.equals(user.getPasswordHash())) {
            JOptionPane.showMessageDialog(dialog, "Senha incorreta.", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        result = username;
        dialog.dispose();
    }

    private void handleCreate() {
        String username = normalizeUsername(usernameField.getText());
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Preencha usuário e senha.", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passwordHash = hashPassword(password);
        User existing = repository.getUser(username);
        boolean isSuperuser = existing != null && existing.isSuperuser();

        // Upsert: cria novo usuário ou atualiza senha de usuário existente.
        repository.createUser(username, passwordHash, isSuperuser);
        if (existing == null) {
            JOptionPane.showMessageDialog(dialog, "Usuário criado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(dialog, "Senha atualizada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        }

        usernameField.setText("");
        passwordField.setText("");
    }

    private String normalizeUsername(String rawUsername) {
        if (rawUsername == null) {
            return "";
        }

        return rawUsername.trim().toLowerCase(Locale.ROOT);
    }

    private void handleRanking() {
        RankingDialog.show(dialog, repository);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer hash da senha", e);
        }
    }
}
