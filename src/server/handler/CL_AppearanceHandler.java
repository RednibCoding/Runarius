import java.io.IOException;
import java.net.Socket;

/**
 * Handles player appearance changes.
 */
public class CL_AppearanceHandler implements IPacketHandler {
	@Override
	public void handle(Socket socket, Buffer data) {
		try {
			byte headGender = data.getByte();
			byte headType = data.getByte();
			byte bodyGender = data.getByte();
			data.getByte(); // 2colour in docs (unused for now)
			byte hairColour = data.getByte();
			byte topColour = data.getByte();
			byte bottomColour = data.getByte();
			byte skinColour = data.getByte();

			ServerContext context = ServerContext.get();
			PlayerRepository players = context.getPlayers();
			Player player = players.findBySocket(socket).orElse(null);
			if (player == null) {
				Logger.error("Appearance: player not found for socket");
				return;
			}

			player.setAppearance(headGender, headType, bodyGender,
				hairColour, topColour, bottomColour, skinColour);

			Logger.info(player.getUsername() + " changed appearance");
			Logger.info("Appearance sprites head=" + (headType & 0xFF)
				+ " body=" + (bodyGender & 0xFF));

			players.forEachOnline(viewer -> {
				try {
					if (viewer == player) {
						PlayerPacketSender.sendAppearance(player, player);
					} else if (viewer.getKnownPlayers().contains(player) || viewer.getAddedPlayers().contains(player)) {
						PlayerPacketSender.sendAppearance(viewer, player);
					}
				} catch (IOException ioException) {
					Logger.error("Failed to broadcast appearance to " + viewer.getUsername() + ": " + ioException.getMessage());
				}
			});

		} catch (Exception ex) {
			Logger.error("Appearance error: " + ex.getMessage());
		}
	}
}
