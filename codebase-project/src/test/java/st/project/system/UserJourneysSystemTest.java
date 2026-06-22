package st.project.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st.project.model.user.User;
import st.project.repository.JdbcLeaderboardRepository;
import st.project.view.LoginPO;
import st.project.view.UserManagementPO;

import java.io.File;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class UserJourneysSystemTest {

    private JdbcLeaderboardRepository realRepository;
    private File tempDb;

    @BeforeEach
    void setUpDatabase() throws Exception {
        // Usa o repositório real para teste de sistema
        tempDb = File.createTempFile("system_journeys_", ".db");
        tempDb.deleteOnExit();
        realRepository = new JdbcLeaderboardRepository(tempDb.getAbsolutePath());
    }

    // JOGADOR COMUM (CRIAR CONTA E ACESSAR JOGO)
    @Test
    void newPlayerJourney() {
        LoginPO loginPage = new LoginPO(realRepository);

        // O usuário preenche a tela de login. Como a conta não existe, o sistema a cria e o loga.
        String jogadorAutenticado = loginPage.autenticar("novo_heroi", "senha_forte");

        // O usuário foi validado e passado para o GameFlowManager
        assertEquals("novo_heroi", jogadorAutenticado);

        // Validação de Sistema: O banco de dados foi impactado pela interface
        User userDb = realRepository.getUser("novo_heroi");
        assertNotNull(userDb);
        assertFalse(userDb.isSuperuser(), "Um novo cadastro via login padrão não deve ser admin");
    }

    // SUPERUSUÁRIO (LOGIN + GERENCIAMENTO)
    @Test
    void superuserManagementJourney() {
        // Pré-condição do Sistema: Existe um admin e um jogador ruim no banco de produção
        realRepository.createUser("admin_mestre", hashPassword("admin123"), true);
        realRepository.createUser("jogador_toxico", hashPassword("123"), false);

        // Admin faz login na interface
        LoginPO loginPage = new LoginPO(realRepository);
        String usuarioLogado = loginPage.autenticar("admin_mestre", "admin123");
        assertEquals("admin_mestre", usuarioLogado);

        // Admin acessa a tela de gerenciamento
        UserManagementPO managementPage = new UserManagementPO(realRepository, usuarioLogado);

        // Admin deleta o usuário
        managementPage.deletarUsuario("jogador_toxico");

        // Pós-condição: O usuário indesejado sumiu da plataforma
        assertNull(realRepository.getUser("jogador_toxico"), "A jornada de exclusão falhou, usuário ainda existe.");
        assertNotNull(realRepository.getUser("admin_mestre"), "O admin deve continuar existindo.");
    }

    // SEGURANÇA E BLOQUEIO DE LOGIN ERRADO
    @Test
    void wrongPasswordJourney() {
        // Pré-condição: Usuário legítimo existe
        realRepository.createUser("lucas", hashPassword("senha_correta"), false);

        LoginPO loginPage = new LoginPO(realRepository);

        // Tentativa de login com senha errada
        String resultado = loginPage.autenticar("lucas", "senha_errada");

        // A interface deve bloquear e retornar nulo (não deixando ir para o jogo)
        assertNull(resultado, "A jornada deve bloquear logins com senha inválida.");
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}