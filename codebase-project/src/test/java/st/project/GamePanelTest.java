package st.project;

import st.project.controller.GameController;
import st.project.model.game.*;
import st.project.repository.LeaderboardRepository;
import st.project.model.user.LeaderboardEntry;
import st.project.view.GamePanelNew;
import org.junit.jupiter.api.Test;

import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GamePanelTest {

    private static class InMemoryLeaderboardRepository implements LeaderboardRepository {
        private final List<LeaderboardEntry> entries = new ArrayList<>();
        private final List<String> currentPlayers = new ArrayList<>();

        @Override
        public void saveScore(String playerName, long completionMillis) {
            entries.add(new LeaderboardEntry(playerName, completionMillis, "test"));
        }

        @Override
        public void saveCurrentPlayer(String playerName) {
            currentPlayers.add(playerName);
        }

        @Override
        public List<LeaderboardEntry> getTopScores(int limit) {
            return entries;
        }

        @Override
        public void createUser(String playerName, String passwordHash, boolean isSuperuser) {}

        @Override
        public void deleteUser(String playerName) {}

        @Override
        public st.project.model.user.User getUser(String playerName) { return null; }

        @Override
        public List<st.project.model.user.User> getTopUsersByScore(int limit) { return new ArrayList<>(); }

        @Override
        public List<st.project.model.user.User> getTopUsersBySessions(int limit) { return new ArrayList<>(); }

        public List<String> getCurrentPlayers() { return currentPlayers; }
    }

    private Room createRoomSmall() {
        int[][] layout = {
                {1,1,1,1,1},
                {1,0,2,0,1},
                {1,0,1,3,1},
                {1,2,0,2,1},
                {1,1,1,1,1}
        };
        return new Room(layout);
    }

    private GameController createControllerWithDefaultState(InMemoryLeaderboardRepository repo) {
        Room room = createRoomSmall();
        Player player = new Player(1,1,room);
        GameState state = new GameState(1, player, room);
        return new GameController(state, repo, "Player");
    }

    private GamePanelNew createPanelAndStopTimer(GameController controller, InMemoryLeaderboardRepository repo) {
        GamePanelNew panel = new GamePanelNew(controller, repo, "Player");
        try {
            java.lang.reflect.Field f = panel.getClass().getDeclaredField("gameLoop");
            f.setAccessible(true);
            Timer t = (Timer) f.get(panel);
            if (t != null) t.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return panel;
    }

    @Test
    void constructorRegistersPlayerAndInitializesState() {
        InMemoryLeaderboardRepository repo = new InMemoryLeaderboardRepository();
        GameController controller = createControllerWithDefaultState(repo);
        GamePanelNew panel = createPanelAndStopTimer(controller, repo);

        assertEquals(Color.BLACK, panel.getBackground());
        assertTrue(panel.isFocusable());
        assertEquals(1, controller.getGameState().getCurrentLevel());
        assertEquals(900, controller.getGameState().getFramesLeft());
        assertFalse(controller.getGameState().isGameOver());
        assertFalse(controller.getGameState().isGameWon());
        assertTrue(controller.getGameState().getEnemies().isEmpty());
        assertTrue(controller.getGameState().getProjectiles().isEmpty());
        assertEquals(1, repo.getCurrentPlayers().size());
    }

    @Test
    void loadLevel2MakesPlayerAbleToShootAndAddsEnemies() {
        InMemoryLeaderboardRepository repo = new InMemoryLeaderboardRepository();
        GameController controller = createControllerWithDefaultState(repo);

        controller.loadLevel2();
        Player p = controller.getGameState().getPlayer();
        assertEquals(2, controller.getGameState().getCurrentLevel());
        assertTrue(p.canShoot());
        assertEquals(2, controller.getGameState().getEnemies().size());
    }

    @Test
    void updateGameLogicStopsWhenFinishedAndHandlesEnemyProjectileCreation() {
        InMemoryLeaderboardRepository repo = new InMemoryLeaderboardRepository();
        GameController controller = createControllerWithDefaultState(repo);

        // enemy shooting
        Room room = createRoomSmall();
        Player player = new Player(2,3,room);
        Enemy enemy = new Enemy(1,1);
        try { java.lang.reflect.Field sc = enemy.getClass().getDeclaredField("shootCooldown"); sc.setAccessible(true); sc.set(enemy, 0); } catch(Exception ignored){}

        controller.getGameState().setPlayer(player);
        controller.getGameState().setCurrentRoom(room);
        controller.getGameState().setEnemies(new ArrayList<>(List.of(enemy)));
        controller.getGameState().setProjectiles(new ArrayList<>());

        controller.updateGameLogic();
        assertEquals(1, controller.getGameState().getProjectiles().size());
        assertFalse(controller.getGameState().getProjectiles().get(0).isPlayerOwned());
    }

    @Test
    void addingPlayerProjectileConsumesAmmoAndProjectileAppears() {
        InMemoryLeaderboardRepository repo = new InMemoryLeaderboardRepository();
        GameController controller = createControllerWithDefaultState(repo);

        controller.loadLevel2();
        Player p = controller.getGameState().getPlayer();
        p.unlockShooting();
        int before = p.getAmmo();
        controller.addPlayerProjectile(p.getGridX()*40+20, p.getGridY()*40+20, 12, 0);

        assertEquals(before-1, p.getAmmo());
        assertEquals(1, controller.getGameState().getProjectiles().size());
        assertTrue(controller.getGameState().getProjectiles().get(0).isPlayerOwned());
    }

    @Test
    void paintComponentDoesNotThrow() {
        InMemoryLeaderboardRepository repo = new InMemoryLeaderboardRepository();
        GameController controller = createControllerWithDefaultState(repo);
        GamePanelNew panel = createPanelAndStopTimer(controller, repo);

        BufferedImage img = new BufferedImage(300,300,BufferedImage.TYPE_INT_ARGB);
        assertDoesNotThrow(() -> panel.paint(img.getGraphics()));
    }
}
