import java.io.IOException;
import java.net.Socket;

/**
 * Handles CL_OBJECT_CMD2 packet (secondary object interaction).
 * Client sends: [short worldX] [short worldY]
 * Server: looks up object at coordinates and performs secondary action.
 */
public class CL_ObjectCmd2Handler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            ServerContext context = ServerContext.get();
            PlayerRepository players = context.getPlayers();
            WorldService world = context.getWorldService();

            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) return;

            int worldX = data.getShort() & 0xFFFF;
            int worldY = data.getShort() & 0xFFFF;

            Logger.debug("CL_OBJECT_CMD2: " + player.getUsername() + " at (" + worldX + "," + worldY + ")");

            // Check if player is in combat
            if (player.isInCombat()) {
                PlayerPacketSender.sendMessage(player, "You can't do that while in combat");
                return;
            }

            DataLoader.GameObjectData obj = world.getObjectAt(worldX, worldY);
            if (obj == null) {
                Logger.debug("No object found at (" + worldX + "," + worldY + ")");
                PlayerPacketSender.sendMessage(player, "Nothing interesting happens");
                return;
            }

            Logger.info(player.getUsername() + " examines object id=" + obj.id +
                       " at (" + worldX + "," + worldY + ")");

            // Secondary actions are typically "examine" or alternate usage
            PlayerPacketSender.sendMessage(player, "Nothing interesting happens");

        } catch (IOException ex) {
            Logger.error("CL_OBJECT_CMD2 error: " + ex.getMessage());
        }
    }
}
