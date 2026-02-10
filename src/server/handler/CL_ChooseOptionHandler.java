import java.io.IOException;
import java.net.Socket;

/**
 * Handles CL_CHOOSE_OPTION packet.
 * Client sends: [byte optionIndex]
 * Server: processes the player's dialogue choice.
 */
public class CL_ChooseOptionHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            ServerContext context = ServerContext.get();
            PlayerRepository players = context.getPlayers();
            WorldService world = context.getWorldService();

            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) return;

            int optionIndex = data.getByte() & 0xFF;

            Logger.debug("CL_CHOOSE_OPTION: " + player.getUsername() + " chose option " + optionIndex);

            // Close the option menu
            PlayerPacketSender.sendOptionListClose(player);

            Npc npc = player.getInteractingNpc();
            if (npc == null) {
                Logger.debug("CL_CHOOSE_OPTION: No interacting NPC for " + player.getUsername());
                return;
            }

            NpcDefinition def = world.getNpcDefinition(npc.getTypeId());
            String npcName = (def != null) ? def.getName() : "NPC";

            // Handle dialogue responses
            // For now, provide generic responses
            switch (optionIndex) {
                case 0: // First option (greeting/hello)
                    String response = "Welcome to RuneScape! How can I help you?";
                    PlayerPacketSender.sendNpcChat(player, npc, response);
                    break;

                case 1: // Second option (help/goodbye)
                    PlayerPacketSender.sendNpcChat(player, npc, "Goodbye, adventurer!");
                    break;

                case 2: // Third option (goodbye)
                    PlayerPacketSender.sendNpcChat(player, npc, "Safe travels!");
                    break;

                default:
                    PlayerPacketSender.sendMessage(player, npcName + " doesn't understand.");
                    break;
            }

            // Clear interaction
            player.setInteractingNpc(null);

        } catch (IOException ex) {
            Logger.error("CL_CHOOSE_OPTION error: " + ex.getMessage());
        }
    }
}
