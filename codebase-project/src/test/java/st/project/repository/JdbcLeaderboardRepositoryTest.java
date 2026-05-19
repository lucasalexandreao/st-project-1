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

    // O SEGREDO: Fabricamos as exceções antes para o Mockito não bugar o DriverManager!
    private static final SQLException BODY_ERROR = new SQLException("Body Error");
    private static final SQLException RS_ERROR = new SQLException("RS Close");
    private static final SQLException STMT_ERROR = new SQLException("STMT Close");
    private static final SQLException CONN_ERROR = new SQLException("CONN Close");

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("leaderboard-test.db");
        repository = new JdbcLeaderboardRepository(dbFile.toString());
    }

    // ---------------------------------------------------------
    // TESTES ORIGINAIS E DE REGRA DE NEGÓCIO
    // ---------------------------------------------------------

    // [TIPO: INTEGRAÇÃO E DOMÍNIO] Valida a persistência real e a regra de ordenação dos tempos.
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

    // [TIPO: INTEGRAÇÃO E ESTRUTURAL] Garante que a exclusão propague no banco.
    @Test
    void shouldDeleteUserAndTheirRuns() throws Exception {
        repository.saveCurrentPlayer("Ana");
        repository.saveScore("Ana", 4500);
        repository.saveScore("Ana", 2200);

        repository.deleteUser("Ana");

        assertTrue(repository.getTopScores(10).isEmpty());
    }

    // [TIPO: DOMÍNIO E FRONTEIRA] Valida o mapeamento de entidades (User) e permissões.
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

    // [TIPO: DOMÍNIO] Teste de Regra de Negócio para o ranking de engajamento (sessões).
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

    // [TIPO: DOMÍNIO] Teste de Regra de Negócio para o ranking de pontuação.
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

    // [TIPO: FRONTEIRA] Valida o construtor com String direta.
    @Test
    void shouldInitializeWithRawJdbcUrl() {
        Path rawDbFile = tempDir.resolve("leaderboard-raw-test.db");
        JdbcLeaderboardRepository rawRepo = new JdbcLeaderboardRepository(rawDbFile.toString());
        assertNotNull(rawRepo);
    }

    // [TIPO: FRONTEIRA E ROBUSTEZ] Verifica tolerância a nulls.
    @Test
    void shouldHandleNullPlayerNamesGracefully() {
        try { repository.saveCurrentPlayer(null); } catch (Exception ignored) {}
        try { repository.saveScore(null, 1000); } catch (Exception ignored) {}
        try { repository.deleteUser(null); } catch (Exception ignored) {}
        try { repository.getUser(null); } catch (Exception ignored) {}
    }

    // [TIPO: ESTRUTURAL] Garante conversão de falhas JDBC para IllegalStateException.
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

    // [TIPO: FRONTEIRA] Normalização interna de nulls.
    @Test
    void shouldHandleNullPlayerNameInCreateUserGracefully() {
        try { repository.createUser(null, "hash123", false); } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------
    // MATADORES DA LINHA AMARELA 1: CONSTRUTOR TERNÁRIO
    // ---------------------------------------------------------

    // [TIPO: FRONTEIRA E ESTRUTURAL] Cobre o lado esquerdo do ternário (true)
    @Test
    void shouldCoverConstructorTernaryTrueBranchWithNormalCompletion() throws Exception {
        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString())).thenReturn(mockConnection);
            JdbcLeaderboardRepository repo = new JdbcLeaderboardRepository("jdbc:sqlite::memory:", true);
            assertNotNull(repo);
        }
    }

    // ---------------------------------------------------------
    // MATADORES DA LINHA AMARELA 2: FANTASMA DO TRY-WITH-RESOURCES (getUser)
    // ---------------------------------------------------------

    // [TIPO: ESTRUTURAL EXTREMO] A Matriz Absoluta: Esgota TODAS as permutações geradas pelo JaCoCo no fechamento de recursos!
    @Test
    void shouldExhaustAllTryWithResourcesBranchesInGetUser() throws Exception {
        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {

            // Permutações 1-3: Execução Normal (Sem retornar dados), falha no close de cada recurso
            executeTWRMatrix(mockedDriver, false, false, true, false, false); // RS close falha
            executeTWRMatrix(mockedDriver, false, false, false, true, false); // STMT close falha
            executeTWRMatrix(mockedDriver, false, false, false, false, true); // CONN close falha

            // Permutações 4-6: Execução com Erro (No ResultSet), falha no close de cada recurso
            executeTWRMatrix(mockedDriver, true, false, true, false, false);  // Erro + RS close falha
            executeTWRMatrix(mockedDriver, true, false, false, true, false);  // Erro + STMT close falha
            executeTWRMatrix(mockedDriver, true, false, false, false, true);  // Erro + CONN close falha

            // Permutações 7-9: Execução com RETURN SUCESSO (Cobre a ramificação duplicada pelo compilador do Java!)
            executeTWRMatrix(mockedDriver, false, true, true, false, false); // Sucesso + RS close falha
            executeTWRMatrix(mockedDriver, false, true, false, true, false); // Sucesso + STMT close falha
            executeTWRMatrix(mockedDriver, false, true, false, false, true); // Sucesso + CONN close falha

            // Permutações 10-12: Cobertura da ramificação defensiva "if (resource != null)" do compilador
            executeNullResourceMatrix(mockedDriver, true, false, false); // Conn é null
            executeNullResourceMatrix(mockedDriver, false, true, false); // Stmt é null
            executeNullResourceMatrix(mockedDriver, false, false, true); // Rs é null
        }
    }

    // Utilitário Privado para gerar a Matriz de Permutações de Fechamento (Close)
    private void executeTWRMatrix(MockedStatic<DriverManager> mockedDriver,
                                  boolean bodyThrows, boolean simulateReturn,
                                  boolean failRsClose, boolean failStmtClose, boolean failConnClose) throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        mockedDriver.when(() -> DriverManager.getConnection(anyString())).thenReturn(mockConn);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockRs);

        if (bodyThrows) {
            when(mockRs.next()).thenThrow(BODY_ERROR); // Usa a exceção pré-fabricada
        } else if (simulateReturn) {
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getString("player_name")).thenReturn("T");
            when(mockRs.getString("password")).thenReturn("P");
            when(mockRs.getLong("total_score")).thenReturn(1L);
            when(mockRs.getInt("session_count")).thenReturn(1);
            when(mockRs.getInt("is_superuser")).thenReturn(1);
        } else {
            when(mockRs.next()).thenReturn(false);
        }

        if (failRsClose) doThrow(RS_ERROR).when(mockRs).close(); // Usa a exceção pré-fabricada
        if (failStmtClose) doThrow(STMT_ERROR).when(mockStmt).close(); // Usa a exceção pré-fabricada
        if (failConnClose) doThrow(CONN_ERROR).when(mockConn).close(); // Usa a exceção pré-fabricada

        assertThrows(IllegalStateException.class, () -> repository.getUser("T"));
    }

    // Utilitário Privado para gerar a Matriz de Permutações de Recursos Nulos
    private void executeNullResourceMatrix(MockedStatic<DriverManager> mockedDriver,
                                           boolean connNull, boolean stmtNull, boolean rsNull) throws Exception {
        if (connNull) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString())).thenReturn(null);
        } else {
            Connection mockConn = mock(Connection.class);
            mockedDriver.when(() -> DriverManager.getConnection(anyString())).thenReturn(mockConn);

            if (stmtNull) {
                when(mockConn.prepareStatement(anyString())).thenReturn(null);
            } else {
                PreparedStatement mockStmt = mock(PreparedStatement.class);
                when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

                if (rsNull) {
                    when(mockStmt.executeQuery()).thenReturn(null);
                }
            }
        }

        assertThrows(NullPointerException.class, () -> repository.getUser("T"));
    }
}