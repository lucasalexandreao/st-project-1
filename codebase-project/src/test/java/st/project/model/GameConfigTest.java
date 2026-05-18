package st.project.model;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    // ESTRUTURAL E FRONTEIRA EXTREMA (Código Inalcançável)
    @Test
    void shouldThrowExceptionWhenTryingToInstantiate() throws NoSuchMethodException {
        // Pegamos o construtor privado "escondido" via Reflection
        Constructor<GameConfig> constructor = GameConfig.class.getDeclaredConstructor();

        // Quebramos o cadeado do Java para acessar o que é privado
        constructor.setAccessible(true);

        // Tentamos criar o objeto e o Java vai jogar uma InvocationTargetException
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);

        // Verificamos se a causa original da falha foi exatamente o AssertionError que você programou
        assertTrue(exception.getCause() instanceof AssertionError, "A exceção interna não é um AssertionError!");
        assertEquals("Cannot instantiate GameConfig", exception.getCause().getMessage());
    }

    // DOMÍNIO (Teste de Sanidade/Regras de Negócio)
    @Test
    void constantsShouldHaveExpectedValues() {
        // Garantimos que ninguém alterou os "números mágicos" do jogo por acidente
        assertEquals(40, GameConfig.TILE_SIZE);
        assertEquals(30, GameConfig.FPS);
        assertEquals(30, GameConfig.GAME_DURATION_SECONDS);
        assertEquals(900, GameConfig.GAME_DURATION_FRAMES);
        assertEquals(3, GameConfig.FRAGMENTS_NEEDED);
        assertEquals(10, GameConfig.INITIAL_AMMO);
        assertEquals(60, GameConfig.ENEMY_SHOOT_COOLDOWN_FRAMES);
    }
}