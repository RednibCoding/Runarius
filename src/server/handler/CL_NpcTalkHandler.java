import java.io.IOException;
import java.net.Socket;

/**
 * Handles CL_NPC_TALK packet.
 * Client sends: [short npcServerIndex]
 * Server: looks up NPC, sends dialogue options or a generic greeting.
 */
public class CL_NpcTalkHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            ServerContext context = ServerContext.get();
            PlayerRepository players = context.getPlayers();
            WorldService world = context.getWorldService();

            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) return;

            int npcServerIndex = data.getShort() & 0xFFFF;

            Npc npc = world.getNpcByServerId(npcServerIndex);
            if (npc == null) {
                Logger.debug("CL_NPC_TALK: NPC not found, serverIndex=" + npcServerIndex);
                return;
            }

            // Check if NPC is in combat
            if (npc.isInCombat()) {
                PlayerPacketSender.sendMessage(player, "The NPC is busy");
                return;
            }

            // Check if player is in combat
            if (player.isInCombat()) {
                PlayerPacketSender.sendMessage(player, "You can't do that while in combat");
                return;
            }

            NpcDefinition def = world.getNpcDefinition(npc.getTypeId());
            String npcName = (def != null) ? def.getName() : "NPC #" + npc.getTypeId();

            Logger.debug("CL_NPC_TALK: " + player.getUsername() + " talks to " + npcName +
                         " (serverIndex=" + npcServerIndex + ")");

            // Store conversation target
            player.setInteractingNpc(npc);

            // Send dialogue options based on NPC type
            // For now, send generic greeting dialogue
            String[] options;
            if (def != null && def.getCommand() != null && !def.getCommand().isEmpty()) {
                // NPCs with a special command get contextual dialogue
                options = new String[] {
                    "Hello, who are you?",
                    "Can you help me?",
                    "Goodbye"
                };
            } else {
                options = new String[] {
                    "Hello",
                    "Goodbye"
                };
            }

            // Send NPC chat bubble to nearby players first
            String greeting = "Hello, what can I do for you?";
            PlayerPacketSender.sendNpcChat(player, npc, greeting);

            // Send option list to the talking player
            PlayerPacketSender.sendOptionList(player, options);

        } catch (IOException ex) {
            Logger.error("CL_NPC_TALK error: " + ex.getMessage());
        }
    }
}
