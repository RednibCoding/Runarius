import java.io.IOException;
import java.net.Socket;

/**
 * Handles CL_SETTINGS_GAME packet.
 * Client sends: [byte settingId] [byte value]
 * Settings: 0=camera, 2=mouseButton, 3=sound
 */
public class CL_GameSettingsHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            ServerContext context = ServerContext.get();
            PlayerRepository players = context.getPlayers();

            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) return;

            int settingId = data.getByte() & 0xFF;
            int value = data.getByte() & 0xFF;

            Logger.debug("CL_SETTINGS_GAME: " + player.getUsername() +
                         " setting=" + settingId + " value=" + value);

            switch (settingId) {
                case 0: // Camera auto mode
                    player.setCameraModeAuto(value == 1);
                    break;
                case 2: // Mouse button one (single-click)
                    player.setMouseButtonOne(value == 1);
                    break;
                case 3: // Sound disabled
                    player.setSoundDisabled(value == 1);
                    break;
                default:
                    Logger.debug("Unknown game setting: " + settingId);
                    break;
            }

        } catch (Exception ex) {
            Logger.error("CL_SETTINGS_GAME error: " + ex.getMessage());
        }
    }
}
