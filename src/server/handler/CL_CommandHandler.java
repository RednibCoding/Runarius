import java.io.IOException;
import java.net.Socket;
import java.util.Collection;

/**
 * Handles server commands sent by players (prefixed with :: in chat).
 * Example: ::pos, ::teleport 130 655, ::item 10 100
 */
public class CL_CommandHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            String command = data.getString();

            PlayerRepository players = ServerContext.get().getPlayers();
            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("Command: player not found for socket");
                return;
            }

            Logger.info(player.getUsername() + " command: ::" + command);

            String[] parts = command.split(" ");
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "pos":
                    sendMessage(player, "@gre@Position: (" + player.getX() + ", " + player.getY() + ") plane=" + player.getPlaneIndex());
                    break;

                case "teleport":
                case "tp":
                    handleTeleport(player, parts);
                    break;

                case "npccount":
                    int npcCount = ServerContext.get().getWorldService().getNpcs().size();
                    sendMessage(player, "@gre@Total NPCs loaded: " + npcCount);
                    break;

                case "nearby":
                    int radius = ServerContext.get().getConfig().getVisibilityRadius();
                    var nearbyNpcs = ServerContext.get().getWorldService().getNearbyNpcs(player.getX(), player.getY(), radius);
                    sendMessage(player, "@gre@Nearby NPCs: " + nearbyNpcs.size() + " (within " + radius + " tiles)");
                    break;

                case "item":
                    handleItem(player, parts);
                    break;

                case "setstat":
                    handleSetStat(player, parts);
                    break;

                case "heal":
                    handleHeal(player);
                    break;

                case "players":
                case "online":
                    handlePlayersList(player);
                    break;

                case "help":
                    sendMessage(player, "@yel@Commands: ::pos ::tp x y ::item id [amount]");
                    sendMessage(player, "@yel@::setstat id level ::heal ::players ::npccount ::nearby");
                    break;

                default:
                    sendMessage(player, "@red@Unknown command: ::" + cmd + " (try ::help)");
                    break;
            }

        } catch (Exception ex) {
            Logger.error("Command error: " + ex.getMessage());
        }
    }

    private void handleTeleport(Player player, String[] parts) throws IOException {
        if (parts.length >= 3) {
            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                player.clearWalkQueue();
                player.setX(x);
                player.setY(y);
                // Force re-send of region data
                player.getKnownPlayers().clear();
                player.getKnownNpcs().clear();
                CL_WalkHandler.sendRegionPlayersUpdate(player);
                PlayerPacketSender.sendRegionNpcs(player);
                PlayerPacketSender.sendRegionObjects(player);
                PlayerPacketSender.sendRegionWallObjects(player);
                PlayerPacketSender.sendRegionGroundItems(player);
                sendMessage(player, "@gre@Teleported to (" + x + ", " + y + ")");
            } catch (NumberFormatException ex) {
                sendMessage(player, "@red@Usage: ::teleport x y");
            }
        } else {
            sendMessage(player, "@red@Usage: ::teleport x y");
        }
    }

    /**
     * Give an item to the player.
     * Usage: ::item itemId [amount]
     */
    private void handleItem(Player player, String[] parts) throws IOException {
        if (parts.length < 2) {
            sendMessage(player, "@red@Usage: ::item itemId [amount]");
            return;
        }

        try {
            int itemId = Integer.parseInt(parts[1]);
            int amount = 1;
            if (parts.length >= 3) {
                amount = Integer.parseInt(parts[2]);
            }

            if (player.getInventory().size() >= 30) {
                sendMessage(player, "@red@Inventory is full!");
                return;
            }

            if (player.addItem(itemId, amount)) {
                PlayerPacketSender.sendFullInventory(player);
                sendMessage(player, "@gre@Added item " + itemId + " x" + amount);
            } else {
                sendMessage(player, "@red@Failed to add item (inventory full?)");
            }
        } catch (NumberFormatException ex) {
            sendMessage(player, "@red@Usage: ::item itemId [amount]");
        }
    }

    /**
     * Set a player's stat level.
     * Usage: ::setstat statId level
     * Stats: 0=Attack, 1=Defence, 2=Strength, 3=Hits, 4=Ranged, 5=Prayer, 6=Magic
     */
    private void handleSetStat(Player player, String[] parts) throws IOException {
        if (parts.length < 3) {
            sendMessage(player, "@red@Usage: ::setstat statId level");
            sendMessage(player, "@yel@Stats: 0=Atk 1=Def 2=Str 3=Hits 4=Range 5=Prayer 6=Magic");
            return;
        }

        try {
            int statId = Integer.parseInt(parts[1]);
            int level = Integer.parseInt(parts[2]);

            if (statId < 0 || statId >= 18) {
                sendMessage(player, "@red@Invalid stat ID (0-17)");
                return;
            }
            if (level < 1 || level > 99) {
                sendMessage(player, "@red@Level must be 1-99");
                return;
            }

            player.getBaseStats()[statId] = level;
            player.getCurrentStats()[statId] = level;

            // Send stat update to client
            PlayerPacketSender.sendStatUpdate(player, statId);
            sendMessage(player, "@gre@Set stat " + statId + " to level " + level);
        } catch (NumberFormatException ex) {
            sendMessage(player, "@red@Usage: ::setstat statId level");
        }
    }

    /**
     * Heal the player to full HP and restore all stats.
     */
    private void handleHeal(Player player) throws IOException {
        for (int i = 0; i < 18; i++) {
            player.getCurrentStats()[i] = player.getBaseStats()[i];
        }

        // Send full stat list update
        PlayerPacketSender.sendPlayerStatList(player);
        sendMessage(player, "@gre@All stats restored!");
    }

    /**
     * List all online players.
     */
    private void handlePlayersList(Player player) throws IOException {
        Collection<Player> onlinePlayers = ServerContext.get().getPlayers().getOnlinePlayers();
        sendMessage(player, "@gre@Online players (" + onlinePlayers.size() + "):");
        for (Player p : onlinePlayers) {
            sendMessage(player, "  @whi@" + p.getUsername() + " @gre@(" + p.getX() + ", " + p.getY() + ") CB:" + p.getCombatLevel());
        }
    }

    private void sendMessage(Player player, String message) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_MESSAGE.value);
        out.putString(message);

        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
}
