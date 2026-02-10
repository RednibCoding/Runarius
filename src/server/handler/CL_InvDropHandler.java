import java.net.Socket;

/**
 * Handles dropping an inventory item.
 * Client sends: [short slotIndex]
 */
public class CL_InvDropHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            int slotIndex = data.getShort() & 0xFFFF;

            PlayerRepository players = ServerContext.get().getPlayers();
            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("InvDrop: player not found for socket");
                return;
            }

            if (slotIndex < 0 || slotIndex >= player.getInventory().size()) {
                Logger.error("InvDrop: invalid slot " + slotIndex + " for " + player.getUsername());
                return;
            }

            Item item = player.getInventory().get(slotIndex);
            int itemId = item.getId();
            boolean wasEquipped = item.isEquipped();

            // Remove from inventory
            player.getInventory().remove(slotIndex);

            // Send removal to client
            PlayerPacketSender.sendInventoryItemRemove(player, slotIndex);

            // If it was equipped, broadcast appearance change
            if (wasEquipped) {
                VisibilityService visibility = ServerContext.get().getVisibilityService();
                ServerContext.get().getPlayers().forEachOnline(viewer -> {
                    if (!visibility.isWithinRange(viewer, player)) return;
                    try {
                        PlayerPacketSender.sendAppearance(viewer, player);
                    } catch (Exception ex) {
                        Logger.error("Failed to send appearance to " + viewer.getUsername());
                    }
                });
            }

            // TODO: Add item to ground at player's position (ground items system)

            Logger.info(player.getUsername() + " dropped item " + itemId + " from slot " + slotIndex);

        } catch (Exception ex) {
            Logger.error("InvDrop error: " + ex.getMessage());
        }
    }
}
