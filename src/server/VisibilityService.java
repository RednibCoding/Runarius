import java.util.ArrayList;
import java.util.List;

public final class VisibilityService {
    private final int radius;
    private final PlayerRepository players;

    public VisibilityService(int radius, PlayerRepository players) {
        this.radius = radius;
        this.players = players;
    }

    public void refreshVisibility(Player player) {
        List<Player> knownSnapshot = new ArrayList<>(player.getKnownPlayers());
        for (Player knownPlayer : knownSnapshot) {
            if (!isWithinRange(player, knownPlayer)) {
                player.getRemovedPlayers().add(knownPlayer);
            }
        }

        for (Player candidate : players.getOnlinePlayers()) {
            if (candidate == player) {
                continue;
            }
            if (!player.getKnownPlayers().contains(candidate) && isWithinRange(player, candidate)) {
                player.getAddedPlayers().add(candidate);
            }
        }
    }

    public void establishMutualVisibility(Player player) {
        refreshVisibility(player);
        for (Player other : new ArrayList<>(player.getAddedPlayers())) {
            if (other == player) {
                continue;
            }
            if (!other.getKnownPlayers().contains(player)) {
                other.getAddedPlayers().add(player);
            }
        }
    }

    public void recordMovement(Player moved) {
        players.forEachOnline(observer -> {
            if (observer == moved) {
                return;
            }
            if (!observer.getKnownPlayers().contains(moved)) {
                return;
            }

            if (isWithinRange(observer, moved)) {
                observer.getMovedPlayers().add(moved);
            } else {
                observer.getRemovedPlayers().add(moved);
            }
        });
    }

    public void handlePlayerRemoval(Player departing) {
        players.forEachOnline(player -> {
            player.getKnownPlayers().remove(departing);
            player.getAddedPlayers().remove(departing);
            player.getMovedPlayers().remove(departing);
            player.getRemovedPlayers().remove(departing);
        });
    }

    public boolean isWithinRange(Player a, Player b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        return Math.max(dx, dy) <= radius;
    }
}
