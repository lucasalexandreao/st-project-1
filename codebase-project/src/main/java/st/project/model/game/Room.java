package st.project.model.game;

/**
 * Representa um local em 2D. Agora guarda a grade (mapa) visual da sala.
 */
public class Room {

    // Matriz que representa a sala (0=chão, 1=parede, 2=chave, 3=saída trancada, 4=saída aberta)
    private int[][] mapLayout;

    public static final int TILE_FLOOR = 0;
    public static final int TILE_WALL = 1;
    public static final int TILE_KEY = 2;
    public static final int TILE_EXIT_LOCKED = 3;
    public static final int TILE_EXIT_OPEN = 4;

    public Room(int[][] layout) {
        if (layout.length == 0) {
            throw new IllegalArgumentException("A room nao pode ter 0 blocos.");
        }

        int totalBlocks = layout.length * layout[0].length;
        if (totalBlocks < 5) {
            throw new IllegalArgumentException("A room precisa ter ao menos 5 blocos disponíveis");
        }

        int keyCount = 0;
        int lockedExitCount = 0;
        for (int[] row : layout) {
            for (int tile : row) {
                if (tile == TILE_KEY) {
                    keyCount++;
                }
                if (tile == TILE_EXIT_LOCKED) {
                    lockedExitCount++;
                }
            }
        }

        if (keyCount < 3) {
            throw new IllegalArgumentException("A room precisa ter ao menos 3 chaves.");
        }

        if (lockedExitCount != 1) {
            throw new IllegalArgumentException("A room precisa ter exatamente 1 porta de saida trancada.");
        }

        this.mapLayout = layout;
    }

    // Retorna a grade da sala
    public int[][] getMapLayout() {
        return mapLayout;
    }

    // Retorna a largura e altura da sala (em tiles)
    public int getWidth() { return mapLayout[0].length; }
    public int getHeight() { return mapLayout.length; }

    /**
     * Tenta pegar um item na posição (x,y). Retorna qual item foi pego.
     */
    public int takeItemAt(int x, int y) {
        int tile = mapLayout[y][x];
        if (tile == TILE_KEY) {
            mapLayout[y][x] = TILE_FLOOR; // Remove o fragmento e põe chão
            return TILE_KEY;
        }
        return -1; // Nada foi pego
    }

    // Altera a porta de saída de trancada para aberta
    public void openExit(int exitX, int exitY) {
        if (mapLayout[exitY][exitX] == TILE_EXIT_LOCKED) {
            mapLayout[exitY][exitX] = TILE_EXIT_OPEN;
        }
    }
}