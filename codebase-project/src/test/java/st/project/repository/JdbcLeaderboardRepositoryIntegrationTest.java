package st.project.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st.project.model.user.User;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JdbcLeaderboardRepositoryIntegrationTest {

    private JdbcLeaderboardRepository repository;
    private File tempDbFile;

    @BeforeEach
    void setupCleanDatabase() throws IOException {
        tempDbFile = File.createTempFile("integration_mcdc_test_", ".db");
        tempDbFile.deleteOnExit();

        // Cobertura MC/DC: Avalia a decisão do construtor (rawUrl = false)
        repository = new JdbcLeaderboardRepository(tempDbFile.getAbsolutePath(), false);
    }

    @AfterEach
    void tearDownDatabase() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    // --- COBERTURA MC/DC E ESTRUTURAL ---

    @Test
    void testConstrutorDecisaoRawUrl() {
        // Cobertura MC/DC: Avalia a decisão do construtor com rawUrl = true
        JdbcLeaderboardRepository rawRepo = new JdbcLeaderboardRepository("jdbc:sqlite:" + tempDbFile.getAbsolutePath(), true);
        assertDoesNotThrow(() -> rawRepo.getUser("admin"), "Deve conectar com a URL raw corretamente");
    }

    @Test
    void testTratamentoDeNulosEBrancosNoNomeDoJogador_MCDC() {
        // O método getUser/saveScore possui a decisão: (playerName == null)
        // MC/DC Requer testar: 1) playerName = null, 2) playerName válido mas com espaços

        // Condição 1: Nulo (Avalia TRUE para playerName == null)
        assertNull(repository.getUser(null), "Pesquisar por nulo não deve falhar, deve retornar nulo");

        // Condição 2: Espaços em branco (Avalia FALSE para null, aciona o .trim())
        repository.createUser("  marcos  ", "hash", false);
        User user = repository.getUser("marcos");
        assertNotNull(user, "O repositório deve sanitizar espaços em branco usando trim() antes de persistir e buscar");
    }

    // --- TESTES DE DOMÍNIO E INTEGRAÇÃO DE FLUXO ---

    @Test
    void testCriacaoEBuscaDeUsuario() {
        repository.createUser("lucas", "senha123", true);

        User recuperado = repository.getUser("lucas");
        assertNotNull(recuperado);
        assertEquals("lucas", recuperado.getPlayerName());
        assertEquals("senha123", recuperado.getPasswordHash());
        assertTrue(recuperado.isSuperuser(), "A flag de superusuário deve ser persistida");
    }

    // --- TESTES DE FRONTEIRA E AGREGAÇÕES SQL (Slide 52/53) ---

    @Test
    void testFronteirasDeLimitEOrdenacaoEmAgregacoes() throws Exception {
        repository.createUser("jogador_a", "h", false);
        repository.createUser("jogador_b", "h", false);
        repository.createUser("jogador_c", "h", false);

        // PREPARAÇÃO DO CENÁRIO (Arrange)
        // Injetamos os valores diretamente na tabela 'players' via SQL puro
        // para isolar o teste e avaliar estritamente a query de busca.
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
             java.sql.PreparedStatement stmt = conn.prepareStatement("UPDATE players SET session_count = ? WHERE player_name = ?")) {

            // Força C a ter 2 sessões
            stmt.setInt(1, 2);
            stmt.setString(2, "jogador_c");
            stmt.executeUpdate();

            // Força B a ter 1 sessão
            stmt.setInt(1, 1);
            stmt.setString(2, "jogador_b");
            stmt.executeUpdate();

            // Força A a ter 1 sessão
            stmt.setInt(1, 1);
            stmt.setString(2, "jogador_a");
            stmt.executeUpdate();
        }

        // AÇÃO 1: Fronteira = 0
        List<User> zeroUsers = repository.getTopUsersBySessions(0);
        assertTrue(zeroUsers.isEmpty(), "Fronteira On-Point (0) deve retornar lista vazia");

        // AÇÃO 2: Fronteira exata no meio do empate (Limit = 2)
        List<User> topDois = repository.getTopUsersBySessions(2);
        assertEquals(2, topDois.size());

        // PÓS-CONDIÇÃO: Validando o ORDER BY do SQL
        // 1º Lugar deve ser o C (Tem mais sessões: 2)
        assertEquals("jogador_c", topDois.get(0).getPlayerName(), "C tem mais sessões, deve vir primeiro independente do nome");

        // 2º Lugar: A e B têm 1 sessão cada. O desempate no SQL é "player_name ASC".
        // Portanto, 'jogador_a' deve vencer 'jogador_b' e ocupar a vaga final.
        assertEquals("jogador_a", topDois.get(1).getPlayerName(), "No empate de sessões, a ordem alfabética deve prevalecer");
    }

    @Test
    void testDelecaoDeUsuarioIntegrada() {
        repository.createUser("fantasma", "hash", false);
        assertNotNull(repository.getUser("fantasma"));

        repository.deleteUser("fantasma");
        assertNull(repository.getUser("fantasma"), "O usuário deve ser fisicamente removido do banco");
    }
}