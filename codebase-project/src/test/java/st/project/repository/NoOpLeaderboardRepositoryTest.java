package st.project.repository;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NoOpLeaderboardRepositoryTest {

    // ESTRUTURAL E DOMÍNIO
    @Test
    void shouldDoNothingAndReturnEmptyLists() {
        NoOpLeaderboardRepository repo = new NoOpLeaderboardRepository();

        // Verifica se os métodos void não estouram erros ao serem chamados
        assertDoesNotThrow(() -> repo.saveScore("Player", 100));
        assertDoesNotThrow(() -> repo.saveCurrentPlayer("Player"));
        assertDoesNotThrow(() -> repo.createUser("Player", "hash", false));
        assertDoesNotThrow(() -> repo.deleteUser("Player"));

        // Verifica se os retornos são nulos ou listas vazias seguras
        assertNull(repo.getUser("Player"));
        assertTrue(repo.getTopScores(10).isEmpty());
        assertTrue(repo.getTopUsersByScore(10).isEmpty());
        assertTrue(repo.getTopUsersBySessions(10).isEmpty());
    }
}