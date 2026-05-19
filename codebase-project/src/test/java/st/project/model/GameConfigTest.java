package st.project.model;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    // ESTRUTURAL E FRONTEIRA
    @Test
    void shouldThrowExceptionWhenTryingToInstantiate() throws NoSuchMethodException {
        Constructor<GameConfig> constructor = GameConfig.class.getDeclaredConstructor();

        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);

        assertTrue(exception.getCause() instanceof AssertionError, "A exceção interna não é um AssertionError!");
        assertEquals("Cannot instantiate GameConfig", exception.getCause().getMessage());
    }

    // DOMÍNIO
    @Test
    void constantsShouldHaveExpectedValues() {
        assertEquals(40, GameConfig.TILE_SIZE);
        assertEquals(30, GameConfig.FPS);
        assertEquals(30, GameConfig.GAME_DURATION_SECONDS);
        assertEquals(900, GameConfig.GAME_DURATION_FRAMES);
        assertEquals(3, GameConfig.FRAGMENTS_NEEDED);
        assertEquals(10, GameConfig.INITIAL_AMMO);
        assertEquals(60, GameConfig.ENEMY_SHOOT_COOLDOWN_FRAMES);
    }
}