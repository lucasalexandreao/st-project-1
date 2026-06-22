package st.project.view;

import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import st.project.repository.LeaderboardRepository;

import javax.swing.*;
import java.lang.reflect.Field;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

public class LoginPO extends GamePageObject {
    private final LeaderboardRepository repo;
    private boolean ready = false;

    public LoginPO(LeaderboardRepository repo) {
        this.repo = repo;
    }

    @Override
    public void isReady() {
        if (repo == null) throw new IllegalStateException("Repositório não injetado");
        this.ready = true;
    }

    public String autenticar(String user, String pass) {
        isReady();

        try (MockedStatic<JOptionPane> optionPaneMock = mockStatic(JOptionPane.class);
             MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class, (mock, context) -> {
                 doNothing().when(mock).setVisible(anyBoolean());
             })) {

            LoginDialog loginDialog = new LoginDialog(repo);

            JFrame dummyParent = new JFrame();
            loginDialog.show(dummyParent);

            Field uField = LoginDialog.class.getDeclaredField("usernameField");
            uField.setAccessible(true);
            JTextField userText = (JTextField) uField.get(loginDialog);

            Field pField = LoginDialog.class.getDeclaredField("passwordField");
            pField.setAccessible(true);
            JPasswordField passText = (JPasswordField) pField.get(loginDialog);

            userText.setText(user);
            passText.setText(pass);

            if (repo.getUser(user) == null) {
                Field createBtnField = LoginDialog.class.getDeclaredField("createButton");
                createBtnField.setAccessible(true);
                JButton createButton = (JButton) createBtnField.get(loginDialog);

                for (ActionListener al : createButton.getActionListeners()) {
                    al.actionPerformed(new ActionEvent(createButton, ActionEvent.ACTION_PERFORMED, ""));
                }

                userText.setText(user);
                passText.setText(pass);
            }

            Field loginBtnField = LoginDialog.class.getDeclaredField("loginButton");
            loginBtnField.setAccessible(true);
            JButton loginButton = (JButton) loginBtnField.get(loginDialog);

            for (ActionListener al : loginButton.getActionListeners()) {
                al.actionPerformed(new ActionEvent(loginButton, ActionEvent.ACTION_PERFORMED, ""));
            }

            Field resultField = LoginDialog.class.getDeclaredField("result");
            resultField.setAccessible(true);
            return (String) resultField.get(loginDialog);

        } catch (Exception e) {
            throw new RuntimeException("Erro no Page Object de Login", e);
        }
    }
}