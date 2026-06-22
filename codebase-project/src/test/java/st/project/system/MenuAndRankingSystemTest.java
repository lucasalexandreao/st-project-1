package st.project.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st.project.controller.MenuController;
import st.project.repository.JdbcLeaderboardRepository;
import st.project.view.MenuPO;
import st.project.view.RankingPO;
import st.project.view.PostGamePO;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class MenuAndRankingSystemTest {

    private JdbcLeaderboardRepository repoReal;
    private File tempDb;

    @BeforeEach
    void setUp() throws Exception {
        // Banco real isolado para o teste de sistema da jornada do Ranking
        tempDb = File.createTempFile("system_menu_ranking_", ".db");
        tempDb.deleteOnExit();
        repoReal = new JdbcLeaderboardRepository(tempDb.getAbsolutePath());
    }

    // NAVEGAÇÃO BÁSICA DO JOGADOR NO MENU
    @Test
    void menuNavigation() {
        if (GraphicsEnvironment.isHeadless()) return;

        AtomicBoolean intentToPlay = new AtomicBoolean(false);
        AtomicBoolean intentToExit = new AtomicBoolean(false);

        MenuController controller = new MenuController(
                () -> intentToPlay.set(true),
                () -> intentToExit.set(true)
        );

        MenuPO menuPage = new MenuPO(controller);

        // O usuário abre o jogo e decide clicar em Jogar
        menuPage.clicarJogar();
        assertTrue(intentToPlay.get(), "A jornada falhou: O clique em 'Jogar' não despachou a transição de estado.");

        // O usuário abre o jogo e decide sair
        menuPage.clicarSair();
        assertTrue(intentToExit.get(), "A jornada falhou: O clique em 'Sair' não despachou o encerramento do app.");
    }

    // VISUALIZAÇÃO DE MELHORES JOGADORES (RANKING)
    @Test
    void leaderboardJourney() throws Exception {
        // 1. Pré-Condição do Sistema: O banco possui registros de jogos anteriores
        repoReal.createUser("flash", "hash_senha", false);

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + tempDb.getAbsolutePath());
             java.sql.PreparedStatement insertScore = conn.prepareStatement("INSERT INTO leaderboard (player_name, completion_millis, played_at) VALUES (?, ?, ?)");
             java.sql.PreparedStatement updateSession = conn.prepareStatement("UPDATE players SET session_count = 1 WHERE player_name = ?")) {

            insertScore.setString(1, "flash");
            insertScore.setLong(2, 15000L); // 15 segundos
            insertScore.setString(3, "agora");
            insertScore.executeUpdate();

            updateSession.setString(1, "flash");
            updateSession.executeUpdate();
        }

        // O usuário abre o sistema na página de Ranking
        RankingPO rankingPage = new RankingPO(repoReal);

        // Extrai as visualizações da UI
        String telaPorTempo = rankingPage.extrairTextoDoRankingPorTempo();
        String telaPorSessoes = rankingPage.extrairTextoDoRankingPorSessoes();

        // O usuário consegue ver o jogador e seus respectivos resultados?
        assertTrue(telaPorTempo.contains("flash"), "O nome do jogador deveria estar visível na aba de Tempo.");
        assertTrue(telaPorTempo.contains("15,00s") || telaPorTempo.contains("15.00s"), "O tempo de 15 segundos deveria ser exibido.");

        assertTrue(telaPorSessoes.contains("flash"), "O nome do jogador deveria estar visível na aba de Sessões.");
        assertTrue(telaPorSessoes.contains("Sessões: 1"), "A quantidade de sessões jogadas deveria aparecer na UI.");
    }

    // DECISÕES DE FIM DE JOGO (POST-GAME)
    @Test
    void postGameJourney() {
        PostGamePO postGamePage = new PostGamePO();
        java.util.List<st.project.model.user.LeaderboardEntry> rankingVazio = java.util.Collections.emptyList();

        // Cenário A: Usuário ganha o jogo e clica em "Jogar Novamente"
        st.project.view.PostGameDialog.PostGameAction decisaoJogar =
                postGamePage.interagirComTelaFinal(null, true, rankingVazio, 45000L, 0);
        assertEquals(st.project.view.PostGameDialog.PostGameAction.PLAY_AGAIN, decisaoJogar,
                "A jornada falhou: O sistema não redirecionou para um novo jogo.");

        // Cenário B: Usuário perde o jogo e clica em "Voltar ao Menu"
        st.project.view.PostGameDialog.PostGameAction decisaoMenu =
                postGamePage.interagirComTelaFinal(null, false, rankingVazio, 12000L, 1);
        assertEquals(st.project.view.PostGameDialog.PostGameAction.RETURN_TO_MENU, decisaoMenu,
                "A jornada falhou: O sistema não redirecionou de volta para o menu inicial.");

        // Cenário C: Usuário desiste e clica em "Fechar"
        st.project.view.PostGameDialog.PostGameAction decisaoSair =
                postGamePage.interagirComTelaFinal(null, true, rankingVazio, 20000L, 2);
        assertEquals(st.project.view.PostGameDialog.PostGameAction.CLOSE, decisaoSair,
                "A jornada falhou: O sistema não executou a rotina de encerramento.");
    }
}