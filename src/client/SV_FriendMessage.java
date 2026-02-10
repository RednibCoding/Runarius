import java.net.Socket;

public class SV_FriendMessage implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        long usernameHash = data.getLong();
        int messageId = data.getInt(); // Message ID to prevent duplicates

        // Read remaining bytes as scrambled message
        int msgLen = data.remaining();
        byte[] scrambledBytes = new byte[msgLen];
        for (int i = 0; i < msgLen; i++) {
            scrambledBytes[i] = data.getByte();
        }

        // Descramble and filter
        String filteredMessage = WordFilter.filter(ChatMessage.descramble(scrambledBytes, 0, msgLen));

        // Show the message
        client.showServerMessage("@pri@" + Utility.hash2username(usernameHash) + " tells you: " + filteredMessage);

        Logger.debug("Private message from " + Utility.hash2username(usernameHash) + ": " + filteredMessage);
    }
}