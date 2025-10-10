/**
 * @deprecated Replaced by {@link PacketDispatcher}. Left in place for
 * transitional compatibility.
 */
@Deprecated
public final class ServerSidePacketHandlers {
    private ServerSidePacketHandlers() {}

    public static IPacketHandler getHandlerByOpcode(short opcode) {
        return ServerContext.get().getPacketDispatcher().get(opcode);
    }
}
