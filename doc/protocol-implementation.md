# Runarius Protocol Implementation Guide

This document describes the client-server communication protocol implementation for Runarius (RSC-based MMORPG). This is a living document that will be updated as we implement and discover more about the protocol.

**Last Updated:** 2025-10-10

---

## ⚠️ CRITICAL: Packet Format Migration ⚠️

**THE MOST IMPORTANT THING TO UNDERSTAND ABOUT THIS CODEBASE**

The project is currently migrating from an OLD packet format to a NEW packet format. **These formats are INCOMPATIBLE and must be migrated one packet at a time.**

### OLD Format (Legacy - DO NOT USE FOR NEW CODE)

Used by the legacy `ClientStream` and `Packet_` classes:

```
[2 bytes] Length (big-endian, includes opcode + data, NOT the length bytes themselves)
[1 byte]  Opcode
[N bytes] Data
```

**Problems with old format:**
- Only 1-byte opcode (limits to 256 packet types)
- Incompatible with server's expected format
- Uses buffered sending via `ClientStream` thread

**Still used by:** Most in-game packets that haven't been migrated yet

### NEW Format (Target Architecture - USE THIS)

Used by the modern `Buffer` class:

```
[2 bytes] Length (big-endian, includes opcode + data, NOT the length bytes themselves)
[2 bytes] Opcode (big-endian short, allows 65,536 packet types)
[N bytes] Data
```

**Benefits:**
- Clean, consistent format
- Direct socket writes (no buffering complications)
- Compatible with server's `ClientHandler`
- Easier to debug and maintain

**Currently used by:** Session, Login, Walk packets (and growing!)

### Migration Process

**To migrate a packet from OLD to NEW format:**

1. **Create new sender method in `GameConnection.java`:**
   ```java
   protected void sendExamplePacket(int param1, String param2) {
       try {
           Buffer out = new Buffer();
           out.putShort(Opcodes.Client.CL_EXAMPLE.value);  // 2-byte opcode
           out.putInt(param1);
           out.putString(param2);
           
           OutputStream outputStream = socket.getOutputStream();
           outputStream.write(out.toArrayWithLen());
           outputStream.flush();
           
           Logger.debug("Sent example packet: param1=" + param1);
       } catch (IOException ex) {
           Logger.error("Failed to send example packet: " + ex.getMessage());
       }
   }
   ```

2. **Replace OLD calls in `mudclient.java`:**
   ```java
   // OLD - DELETE THIS:
   super.clientStream.newPacket(Opcodes.Client.CL_EXAMPLE.value);
   super.clientStream.putInt(param1);
   super.clientStream.putString(param2);
   super.clientStream.sendPacket();
   
   // NEW - USE THIS:
   sendExamplePacket(param1, param2);
   ```

3. **Server-side handler works automatically** (already expects NEW format):
   ```java
   // In ServerSidePacketHandlers - already registered:
   packetHandlers.put(Opcodes.Client.CL_EXAMPLE, new CL_ExampleHandler()::handle);
   ```

4. **Test thoroughly** before moving to the next packet!

### Migrated Packets (✅ Using NEW Format)

- ✅ `CL_SESSION` - Session request
- ✅ `CL_LOGIN` - Login authentication
- ✅ `CL_WALK` - Player movement
- ✅ `CL_WALK_ACTION` - Player movement with action

### Packets Still Using OLD Format (❌ Need Migration)

Most other in-game packets - to be migrated incrementally.

---

## Table of Contents

