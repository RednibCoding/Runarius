import java.util.EnumMap;
import java.util.Map;


public final class PacketDispatcher {
    private final Map<Opcodes.Client, IPacketHandler> handlers = new EnumMap<>(Opcodes.Client.class);

    public PacketDispatcher() {
        register(Opcodes.Client.CL_SESSION, new CL_SessionHandler());
        register(Opcodes.Client.CL_LOGIN, new CL_LoginHandler());
        register(Opcodes.Client.CL_REGISTER_ACCOUNT, new CL_RegisterHandler());
        register(Opcodes.Client.CL_PING, new CL_PingHandler());
        register(Opcodes.Client.CL_CLOSE_CONNECTION, new CL_CloseConnectionHandler());
        register(Opcodes.Client.CL_LOGOUT, new CL_LogoutHandler());

        register(Opcodes.Client.CL_FRIEND_ADD, new CL_FriendAddHandler());
        register(Opcodes.Client.CL_FRIEND_REMOVE, new CL_FriendRemoveHandler());
        register(Opcodes.Client.CL_IGNORE_ADD, new CL_IgnoreAddHandler());
        register(Opcodes.Client.CL_IGNORE_REMOVE, new CL_IgnoreRemoveHandler());
        register(Opcodes.Client.CL_SETTINGS_PRIVACY, new CL_PrivacySettingsHandler());
        register(Opcodes.Client.CL_CHAT, new CL_ChatHandler());
        register(Opcodes.Client.CL_PM, new CL_PMHandler());

        register(Opcodes.Client.CL_COMMAND, new CL_CommandHandler());

        register(Opcodes.Client.CL_APPEARANCE, new CL_AppearanceHandler());
        register(Opcodes.Client.CL_COMBAT_STYLE, new CL_CombatStyleHandler());

        register(Opcodes.Client.CL_WALK, new CL_WalkHandler());
        register(Opcodes.Client.CL_WALK_ACTION, new CL_WalkHandler());

        register(Opcodes.Client.CL_INV_WEAR, new CL_InvWearHandler());
        register(Opcodes.Client.CL_INV_UNEQUIP, new CL_InvUnequipHandler());
        register(Opcodes.Client.CL_INV_DROP, new CL_InvDropHandler());
    }

    private void register(Opcodes.Client opcode, IPacketHandler handler) {
        handlers.put(opcode, handler);
    }

    public IPacketHandler get(short opcode) {
        Opcodes.Client clientOpcode = Opcodes.Client.valueOf(opcode);
        return handlers.get(clientOpcode);
    }
}
