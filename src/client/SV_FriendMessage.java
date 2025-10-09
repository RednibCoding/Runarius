import java.net.Socket;

public class SV_FriendMessage implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        long usernameHash = data.getLong();
        int messageId = data.getInt(); // Message ID to prevent duplicates
        String message = data.getString();
        
        // Filter the message
        String filteredMessage = WordFilter.filter(ChatMessage.descramble(message.getBytes(), 0, message.length()));
        
        // Show the message
        client.showServerMessage("@pri@" + Utility.hash2username(usernameHash) + " tells you: " + filteredMessage);
        
        Logger.debug("Private message from " + Utility.hash2username(usernameHash) + ": " + filteredMessage);
    }
}