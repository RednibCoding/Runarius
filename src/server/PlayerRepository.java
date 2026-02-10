import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PlayerRepository {
    private final Map<String, Player> playersByUsername = new ConcurrentHashMap<>();
    private final Map<Long, Player> playersBySession = new ConcurrentHashMap<>();
    private final Map<Long, Player> pendingPlayers = new ConcurrentHashMap<>();
    private volatile int nextServerId = 1;

    public void registerPending(Player player) {
        pendingPlayers.put(player.getSessionId(), player);
        Logger.info("Pending player created: " + player.getUsername() + " (session: " + player.getSessionId() + ")");
    }

    public Optional<Player> consumePending(long sessionId) {
        Player player = pendingPlayers.remove(sessionId);
        return Optional.ofNullable(player);
    }

    public void removePending(Player player) {
        pendingPlayers.remove(player.getSessionId());
    }

    public void addPlayer(Player player) {
        player.setServerId(nextServerId++);
        playersByUsername.put(player.getUsername().toLowerCase(), player);
        playersBySession.put(player.getSessionId(), player);
        Logger.info("Player " + player.getUsername() + " joined the world (ID: " + player.getServerId() + ")");
    }

    public void removePlayer(Player player) {
        playersByUsername.remove(player.getUsername().toLowerCase());
        playersBySession.remove(player.getSessionId());
        Logger.info("Player " + player.getUsername() + " left the world");
    }

    public Optional<Player> findByUsername(String username) {
        return Optional.ofNullable(playersByUsername.get(username.toLowerCase()));
    }

    public Optional<Player> findBySessionId(long sessionId) {
        return Optional.ofNullable(playersBySession.get(sessionId));
    }

    public Optional<Player> findBySocket(Socket socket) {
        return playersByUsername.values().stream()
            .filter(player -> player.getSocket() == socket)
            .findFirst();
    }

    public Optional<Player> findByUsernameHash(long usernameHash) {
        return playersByUsername.values().stream()
            .filter(player -> player.getUsernameHash() == usernameHash)
            .findFirst();
    }

    public Optional<Player> findPendingBySocket(Socket socket) {
        return pendingPlayers.values().stream()
            .filter(player -> player.getSocket() == socket)
            .findFirst();
    }

    public Collection<Player> getOnlinePlayers() {
        return Collections.unmodifiableCollection(playersByUsername.values());
    }

    public void forEachOnline(Consumer<Player> consumer) {
        new ArrayList<>(playersByUsername.values()).forEach(consumer);
    }

    public boolean isOnline(long usernameHash) {
        return playersByUsername.values().stream()
            .anyMatch(player -> player.getUsernameHash() == usernameHash);
    }

    public boolean isOnline(String username) {
        return playersByUsername.containsKey(username.toLowerCase());
    }
}
