import java.net.Socket;

public class SV_FriendStatusChange implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        long usernameHash = data.getLong();
        byte online = data.getByte();
        
        // Update friend status
        for (int i = 0; i < client.friendListCount; i++) {
            if (client.friendListHashes[i] == usernameHash) {
                if (client.friendListOnline[i] == 0 && online != 0) {
                    client.showServerMessage("@pri@" + Utility.hash2username(usernameHash) + " has logged in");
                }
                if (client.friendListOnline[i] != 0 && online == 0) {
                    client.showServerMessage("@pri@" + Utility.hash2username(usernameHash) + " has logged out");
                }
                client.friendListOnline[i] = online;
                client.sortFriendsList();
                return;
            }
        }
        
        // Friend not in list, add them
        client.friendListHashes[client.friendListCount] = usernameHash;
        client.friendListOnline[client.friendListCount] = online;
        client.friendListCount++;
        client.sortFriendsList();
        
        Logger.debug("Friend status changed: " + Utility.hash2username(usernameHash) + " = " + online);
    }
}