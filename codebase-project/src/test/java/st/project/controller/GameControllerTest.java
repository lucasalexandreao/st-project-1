package st.project.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st.project.model.game.*;
import st.project.model.user.LeaderboardEntry;
import st.project.model.user.User;
import st.project.repository.LeaderboardRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameControllerTest {

    // DUBLÊ DE TESTE: FAKE
    // Simulamos o banco de dados na memória RAM para não usar o SQLite nos testes unitários
    private static class FakeLeaderboardRepository implements LeaderboardRepository {
        public List<LeaderboardEntry> entries = new ArrayList<>();
        public String lastSavedPlayer = null;

        @Override
        public void saveScore(String playerName, long completionMillis) {
            entries.add(new LeaderboardEntry(playerName, completionMillis, "agora"));
        }

        @Override
        public void saveCurrentPlayer(String playerName) {
            this.lastSavedPlayer = playerName;
        }

        @Override
        public List<LeaderboardEntry> getTopScores(int limit) { return entries; }

        @Override
        public void createUser(String playerName, String passwordHash, boolean isSuperuser) {}

        @Override
        public void deleteUser(String playerName) {}

        @Override
        public User getUser(String playerName) { return null; }

        @Override
        public List<User> getTopUsersByScore(int limit) { return new ArrayList<>(); }

        @Override
        public List<User> getTopUsersBySessions(int limit) { return new ArrayList<>(); }
    }

    private FakeLeaderboardRepository fakeRepo;
    private GameController controller;
    private GameState state;

    @BeforeEach
    void setUp() {
        fakeRepo = new FakeLeaderboardRepository();

        // Usamos um mapa 5x5 para facilitar cálculos de física e colisões na maioria dos testes
        int[][] layout = {
                {1, 1, 1, 1, 1},
                {1, 0, 2, 0, 1}, // (1,1)=Chão, (2,1)=Chave, (3,1)=Chão
                {1, 0, 1, 3, 1}, // (1,2)=Chão, (2,2)=Parede, (3,2)=PortaTrancada
                {1, 2, 0, 2, 1}, // (1,3)=Chave, (2,3)=Chão, (3,3)=Chave
                {1, 1, 1, 1, 1}
        };
        Room room = new Room(layout);
        Player player = new Player(1, 1, room);

        state = new GameState(1, player, room);
        controller = new GameController(state, fakeRepo, "HeroiTeste");
    }

    // ---------------------------------------------------------
    // BLOCO 1: INICIALIZAÇÃO E REGRAS DE NEGÓCIO BÁSICAS
    // ---------------------------------------------------------

    // TESTE DE DOMÍNIO E INTEGRAÇÃO (Uso do Fake)
    @Test
    void shouldInitializeCorrectlyAndSavePlayerToFakeRepo() {
        assertEquals("HeroiTeste", fakeRepo.lastSavedPlayer, "O controller não salvou o nome no repositório!");
        assertEquals(1, state.getCurrentLevel());
        assertFalse(state.isGameOver());
        assertTrue(state.getEnemies().isEmpty());
    }

    // TESTE DE FRONTEIRA E ESTRUTURAL
    @Test
    void shouldTriggerGameOverWhenTimeRunsOut() {
        state.setFramesLeft(1);
        controller.updateGameLogic();

        assertEquals(0, state.getFramesLeft());
        assertTrue(state.isGameOver(), "O jogo não acabou quando o tempo zerou!");
        assertTrue(fakeRepo.entries.isEmpty(), "Salvou pontuação mesmo perdendo!");
    }

    // TESTE DE DOMÍNIO
    @Test
    void shouldTriggerGameOverWhenEnemyTouchesPlayer() {
        state.getEnemies().add(new Enemy(1, 1));
        controller.updateGameLogic();
        assertTrue(state.isGameOver(), "O jogador não morreu ao encostar no inimigo!");
    }

    // TESTE DE DOMÍNIO E INTEGRAÇÃO
    @Test
    void shouldAdvanceLevelOrWinWhenEnteringOpenExit() {
        state.getCurrentRoom().getMapLayout()[2][3] = Room.TILE_EXIT_OPEN;
        state.getPlayer().setPosition(2, 2);
        controller.handlePlayerMovement(3, 2);

        assertEquals(2, state.getCurrentLevel());

        state.getCurrentRoom().getMapLayout()[5][14] = Room.TILE_EXIT_OPEN;
        state.getPlayer().setPosition(13, 5);
        controller.handlePlayerMovement(14, 5);

        assertTrue(state.isGameWon(), "O jogo não foi vencido!");
        assertEquals(1, fakeRepo.entries.size(), "A vitória não foi salva no banco de dados!");
    }

    // ---------------------------------------------------------
    // BLOCO 2: MECÂNICAS DE TIRO E COMBATE
    // ---------------------------------------------------------

    // TESTE DE DOMÍNIO
    @Test
    void shouldAllowPlayerToShootAndConsumeAmmo() {
        state.getPlayer().unlockShooting();
        int initialAmmo = state.getPlayer().getAmmo();
        controller.addPlayerProjectile(50, 50, 10, 0);

        assertEquals(initialAmmo - 1, state.getPlayer().getAmmo(), "Não gastou munição!");
        assertEquals(1, state.getProjectiles().size(), "O tiro não foi adicionado ao jogo!");
        assertTrue(state.getProjectiles().get(0).isPlayerOwned());
    }

    // TESTE ESTRUTURAL E DOMÍNIO
    @Test
    void shouldFireEnemyProjectileWhenCooldownIsZero() {
        state.getEnemies().add(new Enemy(1, 3));
        for (int i = 0; i < 60; i++) {
            controller.updateGameLogic();
        }

        assertFalse(state.getProjectiles().isEmpty(), "O inimigo não atirou!");
        assertFalse(state.getProjectiles().get(0).isPlayerOwned(), "O tiro do inimigo foi marcado como do jogador!");
    }

    // TESTE DE DOMÍNIO
    @Test
    void shouldHandlePlayerProjectileHittingEnemy() {
        state.getEnemies().add(new Enemy(3, 3));
        state.getProjectiles().add(new Projectile(140, 140, 0, 0, true));

        controller.updateGameLogic();

        assertTrue(state.getEnemies().isEmpty(), "O inimigo não morreu com o tiro!");
        assertTrue(state.getProjectiles().isEmpty(), "O tiro não foi destruído após acertar o inimigo!");
    }

    // TESTE DE DOMÍNIO
    @Test
    void shouldHandleEnemyProjectileHittingPlayer() {
        state.getProjectiles().add(new Projectile(60, 60, 0, 0, false));
        controller.updateGameLogic();
        assertTrue(state.isGameOver(), "O jogador sobreviveu a um tiro inimigo!");
    }

    // TESTE ESTRUTURAL
    @Test
    void shouldRemoveProjectileWhenItHitsWall() {
        Projectile p = new Projectile(20, 20, 0, 0, true);
        state.getProjectiles().add(p);
        controller.updateGameLogic();
        assertTrue(state.getProjectiles().isEmpty(), "O tiro atravessou a parede!");
    }

    // ---------------------------------------------------------
    // BLOCO 3: MOVIMENTO, ITENS E LIMITES DO MAPA
    // ---------------------------------------------------------

    // TESTE DE FRONTEIRA E ESTRUTURAL
    @Test
    void shouldRemoveProjectileOutOfBounds() {
        state.getProjectiles().add(new Projectile(-50, 60, 0, 0, true)); // Saiu Esquerda
        state.getProjectiles().add(new Projectile(250, 60, 0, 0, true)); // Saiu Direita
        state.getProjectiles().add(new Projectile(60, -50, 0, 0, true)); // Saiu Cima
        state.getProjectiles().add(new Projectile(60, 250, 0, 0, true)); // Saiu Baixo

        controller.updateGameLogic();

        assertTrue(state.getProjectiles().isEmpty(), "Os tiros fora do mapa não foram removidos!");
    }

    // TESTE ESTRUTURAL E DOMÍNIO
    @Test
    void shouldCollectKeysAndOpenExitAutomatically() {
        controller.loadLevel1();
        state.getCurrentRoom().getMapLayout()[1][2] = Room.TILE_KEY;
        state.getPlayer().collectKey();
        state.getPlayer().collectKey();

        controller.handlePlayerMovement(2, 1);

        assertEquals(3, state.getPlayer().getKeyCount());
        assertEquals(Room.TILE_EXIT_OPEN, state.getCurrentRoom().getMapLayout()[5][14], "A saída não abriu!");
    }

    // ---------------------------------------------------------
    // BLOCO 4: ESTRUTURA E EXAUSTÃO DE CAMINHOS LOGICOS (||)
    // ---------------------------------------------------------

    // TESTE ESTRUTURAL (Cobre TODAS as combinações lógicas de colisão: || de TILE_WALL e TILE_EXIT_LOCKED)
    @Test
    void shouldProperlyEvaluateWallAndLockedExitConditions() {
        state.getPlayer().setPosition(1, 1); // Chão

        // 1. Move Direita para (2,1) -> É uma Chave. A condição (Wall || Locked) dá (FALSE || FALSE). Ele deve andar!
        controller.handlePlayerMovement(2, 1);
        assertEquals(2, state.getPlayer().getGridX());

        // 2. Move Baixo para (2,2) -> É uma Parede! A condição dá (TRUE || NãoLido). Ele é bloqueado!
        controller.handlePlayerMovement(2, 2);
        assertEquals(1, state.getPlayer().getGridY(), "O jogador atravessou a parede!");

        // 3. Move Direita para (3,1) -> É Chão. (FALSE || FALSE).
        controller.handlePlayerMovement(3, 1);

        // 4. Move Baixo para (3,2) -> É a PORTA TRANCADA! Condição (FALSE || TRUE). Ele é bloqueado!
        controller.handlePlayerMovement(3, 2);
        assertEquals(1, state.getPlayer().getGridY(), "O jogador atravessou a porta trancada!");
    }

    // TESTE ESTRUTURAL (Exaustivo de Movimentos)
    @Test
    void exhaustivePlayerMovementDirections() {
        state.getPlayer().setPosition(1, 2); // Chão livre
        controller.handlePlayerMovement(1, 1); // Move CIMA
        assertEquals(1, state.getPlayer().getGridY());

        state.getPlayer().setPosition(1, 1);
        controller.handlePlayerMovement(1, 2); // Move BAIXO
        assertEquals(2, state.getPlayer().getGridY());

        state.getPlayer().setPosition(3, 1);
        controller.handlePlayerMovement(2, 1); // Move ESQUERDA
        assertEquals(2, state.getPlayer().getGridX());

        state.getPlayer().setPosition(1, 1);
        controller.handlePlayerMovement(2, 1); // Move DIREITA
        assertEquals(2, state.getPlayer().getGridX());

        state.getPlayer().setPosition(1, 1);
        controller.handlePlayerMovement(1, 1); // NÃO MOVE
        assertEquals(1, state.getPlayer().getGridX());
        assertEquals(1, state.getPlayer().getGridY());
    }

    // ---------------------------------------------------------
    // BLOCO 5: COBERTURA MC/DC EXTREMA E REFLECTION PARA CÓDIGO INALCANÇÁVEL
    // ---------------------------------------------------------

    // TESTE ESTRUTURAL / MC/DC (Colisão parcial do tiro do JOGADOR)
    @Test
    void shouldMissEnemyIfCoordinatesOnlyPartiallyMatch() {
        state.getEnemies().add(new Enemy(1, 3)); // Pixel 60, 140

        // Bate X (60), Erra Y (100) -> Mata o amarelo da segunda condição do &&
        state.getProjectiles().add(new Projectile(60, 100, 0, 0, true));
        // Erra X (100), Bate Y (140)
        state.getProjectiles().add(new Projectile(100, 140, 0, 0, true));

        controller.updateGameLogic();

        assertEquals(1, state.getEnemies().size());
        assertEquals(2, state.getProjectiles().size());
    }

    // TESTE ESTRUTURAL / MC/DC (Colisão parcial do tiro do INIMIGO)
    @Test
    void shouldMissPlayerIfEnemyProjectileCoordinatesOnlyPartiallyMatch() {
        state.getPlayer().setPosition(1, 1); // Pixel 60, 60

        // Bate X (60), Erra Y (100) -> Mata o amarelo da segunda condição do && (Player)
        state.getProjectiles().add(new Projectile(60, 100, 0, 0, false));
        // Erra X (100), Bate Y (60)
        state.getProjectiles().add(new Projectile(100, 60, 0, 0, false));

        controller.updateGameLogic();

        assertFalse(state.isGameOver(), "Morreu para tiro de raspão!");
    }

    // TESTE ESTRUTURAL E FRONTEIRA EXTREMA (Reflection no return false e no && de limites)
    @Test
    void shouldCoverUnreachableReturnFalseInWallCheck() throws Exception {
        // O isProjectileInWall NUNCA recebe tiro fora da tela em runtime normal.
        java.lang.reflect.Method inWallMethod = GameController.class.getDeclaredMethod("isProjectileInWall", Projectile.class);
        inWallMethod.setAccessible(true);

        // Cobrindo todas as 4 quebras lógicas do && (X < 0, X > width, Y < 0, Y > height)
        boolean w1 = (boolean) inWallMethod.invoke(controller, new Projectile(-50, 60, 0, 0, true)); // Falha em gridX >= 0
        boolean w2 = (boolean) inWallMethod.invoke(controller, new Projectile(250, 60, 0, 0, true)); // Falha em gridX < width
        boolean w3 = (boolean) inWallMethod.invoke(controller, new Projectile(60, -50, 0, 0, true)); // Falha em gridY >= 0
        boolean w4 = (boolean) inWallMethod.invoke(controller, new Projectile(60, 250, 0, 0, true)); // Falha em gridY < height

        // Todas devem chegar ao "return false" vermelho final
        assertFalse(w1);
        assertFalse(w2);
        assertFalse(w3);
        assertFalse(w4);
    }

    // TESTE ESTRUTURAL (isGameFinished com GameWon = true)
    @Test
    void shouldReturnEarlyFromMethodsIfGameIsWon() {
        state.setGameWon(true);
        state.setGameOver(false);

        controller.updateGameLogic();
        controller.handlePlayerMovement(1, 2);
        controller.addPlayerProjectile(60, 60, 10, 0);

        assertEquals(1, state.getPlayer().getGridY());
        assertTrue(state.getProjectiles().isEmpty());
    }

    // TESTE ESTRUTURAL (isGameFinished com GameOver = true)
    @Test
    void shouldReturnEarlyFromMethodsIfGameOver() {
        state.setGameOver(true);
        state.setGameWon(false);

        controller.updateGameLogic();
        controller.handlePlayerMovement(1, 2);
        controller.addPlayerProjectile(60, 60, 10, 0);

        assertEquals(1, state.getPlayer().getGridY());
        assertTrue(state.getProjectiles().isEmpty());
    }

    // TESTE ESTRUTURAL (Early Return do ScoreSaved)
    @Test
    void shouldReturnEarlyIfScoreIsAlreadySaved() {
        try {
            java.lang.reflect.Method method = GameController.class.getDeclaredMethod("finishGame", boolean.class);
            method.setAccessible(true);

            method.invoke(controller, true); // Salva a 1ª vez
            method.invoke(controller, true); // Aciona o return early amarelo

            assertEquals(1, fakeRepo.entries.size());
        } catch (Exception e) {
            fail("Erro no Reflection: " + e.getMessage());
        }
    }

    // TESTE ESTRUTURAL E DOMÍNIO (Falta de Munição)
    @Test
    void shouldNotShootIfNoAmmo() {
        state.getPlayer().unlockShooting();
        while(state.getPlayer().hasAmmo()) state.getPlayer().decreaseAmmo();

        controller.addPlayerProjectile(60, 60, 10, 0);
        assertTrue(state.getProjectiles().isEmpty());
    }

    // TESTE ESTRUTURAL (Chave sem abrir a porta)
    @Test
    void shouldPickUpKeyWithoutOpeningExit() {
        state.getPlayer().setPosition(1, 1);
        state.getCurrentRoom().getMapLayout()[1][2] = Room.TILE_KEY;

        controller.handlePlayerMovement(2, 1);
        assertEquals(1, state.getPlayer().getKeyCount());
        assertEquals(Room.TILE_EXIT_LOCKED, state.getCurrentRoom().getMapLayout()[2][3]);
    }

    // TESTE ESTRUTURAL (Tiros vagando no vazio)
    @Test
    void shouldNotRemoveProjectileIfItMissesEnemiesAndWalls() {
        state.getEnemies().add(new Enemy(4, 4));
        state.getProjectiles().add(new Projectile(60, 60, 5, 0, true));
        state.getProjectiles().add(new Projectile(60, 60, 5, 0, false));

        controller.updateGameLogic();
        assertEquals(2, state.getProjectiles().size());
    }

    // TESTE ESTRUTURAL (Cobertura de Getters)
    @Test
    void shouldLoadLevel1AndCoverGetters() {
        controller.loadLevel1();
        assertEquals(1, controller.getGameState().getCurrentLevel());
        assertNotNull(controller.getLeaderboard());
        assertTrue(controller.getElapsedMillis() >= 0);
    }
}