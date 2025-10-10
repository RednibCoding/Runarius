import java.io.IOException;

public final class PlayerPacketSender {
    private PlayerPacketSender() {}

    public static void sendAppearance(Player viewer, Player target) throws IOException {
        if (viewer == null || target == null || viewer.getSocket() == null) {
            return;
        }

        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_PLAYER_UPDATE.value);
        out.putShort((short) 1);
        out.putShort((short) target.getServerId());
        out.putByte((byte) 5);
        out.putShort((short) target.getServerId());
        out.putLong(target.getUsernameHash());
        out.putByte((byte) 12);

        for (int slotValue : buildEquipmentSlots(target)) {
            out.putByte((byte) slotValue);
        }

        out.putByte((byte) target.getHairColor());
        out.putByte((byte) target.getTopColor());
        out.putByte((byte) target.getBottomColor());
        out.putByte((byte) target.getSkinColor());
        out.putByte((byte) target.getCombatLevel());
        out.putByte((byte) 0);

        viewer.getSocket().getOutputStream().write(out.toArrayWithLen());
        viewer.getSocket().getOutputStream().flush();

        Logger.debug("Sent appearance of " + target.getUsername() + " to " + viewer.getUsername());
    }

    private static int[] buildEquipmentSlots(Player target) {
        int[] equippedItems = new int[12];
        equippedItems[0] = 0;
        equippedItems[1] = target.getHeadType();
        equippedItems[2] = target.getBodyGender() + 1;
        equippedItems[3] = target.getBodyGender() + 2;
        for (int i = 4; i < equippedItems.length; i++) {
            equippedItems[i] = 0;
        }
        return equippedItems;
    }
}