1. [Connection & Authentication Flow](#connection--authentication-flow)
2. [Packet Structure](#packet-structure)
3. [Login Sequence](#login-sequence)
4. [Game Initialization Packets](#game-initialization-packets)
5. [World Data Packets](#world-data-packets)
6. [Player Appearance System](#player-appearance-system)
7. [Packet Format Details](#packet-format-details)

---

## Connection & Authentication Flow

### Overview
The connection process follows this sequence:

```
Client                          Server
  |                               |
  |--- CL_SESSION --------------> |  (Request session with username)
  | <------------- sessionID ---- |  (8-byte session ID)
  |                               |
  |--- CL_LOGIN ----------------> |  (Login with credentials)
  | <-------- LoginResponse ----- |  (4-byte response code)
  |                               |
  |  [Game packets exchanged]     |
  |                               |
```

### 1. Session Request (CL_SESSION)

**Opcode:** Defined in `Opcodes.Client.CL_SESSION`

**Client sends:**
```
[2 bytes] Packet length (includes length field itself)
[2 bytes] Opcode
[string]  Username (length-prefixed string)
```

**Server responds:**
```
[8 bytes] Session ID (long)
          - If 0: Login server offline
          - If non-zero: Valid session created
```

### 2. Login Request (CL_LOGIN)

**Opcode:** Defined in `Opcodes.Client.CL_LOGIN`

**Client sends:**
```
[2 bytes] Packet length
[2 bytes] Opcode
[4 bytes] Client version (int)
[8 bytes] Session ID (from previous step)
[string]  Username
[string]  Password
```

**Server responds:**
```
[4 bytes] Login response code (int)
          - 0: Success
          - 3: Invalid credentials
          - 4: Account banned
          - 5: Already logged in
          - 25: Success (moderator)
```

**Implementation Notes:**
- Session ID validation ensures the client has a valid session
- Credentials are checked against pending players stored during CL_SESSION
- After successful login, server immediately sends game initialization packets

---

## Packet Structure

### Wire Format

All packets on the network follow this structure:

```
[2 bytes] Total packet length (including these 2 bytes)
[2 bytes] Opcode
[N bytes] Packet data (length = total_length - 4)
```

### Internal Handling

**Server → Client packets:**
- Server builds packet using `Buffer` class
- Calls `buffer.toArrayWithLen()` which prepends the 2-byte length
- Result: `[length][opcode][data]`

**Client receives:**
```java
// GameConnection.handlePacket() reads:
short length = read 2 bytes          // Total packet length
short opcode = read 2 bytes          // Packet opcode
byte[] data = read (length - 2) bytes // Remaining data
```

**Old mudclient.handleIncomingPacket() expects:**
```
pdata[0]  = Opcode low byte
pdata[1+] = Actual packet data
```

**Fix applied:** `GameConnection` prepends opcode byte to data before passing to old handler:
```java
byte[] dataBuffer = new byte[rawDataBuffer.length + 1];
dataBuffer[0] = (byte)(opcodeValue & 0xFF);
System.arraycopy(rawDataBuffer, 0, dataBuffer, 1, rawDataBuffer.length);
```

---

## Login Sequence

After successful login (LoginResponse = 0), the server sends a sequence of packets to initialize the game state:

```
Server → Client packet sequence:
1. SV_WORLD_INFO           (World/player info)
2. SV_PLAYER_STAT_LIST     (Skills and experience)
3. SV_INVENTORY_ITEMS      (Inventory contents)
4. SV_FRIEND_LIST          (Friends list)
5. SV_IGNORE_LIST          (Ignore list)
6. SV_PRIVACY_SETTINGS     (Privacy preferences)
7. SV_REGION_PLAYERS       (Player positions - triggers map load)
8. SV_REGION_OBJECTS       (World objects)
9. SV_REGION_WALL_OBJECTS  (Walls/fences)
10. SV_REGION_GROUND_ITEMS (Items on ground)
11. SV_REGION_NPCS         (NPC positions)
12. SV_REGION_PLAYER_UPDATE (Player appearance)
```

---

## Game Initialization Packets

### 1. SV_WORLD_INFO (Opcode 25)

**Purpose:** Initialize world dimensions and player's server index

**Structure:**
```
[2 bytes] Opcode (25)
[2 bytes] Player server index (localPlayerServerIndex)
[2 bytes] Plane width (world width in tiles)
[2 bytes] Plane height (world height in tiles)
[2 bytes] Plane index (current height level, usually 0)
[2 bytes] Plane multiplier (usually 1)
```

**Client behavior:**
- Sets `localPlayerServerIndex` (used to identify the local player)
- Sets world dimensions
- Sets `loadingArea = true` (shows "Loading... Please wait")
- Waits for `SV_REGION_PLAYERS` to load the map

**Example values:**
```
serverIndex: 1
planeWidth: 944
planeHeight: 944
planeIndex: 0
multiplier: 1
```

### 2. SV_PLAYER_STAT_LIST (Opcode 156)

**Purpose:** Send all player skills, experience, and quest points

**Structure:**
```
[2 bytes] Opcode (156)

// 18 skills, current levels
For i = 0 to 17:
  [1 byte] Current stat level

// 18 skills, base levels
For i = 0 to 17:
  [1 byte] Base stat level

// 18 skills, experience
For i = 0 to 17:
  [4 bytes] Experience (int)

[1 byte] Quest points
```

**Skills order (0-17):**
```
0:  Attack        7:  Magic         14: Agility
1:  Defense       8:  Cooking       15: Herblore
2:  Strength      9:  Woodcutting   16: Thieving
3:  Hitpoints     10: Fletching     17: Prayer
4:  Ranged        11: Fishing
5:  Prayer        12: Firemaking
6:  Magic         13: Crafting
```

**Default starting values:**
- Most skills: Level 1, Experience 0
- Hitpoints: Level 10, Experience 1154

### 3. SV_INVENTORY_ITEMS (Opcode 53)

**Purpose:** Send player's inventory contents

**Structure:**
```
[2 bytes] Opcode (53)
[1 byte]  Item count

For each item:
  [2 bytes] Item ID
  [4 bytes] Amount (only if item is stackable)
```

**Empty inventory:**
```
[2 bytes] Opcode (53)
[1 byte]  0 (no items)
```

### 4. SV_FRIEND_LIST (Opcode 71)

**Purpose:** Send player's friends list

**Structure:**
```
[2 bytes] Opcode (71)
[1 byte]  Friend count

For each friend:
  [8 bytes] Username hash (long)
  [1 byte]  Online status (0=offline, 1=online)
```

### 5. SV_IGNORE_LIST (Opcode 109)

**Purpose:** Send player's ignore list

**Structure:**
```
[2 bytes] Opcode (109)
[1 byte]  Ignore count

For each ignored player:
  [8 bytes] Username hash (long)
```

### 6. SV_PRIVACY_SETTINGS (Opcode 51)

**Purpose:** Send player's privacy preferences

**Structure:**
```
[2 bytes] Opcode (51)
[1 byte]  Block chat (0=no, 1=yes)
[1 byte]  Block private messages (0=no, 1=yes)
[1 byte]  Block trade requests (0=no, 1=yes)
[1 byte]  Block duel requests (0=no, 1=yes)
```

---

## World Data Packets

### 7. SV_REGION_PLAYERS (Opcode 191) - BIT-PACKED FORMAT

**Purpose:** Send player positions and create local player entity

**Structure:** This packet uses **bit-packing** instead of byte-aligned data!

```
[2 bytes] Opcode (191)
[N bytes] Bit-packed data (see below)
```

**Bit-packed data format:**
```
Bit offset 0:  [11 bits] Local player region X coordinate
Bit offset 11: [13 bits] Local player region Y coordinate
Bit offset 24: [4 bits]  Animation state (0 = standing)
Bit offset 28: [8 bits]  Known player count (other players in view)

For each known player (if count > 0):
  [1 bit]   Requires update flag
  [1 bit]   Update type (0=movement, 1=animation)
  ... (more complex bit-packed data for other players)
```

**Server implementation:**
```java
byte[] bitData = new byte[5]; // 40 bits minimum
int bitOffset = 0;

// Player coordinates (regionX, regionY from player position)
NetHelper.setBitMask(bitData, bitOffset, 11, regionX);
bitOffset += 11;
NetHelper.setBitMask(bitData, bitOffset, 13, regionY);
bitOffset += 13;

// Animation (0 = standing)
NetHelper.setBitMask(bitData, bitOffset, 4, 0);
bitOffset += 4;

// Known player count (0 for solo)
NetHelper.setBitMask(bitData, bitOffset, 8, 0);
```

**Client behavior:**
```java
// Client reads at bit offset 8 (not 0!) because pdata[0] contains opcode
int k7 = 8; // Start reading at bit 8 = pdata[1]
localRegionX = Utility.getBitMask(pdata, k7, 11);
k7 += 11;
localRegionY = Utility.getBitMask(pdata, k7, 13);
k7 += 13;
int anim = Utility.getBitMask(pdata, k7, 4);
k7 += 4;

// Calls loadNextRegion(localRegionX, localRegionY)
// This loads map sections from JAG files
// Creates localPlayer entity at playerServer[localPlayerServerIndex]
// Sets loadingArea = false (stops "Loading..." screen)
```

**Critical notes:**
- Packet length is 7 bytes total: 2 (length) + 2 (opcode) + 3 (data - but we send 5 bytes of bit data)
- The bit offset in server code starts at 0 because we're writing to a clean buffer
- The bit offset in client code starts at 8 because `pdata[0]` contains the opcode byte
- This packet triggers map loading from `land63.jag` and `maps63.jag` files

### 8-11. Region Data Packets (Empty for now)

All follow the same simple structure:

**SV_REGION_OBJECTS (Opcode 48):**
```
[2 bytes] Opcode (48)
[2 bytes] Object count (0)
```

**SV_REGION_WALL_OBJECTS (Opcode 91):**
```
[2 bytes] Opcode (91)
[2 bytes] Wall object count (0)
```

**SV_REGION_GROUND_ITEMS (Opcode 99):**
```
[2 bytes] Opcode (99)
[2 bytes] Ground item count (0)
```

**SV_REGION_NPCS (Opcode 79):**
```
[2 bytes] Opcode (79)
[2 bytes] NPC count (0)
```

---

## Player Appearance System

### 12. SV_REGION_PLAYER_UPDATE (Opcode 234)

**Purpose:** Update player appearance, equipment, name, and combat level

**Structure:**
```
[2 bytes] Opcode (234)
[2 bytes] Update count (number of player updates in this packet)

For each update:
  [2 bytes] Player server index (which player to update)
  [1 byte]  Update type (5 = appearance)
  
  If updateType == 5:
    [2 bytes] Server ID (player's server index, sent again)
    [8 bytes] Username hash (long)
    [1 byte]  Equipped items count (always 12 for players)
    
    For i = 0 to 11:
      [1 byte] Equipped item sprite ID
    
    [1 byte]  Hair color index
    [1 byte]  Top (shirt) color index
    [1 byte]  Bottom (pants) color index
    [1 byte]  Skin color index
    [1 byte]  Combat level
    [1 byte]  Skull visible (0=no, 1=yes for PKing)
```

**Equipped items mapping (12 slots):**
```
Slot 0:  Cape/back (0 = nothing)
Slot 1:  Hair/head (sprite ID from headType)
Slot 2:  Body/torso (sprite ID from bodyGender + 1)
Slot 3:  Legs (sprite ID from bodyGender + 2)
Slot 4:  Boots (0 = nothing)
Slot 5:  Gloves (0 = nothing)
Slot 6:  Right hand weapon (0 = nothing)
Slot 7:  Left hand shield (0 = nothing)
Slot 8-11: Additional slots (0 = nothing)
```

**Example - Default male player appearance:**
```
Update count: 1
Player index: 1
Update type: 5
Server ID: 1
Username hash: 24831 (for "Red")
Equipment count: 12
Equipped items: [0, 1, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0]
  - Slot 1: 1 (male head style 1)
  - Slot 2: 2 (male torso)
  - Slot 3: 3 (male legs)
Hair color: 2 (brown)
Top color: 8 (green)
Bottom color: 14 (brown)
Skin color: 0 (light)
Combat level: 3
Skull: 0 (not visible)
```

**Client behavior:**
```java
// Reads update count from pdata[1-2]
int updateCount = Utility.getUnsignedShort(pdata, 1);

// For each update:
int playerId = Utility.getUnsignedShort(pdata, offset);
GameCharacter character = playerServer[playerId];

if (character != null && updateType == 5) {
  // Read appearance data
  character.serverId = readShort();
  character.hash = readLong();
  character.name = Utility.hash2username(hash);
  
  int equippedCount = readByte();
  for (int i = 0; i < equippedCount; i++) {
    character.equippedItem[i] = readByte();
  }
  
  character.colourHair = readByte();
  character.colourTop = readByte();
  character.colourBottom = readByte();
  character.colourSkin = readByte();
  character.level = readByte();
  character.skullVisible = readByte();
}
```

**Rendering:**
- Player is only visible if `character.colourBottom != 255`
- Each equipped item slot is rendered as a sprite layer
- Sprites are colored using the hair/top/bottom/skin color palettes
- If no equipment is in a slot (value 0), that body part is invisible
- Minimum required for visibility: slots 1, 2, 3 (head, body, legs)

---

## Packet Format Details

### String Encoding

Strings are length-prefixed:
```
[2 bytes] String length (short)
[N bytes] String characters (UTF-8)
```

**Server (Buffer class):**
```java
out.putString("username");
// Writes: [2 bytes length][bytes of "username"]
```

**Client:**
```java
int length = readShort();
byte[] chars = readBytes(length);
String str = new String(chars);
```

### Username Hashing

Usernames are hashed to 64-bit longs for efficient storage and comparison.

**Hash algorithm:** See `Utility.username2hash()` and `Utility.hash2username()`

**Example:**
```
Username: "Red"
Hash: 24831 (long)
```

Used in:
- Friend lists
- Ignore lists
- Player appearance packets
- Chat messages

### Color Palettes

Colors are stored as palette indices (0-255):

**Hair colors (characterHairColours):**
```
0: Bald/none
1: Blonde
2: Brown
3: Black
etc.
```

**Top/Bottom colors (characterTopBottomColours):**
```
0-15: Various colors for clothing
8: Green (default shirt)
14: Brown (default pants)
```

**Skin colors (characterSkinColours):**
```
0: Light
1: Medium
2: Dark
etc.
```

---

## Combat Level Calculation

The combat level is calculated server-side using the RSC formula:

```java
public int getCombatLevel() {
    int attack = currentStats[0];
    int defense = currentStats[1];
    int strength = currentStats[2];
    int hitpoints = currentStats[3];
    int ranged = currentStats[4];
    int prayer = currentStats[5];
    int magic = currentStats[6];
    
    double base = (defense + hitpoints + prayer / 2.0) / 4.0;
    double melee = (attack + strength) / 4.0;
    double range = (ranged * 3.0 / 2.0) / 4.0;
    double mage = (magic * 3.0 / 2.0) / 4.0;
    
    return (int) (base + Math.max(melee, Math.max(range, mage)));
}
```

**Starting player (all stats = 1, HP = 10):**
```
base = (1 + 10 + 1/2) / 4 = 2.875
melee = (1 + 1) / 4 = 0.5
Combat level = 2.875 + 0.5 = 3.375 → 3
```

---

## Map Loading

### Coordinate System

**World coordinates:**
- Full world is 944x944 tiles (planeWidth x planeHeight)
- Player spawns at (1400, 1400)

**Section coordinates:**
- World is divided into 48x48 tile sections
- Section X = (worldX + 24) / 48
- Section Y = (worldY + 24) / 48

**Example - Player at (1400, 1400):**
```
sectionX = (1400 + 24) / 48 = 29.67 → 29
sectionY = (1400 + 24) / 48 = 29.67 → 29
```

### JAG File Loading

The client loads map data from JAG archive files:

**land63.jag** - Landscape/terrain data  
**maps63.jag** - Object placement data

**Map naming convention:**
```
Format: m{plane}{sectionX}{sectionY}
Example: m04949 = plane 0, section 49, section 49

Plane levels:
- 0: Ground level
- 1: First floor
- 2: Second floor
```

**Loading process:**
1. `SV_REGION_PLAYERS` packet received with player position
2. Client calls `loadNextRegion(localRegionX, localRegionY)`
3. Calculates section coordinates
4. Loads 4 sections (2x2 grid around player)
5. For each section, loads all 3 plane levels
6. Terrain data extracted from JAG files
7. `loadingArea = false` (game becomes playable)

---

## Movement System

### Overview

The movement system allows players to walk through the game world using a client-server synchronized approach. The client calculates pathfinding locally and sends walk commands to the server, which processes movement step-by-step using a game tick system (640ms per tick).

### CL_WALK Packet (Opcode 187)

**Direction:** Client → Server

**Status:** ✅ FULLY IMPLEMENTED with step-by-step movement

**Format (NEW - migrated):**
```
[2 bytes] Length (includes opcode + data)
[2 bytes] Opcode (187 for normal walk, 16 for walk-to-action)
[2 bytes] Start X (absolute world coordinate - where client thinks player is)
[2 bytes] Start Y (absolute world coordinate - where client thinks player is)
[N pairs] Walk path waypoints (signed byte pairs: deltaX, deltaY from start)
```

---

### CRITICAL: Waypoint Interpretation

**How the deltas work:**
- Each delta pair represents a **waypoint** relative to the START position
- Deltas are **CUMULATIVE from start**, NOT relative to previous step
- Each delta is a **signed byte** (-128 to +127)

**Example:**
```
Start position: (100, 100)
Path deltas: [(1, 0), (2, 0), (3, 0)]

Waypoints created:
1. (100 + 1, 100 + 0) = (101, 100)
2. (100 + 2, 100 + 0) = (102, 100)
3. (100 + 3, 100 + 0) = (103, 100)

Server creates smooth steps:
(100,100) -> (101,100) -> (102,100) -> (103,100)
```

**NOT like this (incorrect interpretation):**
```
(100,100) + (1,0) = (101,100)
(101,100) + (2,0) = (103,100)  <- WRONG!
(103,100) + (3,0) = (106,100)  <- WRONG!
```

---

### Client Implementation

**Packet Building (`GameConnection.java`):**
```java
protected void sendWalkPacket(int targetX, int targetY, byte[] walkPath, 
                              int stepCount, boolean isAction) {
    Buffer out = new Buffer();
    out.putShort(isAction ? Opcodes.Client.CL_WALK_ACTION.value : 
                            Opcodes.Client.CL_WALK.value);
    out.putShort((short) targetX);  // Absolute world coordinates
    out.putShort((short) targetY);
    
    // Walk path waypoints (cumulative deltas from start)
    if (walkPath != null && stepCount > 0) {
        out.put(walkPath, 0, stepCount * 2);
    }
    
    OutputStream outputStream = socket.getOutputStream();
    outputStream.write(out.toArrayWithLen());
    outputStream.flush();
}
```

**Path Calculation (`mudclient.java`):**
```java
// startX, startY are region-relative coordinates
// walkPathX[], walkPathY[] contain the calculated path
byte[] walkPath = new byte[(steps + 1) * 2];
int pathIndex = 0;
for (int l1 = steps; l1 >= 0 && l1 > steps - 25; l1--) {
    // Deltas relative to start position
    walkPath[pathIndex++] = (byte) (walkPathX[l1] - startX);
    walkPath[pathIndex++] = (byte) (walkPathY[l1] - startY);
}
int actualSteps = pathIndex / 2;

// Send with absolute world coordinates
sendWalkPacket(startX + regionX, startY + regionY, walkPath, actualSteps, false);
```

**Key Points:**
- Client uses pathfinding to calculate route around obstacles
- Maximum 25 waypoints per packet
- Empty path (stepCount=0) = player clicked on current tile

---

### Server Implementation

**Handler (`CL_WalkHandler.java`):**

1. **Read packet data:**
```java
short startX = data.getShort();  // Where client thinks player is
short startY = data.getShort();
int stepCount = data.remaining() / 2;
```

2. **Handle client-server desync:**
```java
int currentX = player.getX();  // Server's current position
int currentY = player.getY();

if (currentX != startX || currentY != startY) {
    // Create intermediate steps to sync positions
    createStraightLineSteps(player, currentX, currentY, startX, startY);
    currentX = startX;
    currentY = startY;
}
```

3. **Process waypoints:**
```java
for (int i = 0; i < stepCount; i++) {
    byte deltaX = data.getByte();
    byte deltaY = data.getByte();
    
    // Calculate waypoint (cumulative from start)
    int waypointX = startX + deltaX;
    int waypointY = startY + deltaY;
    
    // Create smooth steps from current position to waypoint
    createStraightLineSteps(player, currentX, currentY, waypointX, waypointY);
    
    // Update for next waypoint
    currentX = waypointX;
    currentY = waypointY;
}
```

4. **Create smooth steps:**
```java
private void createStraightLineSteps(Player player, int startX, int startY, 
                                     int endX, int endY) {
    // Determine direction (-1, 0, or 1)
    int deltaX = startX < endX ? 1 : (startX > endX ? -1 : 0);
    int deltaY = startY < endY ? 1 : (startY > endY ? -1 : 0);
    
    // Handle non-diagonal paths
    if (Math.abs(endX - startX) != Math.abs(endY - startY)) {
        if (endX - startX != 0) {
            deltaY = 0;  // Move only in X
        } else {
            deltaX = 0;  // Move only in Y
        }
    }
    
    // Add steps to walk queue
    int totalSteps = Math.abs(endX - startX) + Math.abs(endY - startY);
    int currentSteps = 0;
    while (currentSteps < totalSteps) {
        player.addToWalkQueue(deltaX, deltaY);
        currentSteps += Math.abs(deltaX) + Math.abs(deltaY);
    }
}
```

---

### Game Tick System

**Tick Processing (`GameTick.java`):**
- Runs every **640ms** (matching original RSC timing)
- Processes **one step** from each player's walk queue per tick
- Sends position update after each step

```java
private void processPlayerMovement() {
    for (Player player : world.getAllPlayers()) {
        if (!player.hasWalkSteps()) continue;
        
        // Get next step from queue
        int[] step = player.getWalkQueue().poll();
        int deltaX = step[0];
        int deltaY = step[1];
        
        // Update position
        int newX = player.getX() + deltaX;
        int newY = player.getY() + deltaY;
        player.setX((short) newX);
        player.setY((short) newY);
        
        // Send update to client
        CL_WalkHandler.sendPositionUpdate(player);
    }
}
```

**Result:** Smooth walking animation at ~1.5 steps per second

---

### SV_REGION_PLAYERS Position Update (Opcode 191)

**Direction:** Server → Client

**Purpose:** Update player position after each movement step

**Format:**
```
[2 bytes] Length (7 bytes total)
[2 bytes] Opcode (191)
[5 bytes] Bit-packed position data:
  [11 bits] Player X (absolute world coordinate)
  [13 bits] Player Y (absolute world coordinate)
  [4 bits]  Animation (0 = standing, 1-8 = walking directions)
  [8 bits]  Known player count (0 = just local player)
```

**Bit-packing Details:**
```java
byte[] bitData = new byte[5];
int bitOffset = 0;

NetHelper.setBitMask(bitData, 0, 11, playerX);   // Bits 0-10
NetHelper.setBitMask(bitData, 11, 13, playerY);  // Bits 11-23
NetHelper.setBitMask(bitData, 24, 4, animation); // Bits 24-27
NetHelper.setBitMask(bitData, 28, 8, playerCnt); // Bits 28-35
```

**Client Processing:**
```java
// Client reads starting at bit 8 (pdata[0] = opcode)
int bitOffset = 8;
int absoluteX = Utility.getBitMask(pdata, bitOffset, 11);
bitOffset += 11;
int absoluteY = Utility.getBitMask(pdata, bitOffset, 13);

// Convert to region-relative coordinates
loadNextRegion(absoluteX, absoluteY);  // May load new map sections
int localX = absoluteX - regionX;
int localY = absoluteY - regionY;

// Convert to screen pixels
int pixelX = localX * magicLoc + 64;
int pixelY = localY * magicLoc + 64;
```

---

### Example Walk Sequence

**Scenario:** Player at (1400, 1400) clicks to walk to (1403, 1403)

1. **Client calculates path:**
   - Pathfinding: (1400,1400) → (1401,1401) → (1402,1402) → (1403,1403)
   - Waypoints: [(1,1), (2,2), (3,3)] (cumulative deltas from start)

2. **Client sends CL_WALK:**
   ```
   Start: (1400, 1400)
   Steps: [(1,1), (2,2), (3,3)]
   ```

3. **Server receives and processes:**
   ```
   Server position: (1400, 1400)
   Client position: (1400, 1400) ✓ synced
   
   Waypoint 0: (1400+1, 1400+1) = (1401, 1401)
     Queue: (1, 1)
   Waypoint 1: (1400+2, 1400+2) = (1402, 1402)
     Queue: (1, 1)
   Waypoint 2: (1400+3, 1400+3) = (1403, 1403)
     Queue: (1, 1)
   
   Total queued: 3 steps
   ```

4. **Game ticks process movement:**
   ```
   Tick 1 (T+0ms):    (1400,1400) + (1,1) = (1401,1401) -> Send update
   Tick 2 (T+640ms):  (1401,1401) + (1,1) = (1402,1402) -> Send update
   Tick 3 (T+1280ms): (1402,1402) + (1,1) = (1403,1403) -> Send update
   ```

5. **Client receives position updates:**
   - Displays player smoothly walking diagonally
   - Each update triggers character position refresh
   - Total time: ~2 seconds for 3 tiles

---

### Position Desync Handling

**Problem:** Client and server positions can become desynced due to:
- Network latency
- Packet loss
- Race conditions between movement and updates

**Solution:**
```java
if (currentX != startX || currentY != startY) {
    Logger.debug("Position desync detected");
    createStraightLineSteps(player, currentX, currentY, startX, startY);
}
```

**Example:**
```
Server thinks: (1400, 1400)
Client thinks: (1402, 1400)

Server creates intermediate steps:
(1400,1400) -> (1401,1400) -> (1402,1400)

Then processes client's path from (1402, 1400)
```

---

### Walk-to-Action (Opcode 16)

**Same format as CL_WALK** but triggers an action upon reaching destination:
- Attacking NPC/player
- Using object
- Picking up item
- Trading with player

Currently processed identically to CL_WALK. Action handling TODO.

---

### Performance Characteristics

- **Tick Rate:** 640ms (1.5625 ticks/second)
- **Movement Speed:** ~1.5 tiles/second
- **Pathfinding:** Client-side (no server load)
- **Max Path Length:** 25 waypoints
- **Network Efficiency:** ~6-56 bytes per walk command
- **Smooth Animation:** Yes (step-by-step processing)

---

### Current Limitations & TODOs

- ✅ Step-by-step movement implemented
- ✅ Position desync handling
- ✅ Client-server synchronization
- ⏳ Walking animation direction (always 0 = standing)
- ⏳ Broadcast movement to nearby players
- ⏳ Collision detection (server-side validation)
- ⏳ Run/walk toggle (speed variation)
- ⏳ Walk-to-action completion triggers

---

## Implementation Status

### ✅ Completed

#### Core Systems
- Connection and session management
- Login authentication (CL_SESSION, CL_LOGIN)
- World info transmission (SV_WORLD_INFO)
- Player stats system (18 skills, SV_PLAYER_STAT_LIST)
- Inventory system (empty, SV_INVENTORY_ITEMS)
- Friend/ignore lists (empty, SV_FRIEND_LIST, SV_IGNORE_LIST)
- Privacy settings (SV_PRIVACY_SETTINGS)

#### World & Rendering
- Region player positioning (bit-packed, SV_REGION_PLAYERS)
- Map loading from JAG files (land63.jag, maps63.jag)
- Player appearance system (SV_REGION_PLAYER_UPDATE)
- Player rendering with equipment (12 equipment slots)
- Basic player model (head, body, legs required for visibility)

#### Movement System ✅ **FULLY IMPLEMENTED**
- **Packet migration to NEW format** (CL_WALK, CL_WALK_ACTION)
- **Game tick system** (640ms interval)
- **Step-by-step movement** processing
- **Walk queue** with waypoint interpretation
- **Position desync** handling
- **Smooth client-server** synchronization
- **Position updates** (SV_REGION_PLAYERS) after each step

### 🚧 In Progress

- Movement position broadcasting (multiplayer visibility)
- Walking animation direction (currently always standing)
- Server-side collision detection
- Walk-to-action completion triggers

### 📋 TODO

#### Immediate
- Broadcast movement to nearby players
- Set walking animation based on movement direction
- Validate paths against collision map
- Implement walk-to-action handlers

#### Future Features
- Player-to-player interaction
- Combat system
- Trading system
- Item pickup/drop
- Skill training
- Quest system
- Full multi-player synchronization
- Chat system improvements
- Object/wall/item interaction

---

## Debugging Tips

### Packet Inspection

**Enable debug logging:**
```java
Logger.setLevel(Logger.LEVEL_DEBUG);
```

**Common issues:**

1. **"Loading... Please wait" stuck**
   - Check `SV_REGION_PLAYERS` packet sent
   - Verify bit-packing is correct
   - Ensure coordinates have terrain data in JAG files

2. **Player not visible**
   - Check `colourBottom != 255` (255 = invisible)
   - Verify equipped items array has values in slots 1, 2, 3
   - Check appearance packet sent after region players

3. **Disconnects after login**
   - Check packet length calculations
   - Verify `pdata[0]` contains opcode for old handlers
   - Check for exceptions in packet processing

### Useful Debug Logs

**Client:**
```java
Logger.debug("Received packet: " + opcode + " (length=" + length + ")");
Logger.debug("Player rendering: colourBottom=" + character.colourBottom);
Logger.debug("Created localPlayer: serverIndex=" + localPlayerServerIndex);
```

**Server:**
```java
Logger.info("Player " + username + " logged in successfully");
Logger.debug("Sent region players data for position " + x + ", " + y);
Logger.debug("Appearance: equipped=" + Arrays.toString(equippedItems));
```

---

## Key Lessons Learned

### Packet Format Migration is Critical

The single most important architectural decision in this project is the **incremental migration from OLD (1-byte opcode) to NEW (2-byte opcode) packet format**.

**Why this matters:**
- The OLD format (`ClientStream`/`Packet_`) is incompatible with modern server architecture
- Trying to support both formats simultaneously in `ClientHandler` causes bugs and confusion
- The ONLY correct approach is to migrate packets one-by-one at the SOURCE (client-side)

**Success pattern:**
1. Create new `send*Packet()` method in `GameConnection` using `Buffer`
2. Replace old `clientStream.newPacket()` calls in `mudclient.java`
3. Server handlers work immediately (already expect NEW format)
4. Test thoroughly, then move to next packet

**DO NOT:**
- ❌ Try to make `ClientHandler` support both formats
- ❌ Create "interceptors" or "converters" in the network layer
- ❌ Modify `Packet_` or `ClientStream` to change their format

**DO:**
- ✅ Replace packet sending at the source (client-side)
- ✅ Migrate one packet type at a time
- ✅ Test each migration thoroughly
- ✅ Document migrated packets

---

## Protocol Version

**Current implementation:** Based on RSC build 203/204  
**JAG file version:** 63 (land63.jag, maps63.jag)

---

## References

- Original RSC client decompilation (mudclient.java)
- RSC protocol documentation (doc/server-to-client.txt, doc/client-to-server.txt)
- GameData.java (sprite/animation definitions)

---

**Document maintained by:** Runarius Development Team  
**Last major update:** 2025-10-10 - Walk packet migration completed ✅
