package st.project.controller;

import st.project.repository.LeaderboardRepository;
import st.project.view.LoginDialog;
import javax.swing.*;

public class AuthController {
    private final LeaderboardRepository repository;

    public AuthController(LeaderboardRepository repository) {
        this.repository = repository;
    }

    public String showLoginDialog(JFrame parent) {
        LoginDialog loginDialog = new LoginDialog(repository);
        return loginDialog.show(parent);
    }

    public String promptForLogin() {
        JFrame tempFrame = new JFrame();
        tempFrame.setVisible(false);
        String result = showLoginDialog(tempFrame);
        tempFrame.dispose();
        return result;
    }

    public String getPlayerNameOrNull(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
