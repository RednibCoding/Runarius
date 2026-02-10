import java.io.IOException;
import java.net.Socket;

/**
 * Handles CL_GROUNDITEM_TAKE packet.
 * Client sends: [short worldX] [short worldY] [short itemId] [short itemIndex]
 * Server: removes from world, adds to player inventory, sends update.
 */
public class CL_GroundItemTakeHandler implements IPacketHandler {
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
            int itemId = data.getShort() & 0xFFFF;
            int itemIndex = data.getShort() & 0xFFFF; // secondary index (unused for now)

            Logger.debug("CL_GROUNDITEM_TAKE: player=" + player.getUsername() +
                         " item=" + itemId + " at (" + worldX + "," + worldY + ")");

            // Check if player is in combat
            if (player.isInCombat()) {
                PlayerPacketSender.sendMessage(player, "You can't do that while in combat");
                return;
            }

            // Check inventory space
            if (player.getInventory().size() >= 30) {
                PlayerPacketSender.sendMessage(player, "Your inventory is full");
                return;
            }

            // Try to remove the ground item from the world
            if (!world.removeGroundItem(worldX, worldY, itemId)) {
                PlayerPacketSender.sendMessage(player, "That item is no longer there");
                return;
            }

            // Add item to player's inventory
            player.addItem(itemId, 1);

            // Send inventory update
            PlayerPacketSender.sendFullInventory(player);

            // Get item name for message
            ItemDefinition def = world.getItemDefinition(itemId);
            String itemName = (def != null) ? def.getName() : "item #" + itemId;
            Logger.info(player.getUsername() + " picked up: " + itemName + " at (" + worldX + "," + worldY + ")");

        } catch (IOException ex) {
            Logger.error("CL_GROUNDITEM_TAKE error: " + ex.getMessage());
        }
    }
}
