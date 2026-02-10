import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class GameConnection extends GameShell {

    public static int clientVersion = 1;
    public static int maxReadTries;
    private final int maxSocialListSize = 100;
    public String server;
    public int port;
    public ClientStream clientStream;
    public Socket socket;
    public int friendListCount;
    public long friendListHashes[];
    public int friendListOnline[];
    public int ignoreListCount;
    public long ignoreList[];
    public int settingsBlockChat;
    public int settingsBlockPrivate;
    public int settingsBlockTrade;
    public int settingsBlockDuel;
    public long sessionID;
    public int worldFullTimeout;
    public int moderatorLevel;
    String username;
    String password;
    byte incomingPacket[];
    int autoLoginTimeout;
    long packetLastRead;
    private int anIntArray629[];
    private int anInt630;

    public GameConnection() {
        server = "127.0.0.1";
        port = 43594;
        username = "";
        password = "";
        incomingPacket = new byte[5000];
        friendListHashes = new long[200];
        friendListOnline = new int[200];
        ignoreList = new long[maxSocialListSize];
        anIntArray629 = new int[maxSocialListSize];
    }

    protected void registerAccount(String user, String pass) {
        if (worldFullTimeout > 0) {
            showLoginScreenStatus("Please wait...", "Connecting to server");
            try {
                Thread.sleep(2000L);
            } catch (Exception Ex) {
            }
            showLoginScreenStatus("Sorry! The server is currently full.", "Please try again later");
            return;
        }
        try {
            this.username = user;
            this.password = pass;

            showLoginScreenStatus("Please wait...", "Connecting to server");

            if (socket == null) {
                socket = NetHelper.createSocket(server, port);
            }
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_SESSION.value); 
            out.putString(username);
            outputStream.write(out.toArrayWithLen());
            outputStream.flush();

            Buffer in = new Buffer(inputStream.readNBytes(Long.BYTES));
            long sessid = in.getLong();
            sessionID = sessid;

            if (sessid == 0L) {
                showLoginScreenStatus("Login server offline.", "Please try again later");
                return;
            }

            out = new Buffer();
            out.putShort(Opcodes.Client.CL_REGISTER_ACCOUNT.value);
            out.putInt(clientVersion);
            out.putLong(sessionID);
            out.putString(user);
            out.putString(pass);
            outputStream.write(out.toArrayWithLen());
            outputStream.flush();

            in = new Buffer(inputStream.readNBytes(Integer.BYTES));
            RegistrationResponse registrationResponse = RegistrationResponse.fromCode(in.getInt());

            showLoginScreenStatus(registrationResponse.getMessage(), registrationResponse.getSubMessage());
            
        } catch(Exception ex) {
            // This should catch any I/O issues
            ex.printStackTrace();
            showLoginScreenStatus("Error unable to create user.", "Unrecognised response code");
        }
    }

    protected void login(String username, String password) {
        if (username.trim().length() == 0 || password.trim().length() == 0) {
            showLoginScreenStatus("You must enter both a username", "and a password - Please try again");
            return;
        }
        showLoginScreenStatus("Please wait...", "Connecting to server");

        try {

            if (socket == null) {
                socket = NetHelper.createSocket(server, port);
            }
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_SESSION.value); 
            out.putString(username);
            outputStream.write(out.toArrayWithLen());
            outputStream.flush();

            Buffer in = new Buffer(inputStream.readNBytes(Long.BYTES));
            long sessid = in.getLong();
            sessionID = sessid;

            if (sessid == 0L) {
                showLoginScreenStatus("Login server offline.", "Please try again later");
                return;
            }

            out = new Buffer();
            out.putShort(Opcodes.Client.CL_LOGIN.value);
            out.putInt(clientVersion);
            out.putLong(sessionID);
            out.putString(username);
            out.putString(password);
            outputStream.write(out.toArrayWithLen());
            outputStream.flush();

            in = new Buffer(inputStream.readNBytes(Integer.BYTES));
            LoginResponse loginResponse = LoginResponse.fromCode(in.getInt());

            if (loginResponse.getCode() == 25) {
                moderatorLevel = 1;
                autoLoginTimeout = 0;
                // TODO: REMOVE THIS - Temporary compatibility layer for old packet handling
                // Once all packets are migrated to the new handler system (ClientSidePacketHandlers),
                // remove clientStream initialization entirely and delete the ClientStream class.
                if (clientStream == null) {
                    clientStream = new ClientStream(socket, (GameShell) this);
                }
                resetGame();
                return;
            }
            if (loginResponse.getCode() == 0) {
                moderatorLevel = 0;
                autoLoginTimeout = 0;
                // TODO: REMOVE THIS - Temporary compatibility layer for old packet handling
                // Once all packets are migrated to the new handler system (ClientSidePacketHandlers),
                // remove clientStream initialization entirely and delete the ClientStream class.
                if (clientStream == null) {
                    clientStream = new ClientStream(socket, (GameShell) this);
                }
                resetGame();
                return;
            }
            if (loginResponse.getCode() == 1) {
                autoLoginTimeout = 0;
                method37();
                return;
            }

            showLoginScreenStatus(loginResponse.getMessage(), loginResponse.getMessage());


        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Unable to create socket: " + ex.getMessage());
        }
                                                                
    }
    protected void closeConnection() {
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_CLOSE_CONNECTION.value);
            sendPacket(out);
        } catch (IOException ex) {
            // Ignore - we're disconnecting
        }
        if (clientStream != null) {
            try {
                clientStream.closeStream();
            } catch (Exception ex) {}
            clientStream = null;
        }
        username = "";
        password = "";
        resetLoginVars();
    }

    protected void lostConnection() {
        try {
            throw new Exception("");
        } catch (Exception ex) {
            System.out.println("loast connection: ");
            ex.printStackTrace();
        }
        System.out.println("Lost connection");
        autoLoginTimeout = 10;
        login(username, password);
    }

    protected void checkConnection() {
        long l = System.currentTimeMillis();
        long diff = l - packetLastRead;
        InputStream inStream;
        try {
            inStream = socket.getInputStream();
            // FIXED: Read packets whenever they're available, not just after 5 second delay
            // The old logic (diff > 5000L) caused packets sent immediately after login to be missed
            if (inStream.available() > 0) {
                packetLastRead = l;
                handlePacket(socket);
            }
            
        } catch (IOException Ex) {
            lostConnection();
            return;
        }
    }

    private void handlePacket(Socket socket) {
        InputStream inStream;
        try {
            inStream = socket.getInputStream();

            if (inStream.available() >= 4) {
                byte[] lengthOpcodeBuffer = inStream.readNBytes(4);
                ByteBuffer headerBuffer = ByteBuffer.wrap(lengthOpcodeBuffer);
                short length = headerBuffer.getShort();
                short opcodeValue = headerBuffer.getShort();
    
                Opcodes.Server opcode = Opcodes.Server.valueOf(opcodeValue);
    
                // Ensure that the full packet data is available
                if (inStream.available() >= length - 4) {
                    byte[] rawDataBuffer = inStream.readNBytes(length - 2); // read length without length-bytes (2)
                    
                    // Try to handle with registered handler first
                    IClientPacketHandler handler = ClientSidePacketHandlers.getHandlerByOpcode(opcodeValue);
    
                    if (handler != null) {
                        // For new handlers, pass data without opcode prefix
                        Buffer data = new Buffer(rawDataBuffer);
                        handler.handle(this, socket, data);
                    } else {
                        // TODO: REMOVE THIS - Temporary fallback to old packet handling
                        // Once all packets have handlers registered in ClientSidePacketHandlers,
                        // remove this else block and the entire mudclient.handleIncomingPacket() method.
                        
                        // Old mudclient code expects pdata[0] to be the opcode byte
                        // Create new buffer with opcode at index 0
                        byte[] dataBuffer = new byte[rawDataBuffer.length + 1];
                        dataBuffer[0] = (byte)(opcodeValue & 0xFF); // Low byte of opcode
                        System.arraycopy(rawDataBuffer, 0, dataBuffer, 1, rawDataBuffer.length);
                        
                        handleIncomingPacket(opcode, opcodeValue, dataBuffer.length, dataBuffer);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.error(ex.getMessage());
            return;
        }
    }

    private void handlePacket_OLD(Opcodes.Server opcode, int ptype, int psize) {
        //ptype = clientStream.isaacCommand(ptype);
        System.out.println(String.format("opcode:%s(%d) psize:%d", opcode.name(), ptype, psize));
//         System.out.println("opcode:" + opcode + " psize:" + psize);
        if (opcode == Opcodes.Server.SV_MESSAGE) {
            String s = new String(incomingPacket, 1, psize - 1);
            showServerMessage(s);
        }
        if (opcode == Opcodes.Server.SV_CLOSE_CONNECTION)
            closeConnection();
        if (opcode == Opcodes.Server.SV_LOGOUT_DENY) {
            cantLogout();
            return;
        }
        if (opcode == Opcodes.Server.SV_FRIEND_LIST) {
            friendListCount = Utility.getUnsignedByte(incomingPacket[1]);
            for (int k = 0; k < friendListCount; k++) {
                friendListHashes[k] = Utility.getUnsignedLong(incomingPacket, 2 + k * 9);
                friendListOnline[k] = Utility.getUnsignedByte(incomingPacket[10 + k * 9]);
            }

            sortFriendsList();
            return;
        }
        if (opcode == Opcodes.Server.SV_FRIEND_STATUS_CHANGE) {
            long hash = Utility.getUnsignedLong(incomingPacket, 1);
            int online = incomingPacket[9] & 0xff;
            for (int i2 = 0; i2 < friendListCount; i2++)
                if (friendListHashes[i2] == hash) {
                    if (friendListOnline[i2] == 0 && online != 0)
                        showServerMessage("@pri@" + Utility.hash2username(hash) + " has logged in");
                    if (friendListOnline[i2] != 0 && online == 0)
                        showServerMessage("@pri@" + Utility.hash2username(hash) + " has logged out");
                    friendListOnline[i2] = online;
                    psize = 0; // not sure what this is for
                    sortFriendsList();
                    return;
                }

            friendListHashes[friendListCount] = hash;
            friendListOnline[friendListCount] = online;
            friendListCount++;
            sortFriendsList();
            return;
        }
        if (opcode == Opcodes.Server.SV_IGNORE_LIST) {
            ignoreListCount = Utility.getUnsignedByte(incomingPacket[1]);
            for (int i1 = 0; i1 < ignoreListCount; i1++)
                ignoreList[i1] = Utility.getUnsignedLong(incomingPacket, 2 + i1 * 8);

            return;
        }
        if (opcode == Opcodes.Server.SV_PRIVACY_SETTINGS) {
            settingsBlockChat = incomingPacket[1];
            settingsBlockPrivate = incomingPacket[2];
            settingsBlockTrade = incomingPacket[3];
            settingsBlockDuel = incomingPacket[4];
            return;
        }
        if (opcode == Opcodes.Server.SV_FRIEND_MESSAGE) {
            long from = Utility.getUnsignedLong(incomingPacket, 1);
            int k1 = Utility.getUnsignedInt(incomingPacket, 9); // is this some sort of message id ?
            for (int j2 = 0; j2 < maxSocialListSize; j2++)
                if (anIntArray629[j2] == k1)
                    return;

            anIntArray629[anInt630] = k1;
            anInt630 = (anInt630 + 1) % maxSocialListSize;
            String msg = WordFilter.filter(ChatMessage.descramble(incomingPacket, 13, psize - 13));
            showServerMessage("@pri@" + Utility.hash2username(from) + ": tells you " + msg);
            return;
        } else {
            handleIncomingPacket(opcode, ptype, psize, incomingPacket);
            return;
        }
    }

    protected void drawTextBox(String s, String s1) {
        Graphics g = getGraphics();
        Font font = new Font("Helvetica", 1, 15);
        char c = '\u0200';
        char c1 = '\u0158';
        g.setColor(Color.black);
        g.fillRect(c / 2 - 140, c1 / 2 - 25, 280, 50);
        g.setColor(Color.white);
        g.drawRect(c / 2 - 140, c1 / 2 - 25, 280, 50);
        drawString(g, s, font, c / 2, c1 / 2 - 10);
        drawString(g, s1, font, c / 2, c1 / 2 + 10);
    }

    public void sortFriendsList() {
        boolean flag = true;
        while (flag) {
            flag = false;
            for (int i = 0; i < friendListCount - 1; i++)
                if (friendListOnline[i] != 255 && friendListOnline[i + 1] == 255 || friendListOnline[i] == 0 && friendListOnline[i + 1] != 0) {
                    int j = friendListOnline[i];
                    friendListOnline[i] = friendListOnline[i + 1];
                    friendListOnline[i + 1] = j;
                    long l = friendListHashes[i];
                    friendListHashes[i] = friendListHashes[i + 1];
                    friendListHashes[i + 1] = l;
                    flag = true;
                }

        }
    }

    protected void sendPrivacySettings(int chat, int priv, int trade, int duel) {
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_SETTINGS_PRIVACY.value);
            out.putByte((byte) chat);
            out.putByte((byte) priv);
            out.putByte((byte) trade);
            out.putByte((byte) duel);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send privacy settings: " + ex.getMessage());
        }
    }

    protected void ignoreAdd(String s) {
        long l = Utility.username2hash(s);
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_IGNORE_ADD.value);
            out.putLong(l);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send ignore add: " + ex.getMessage());
        }
        for (int i = 0; i < ignoreListCount; i++)
            if (ignoreList[i] == l)
                return;

        if (ignoreListCount >= maxSocialListSize) {
            return;
        } else {
            ignoreList[ignoreListCount++] = l;
            return;
        }
    }

    protected void ignoreRemove(long l) {
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_IGNORE_REMOVE.value);
            out.putLong(l);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send ignore remove: " + ex.getMessage());
        }
        for (int i = 0; i < ignoreListCount; i++)
            if (ignoreList[i] == l) {
                ignoreListCount--;
                for (int j = i; j < ignoreListCount; j++)
                    ignoreList[j] = ignoreList[j + 1];

                return;
            }

    }

    protected void friendAdd(String s) {
        long l = Utility.username2hash(s);
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_FRIEND_ADD.value);
            out.putLong(l);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send friend add: " + ex.getMessage());
        }
        for (int i = 0; i < friendListCount; i++)
            if (friendListHashes[i] == l)
                return;

        if (friendListCount >= maxSocialListSize) {
            return;
        } else {
            friendListHashes[friendListCount] = l;
            friendListOnline[friendListCount] = 0;
            friendListCount++;
            return;
        }
    }

    protected void friendRemove(long l) {
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_FRIEND_REMOVE.value);
            out.putLong(l);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send friend remove: " + ex.getMessage());
        }
        for (int i = 0; i < friendListCount; i++) {
            if (friendListHashes[i] != l)
                continue;
            friendListCount--;
            for (int j = i; j < friendListCount; j++) {
                friendListHashes[j] = friendListHashes[j + 1];
                friendListOnline[j] = friendListOnline[j + 1];
            }

            break;
        }

        showServerMessage("@pri@" + Utility.hash2username(l) + " has been removed from your friends list");
    }

    protected void sendPrivateMessage(long u, byte buff[], int len) {
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_PM.value);
            out.putLong(u);
            out.put(buff, 0, len);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send private message: " + ex.getMessage());
        }
    }

    protected void sendChatMessage(byte buff[], int len) {
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_CHAT.value);
            out.put(buff, 0, len);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send chat message: " + ex.getMessage());
        }
    }

    protected void sendCommandString(String s) {
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_COMMAND.value);
            out.putString(s);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send command: " + ex.getMessage());
        }
    }

    /**
     * Send inventory equip packet (new Buffer format).
     */
    protected void sendInvWear(int slotIndex) {
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_INV_WEAR.value);
            out.putShort((short) slotIndex);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send inv wear: " + ex.getMessage());
        }
    }

    /**
     * Send inventory unequip packet (new Buffer format).
     */
    protected void sendInvUnequip(int slotIndex) {
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_INV_UNEQUIP.value);
            out.putShort((short) slotIndex);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send inv unequip: " + ex.getMessage());
        }
    }

    /**
     * Send inventory drop packet (new Buffer format).
     */
    protected void sendInvDrop(int slotIndex) {
        try {
            Buffer out = new Buffer();
            out.putShort(Opcodes.Client.CL_INV_DROP.value);
            out.putShort((short) slotIndex);
            sendPacket(out);
        } catch (IOException ex) {
            Logger.error("Failed to send inv drop: " + ex.getMessage());
        }
    }

    protected void showLoginScreenStatus(String s, String s1) {
    }

    /**
     * Send a packet using the new Buffer format (2-byte opcode).
     * All packet sends should use this method.
     */
    private void sendPacket(Buffer out) throws IOException {
        if (socket != null && !socket.isClosed()) {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(out.toArrayWithLen());
            outputStream.flush();
        }
    }

    protected void method37() {
    }

    protected void resetGame() {
    }

    protected void resetLoginVars() {
    }

    protected void cantLogout() {
    }

    protected void handleIncomingPacket(Opcodes.Server opcode, int ptype, int len, byte data[]) {
    }

    protected void showServerMessage(String s) {
    }

    // protected boolean method43() {
    //     return true;
    // }

    protected int getLinkUID() {
        return 0;
    }

    /**
     * NEW PACKET FORMAT: Send walk packet using Buffer (2-byte opcode format).
     * This replaces the old ClientStream walk packet sending.
     * 
     * @param targetX Absolute world X coordinate
     * @param targetY Absolute world Y coordinate
     * @param walkPath Array of walk path steps (alternating deltaX, deltaY)
     * @param stepCount Number of steps in the path
     * @param isAction True for CL_WALK_ACTION, false for CL_WALK
     */
    protected void sendWalkPacket(int targetX, int targetY, byte[] walkPath, int stepCount, boolean isAction) {
        try {
            Buffer out = new Buffer();
            
            // Use appropriate opcode
            if (isAction) {
                out.putShort(Opcodes.Client.CL_WALK_ACTION.value);
            } else {
                out.putShort(Opcodes.Client.CL_WALK.value);
            }
            
            // Target coordinates
            out.putShort((short) targetX);
            out.putShort((short) targetY);
            
            // Walk path steps (byte pairs: deltaX, deltaY)
            if (walkPath != null && stepCount > 0) {
                out.put(walkPath, 0, stepCount * 2);
            }
            
            // Send with new packet format
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(out.toArrayWithLen());
            outputStream.flush();
            
            Logger.debug("Sent walk packet: target=(" + targetX + "," + targetY + "), steps=" + stepCount + ", action=" + isAction);
            
        } catch (IOException ex) {
            Logger.error("Failed to send walk packet: " + ex.getMessage());
        }
    }
}
