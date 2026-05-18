package st.project.model.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LevelTest {

    // ESTRUTURAL E DOMÍNIO
    @Test
    void shouldStoreLevelDataAndEnemySpawns() {
        int[][] layout = {{1, 1}, {1, 1}};
        Level level = new Level(2, layout, 900);

        assertEquals(2, level.getLevelNumber());
        assertArrayEquals(layout, level.getLayout());
        assertEquals(900, level.getDurationFrames());
        assertTrue(level.getEnemySpawns().isEmpty());

        level.addEnemySpawn(5, 5);
        assertEquals(1, level.getEnemySpawns().size());
        assertEquals(5, level.getEnemySpawns().get(0).getGridX());
        assertEquals(5, level.getEnemySpawns().get(0).getGridY());
    }
}