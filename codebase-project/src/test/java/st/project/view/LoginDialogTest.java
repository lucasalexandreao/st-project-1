package st.project.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import st.project.model.user.User;
import st.project.repository.LeaderboardRepository;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginDialogTest {

    private LeaderboardRepository repo;
    private LoginDialog loginDialog;
    private JDialog mockDialog;
    private JTextField usernameField;
    private JPasswordField passwordField;

    @BeforeEach
    void setUp() throws Exception {
        repo = mock(LeaderboardRepository.class);
        loginDialog = new LoginDialog(repo);

        mockDialog = mock(JDialog.class);
        usernameField = new JTextField();
        passwordField = new JPasswordField();

        setField("dialog", mockDialog);
        setField("usernameField", usernameField);
        setField("passwordField", passwordField);
    }

    // [TIPO: UTILITÁRIO] Métodos auxiliares de Reflexão (Reflection) para manipulação de campos e métodos privados.
    private void setField(String fieldName, Object value) throws Exception {
        Field field = LoginDialog.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(loginDialog, value);
    }

    // [TIPO: UTILITÁRIO] Recupera o valor de campos encapsulados durante as asserções.
    private Object getFieldValue(String fieldName) throws Exception {
        Field field = LoginDialog.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(loginDialog);
    }

    // [TIPO: UTILITÁRIO] Invoca métodos privados ignorando o bloqueio de segurança do modificador de acesso.
    private Object invokeMethod(String methodName, Object... args) throws Exception {
        for (Method m : LoginDialog.class.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                m.setAccessible(true);
                return m.invoke(loginDialog, args);
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    // [TIPO: INTEGRAÇÃO DE GUI] Teste de thread assíncrona para simulação de cliques na interface modal sem travamento (Deadlock).
    @Test
    void shouldShowDialogAndTriggerAllButtonLambdas() throws Exception {
        JFrame dummyParent = new JFrame();

        Thread ghostClicker = new Thread(() -> {
            try {
                Thread.sleep(500);

                SwingUtilities.invokeLater(() -> {
                    try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class);
                         MockedStatic<RankingDialog> rankingMock = mockStatic(RankingDialog.class)) {

                        Field loginBtn = LoginDialog.class.getDeclaredField("loginButton");
                        loginBtn.setAccessible(true);
                        ((JButton) loginBtn.get(loginDialog)).doClick();

                        Field createBtn = LoginDialog.class.getDeclaredField("createButton");
                        createBtn.setAccessible(true);
                        ((JButton) createBtn.get(loginDialog)).doClick();

                        Field rankingBtn = LoginDialog.class.getDeclaredField("rankingButton");
                        rankingBtn.setAccessible(true);
                        ((JButton) rankingBtn.get(loginDialog)).doClick();

                        Field dialogField = LoginDialog.class.getDeclaredField("dialog");
                        dialogField.setAccessible(true);
                        ((JDialog) dialogField.get(loginDialog)).dispose();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            } catch (InterruptedException ignored) {}
        });

        ghostClicker.start();
        loginDialog.show(dummyParent);

        assertNull(getFieldValue("result"));
        dummyParent.dispose();
    }

    // [TIPO: LÓGICA E FRONTEIRA] Valida o bloqueio de autenticação para entradas vazias com Curto-Circuito (Short-Circuit) lógico.
    @Test
    void shouldBlockLoginWithEmptyFields() throws Exception {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {
            usernameField.setText("");
            passwordField.setText("");
            invokeMethod("handleLogin");

            usernameField.setText("lucas");
            passwordField.setText("");
            invokeMethod("handleLogin");

            usernameField.setText("");
            passwordField.setText("123");
            invokeMethod("handleLogin");

            optionMock.verify(() -> JOptionPane.showMessageDialog(eq(mockDialog), anyString(), eq("Erro"), eq(JOptionPane.WARNING_MESSAGE)), times(3));
        }
    }

    // [TIPO: LÓGICA DE NEGÓCIO] Garante o bloqueio e a emissão do alerta de falha quando o repositório não localiza a entidade na base.
    @Test
    void shouldBlockLoginWhenUserNotFound() throws Exception {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {
            usernameField.setText("fantasma");
            passwordField.setText("123");
            when(repo.getUser("fantasma")).thenReturn(null);

            invokeMethod("handleLogin");

            optionMock.verify(() -> JOptionPane.showMessageDialog(eq(mockDialog), eq("Usuário não encontrado."), eq("Erro"), eq(JOptionPane.WARNING_MESSAGE)));
        }
    }

    // [TIPO: LÓGICA DE SEGURANÇA] Valida a proteção contra acessos não autorizados por comparação falha de hash de senha.
    @Test
    void shouldBlockLoginWithWrongPassword() throws Exception {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {
            usernameField.setText("admin");
            passwordField.setText("senha_errada");

            User mockUser = new User("admin", "hash_certo", 0, 0, false);
            when(repo.getUser("admin")).thenReturn(mockUser);

            invokeMethod("handleLogin");

            optionMock.verify(() -> JOptionPane.showMessageDialog(eq(mockDialog), eq("Senha incorreta."), eq("Erro"), eq(JOptionPane.WARNING_MESSAGE)));
        }
    }

    // [TIPO: FLUXO FELIZ E ESTADO] Testa o sucesso da autenticação e a gravação do estado (result) antes da destruição da interface.
    @Test
    void shouldLoginSuccessfully() throws Exception {
        usernameField.setText("admin");
        passwordField.setText("senha_correta");

        String validHash = (String) invokeMethod("hashPassword", "senha_correta");
        User mockUser = new User("admin", validHash, 0, 0, false);
        when(repo.getUser("admin")).thenReturn(mockUser);

        invokeMethod("handleLogin");

        assertEquals("admin", getFieldValue("result"));
        verify(mockDialog).dispose();
    }

    // [TIPO: LÓGICA E FRONTEIRA] Testa a matriz de exaustão das condicionais de curto-circuito (||) para o bloqueio de criação de usuários inválidos.
    @Test
    void shouldBlockCreateWithEmptyFields() throws Exception {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {
            usernameField.setText("");
            passwordField.setText("");
            invokeMethod("handleCreate");

            usernameField.setText("lucas");
            passwordField.setText("");
            invokeMethod("handleCreate");

            usernameField.setText("");
            passwordField.setText("123");
            invokeMethod("handleCreate");

            optionMock.verify(() -> JOptionPane.showMessageDialog(eq(mockDialog), anyString(), eq("Erro"), eq(JOptionPane.WARNING_MESSAGE)), times(3));
        }
    }

    // [TIPO: INTEGRAÇÃO E LÓGICA] Garante a invocação do repositório para o cadastro e o reset do estado da GUI (campos vazios).
    @Test
    void shouldCreateNewUserSuccessfully() throws Exception {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {
            usernameField.setText("novo_jogador");
            passwordField.setText("12345");
            when(repo.getUser("novo_jogador")).thenReturn(null);

            invokeMethod("handleCreate");

            String expectedHash = (String) invokeMethod("hashPassword", "12345");
            verify(repo).createUser("novo_jogador", expectedHash, false);

            optionMock.verify(() -> JOptionPane.showMessageDialog(eq(mockDialog), eq("Usuário criado com sucesso!"), eq("Sucesso"), eq(JOptionPane.INFORMATION_MESSAGE)));

            assertEquals("", usernameField.getText());
            assertEquals("", new String(passwordField.getPassword()));
        }
    }

    // [TIPO: LÓGICA E ESTRUTURAL] Valida a operação de Upsert (Update) garantindo a preservação dos privilégios de administração originais.
    @Test
    void shouldUpdateExistingUserPasswordSuccessfully() throws Exception {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {

            usernameField.setText("admin_existente");
            passwordField.setText("nova_senha");
            User existingAdmin = new User("admin_existente", "hash_velho", 0, 0, true);
            when(repo.getUser("admin_existente")).thenReturn(existingAdmin);

            invokeMethod("handleCreate");
            verify(repo).createUser("admin_existente", (String) invokeMethod("hashPassword", "nova_senha"), true);

            usernameField.setText("jogador_comum");
            passwordField.setText("outra_senha");
            User existingPlayer = new User("jogador_comum", "hash_velho", 0, 0, false);
            when(repo.getUser("jogador_comum")).thenReturn(existingPlayer);

            invokeMethod("handleCreate");
            verify(repo).createUser("jogador_comum", (String) invokeMethod("hashPassword", "outra_senha"), false);

            optionMock.verify(() -> JOptionPane.showMessageDialog(eq(mockDialog), eq("Senha atualizada com sucesso!"), eq("Sucesso"), eq(JOptionPane.INFORMATION_MESSAGE)), times(2));
        }
    }

    // [TIPO: DOMÍNIO DE STRING] Verifica a correta normalização do identificador (lowercase e remoção de espaços).
    @Test
    void shouldNormalizeUsernameCorrectly() throws Exception {
        assertEquals("", invokeMethod("normalizeUsername", (String) null));
        assertEquals("lucas", invokeMethod("normalizeUsername", "  LUCAS  "));
    }

    // [TIPO: INTEGRAÇÃO E ESTRUTURAL] Testa o acoplamento correto com o Dialog de Classificação (Ranking).
    @Test
    void shouldOpenRankingDialog() throws Exception {
        try (MockedStatic<RankingDialog> rankingMock = mockStatic(RankingDialog.class)) {
            invokeMethod("handleRanking");
            rankingMock.verify(() -> RankingDialog.show(mockDialog, repo));
        }
    }

    // [TIPO: EXCEÇÃO E ALGORITMO] Força uma quebra na fábrica de digestão (SHA-256) para testar a escalada de erros via RuntimeException.
    @Test
    void shouldThrowExceptionWhenHashingFails() {
        try (MockedStatic<MessageDigest> mdMock = mockStatic(MessageDigest.class)) {
            mdMock.when(() -> MessageDigest.getInstance(anyString())).thenThrow(new NoSuchAlgorithmException("Força de Falha no Hash"));

            InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> invokeMethod("hashPassword", "senha_secreta"));

            assertTrue(ex.getCause() instanceof RuntimeException);
            assertEquals("Erro ao fazer hash da senha", ex.getCause().getMessage());
        }
    }
}