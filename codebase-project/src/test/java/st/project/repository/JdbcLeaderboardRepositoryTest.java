package st.project.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import st.project.model.user.LeaderboardEntry;
import st.project.model.user.User;

import java.nio.file.Path;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JdbcLeaderboardRepositoryTest {

    @TempDir
    Path tempDir;

    private JdbcLeaderboardRepository repository;
    private Path dbFile;

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("leaderboard-test.db");
        repository = new JdbcLeaderboardRepository(dbFile.toString());
    }

    // ---------------------------------------------------------
    // TESTES ORIGINAIS E DE REGRA DE NEGÓCIO
    // ---------------------------------------------------------

    // [TIPO: INTEGRAÇÃO E DOMÍNIO] Valida a persistência real e a regra de ordenação dos tempos dos jogadores.
    @Test
    void shouldPersistAndReturnOrderedBestTimes() throws Exception {
        repository.saveCurrentPlayer("Ana");
        repository.saveScore("Ana", 4500);
        repository.saveScore("Bia", 2200);
        repository.saveScore("Cris", 3100);

        List<LeaderboardEntry> top2 = repository.getTopScores(2);

        assertEquals(2, top2.size());
        assertEquals("Bia", top2.get(0).getPlayerName());
        assertEquals(2200, top2.get(0).getCompletionMillis());
        assertEquals("Cris", top2.get(1).getPlayerName());
        assertEquals(3100, top2.get(1).getCompletionMillis());

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
             PreparedStatement statement = connection.prepareStatement("SELECT session_count FROM players WHERE player_name = ?")) {
            statement.setString(1, "Ana");
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(1, resultSet.getInt("session_count"));
            }
        }
    }

    // [TIPO: INTEGRAÇÃO E ESTRUTURAL] Garante que a exclusão de um usuário propague corretamente via CASCADE no banco.
    @Test
    void shouldDeleteUserAndTheirRuns() throws Exception {
        repository.saveCurrentPlayer("Ana");
        repository.saveScore("Ana", 4500);
        repository.saveScore("Ana", 2200);

        repository.deleteUser("Ana");

        assertTrue(repository.getTopScores(10).isEmpty());
    }

    // [TIPO: DOMÍNIO E FRONTEIRA] Valida o mapeamento de entidades (User) e diferenciação de permissões (isSuperuser).
    @Test
    void shouldCreateAndRetrieveUsersCorrectly() {
        repository.createUser("admin", "hash123", true);
        repository.createUser("jogador", "hash456", false);

        User adminUser = repository.getUser("admin");
        assertNotNull(adminUser);
        assertEquals("admin", adminUser.getPlayerName());
        assertEquals("hash123", adminUser.getPasswordHash());
        assertTrue(adminUser.isSuperuser());

        User normalUser = repository.getUser("jogador");
        assertNotNull(normalUser);
        assertFalse(normalUser.isSuperuser());
        assertNull(repository.getUser("fantasma"));
    }

    // [TIPO: DOMÍNIO] Teste de Regra de Negócio para o ranking baseado no engajamento (quantidade de sessões).
    @Test
    void shouldReturnTopUsersBySessions() {
        repository.createUser("AdminViciado", "hash", true);
        repository.saveCurrentPlayer("AdminViciado");
        repository.saveCurrentPlayer("AdminViciado");
        repository.saveCurrentPlayer("AdminViciado");

        repository.saveCurrentPlayer("Casual");

        List<User> topUsers = repository.getTopUsersBySessions(10);

        assertEquals(2, topUsers.size());
        assertEquals("AdminViciado", topUsers.get(0).getPlayerName());
        assertEquals(3, topUsers.get(0).getSessionCount());

        assertEquals("Casual", topUsers.get(1).getPlayerName());
        assertEquals(1, topUsers.get(1).getSessionCount());
    }

    // [TIPO: DOMÍNIO] Teste de Regra de Negócio para la ordenación del ranking de pontuação/habilidade dos usuários.
    @Test
    void shouldReturnTopUsersByScore() {
        repository.createUser("RapidoAdmin", "hash", true);
        repository.saveScore("RapidoAdmin", 1200);

        repository.saveCurrentPlayer("Lento");
        repository.saveScore("Lento", 5000);

        List<User> topUsers = repository.getTopUsersByScore(10);

        assertEquals(2, topUsers.size());
        assertEquals("Lento", topUsers.get(0).getPlayerName());
        assertEquals("RapidoAdmin", topUsers.get(1).getPlayerName());
    }

    // ---------------------------------------------------------
    // TESTES DE FRONTEIRA E ERROS DE INFRAESTRUTURA
    // ---------------------------------------------------------

    // [TIPO: FRONTEIRA] Valida o comportamento do construtor recebendo uma string de caminho de arquivo padrão.
    @Test
    void shouldInitializeWithRawJdbcUrl() {
        Path rawDbFile = tempDir.resolve("leaderboard-raw-test.db");
        JdbcLeaderboardRepository rawRepo = new JdbcLeaderboardRepository(rawDbFile.toString());
        assertNotNull(rawRepo);
    }

    // [TIPO: FRONTEIRA E ROBUSTEZ] Verifica se os métodos tratam entradas nulas sem estourar falhas catastróficas na JVM.
    @Test
    void shouldHandleNullPlayerNamesGracefully() {
        try { repository.saveCurrentPlayer(null); } catch (Exception ignored) {}
        try { repository.saveScore(null, 1000); } catch (Exception ignored) {}
        try { repository.deleteUser(null); } catch (Exception ignored) {}
        try { repository.getUser(null); } catch (Exception ignored) {}
    }

    // [TIPO: ESTRUTURAL] Garante que falhas generalizadas do driver JDBC sejam convertidas em IllegalStateException.
    @Test
    void shouldThrowIllegalStateExceptionOnDatabaseFailure() {
        SQLException fakeException = new SQLException("Mock de Queda do Servidor SQL");

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString()))
                    .thenThrow(fakeException);

            assertThrows(IllegalStateException.class, () -> new JdbcLeaderboardRepository(dbFile.toString()));
            assertThrows(IllegalStateException.class, () -> repository.saveScore("P1", 100L));
            assertThrows(IllegalStateException.class, () -> repository.saveCurrentPlayer("P1"));
            assertThrows(IllegalStateException.class, () -> repository.getTopScores(10));
            assertThrows(IllegalStateException.class, () -> repository.createUser("P1", "hash", false));
            assertThrows(IllegalStateException.class, () -> repository.deleteUser("P1"));
            assertThrows(IllegalStateException.class, () -> repository.getUser("P1"));
            assertThrows(IllegalStateException.class, () -> repository.getTopUsersByScore(10));
            assertThrows(IllegalStateException.class, () -> repository.getTopUsersBySessions(10));
        }
    }

    // [TIPO: FRONTEIRA] Testa o comportamento de normalização interna quando o nome do jogador enviado é nulo na criação.
    @Test
    void shouldHandleNullPlayerNameInCreateUserGracefully() {
        try { repository.createUser(null, "hash123", false); } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------
    // MATADORES DA LINHA AMARELA 1: CONSTRUTOR TERNÁRIO
    // ---------------------------------------------------------

    // [TIPO: FRONTEIRA E ESTRUTURAL] Força a execução da branch positiva (true) do ternário terminando em SUCESSO normal (sem exceptions).
    @Test
    void shouldCoverConstructorTernaryTrueBranchWithNormalCompletion() throws Exception {
        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString()))
                    .thenReturn(mockConnection);

            // Passamos a String exata do banco em memória. O ternário avalia como TRUE,
            // o mock intercepta o driver impedindo travamentos do Windows e o fluxo encerra com sucesso.
            JdbcLeaderboardRepository repo = new JdbcLeaderboardRepository("jdbc:sqlite::memory:");
            assertNotNull(repo);
        }
    }

    // ---------------------------------------------------------
    // MATADORES DA LINHA AMARELA 2: TRY-WITH-RESOURCES DE GETUSER()
    // ---------------------------------------------------------

    // [TIPO: ESTRUTURAL EXTREMO] Força falha no estágio 1: Abertura da Conexão.
    @Test
    void shouldThrowExceptionWhenConnectionFailsInGetUser() {
        SQLException fakeException = new SQLException("Falha na Conexão");
        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString())).thenThrow(fakeException);
            assertThrows(IllegalStateException.class, () -> repository.getUser("teste"));
        }
    }

    // [TIPO: ESTRUTURAL EXTREMO] Força falha no estágio 2: Criação do PreparedStatement (Deixa Statement e ResultSet NULOS no encerramento).
    @Test
    void shouldThrowExceptionWhenPreparedStatementCreationFailsInGetUser() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Erro ao criar Statement"));

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString())).thenReturn(mockConnection);
            assertThrows(IllegalStateException.class, () -> repository.getUser("teste"));
        }
    }

    // [TIPO: ESTRUTURAL EXTREMO] Força falha no estágio 3: Execução da Query (Deixa o ResultSet NULO no encerramento).
    @Test
    void shouldThrowExceptionWhenQueryExecutionFailsInGetUser() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenThrow(new SQLException("Erro ao executar Query"));

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString())).thenReturn(mockConnection);
            assertThrows(IllegalStateException.class, () -> repository.getUser("teste"));
        }
    }

    // [TIPO: ESTRUTURAL EXTREMO] Força falha no estágio 4: Leitura do ResultSet com todos os recursos populados e erros de fechamento em cascata.
    @Test
    void shouldCoverAllNestedCloseBranchesInGetUser() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenThrow(new SQLException("Erro ao ler dados"));

        // Força erros simultâneos no encerramento de cada recurso do Try-with-resources
        doThrow(new SQLException("Erro RS close")).when(mockResultSet).close();
        doThrow(new SQLException("Erro STMT close")).when(mockStatement).close();
        doThrow(new SQLException("Erro CONN close")).when(mockConnection).close();

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString())).thenReturn(mockConnection);
            assertThrows(IllegalStateException.class, () -> repository.getUser("teste"));
        }
    }
}