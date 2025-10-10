/**
 * Legacy compatibility facade for pre-refactor code. Prefer using {@link ServerContext}
 * and the dedicated services directly.
 */
@Deprecated
public final class GameWorld {
    private GameWorld() {}

    private static PlayerRepository players() {
        return ServerContext.get().getPlayers();
    }

    private static WorldService world() {
        return ServerContext.get().getWorldService();
    }

    private static VisibilityService visibility() {
        return ServerContext.get().getVisibilityService();
    }

    public static void addPendingPlayer(Player player) {
        players().registerPending(player);
    }

    public static Player getPendingPlayer(long sessionId) {
        return players().consumePending(sessionId).orElse(null);
    }

    public static void addPlayer(Player player) {
        world().spawnPlayer(player);
        players().addPlayer(player);
    }

    public static void removePlayer(Player player) {
        visibility().handlePlayerRemoval(player);
        players().removePlayer(player);
    }

    public static Player getPlayer(String username) {
        return players().findByUsername(username).orElse(null);
    }

    public static Player getPlayerBySessionId(long sessionId) {
        return players().findBySessionId(sessionId).orElse(null);
    }

    public static Player getPlayerBySocket(java.net.Socket socket) {
        return players().findBySocket(socket).orElseGet(() -> players().findPendingBySocket(socket).orElse(null));
    }

    public static java.util.List<Player> getAllPlayers() {
        return new java.util.ArrayList<>(players().getOnlinePlayers());
    }

    public static java.util.List<Player> getNearbyPlayers(int x, int y, int radius) {
        java.util.List<Player> result = new java.util.ArrayList<>();
        for (Player candidate : players().getOnlinePlayers()) {
            int dx = Math.abs(candidate.getX() - x);
            int dy = Math.abs(candidate.getY() - y);
            if (Math.max(dx, dy) <= radius) {
                result.add(candidate);
            }
        }
        return result;
    }

    public static boolean isPlayerOnline(String username) {
        return players().findByUsername(username).isPresent();
    }

    public static boolean isPlayerOnline(long usernameHash) {
        for (Player player : players().getOnlinePlayers()) {
            if (player.getUsernameHash() == usernameHash) {
                return true;
            }
        }
        return false;
    }

    public static int getPlaneWidth() {
        return world().getWorldWidth();
    }

    public static int getPlaneHeight() {
        return world().getWorldHeight();
    }

    public static int getPlaneMultiplier() {
        return 1;
    }
}
