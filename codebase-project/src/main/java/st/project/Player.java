package st.project;

public class Player {
    private int gridX;
    private int gridY;
    private int keyFragments;
    public static final int FRAGMENTS_NEEDED = 3;

    private int lastDirX = 1;
    private int lastDirY = 0;
    private boolean canShoot = false;

    private int ammo;

    public Player(int startX, int startY) {
        this(startX, startY, null);
    }

    public Player(int startX, int startY, Room room) {
        if (room != null) {
            validateSpawnPosition(startX, startY, room);
        }

        this.gridX = startX;
        this.gridY = startY;
        this.keyFragments = 0;
        this.ammo = 0; // Começa sem munição
    }

    private void validateSpawnPosition(int x, int y, Room room) {

        if (x < 0 || y < 0 || x >= room.getWidth() || y >= room.getHeight()) {
            throw new IllegalArgumentException("Player spawn precisa estar dentro da tela: x >= 0, y >= 0, x < largura, y < altura.");
        }

        if (room.getMapLayout()[y][x] != Room.TILE_FLOOR) {
            throw new IllegalArgumentException("Player spawn precisa ser em TILE_FLOOR.");
        }

    }

    public void moveUp()    { gridY--; setLastDir(0, -1); }
    public void moveDown()  { gridY++; setLastDir(0, 1); }
    public void moveLeft()  { gridX--; setLastDir(-1, 0); }
    public void moveRight() { gridX++; setLastDir(1, 0); }

    private void setLastDir(int dx, int dy) {
        this.lastDirX = dx;
        this.lastDirY = dy;
    }

    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }
    public void setPosition(int x, int y) { this.gridX = x; this.gridY = y; }
    public void setPosition(int x, int y, Room room) {
        validateSpawnPosition(x, y, room);
        setPosition(x, y);
    }

    public int getKeyCount() { return keyFragments; }
    public void collectKey() { keyFragments++; }
    public void resetKeys() { keyFragments = 0; }
    public boolean hasAllKeys() { return keyFragments >= FRAGMENTS_NEEDED; }

    public int getLastDirX() { return lastDirX; }
    public int getLastDirY() { return lastDirY; }

    public boolean canShoot() { return canShoot; }

    public int getAmmo() { return ammo; }
    public boolean hasAmmo() { return ammo > 0; }
    public void decreaseAmmo() {
        if (ammo > 0) ammo--;
    }

    public void unlockShooting() {
        this.canShoot = true;
        this.ammo = 10;
    }
}