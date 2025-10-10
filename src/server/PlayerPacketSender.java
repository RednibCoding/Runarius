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
        int[] equipment = buildEquipmentSlots(target);
        out.putByte((byte) equipment.length);

        for (int slotValue : equipment) {
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

        Logger.info("Appearance packet for " + target.getUsername() + ": equipment="
            + java.util.Arrays.toString(equipment)
            + " hair=" + target.getHairColor()
            + " top=" + target.getTopColor()
            + " bottom=" + target.getBottomColor()
            + " skin=" + target.getSkinColor());
    }

    private static int[] buildEquipmentSlots(Player target) {
    int[] equippedItems = new int[12];

    int headSprite = target.getHeadType();
    int bodyBase = target.getBodyGender();

    equippedItems[0] = 0; // cape/back (none)
    equippedItems[1] = headSprite; // head/hair (already 1-based)
    equippedItems[2] = bodyBase + 1; // torso sprite offset relative to body index
    equippedItems[3] = bodyBase + 2; // matching leg sprite for selected body

        for (int slot = 4; slot < equippedItems.length; slot++) {
            equippedItems[slot] = 0;
        }

        return equippedItems;
    }
}
