import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ServerContext {
    private static ServerContext instance;

    private final ServerConfig config;
    private final ExecutorService workerPool;
    private final PlayerRepository players;
    private final WorldService worldService;
    private final VisibilityService visibilityService;
    private final PacketDispatcher packetDispatcher;
    private final GameLoop gameLoop;

    private ServerContext(ServerConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.workerPool = Executors.newFixedThreadPool(config.getWorkerThreads());
        this.players = new PlayerRepository();
        this.worldService = new WorldService();
        this.visibilityService = new VisibilityService(config.getVisibilityRadius(), players);
        this.packetDispatcher = new PacketDispatcher();
        this.gameLoop = new GameLoop(this);
    }

    public static synchronized void initialize(ServerConfig config) {
        if (instance != null) {
            throw new IllegalStateException("ServerContext already initialized");
        }
        instance = new ServerContext(config);
    }

    public static ServerContext get() {
        if (instance == null) {
            throw new IllegalStateException("ServerContext not initialized");
        }
        return instance;
    }

    public ServerConfig getConfig() {
        return config;
    }

    public ExecutorService getWorkerPool() {
        return workerPool;
    }

    public PlayerRepository getPlayers() {
        return players;
    }

    public WorldService getWorldService() {
        return worldService;
    }

    public VisibilityService getVisibilityService() {
        return visibilityService;
    }

    public PacketDispatcher getPacketDispatcher() {
        return packetDispatcher;
    }

    public GameLoop getGameLoop() {
        return gameLoop;
    }

    public void shutdown() {
        gameLoop.stop();
        workerPool.shutdown();
    }
}
