package st.project.model.game;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    // ESTRUTURAL
    @Test
    void shouldGetAndSetPropertiesCorrectly() {
        // Precisamos de um layout válido (mínimo 5 blocos, 3 chaves e 1 porta) para passar no construtor
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_KEY, Room.TILE_KEY, Room.TILE_EXIT_LOCKED}
        };
        Room room = new Room(layout);
        Player player = new Player(0, 0, room);

        GameState state = new GameState(1, player, room);

        // Nível
        state.setCurrentLevel(3);
        assertEquals(3, state.getCurrentLevel());

        // Tempo
        state.setRunStartMillis(1000L);
        assertEquals(1000L, state.getRunStartMillis());

        state.setElapsedMillis(500L);
        assertEquals(500L, state.getElapsedMillis());

        // Listas
        state.setEnemies(new ArrayList<>());
        assertTrue(state.getEnemies().isEmpty());

        state.setProjectiles(new ArrayList<>());
        assertTrue(state.getProjectiles().isEmpty());

        state.clearEnemies();
        state.clearProjectiles();

        // Quartos e Jogadores
        Room newRoom = new Room(layout);
        state.setCurrentRoom(newRoom);
        assertEquals(newRoom, state.getCurrentRoom());

        Player newPlayer = new Player(0, 0, room);
        state.setPlayer(newPlayer);
        assertEquals(newPlayer, state.getPlayer());

        // Booleans
        state.setGameWon(true);
        assertTrue(state.isGameWon());
        assertTrue(state.isGameFinished()); // gameWon = true faz isGameFinished ser true
    }
}