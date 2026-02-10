import java.io.IOException;
import java.net.Socket;

/**
 * Handles CL_OBJECT_CMD1 packet (primary object interaction).
 * Client sends: [short worldX] [short worldY]
 * Server: looks up object at coordinates and performs primary action.
 */
public class CL_ObjectCmd1Handler implements IPacketHandler {
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

            Logger.debug("CL_OBJECT_CMD1: " + player.getUsername() + " at (" + worldX + "," + worldY + ")");

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

            Logger.info(player.getUsername() + " interacts with object id=" + obj.id +
                       " at (" + worldX + "," + worldY + ")");

            // Handle specific object types
            // Object IDs are from game data - these are common interactive objects
            handleObjectInteraction(player, obj, worldX, worldY);

        } catch (IOException ex) {
            Logger.error("CL_OBJECT_CMD1 error: " + ex.getMessage());
        }
    }

    private void handleObjectInteraction(Player player, DataLoader.GameObjectData obj, int x, int y)
            throws IOException {
        // Common object interactions:
        // Ladders (id 5, 6): change plane
        // Stairs (id 42, 43, 44): change plane
        // Doors: toggle open/closed
        // Other objects: generic message

        switch (obj.id) {
            case 5:   // Ladder up
            case 6:   // Ladder up (variant)
                // Move player up one plane
                if (player.getPlaneIndex() < 3) {
                    player.setY(player.getY() - 944);
                    PlayerPacketSender.sendMessage(player, "You climb the ladder");
                    Logger.info(player.getUsername() + " climbed ladder up to plane " + player.getPlaneIndex());
                } else {
                    PlayerPacketSender.sendMessage(player, "You can't go any higher");
                }
                break;

            case 4:   // Ladder down
                // Move player down one plane
                if (player.getPlaneIndex() > 0) {
                    player.setY(player.getY() + 944);
                    PlayerPacketSender.sendMessage(player, "You climb down the ladder");
                    Logger.info(player.getUsername() + " climbed ladder down to plane " + player.getPlaneIndex());
                } else {
                    PlayerPacketSender.sendMessage(player, "You can't go any lower");
                }
                break;

            case 42:  // Staircase up
            case 43:  // Staircase up (variant)
            case 44:  // Staircase up (variant)
                if (player.getPlaneIndex() < 3) {
                    player.setY(player.getY() - 944);
                    PlayerPacketSender.sendMessage(player, "You walk up the stairs");
                } else {
                    PlayerPacketSender.sendMessage(player, "You can't go any higher");
                }
                break;

            default:
                // Generic interaction message
                PlayerPacketSender.sendMessage(player, "Nothing interesting happens");
                break;
        }
    }
}
