import java.util.HashMap;
import java.util.Map;

public class ServerSidePacketHandlers {
    private static final Map<Opcodes.Client, IPacketHandler> packetHandlers = new HashMap<>();

    static {
        // Connection and session
        packetHandlers.put(Opcodes.Client.CL_SESSION, new CL_SessionHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_LOGIN, new CL_LoginHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_REGISTER_ACCOUNT, new CL_RegisterHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_PING, new CL_PingHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_CLOSE_CONNECTION, new CL_CloseConnectionHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_LOGOUT, new CL_LogoutHandler()::handle);
        
        // Social
        packetHandlers.put(Opcodes.Client.CL_FRIEND_ADD, new CL_FriendAddHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_FRIEND_REMOVE, new CL_FriendRemoveHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_IGNORE_ADD, new CL_IgnoreAddHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_IGNORE_REMOVE, new CL_IgnoreRemoveHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_SETTINGS_PRIVACY, new CL_PrivacySettingsHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_CHAT, new CL_ChatHandler()::handle);
        
        // Player
        packetHandlers.put(Opcodes.Client.CL_APPEARANCE, new CL_AppearanceHandler()::handle);
        packetHandlers.put(Opcodes.Client.CL_COMBAT_STYLE, new CL_CombatStyleHandler()::handle);
    }

    public static IPacketHandler getHandlerByOpcode(short opcode) {
        return packetHandlers.get(Opcodes.Client.valueOf(opcode));
    }
}
