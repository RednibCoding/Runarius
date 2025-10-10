import java.time.Duration;

public final class ServerConfig {
    private final int port;
    private final int workerThreads;
    private final Duration tickInterval;
    private final int visibilityRadius;

    private ServerConfig(Builder builder) {
        this.port = builder.port;
        this.workerThreads = builder.workerThreads;
        this.tickInterval = builder.tickInterval;
        this.visibilityRadius = builder.visibilityRadius;
    }

    public int getPort() {
        return port;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public Duration getTickInterval() {
        return tickInterval;
    }

    public int getVisibilityRadius() {
        return visibilityRadius;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int port = 43594;
        private int workerThreads = 10;
        private Duration tickInterval = Duration.ofMillis(640);
        private int visibilityRadius = 16;

        private Builder() {}

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder workerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }

        public Builder tickInterval(Duration tickInterval) {
            this.tickInterval = tickInterval;
            return this;
        }

        public Builder visibilityRadius(int visibilityRadius) {
            this.visibilityRadius = visibilityRadius;
            return this;
        }

        public ServerConfig build() {
            return new ServerConfig(this);
        }
    }
}
