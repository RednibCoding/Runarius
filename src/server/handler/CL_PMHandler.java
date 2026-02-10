import java.io.IOException;
import java.net.Socket;

/**
 * Handles private messages (PMs) from one player to another.
 * Receives target username hash + scrambled message bytes,
 * looks up the target player, and forwards via SV_FRIEND_MESSAGE.
 */
public class CL_PMHandler implements IPacketHandler {
    private static int nextMessageId = 1;

    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            long targetHash = data.getLong();

            // Read remaining bytes as scrambled message
            int msgLen = data.remaining();
            byte[] scrambledMessage = new byte[msgLen];
            for (int i = 0; i < msgLen; i++) {
                scrambledMessage[i] = data.getByte();
            }

            PlayerRepository players = ServerContext.get().getPlayers();
            Player sender = players.findBySocket(socket).orElse(null);
            if (sender == null) {
                Logger.error("PM: sender not found for socket");
                return;
            }

            // Check if sender has PM blocked
            if (sender.isBlockPrivateMessages()) {
                return;
            }

            // Find recipient by username hash
            Player recipient = players.findByUsernameHash(targetHash).orElse(null);
            if (recipient == null) {
                Logger.info(sender.getUsername() + " PM to offline player (hash=" + targetHash + ")");
                // Player is offline - client already shows "is not online" for non-friends
                return;
            }

            // Check if recipient has sender on ignore list
            if (recipient.getIgnoreList().contains(sender.getUsernameHash())) {
                Logger.debug(sender.getUsername() + " PM blocked (ignored by recipient)");
                return;
            }

            // Check if recipient blocks PMs
            if (recipient.isBlockPrivateMessages()) {
                // When blocking PMs, still allow from friends
                if (!recipient.getFriendList().contains(sender.getUsernameHash())) {
                    Logger.debug(sender.getUsername() + " PM blocked (recipient blocks PMs)");
                    return;
                }
            }

            // Forward the message
            int messageId = nextMessageId++;
            PlayerPacketSender.sendPrivateMessage(recipient, sender.getUsernameHash(), messageId, scrambledMessage);

            Logger.info(sender.getUsername() + " PM -> " + recipient.getUsername() + " (" + msgLen + " bytes)");

        } catch (Exception ex) {
            Logger.error("PM error: " + ex.getMessage());
        }
    }
}
