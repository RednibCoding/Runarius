import java.net.Socket;

/**
 * Handles world information sent from server on login.
 */
public class SV_WorldInfoHandler implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        short serverIndex = data.getShort();
        short planeWidth = data.getShort();
        short planeHeight = data.getShort();
        short planeIndex = data.getShort();
        short planeMultiplier = data.getShort();
        
        Logger.debug("World info: serverIndex=" + serverIndex + 
                    ", planeWidth=" + planeWidth + ", planeHeight=" + planeHeight + 
                    ", planeIndex=" + planeIndex + ", multiplier=" + planeMultiplier);
        
        // Cast to mudclient to access fields
        if (client instanceof mudclient) {
            mudclient mud = (mudclient) client;
            
            // Set the critical fields that the client needs
            mud.loadingArea = true;
            mud.localPlayerServerIndex = serverIndex;
            mud.planeWidth = planeWidth;
            mud.planeHeight = planeHeight;
            mud.planeIndex = planeIndex;
            mud.planeMultiplier = planeMultiplier;
            mud.planeHeight -= planeIndex * planeMultiplier;
        }
        
        // Reset the game to enter the world (sets loggedIn = 1)
        client.resetGame();
    }
}
