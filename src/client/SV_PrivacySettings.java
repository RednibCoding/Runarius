import java.net.Socket;

public class SV_PrivacySettings implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        // Read privacy settings from server
        byte blockChat = data.getByte();
        byte blockPM = data.getByte();
        byte blockTrade = data.getByte();
        byte blockDuel = data.getByte();
        
        client.settingsBlockChat = blockChat;
        client.settingsBlockPrivate = blockPM;
        client.settingsBlockTrade = blockTrade;
        client.settingsBlockDuel = blockDuel;
        
        Logger.debug("Privacy: chat=" + blockChat + ", pm=" + blockPM + ", trade=" + blockTrade + ", duel=" + blockDuel);
    }
}