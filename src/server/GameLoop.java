import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public final class GameLoop {
    private final ServerContext context;
    private final Timer timer;
    private long tickCount = 0;

    public GameLoop(ServerContext context) {
        this.context = context;
        this.timer = new Timer("GameLoop", true);
    }

    public void start() {
        long interval = context.getConfig().getTickInterval().toMillis();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    tick();
                } catch (Exception ex) {
                    Logger.error("GameLoop tick failed: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }, interval, interval);
        Logger.info("GameLoop started (interval=" + interval + "ms)");
    }

    public void stop() {
        timer.cancel();
        Logger.info("GameLoop stopped");
    }

    private void tick() throws Exception {
        tickCount++;
        Logger.debug("=== TICK " + tickCount + " START ===");

        processNpcs();
        processPlayers();

        Logger.debug("=== TICK " + tickCount + " END ===");
    }

    /**
     * Process all NPC AI: random walking within bounds.
     */
    private void processNpcs() {
        List<Npc> npcs = context.getWorldService().getNpcs();
        for (Npc npc : npcs) {
            npc.tryRandomWalk();
        }
    }

    private void processPlayers() throws Exception {
        List<Player> snapshot = new ArrayList<>(context.getPlayers().getOnlinePlayers());
        VisibilityService visibility = context.getVisibilityService();

        for (Player player : snapshot) {
            if (player != null) {
                visibility.refreshVisibility(player);
            }
        }

        for (Player player : snapshot) {
            if (player == null) {
                continue;
            }

            if (processMovement(player)) {
                visibility.recordMovement(player);
                visibility.establishMutualVisibility(player);
            }
        }

        for (Player player : snapshot) {
            if (player == null) {
                continue;
            }

            try {
                CL_WalkHandler.sendRegionPlayersUpdate(player);
            } catch (Exception ex) {
                Logger.error("Region update failed for " + player.getUsername() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        // Send NPC region updates to all players
        for (Player player : snapshot) {
            if (player == null) {
                continue;
            }

            try {
                PlayerPacketSender.sendRegionNpcs(player);
            } catch (Exception ex) {
                Logger.error("NPC region update failed for " + player.getUsername() + ": " + ex.getMessage());
            }
        }
    }

    private boolean processMovement(Player player) {
        if (!player.hasWalkSteps()) {
            player.setWalking(false);
            return false;
        }

        int[] step = player.getWalkQueue().poll();
        if (step == null) {
            player.setWalking(false);
            return false;
        }

        int deltaX = step[0];
        int deltaY = step[1];
        int oldX = player.getX();
        int oldY = player.getY();

    player.setX(oldX + deltaX);
    player.setY(oldY + deltaY);
    int direction = calculateDirection(deltaX, deltaY);
    player.setDirection(direction);
    player.setWalking(true);

        Logger.debug("Player " + player.getUsername() + " walked: (" + oldX + "," + oldY + ") -> (" + player.getX() + "," + player.getY() + ") dir=" + direction);
        return true;
    }

    private int calculateDirection(int deltaX, int deltaY) {
        if (deltaX == 0 && deltaY < 0) return 0;
        if (deltaX > 0 && deltaY < 0) return 1;
        if (deltaX > 0 && deltaY == 0) return 2;
        if (deltaX > 0 && deltaY > 0) return 3;
        if (deltaX == 0 && deltaY > 0) return 4;
        if (deltaX < 0 && deltaY > 0) return 5;
        if (deltaX < 0 && deltaY == 0) return 6;
        if (deltaX < 0 && deltaY < 0) return 7;
        return 0;
    }

    public long getTickCount() {
        return tickCount;
    }
}
