public final class WorldService {
    private static final int WORLD_WIDTH = 944;
    private static final int WORLD_HEIGHT = 944;
    private static final int DEFAULT_SPAWN_X = 1400;
    private static final int DEFAULT_SPAWN_Y = 1400;
    private static final int DEFAULT_PLANE = 0;

    public void spawnPlayer(Player player) {
        player.setX(DEFAULT_SPAWN_X);
        player.setY(DEFAULT_SPAWN_Y);
        player.setPlaneIndex(DEFAULT_PLANE);
    }

    public int getWorldWidth() {
        return WORLD_WIDTH;
    }

    public int getWorldHeight() {
        return WORLD_HEIGHT;
    }

    public int getPlaneMultiplier() {
        return 1;
    }
}
