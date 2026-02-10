import java.io.IOException;
import java.net.Socket;

/**
 * Handles server commands sent by players (prefixed with :: in chat).
 * Example: ::pos, ::teleport 130 655
 */
public class CL_CommandHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            String command = data.getString();

            PlayerRepository players = ServerContext.get().getPlayers();
            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("Command: player not found for socket");
                return;
            }

            Logger.info(player.getUsername() + " command: ::" + command);

            String[] parts = command.split(" ");
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "pos":
                    sendMessage(player, "@gre@Position: (" + player.getX() + ", " + player.getY() + ") plane=" + player.getPlaneIndex());
                    break;

                case "teleport":
                case "tp":
                    if (parts.length >= 3) {
                        try {
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            player.clearWalkQueue();
                            player.setX(x);
                            player.setY(y);
                            // Force re-send of region data
                            player.getKnownPlayers().clear();
                            player.getKnownNpcs().clear();
                            CL_WalkHandler.sendRegionPlayersUpdate(player);
                            PlayerPacketSender.sendRegionNpcs(player);
                            PlayerPacketSender.sendRegionObjects(player);
                            PlayerPacketSender.sendRegionWallObjects(player);
                            PlayerPacketSender.sendRegionGroundItems(player);
                            sendMessage(player, "@gre@Teleported to (" + x + ", " + y + ")");
                        } catch (NumberFormatException ex) {
                            sendMessage(player, "@red@Usage: ::teleport x y");
                        }
                    } else {
                        sendMessage(player, "@red@Usage: ::teleport x y");
                    }
                    break;

                case "npccount":
                    int npcCount = ServerContext.get().getWorldService().getNpcs().size();
                    sendMessage(player, "@gre@Total NPCs loaded: " + npcCount);
                    break;

                case "nearby":
                    int radius = ServerContext.get().getConfig().getVisibilityRadius();
                    var nearbyNpcs = ServerContext.get().getWorldService().getNearbyNpcs(player.getX(), player.getY(), radius);
                    sendMessage(player, "@gre@Nearby NPCs: " + nearbyNpcs.size() + " (within " + radius + " tiles)");
                    break;

                default:
                    sendMessage(player, "@red@Unknown command: ::" + cmd);
                    break;
            }

        } catch (Exception ex) {
            Logger.error("Command error: " + ex.getMessage());
        }
    }

    private void sendMessage(Player player, String message) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_MESSAGE.value);
        out.putString(message);

        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
}
