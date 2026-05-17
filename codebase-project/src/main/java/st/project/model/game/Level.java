package st.project.model.game;

import java.util.ArrayList;
import java.util.List;

public class Level {
    private final int levelNumber;
    private final int[][] layout;
    private final List<EnemySpawn> enemySpawns;
    private final int durationFrames;

    public Level(int levelNumber, int[][] layout, int durationFrames) {
        this.levelNumber = levelNumber;
        this.layout = layout;
        this.durationFrames = durationFrames;
        this.enemySpawns = new ArrayList<>();
    }

    public int getLevelNumber() { return levelNumber; }
    public int[][] getLayout() { return layout; }
    public int getDurationFrames() { return durationFrames; }
    public List<EnemySpawn> getEnemySpawns() { return enemySpawns; }

    public void addEnemySpawn(int gridX, int gridY) {
        enemySpawns.add(new EnemySpawn(gridX, gridY));
    }

    public static class EnemySpawn {
        private final int gridX;
        private final int gridY;

        public EnemySpawn(int gridX, int gridY) {
            this.gridX = gridX;
            this.gridY = gridY;
        }

        public int getGridX() { return gridX; }
        public int getGridY() { return gridY; }
    }
}
