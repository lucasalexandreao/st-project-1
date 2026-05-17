package st.project.view;

import st.project.repository.LeaderboardRepository;
import st.project.model.user.User;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Locale;

public class UserManagementDialog {
    private final LeaderboardRepository repository;
    private final String superuserName;

    public UserManagementDialog(LeaderboardRepository repository, String superuserName) {
        this.repository = repository;
        this.superuserName = superuserName;
    }

    public void show(Component parent) {
        User currentUser = repository.getUser(superuserName);
        if (currentUser == null || !currentUser.isSuperuser()) {
            JOptionPane.showMessageDialog(parent, "Apenas superusuários podem gerenciar usuários.", "Acesso Negado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Frame owner = null;
        if (parent != null) {
            Window window = SwingUtilities.getWindowAncestor(parent);
            if (window instanceof Frame) {
                owner = (Frame) window;
            }
        }

        JDialog dialog = new JDialog(owner, "Gerenciar Usuários", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);
        dialog.setSize(600, 400);
        if (parent != null) {
            dialog.setLocationRelativeTo(parent);
        } else {
            dialog.setLocationRelativeTo(null);
        }

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Lista de usuários
        JTextArea userList = new JTextArea();
        userList.setEditable(false);
        userList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        refreshUserList(userList);

        JScrollPane scroll = new JScrollPane(userList);
        panel.add(scroll, BorderLayout.CENTER);

        // Botões
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton deleteButton = new JButton("Deletar Usuário");
        JButton closeButton = new JButton("Fechar");

        deleteButton.addActionListener(e -> handleDeleteUser(dialog, userList));
        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void refreshUserList(JTextArea area) {
        List<User> users = repository.getTopUsersBySessions(100);

        StringBuilder sb = new StringBuilder();
        sb.append("LISTA DE USUÁRIOS\n");
        sb.append("=".repeat(70)).append("\n\n");

        if (users.isEmpty()) {
            sb.append("Sem usuários registrados.\n");
        } else {
            sb.append(String.format("%-15s %-20s %-10s\n", "Usuário", "Tipo", "Sessões"));
            sb.append("=".repeat(70)).append("\n");
            for (User user : users) {
                String type = user.isSuperuser() ? "SUPER" : "Normal";
                sb.append(String.format("%-15s %-20s %-10d\n",
                    user.getPlayerName(),
                    type,
                    user.getSessionCount()));
            }
        }

        area.setText(sb.toString());
    }

    private void handleDeleteUser(Component parent, JTextArea userList) {
        String username = JOptionPane.showInputDialog(parent, "Nome do usuário a deletar:", "Deletar Usuário", JOptionPane.QUESTION_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        username = normalizeUsername(username);
        if (username.equals(superuserName)) {
            JOptionPane.showMessageDialog(parent, "Você não pode deletar sua própria conta.", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = repository.getUser(username);
        if (user == null) {
            JOptionPane.showMessageDialog(parent, "Usuário não encontrado.", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(parent,
            "Tem certeza que deseja deletar o usuário '" + username + "'?",
            "Confirmar Deleção",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            repository.deleteUser(username);
            JOptionPane.showMessageDialog(parent, "Usuário deletado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshUserList(userList);
        }
    }

    private String normalizeUsername(String rawUsername) {
        if (rawUsername == null) {
            return "";
        }
        return rawUsername.trim().toLowerCase(Locale.ROOT);
    }
}
