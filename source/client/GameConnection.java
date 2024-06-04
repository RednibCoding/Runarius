package client;

import java.awt.*;
import java.io.IOException;
import java.net.Socket;

import common.NetHelper;
import common.Opcodes;

public class GameConnection extends GameShell {

    public static int clientVersion = 1;
    public static int maxReadTries;
    private final int maxSocialListSize = 100;
    public String server;
    public int port;
    public ClientStream clientStream;
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
            user = Utility.formatAuthString(user, 20);
            this.password = pass;
            pass = Utility.formatAuthString(pass, 20);

            showLoginScreenStatus("Please wait...", "Connecting to server");
            clientStream = new ClientStream(createSocket(server, port), this);
            clientStream.newPacket(Opcodes.Client.CL_SESSION.value);
            clientStream.putByte((int) (Utility.username2hash(user) >> 16 & 31L));
            clientStream.sendAndFlushPacket();
            long sessid = clientStream.getLong();
            sessionID = sessid;
            if (sessid == 0L) {
                showLoginScreenStatus("Login server offline.", "Please try again in a few mins");
                return;
            }
            System.out.println("Verb: Session id: " + sessid);

            clientStream.newPacket(2);
            clientStream.putShort(clientVersion);
            clientStream.putString(user);
            clientStream.putString(pass);
            clientStream.sendAndFlushPacket();

            int response = clientStream.readStream();
            clientStream.closeStream();
            System.out.println("Newplayer response: " + response);

