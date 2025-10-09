import java.net.Socket;

public class SV_FriendList implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        // Read friend list from server
        byte count = data.getByte();
        client.friendListCount = count;
        
        for (int i = 0; i < count; i++) {
            long usernameHash = data.getLong();
            byte online = data.getByte();
            
            client.friendListHashes[i] = usernameHash;
            client.friendListOnline[i] = online;
        }
        
        // Sort friend list (online friends first)
        client.sortFriendsList();
        
        Logger.debug("Friend list: " + count + " friends");
    }
}