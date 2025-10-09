import java.util.HashMap;
import java.util.Map;

/**
 * Registry of packet handlers for the new socket-based architecture.
 * 
 * MIGRATION STRATEGY:
 * - Register new handlers here as you convert packets from the old system
 * - Packets NOT registered here will fall back to mudclient.handleIncomingPacket() (temporary)
 * - Once ALL packets are registered here, remove the fallback and delete old handling code
 * 
 * TODO: Convert remaining packets from mudclient.handleIncomingPacket() to handlers
 * TODO: Once complete, remove clientStream and the old packet handling system
 */
public class ClientSidePacketHandlers {
    private static final Map<Opcodes.Server, IClientPacketHandler> packetHandlers = new HashMap<>();

    static {
        packetHandlers.put(Opcodes.Server.SV_MESSAGE, new SV_MessageHandler()::handle);
        packetHandlers.put(Opcodes.Server.SV_CLOSE_CONNECTION, new SV_CloseConnectionHandler()::handle);
        packetHandlers.put(Opcodes.Server.SV_LOGOUT_DENY, new SV_LogoutDeny()::handle);
        packetHandlers.put(Opcodes.Server.SV_FRIEND_LIST, new SV_FriendList()::handle);
        packetHandlers.put(Opcodes.Server.SV_FRIEND_STATUS_CHANGE, new SV_FriendStatusChange()::handle);
        packetHandlers.put(Opcodes.Server.SV_IGNORE_LIST, new SV_IgnoreList()::handle);
        packetHandlers.put(Opcodes.Server.SV_PRIVACY_SETTINGS, new SV_PrivacySettings()::handle);
        packetHandlers.put(Opcodes.Server.SV_FRIEND_MESSAGE, new SV_FriendMessage()::handle);
        
        // Game state
        packetHandlers.put(Opcodes.Server.SV_WORLD_INFO, new SV_WorldInfoHandler()::handle);
        packetHandlers.put(Opcodes.Server.SV_PLAYER_STAT_LIST, new SV_PlayerStatListHandler()::handle);
        packetHandlers.put(Opcodes.Server.SV_INVENTORY_ITEMS, new SV_InventoryItemsHandler()::handle);
        
        // Region data
        // NOTE: SV_REGION_PLAYERS is handled directly in mudclient.java, not here!
        // TODO: Create proper handler for SV_REGION_PLAYERS once we understand the bit-packed format
        // packetHandlers.put(Opcodes.Server.SV_REGION_PLAYERS, new SV_RegionPlayersHandler()::handle);
        packetHandlers.put(Opcodes.Server.SV_REGION_OBJECTS, new SV_RegionObjectsHandler()::handle);
        packetHandlers.put(Opcodes.Server.SV_REGION_WALL_OBJECTS, new SV_RegionWallObjectsHandler()::handle);
        packetHandlers.put(Opcodes.Server.SV_REGION_GROUND_ITEMS, new SV_RegionGroundItemsHandler()::handle);
        packetHandlers.put(Opcodes.Server.SV_REGION_NPCS, new SV_RegionNPCsHandler()::handle);
    }

    public static IClientPacketHandler getHandlerByOpcode(short opcode) {
        return packetHandlers.get(Opcodes.Server.valueOf(opcode));
    }
}