            switch(response) {
                case 2: // Success
                    resetLoginVars();
                    return;
                case 13: // Username taken
                case 3:
                    showLoginScreenStatus("Username already taken.", "Please choose another username");
                    return;
                case 4: // Username in use.  Distinction??
                    showLoginScreenStatus("That username is already in use.", "Wait 60 seconds then retry");
                    return;
                case 5: // Client has been updated
                    showLoginScreenStatus("The client has been updated.", "Please reload this page");
                    return;
                case 6: // IP address in use
                    showLoginScreenStatus("You may only use 1 character at once.", "Your ip-address is already in use");
                    return;
                case 7: // Spam throttle was hit
                    showLoginScreenStatus("Login attempts exceeded!", "Please try again in 5 minutes");
                    return;
                case 11: // Temporary ban
                    showLoginScreenStatus("Account has been temporarily disabled", "for cheating or abuse");
                    return;
                case 12: // Permanent ban
                    showLoginScreenStatus("Account has been permanently disabled", "for cheating or abuse");
                    return;
                case 14: // server full
                    showLoginScreenStatus("Sorry! The server is currently full.", "Please try again later");
                    worldFullTimeout = 1500;
                    return;
                case 15: // Members account needed
                    showLoginScreenStatus("You need a members account", "to login to this server");
                    return;
                case 16: // Switch to members server
                    showLoginScreenStatus("Please login to a members server", "to access member-only features");
                    return;
                default:
                    showLoginScreenStatus("Error unable to create user.", "Unrecognised response code");
                    return;
            }
        } catch(Exception e) {
            // This should catch any I/O issues
            e.printStackTrace();
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

            Socket socket = NetHelper.createSocket(server, port);
            PacketOut packet = new PacketOut(Opcodes.Client.CL_SESSION.value);

            packet.putString(username);
            packet.Send(socket);

            
            

        } catch (IOException ex) {
            System.out.println("Unable to create socket: " + ex.getMessage());
        }
                                                                
    }

    // protected void login(String u, String p, boolean reconnecting) {
    //     if (worldFullTimeout > 0) {
    //         showLoginScreenStatus("Please wait...", "Connecting to server");
    //         try {
    //             Thread.sleep(2000L);
    //         } catch (Exception Ex) {
    //         }
    //         showLoginScreenStatus("Sorry! The server is currently full.", "Please try again later");
    //         return;
    //     }
    //     try {
    //         username = u;
    //         u = Utility.formatAuthString(u, 20);
    //         password = p;
    //         p = Utility.formatAuthString(p, 20);
    //         if (u.trim().length() == 0) {
    //             showLoginScreenStatus("You must enter both a username", "and a password - Please try again");
    //             return;
    //         }
    //         if (reconnecting)
    //             drawTextBox("Connection lost! Please wait...", "Attempting to re-establish");
    //         else
    //             showLoginScreenStatus("Please wait...", "Connecting to server");
    //         clientStream = new ClientStream(createSocket(server, port), this);
    //         clientStream.maxReadTries = maxReadTries;
    //         long l = Utility.username2hash(u);
    //         clientStream.newPacket(PackedID.Client.CL_SESSION.value);
    //         clientStream.putByte((int) (l >> 16 & 31L));
    //         clientStream.sendAndFlushPacket();
    //         long sessid = clientStream.getLong();
    //         sessionID = sessid;
    //         if (sessid == 0L) {
    //             showLoginScreenStatus("Login server offline.", "Please try again in a few mins");
    //             return;
    //         }
    //         System.out.println("Verb: Session id: " + sessid);
    //         int limit30 = 0;

    //         int ai[] = new int[4];
    //         ai[0] = (int) (Math.random() * 99999999D);
    //         ai[1] = (int) (Math.random() * 99999999D);
    //         ai[2] = (int) (sessid >> 32);
    //         ai[3] = (int) sessid;

    //         clientStream.newPacket(PackedID.Client.CL_LOGIN.value);
    //         if (reconnecting)
    //             clientStream.putByte(1);
    //         else
    //             clientStream.putByte(0);
    //         clientStream.putShort(clientVersion);
    //         clientStream.putByte(limit30);

    //         clientStream.putByte(10);
    //         clientStream.putInt(ai[0]);
    //         clientStream.putInt(ai[1]);
    //         clientStream.putInt(ai[2]);
    //         clientStream.putInt(ai[3]);
    //         clientStream.putInt(getLinkUID());
    //         clientStream.putString(u);
    //         clientStream.putString(p);

    //         clientStream.sendAndFlushPacket();
    //         int resp = clientStream.readStream();
    //         System.out.println("login response:" + resp);
    //         if (resp == 25) {
    //             moderatorLevel = 1;
    //             autoLoginTimeout = 0;
    //             resetGame();
    //             return;
    //         }
    //         if (resp == 0) {
    //             moderatorLevel = 0;
    //             autoLoginTimeout = 0;
    //             resetGame();
    //             return;
    //         }
    //         if (resp == 1) {
    //             autoLoginTimeout = 0;
    //             method37();
    //             return;
    //         }
    //         if (reconnecting) {
    //             u = "";
    //             p = "";
    //             resetLoginVars();
    //             return;
    //         }
    //         if (resp == -1) {
    //             showLoginScreenStatus("Error unable to login.", "Server timed out");
    //             return;
    //         }
    //         if (resp == 3) {
    //             showLoginScreenStatus("Invalid username or password.", "Try again, or create a new account");
    //             return;
    //         }
    //         if (resp == 4) {
    //             showLoginScreenStatus("That username is already logged in.", "Wait 60 seconds then retry");
    //             return;
    //         }
    //         if (resp == 5) {
    //             showLoginScreenStatus("The client has been updated.", "Please reload this page");
    //             return;
    //         }
    //         if (resp == 6) {
    //             showLoginScreenStatus("You may only use 1 character at once.", "Your ip-address is already in use");
    //             return;
    //         }
    //         if (resp == 7) {
    //             showLoginScreenStatus("Login attempts exceeded!", "Please try again in 5 minutes");
    //             return;
    //         }
    //         if (resp == 8) {
    //             showLoginScreenStatus("Error unable to login.", "Server rejected session");
    //             return;
    //         }
    //         if (resp == 9) {
    //             showLoginScreenStatus("Error unable to login.", "Loginserver rejected session");
    //             return;
    //         }
    //         if (resp == 10) {
    //             showLoginScreenStatus("That username is already in use.", "Wait 60 seconds then retry");
    //             return;
    //         }
    //         if (resp == 11) {
    //             showLoginScreenStatus("Account has been temporarily disabled", "for cheating or abuse");
    //             return;
    //         }
    //         if (resp == 12) {
    //             showLoginScreenStatus("Account has been permanently disabled", "for cheating or abuse");
    //             return;
    //         }
    //         if (resp == 14) {
    //             showLoginScreenStatus("Sorry! This world is currently full.", "Please try a different world");
    //             worldFullTimeout = 1500;
    //             return;
    //         }
    //         if (resp == 15) {
    //             showLoginScreenStatus("You need a members account", "to login to this world");
    //             return;
    //         }
    //         if (resp == 16) {
    //             showLoginScreenStatus("Error - no reply from loginserver.", "Please try again");
    //             return;
    //         }
    //         if (resp == 17) {
    //             showLoginScreenStatus("Error - failed to decode profile.", "Contact customer support");
    //             return;
    //         }
    //         if (resp == 18) {
    //             showLoginScreenStatus("Account suspected stolen.", "Press 'recover a locked account' on front page.");
    //             return;
    //         }
    //         if (resp == 20) {
    //             showLoginScreenStatus("Error - loginserver mismatch", "Please try a different world");
    //             return;
    //         }
    //         if (resp == 21) {
    //             showLoginScreenStatus("Unable to login.", "That is not an RS-Classic account");
    //             return;
    //         }
    //         if (resp == 22) {
    //             showLoginScreenStatus("Password suspected stolen.", "Press 'change your password' on front page.");
    //             return;
    //         } else {
    //             showLoginScreenStatus("Error unable to login.", "Unrecognised response code");
    //             return;
    //         }
    //     } catch (Exception exception) {
    //         System.out.println(String.valueOf(exception));
    //     }
    //     if (autoLoginTimeout > 0) {
    //         try {
    //             Thread.sleep(5000L);
    //         } catch (Exception Ex) {
    //         }
    //         autoLoginTimeout--;
    //         login(username, password, reconnecting);
    //     }
    //     if (reconnecting) {
    //         username = "";
    //         password = "";
    //         resetLoginVars();
    //     } else {
    //         showLoginScreenStatus("Sorry! Unable to connect.", "Check internet settings or try another world");
    //     }
    // }

    protected void closeConnection() {
        if (clientStream != null) {
            try {
                clientStream.newPacket(Opcodes.Client.CL_CLOSE_CONNECTION.value);
                clientStream.sendAndFlushPacket();
                clientStream.closeStream();
                clientStream = null;
            } catch (IOException Ex) {
            }
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

    protected void checkConnection() {
        long l = System.currentTimeMillis();
        if (clientStream.hasPacket())
            packetLastRead = l;
        if (l - packetLastRead > 5000L) {
            packetLastRead = l;
            clientStream.newPacket(Opcodes.Client.CL_PING.value);
            clientStream.sendPacket();
        }
        try {
            clientStream.writePacket(20);
        } catch (IOException Ex) {
            lostConnection();
            return;
        }
        if (!method43())
            return;
        int psize = clientStream.readPacket(incomingPacket);
        if (psize > 0) {
            short ptype = (short)(incomingPacket[0] & 0xff);
            handlePacket(Opcodes.Server.valueOf(ptype), ptype, psize);
        }
    }

    private void handlePacket(Opcodes.Server opcode, int ptype, int psize) {
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

    private void sortFriendsList() {
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
        clientStream.newPacket(Opcodes.Client.CL_SETTINGS_PRIVACY.value);
        clientStream.putByte(chat);
        clientStream.putByte(priv);
        clientStream.putByte(trade);
        clientStream.putByte(duel);
        clientStream.sendPacket();
    }

    protected void ignoreAdd(String s) {
        long l = Utility.username2hash(s);
        clientStream.newPacket(Opcodes.Client.CL_IGNORE_ADD.value);
        clientStream.putLong(l);
        clientStream.sendPacket();
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
        clientStream.newPacket(Opcodes.Client.CL_IGNORE_REMOVE.value);
        clientStream.putLong(l);
        clientStream.sendPacket();
        for (int i = 0; i < ignoreListCount; i++)
            if (ignoreList[i] == l) {
                ignoreListCount--;
                for (int j = i; j < ignoreListCount; j++)
                    ignoreList[j] = ignoreList[j + 1];

                return;
            }

    }

    protected void friendAdd(String s) {
        clientStream.newPacket(Opcodes.Client.CL_FRIEND_ADD.value);
        clientStream.putLong(Utility.username2hash(s));
        clientStream.sendPacket();
        long l = Utility.username2hash(s);
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
        clientStream.newPacket(Opcodes.Client.CL_FRIEND_REMOVE.value);
        clientStream.putLong(l);
        clientStream.sendPacket();
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
        clientStream.newPacket(Opcodes.Client.CL_PM.value);
        clientStream.putLong(u);
        clientStream.putBytes(buff, 0, len);
        clientStream.sendPacket();
    }

    protected void sendChatMessage(byte buff[], int len) {
        clientStream.newPacket(Opcodes.Client.CL_CHAT.value);
        clientStream.putBytes(buff, 0, len);
        clientStream.sendPacket();
    }

    protected void sendCommandString(String s) {
        clientStream.newPacket(Opcodes.Client.CL_COMMAND.value);
        clientStream.putString(s);
        clientStream.sendPacket();
    }

    protected void showLoginScreenStatus(String s, String s1) {
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

    protected boolean method43() {
        return true;
    }

    protected int getLinkUID() {
        return 0;
    }

}
