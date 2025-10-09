import java.net.Socket;

/**
 * Handles player stats list sent from server on login.
 */
public class SV_PlayerStatListHandler implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        // Read current stats (18 skills)
        for (int i = 0; i < 18; i++) {
            byte statCurrent = data.getByte();
            // TODO: Store in client
        }
        
        // Read base stats (18 skills)
        for (int i = 0; i < 18; i++) {
            byte statMax = data.getByte();
            // TODO: Store in client
        }
        
        // Read experience (18 skills)
        for (int i = 0; i < 18; i++) {
            int experience = data.getInt();
            // TODO: Store in client
        }
        
        // Read quest points
        byte questPoints = data.getByte();
        
        Logger.debug("Stats received: 18 skills, " + questPoints + " quest points");
        
        // TODO: Store stats in appropriate client variables
    }
}
