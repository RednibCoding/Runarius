import java.io.IOException;
import java.net.Socket;

/**
 * Handles unequipping an inventory item.
 * Client sends: [short slotIndex]
 */
public class CL_InvUnequipHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            int slotIndex = data.getShort() & 0xFFFF;

            PlayerRepository players = ServerContext.get().getPlayers();
            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("InvUnequip: player not found for socket");
                return;
            }

            if (slotIndex < 0 || slotIndex >= player.getInventory().size()) {
                Logger.error("InvUnequip: invalid slot " + slotIndex + " for " + player.getUsername());
                return;
            }

            Item item = player.getInventory().get(slotIndex);

            if (!item.isEquipped()) {
                Logger.debug("InvUnequip: item not equipped at slot " + slotIndex);
                return;
            }

            // Unequip the item
            item.setEquipped(false);

            // Send update to client
            PlayerPacketSender.sendInventoryItemUpdate(player, slotIndex);

            // Update equipment bonuses
            PlayerPacketSender.sendEquipmentBonuses(player);

            // Broadcast appearance change to nearby players
            VisibilityService visibility = ServerContext.get().getVisibilityService();
            PlayerRepository allPlayers = ServerContext.get().getPlayers();
            allPlayers.forEachOnline(viewer -> {
                if (!visibility.isWithinRange(viewer, player)) return;
                try {
                    PlayerPacketSender.sendAppearance(viewer, player);
                } catch (IOException ex) {
                    Logger.error("Failed to send appearance to " + viewer.getUsername());
                }
            });

            Logger.info(player.getUsername() + " unequipped item " + item.getId() + " at slot " + slotIndex);

        } catch (Exception ex) {
            Logger.error("InvUnequip error: " + ex.getMessage());
        }
    }
}
