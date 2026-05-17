package st.project.model;

public final class GameConfig {
    public static final int TILE_SIZE = 40;
    public static final int FPS = 30;
    public static final int GAME_DURATION_SECONDS = 30;
    public static final int GAME_DURATION_FRAMES = GAME_DURATION_SECONDS * FPS; // 900
    public static final int FRAGMENTS_NEEDED = 3;
    public static final int INITIAL_AMMO = 10;
    public static final int ENEMY_SHOOT_COOLDOWN_FRAMES = 60; // 2 seconds at 30 FPS

    private GameConfig() {
        throw new AssertionError("Cannot instantiate GameConfig");
    }
}
