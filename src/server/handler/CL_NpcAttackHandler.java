import java.io.IOException;
import java.net.Socket;

/**
 * Handles CL_NPC_ATTACK packet.
 * Client sends: [short npcServerIndex]
 * Server: initiates combat between player and NPC.
 */
public class CL_NpcAttackHandler implements IPacketHandler {
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
                Logger.debug("CL_NPC_ATTACK: NPC not found, serverIndex=" + npcServerIndex);
                return;
            }

            // Check if player is already in combat
            if (player.isInCombat()) {
                PlayerPacketSender.sendMessage(player, "You are already in combat");
                return;
            }

            // Check if NPC is already in combat
            if (npc.isInCombat()) {
                PlayerPacketSender.sendMessage(player, "Someone else is already fighting that");
                return;
            }

            // Check if NPC is dead
            if (npc.isDead()) {
                PlayerPacketSender.sendMessage(player, "That creature is already dead");
                return;
            }

            // Check if NPC is attackable (has HP)
            NpcDefinition def = world.getNpcDefinition(npc.getTypeId());
            if (def == null || !def.isAttackable()) {
                PlayerPacketSender.sendMessage(player, "You can't attack that");
                return;
            }

            Logger.info(player.getUsername() + " attacks " + def.getName() +
                       " (hp=" + npc.getCurrentHits() + "/" + npc.getMaxHits() + ")");

            // Start combat
            player.setInCombat(true);
            player.setAttackingNpc(npc);
            player.setCombatTimer(3); // First hit on next tick cycle (3-tick combat)

            npc.setInCombat(true);
            npc.setCombatTarget(player);
            npc.setCombatTimer(3);

            // Clear walk queues
            player.clearWalkQueue();

            PlayerPacketSender.sendMessage(player, "You attack the " + def.getName());

        } catch (IOException ex) {
            Logger.error("CL_NPC_ATTACK error: " + ex.getMessage());
        }
    }
}
