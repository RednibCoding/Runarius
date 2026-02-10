import java.io.IOException;
import java.util.List;
import java.util.Set;

public final class PlayerPacketSender {
    private PlayerPacketSender() {}

    /**
     * Send a chat message from one player to a viewer, via SV_REGION_PLAYER_UPDATE updateType=1.
     */
    public static void sendChat(Player viewer, Player speaker, byte[] scrambledMessage) throws IOException {
        if (viewer == null || speaker == null || viewer.getSocket() == null) return;

        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_PLAYER_UPDATE.value);
        out.putShort((short) 1);                       // 1 update
        out.putShort((short) speaker.getServerId());   // who's talking
        out.putByte((byte) 1);                          // updateType = 1 (chat)
        out.putByte((byte) scrambledMessage.length);    // message length
        out.put(scrambledMessage);                      // scrambled message bytes

        viewer.getSocket().getOutputStream().write(out.toArrayWithLen());
        viewer.getSocket().getOutputStream().flush();
    }

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

    // ===== Region Data Packets =====

    /**
     * Send NPC region data using bit-packed format.
     * Format matches what mudclient.handleIncomingPacket() expects for SV_REGION_NPCS.
     *
     * Bit layout (after opcode byte prepended by GameConnection):
     * - knownNpcCount (8 bits) - count of previously known NPCs
     * - Per known NPC: reqUpdate(1), if yes: updateType(1), if moved: direction(3)
     * - Per new NPC: serverIndex(12), areaX(5), areaY(5), sprite(4), npcType(10)
     */
    public static void sendRegionNpcs(Player player) throws IOException {
        if (player == null || player.getSocket() == null) return;

        WorldService world = ServerContext.get().getWorldService();
        int radius = ServerContext.get().getConfig().getVisibilityRadius();
        List<Npc> nearbyNpcs = world.getNearbyNpcs(player.getX(), player.getY(), radius);

        Set<Npc> knownNpcs = player.getKnownNpcs();
        Set<Npc> addedNpcs = player.getAddedNpcs();
        Set<Npc> removedNpcs = player.getRemovedNpcs();

        // Compute visibility changes
        addedNpcs.clear();
        removedNpcs.clear();

        // Find NPCs that are no longer in range
        for (Npc known : knownNpcs) {
            if (!nearbyNpcs.contains(known)) {
                removedNpcs.add(known);
            }
        }

        // Find new NPCs in range
        for (Npc npc : nearbyNpcs) {
            if (!knownNpcs.contains(npc)) {
                addedNpcs.add(npc);
            }
        }

        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_NPCS.value);

        byte[] bitData = new byte[500];
        int bitOffset = 0;

        // Known NPC count
        NetHelper.setBitMask(bitData, bitOffset, 8, knownNpcs.size());
        bitOffset += 8;

        // Process known NPCs
        for (Npc npc : knownNpcs) {
            if (removedNpcs.contains(npc)) {
                // reqUpdate = 1, updateType = 1 (removing), direction = 12 (signal remove)
                NetHelper.setBitMask(bitData, bitOffset, 1, 1);
                bitOffset += 1;
                NetHelper.setBitMask(bitData, bitOffset, 1, 1);
                bitOffset += 1;
                // Write 4-bit value with upper 2 bits = 11 (0xC) to signal removal
                NetHelper.setBitMask(bitData, bitOffset, 4, 12);
                bitOffset += 4;
            } else {
                // reqUpdate = 0 (no change, NPC stays in place)
                NetHelper.setBitMask(bitData, bitOffset, 1, 0);
                bitOffset += 1;
            }
        }

        // Add new NPCs
        for (Npc npc : addedNpcs) {
            if ((bitOffset + 36) / 8 >= 490) break; // leave room

            // serverIndex (12 bits)
            NetHelper.setBitMask(bitData, bitOffset, 12, npc.getServerId());
            bitOffset += 12;

            // Offset from local player (5-bit signed)
            int offsetX = npc.getX() - player.getX();
            int offsetY = npc.getY() - player.getY();
            if (offsetX < 0) offsetX += 32;
            if (offsetY < 0) offsetY += 32;
            NetHelper.setBitMask(bitData, bitOffset, 5, offsetX);
            bitOffset += 5;
            NetHelper.setBitMask(bitData, bitOffset, 5, offsetY);
            bitOffset += 5;

            // Animation/sprite (4 bits)
            NetHelper.setBitMask(bitData, bitOffset, 4, npc.getDirection());
            bitOffset += 4;

            // NPC type (10 bits)
            NetHelper.setBitMask(bitData, bitOffset, 10, npc.getTypeId());
            bitOffset += 10;
        }

        int byteCount = (bitOffset + 7) / 8;
        byte[] packetData = new byte[byteCount];
        System.arraycopy(bitData, 0, packetData, 0, byteCount);
        out.putBytes(packetData);

        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();

        // Update tracking
        knownNpcs.addAll(addedNpcs);
        knownNpcs.removeAll(removedNpcs);
        addedNpcs.clear();
        removedNpcs.clear();

        Logger.debug("Sent NPC region: " + knownNpcs.size() + " known, " +
                     addedNpcs.size() + " new to " + player.getUsername());
    }

    /**
     * Send game objects in the player's region.
     * Uses byte-aligned format expected by mudclient.handleIncomingPacket().
     * Per object: [short objectId] [byte offsetX] [byte offsetY]
     */
    public static void sendRegionObjects(Player player) throws IOException {
        if (player == null || player.getSocket() == null) return;

        WorldService world = ServerContext.get().getWorldService();
        int radius = 24; // larger radius for static objects
        List<DataLoader.GameObjectData> nearbyObjects = world.getNearbyObjects(player.getX(), player.getY(), radius);

        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_OBJECTS.value);

        for (DataLoader.GameObjectData obj : nearbyObjects) {
            int offsetX = obj.x - player.getX();
            int offsetY = obj.y - player.getY();
            if (offsetX < -128 || offsetX > 127 || offsetY < -128 || offsetY > 127) continue;

            out.putShort((short) obj.id);
            out.putByte((byte) offsetX);
            out.putByte((byte) offsetY);
        }

        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();

        Logger.debug("Sent " + nearbyObjects.size() + " objects to " + player.getUsername());
    }

    /**
     * Send wall objects in the player's region.
     * Per wall: [short wallId] [byte offsetX] [byte offsetY] [byte direction]
     */
    public static void sendRegionWallObjects(Player player) throws IOException {
        if (player == null || player.getSocket() == null) return;

        WorldService world = ServerContext.get().getWorldService();
        int radius = 24;
        List<DataLoader.WallObjectData> nearbyWalls = world.getNearbyWallObjects(player.getX(), player.getY(), radius);

        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_WALL_OBJECTS.value);

        for (DataLoader.WallObjectData wall : nearbyWalls) {
            int offsetX = wall.x - player.getX();
            int offsetY = wall.y - player.getY();
            if (offsetX < -128 || offsetX > 127 || offsetY < -128 || offsetY > 127) continue;

            out.putShort((short) wall.id);
            out.putByte((byte) offsetX);
            out.putByte((byte) offsetY);
            out.putByte((byte) wall.direction);
        }

        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();

        Logger.debug("Sent " + nearbyWalls.size() + " wall objects to " + player.getUsername());
    }

    /**
     * Send ground items in the player's region.
     * Per item: [short itemId] [byte offsetX] [byte offsetY]
     */
    public static void sendRegionGroundItems(Player player) throws IOException {
        if (player == null || player.getSocket() == null) return;

        WorldService world = ServerContext.get().getWorldService();
        int radius = 24;
        List<DataLoader.GroundItemData> nearbyItems = world.getNearbyGroundItems(player.getX(), player.getY(), radius);

        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_GROUND_ITEMS.value);

        for (DataLoader.GroundItemData item : nearbyItems) {
            int offsetX = item.x - player.getX();
            int offsetY = item.y - player.getY();
            if (offsetX < -128 || offsetX > 127 || offsetY < -128 || offsetY > 127) continue;

            out.putShort((short) item.id);
            out.putByte((byte) offsetX);
            out.putByte((byte) offsetY);
        }

        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();

        Logger.debug("Sent " + nearbyItems.size() + " ground items to " + player.getUsername());
    }
}
