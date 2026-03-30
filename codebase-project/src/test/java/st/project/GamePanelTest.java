package st.project;

import org.junit.jupiter.api.Test;

import javax.swing.Timer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GamePanelTest {

    private GamePanel createPanelWithStoppedTimer() {
        GamePanel panel = new GamePanel();
        stopTimer(panel);
        return panel;
    }

    private void stopTimer(GamePanel panel) {
        Timer timer = (Timer) getField(panel, "gameLoop");
        timer.stop();
    }

    private Object getField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokePrivate(GamePanel panel, String methodName, Class<?>[] argTypes, Object... args) {
        try {
            Method method = GamePanel.class.getDeclaredMethod(methodName, argTypes);
            method.setAccessible(true);
            return method.invoke(panel, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Room createRoomForMovementAndPaint() {
        int[][] layout = {
                {1, 1, 1, 1, 1},
                {1, 0, 2, 0, 1},
                {1, 0, 1, 3, 1},
                {1, 2, 0, 2, 1},
                {1, 1, 1, 1, 1}
        };
        return new Room(layout);
    }

    private Room createLevel1SizedRoom() {
        int[][] layout = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1},
                {1, 0, 1, 2, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1},
                {1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3},
                {1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1},
                {1, 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1},
                {1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };
        return new Room(layout);
    }

    private KeyEvent keyPressedEvent(GamePanel panel, int keyCode) {
        return new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    @SuppressWarnings("unchecked")
    private List<Projectile> projectiles(GamePanel panel) {
        return (List<Projectile>) getField(panel, "projectiles");
    }

    @SuppressWarnings("unchecked")
    private List<Enemy> enemies(GamePanel panel) {
        return (List<Enemy>) getField(panel, "enemies");
    }

    @Test
    void shouldInitializeDefaultStateInConstructor() {
        GamePanel panel = createPanelWithStoppedTimer();

        assertEquals(Color.BLACK, panel.getBackground());
        assertTrue(panel.isFocusable());
        assertEquals(1, panel.getKeyListeners().length);
        assertEquals(1, (int) getField(panel, "currentLevel"));
        assertEquals(900, (int) getField(panel, "framesLeft"));
        assertFalse((boolean) getField(panel, "gameOver"));
        assertFalse((boolean) getField(panel, "gameWon"));
        assertTrue(enemies(panel).isEmpty());
        assertTrue(projectiles(panel).isEmpty());
    }

    @Test
    void shouldLoadLevel2WhenCalledReflectively() {
        GamePanel panel = createPanelWithStoppedTimer();

        invokePrivate(panel, "loadLevel2", new Class<?>[]{});
        Player player = (Player) getField(panel, "player");

        assertEquals(2, (int) getField(panel, "currentLevel"));
        assertEquals(900, (int) getField(panel, "framesLeft"));
        assertTrue(player.canShoot());
        assertEquals(10, player.getAmmo());
        assertEquals(1, player.getGridX());
        assertEquals(1, player.getGridY());
        assertEquals(2, enemies(panel).size());
        assertTrue(projectiles(panel).isEmpty());
    }

    @Test
    void shouldReturnImmediatelyFromUpdateWhenGameAlreadyEnded() {
        GamePanel panel = createPanelWithStoppedTimer();
        setField(panel, "gameOver", true);
        setField(panel, "framesLeft", 10);

        invokePrivate(panel, "updateGameLogic", new Class<?>[]{});

        assertEquals(10, (int) getField(panel, "framesLeft"));

        setField(panel, "gameOver", false);
        setField(panel, "gameWon", true);
        setField(panel, "framesLeft", 11);

        invokePrivate(panel, "updateGameLogic", new Class<?>[]{});

        assertEquals(11, (int) getField(panel, "framesLeft"));
    }

    @Test
    void shouldSetGameOverWhenTimeRunsOut() {
        GamePanel panel = createPanelWithStoppedTimer();
        setField(panel, "framesLeft", 1);

        invokePrivate(panel, "updateGameLogic", new Class<?>[]{});

        assertTrue((boolean) getField(panel, "gameOver"));
        assertEquals(0, (int) getField(panel, "framesLeft"));
    }

    @Test
    void shouldSetGameOverWhenEnemyTouchesPlayer() {
        GamePanel panel = createPanelWithStoppedTimer();
        Room room = createRoomForMovementAndPaint();
        Player player = new Player(1, 1, room);
        List<Enemy> enemies = new ArrayList<>();
        enemies.add(new Enemy(1, 1));

        setField(panel, "currentRoom", room);
        setField(panel, "player", player);
        setField(panel, "enemies", enemies);
        setField(panel, "projectiles", new ArrayList<Projectile>());

        invokePrivate(panel, "updateGameLogic", new Class<?>[]{});

        assertTrue((boolean) getField(panel, "gameOver"));
    }

    @Test
    void shouldCreateEnemyProjectileWhenCooldownAllowsShot() {
        GamePanel panel = createPanelWithStoppedTimer();
        Room room = createRoomForMovementAndPaint();
        Player player = new Player(2, 3, room);
        Enemy enemy = new Enemy(1, 1);
        setField(enemy, "shootCooldown", 0);
        List<Enemy> enemies = new ArrayList<>();
        enemies.add(enemy);

        setField(panel, "currentRoom", room);
        setField(panel, "player", player);
        setField(panel, "enemies", enemies);
        setField(panel, "projectiles", new ArrayList<Projectile>());

        invokePrivate(panel, "updateGameLogic", new Class<?>[]{});

        assertEquals(1, projectiles(panel).size());
        assertFalse(projectiles(panel).get(0).isPlayerOwned());
    }

    @Test
    void shouldRemoveProjectileWhenHitsWallOrLeavesMap() {
        GamePanel panel = createPanelWithStoppedTimer();
        Room room = createRoomForMovementAndPaint();

        setField(panel, "currentRoom", room);
        setField(panel, "player", new Player(1, 1, room));
        setField(panel, "enemies", new ArrayList<Enemy>());

        List<Projectile> projectiles = new ArrayList<>();
        projectiles.add(new Projectile(80, 80, 0, 0, true));
        projectiles.add(new Projectile(-10, 40, 0, 0, true));
        setField(panel, "projectiles", projectiles);

        invokePrivate(panel, "updateGameLogic", new Class<?>[]{});

        assertTrue(projectiles(panel).isEmpty());
    }

    @Test
    void shouldHandleProjectileCollisionsForPlayerAndEnemyOwnedShots() {
        GamePanel panel = createPanelWithStoppedTimer();
        Room room = createRoomForMovementAndPaint();
        Player player = new Player(1, 1, room);
        Enemy enemy = new Enemy(3, 3);

        setField(panel, "currentRoom", room);
        setField(panel, "player", player);
        setField(panel, "enemies", new ArrayList<>(List.of(enemy)));

        List<Projectile> projectiles = new ArrayList<>();
        projectiles.add(new Projectile(140, 140, 0, 0, true));
        projectiles.add(new Projectile(60, 60, 0, 0, false));
        setField(panel, "projectiles", projectiles);

        invokePrivate(panel, "updateGameLogic", new Class<?>[]{});

        assertTrue(enemies(panel).isEmpty());
        assertEquals(1, projectiles(panel).size());
        assertFalse(projectiles(panel).get(0).isPlayerOwned());
        assertTrue((boolean) getField(panel, "gameOver"));
    }

    @Test
    void shouldMovePlayerAndHandleWallLockedExitAndShootOnKeyPress() {
        GamePanel panel = createPanelWithStoppedTimer();
        Room room = createRoomForMovementAndPaint();
        Player player = new Player(1, 1, room);
        player.unlockShooting();

        setField(panel, "currentRoom", room);
        setField(panel, "player", player);
        setField(panel, "enemies", new ArrayList<Enemy>());
        setField(panel, "projectiles", new ArrayList<Projectile>());

        KeyListener input = panel.getKeyListeners()[0];

        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_SPACE));
        assertEquals(1, projectiles(panel).size());
        assertEquals(9, player.getAmmo());

        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_RIGHT));
        assertEquals(2, player.getGridX());
        assertEquals(1, player.getGridY());

        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_DOWN));
        assertEquals(2, player.getGridX());
        assertEquals(1, player.getGridY());

        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_LEFT));
        assertEquals(1, player.getGridX());
        assertEquals(1, player.getGridY());

        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_UP));
        assertEquals(1, player.getGridX());
        assertEquals(1, player.getGridY());

        setField(panel, "gameOver", true);
        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_RIGHT));
        assertEquals(1, player.getGridX());
    }

    @Test
    void shouldCollectKeysOpenExitAndAdvanceOrWin() {
        GamePanel panel = createPanelWithStoppedTimer();

        Room room = createLevel1SizedRoom();
        Player player = new Player(1, 1, room);
        setField(panel, "currentRoom", room);
        setField(panel, "player", player);
        setField(panel, "enemies", new ArrayList<Enemy>());
        setField(panel, "projectiles", new ArrayList<Projectile>());

        KeyListener input = panel.getKeyListeners()[0];

        player.setPosition(4, 3, room);
        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_LEFT));
        player.setPosition(4, 7, room);
        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_LEFT));
        player.setPosition(5, 8, room);
        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_DOWN));

        assertEquals(3, player.getKeyCount());
        assertEquals(Room.TILE_EXIT_OPEN, room.getMapLayout()[5][14]);

        player.setPosition(13, 5, room);
        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_RIGHT));

        assertEquals(2, (int) getField(panel, "currentLevel"));

        setField(panel, "currentLevel", 2);
        Room level2 = (Room) getField(panel, "currentRoom");
        level2.openExit(14, 5);
        Player level2Player = (Player) getField(panel, "player");
        level2Player.setPosition(13, 5, level2);

        input.keyPressed(keyPressedEvent(panel, KeyEvent.VK_RIGHT));

        assertTrue((boolean) getField(panel, "gameWon"));
    }

    @Test
    void shouldPaintAndRenderEndScreensAndAmmoInfo() {
        GamePanel panel = createPanelWithStoppedTimer();
        Room room = createRoomForMovementAndPaint();
        Player player = new Player(1, 1, room);
        player.unlockShooting();

        setField(panel, "currentRoom", room);
        setField(panel, "player", player);
        setField(panel, "enemies", new ArrayList<>(List.of(new Enemy(3, 3))));
        setField(panel, "projectiles", new ArrayList<>(List.of(new Projectile(40, 40, 0, 0, true))));

        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        assertDoesNotThrow(() -> panel.paintComponent(graphics));

        setField(panel, "gameOver", true);
        assertDoesNotThrow(() -> panel.paintComponent(graphics));

        setField(panel, "gameOver", false);
        setField(panel, "gameWon", true);
        assertDoesNotThrow(() -> panel.paintComponent(graphics));
    }

    @Test
    void shouldDrawAllTileTypesAndIgnoreUnknownType() {
        GamePanel panel = createPanelWithStoppedTimer();
        Graphics graphics = mock(Graphics.class);

        invokePrivate(panel, "drawTile", new Class<?>[]{Graphics.class, int.class, int.class, int.class}, graphics, 1, 1, Room.TILE_WALL);
        invokePrivate(panel, "drawTile", new Class<?>[]{Graphics.class, int.class, int.class, int.class}, graphics, 1, 1, Room.TILE_KEY);
        invokePrivate(panel, "drawTile", new Class<?>[]{Graphics.class, int.class, int.class, int.class}, graphics, 1, 1, Room.TILE_EXIT_LOCKED);
        invokePrivate(panel, "drawTile", new Class<?>[]{Graphics.class, int.class, int.class, int.class}, graphics, 1, 1, Room.TILE_EXIT_OPEN);
        invokePrivate(panel, "drawTile", new Class<?>[]{Graphics.class, int.class, int.class, int.class}, graphics, 1, 1, 999);

        verify(graphics).setColor(Color.DARK_GRAY);
        verify(graphics).setColor(Color.YELLOW);
        verify(graphics).setColor(Color.RED);
        verify(graphics).setColor(Color.CYAN);
    }

    @Test
    void shouldAllowTimerActionToExecuteWithoutExceptions() {
        GamePanel panel = createPanelWithStoppedTimer();
        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);

        assertDoesNotThrow(() -> panel.paint(image.getGraphics()));
    }
}