# Instructions for Runarius

## Project Overview

**Runarius** is a RuneScape Classic (RSC) server and client implementation written in Java. This project is a complete reimplementation of the classic MMORPG RuneScape from 2001-2004, including both the game client and server infrastructure.

### Technology Stack
- **Language:** Java 11+
- **Build System:** Apache Ant
- **Architecture:** Client-server with socket-based binary protocol
- **Graphics:** Custom 3D software renderer (no OpenGL)
- **Data Format:** JAG archives (custom compression format)

---

## Critical Architecture Information

### 1. PACKET FORMAT MIGRATION (CRITICAL!)

**⚠️ THE MOST IMPORTANT THING TO UNDERSTAND ⚠️**

The project is migrating from OLD packet format to NEW packet format. These are **INCOMPATIBLE**!

**OLD Format (Legacy `Packet_` / `ClientStream`):**
```
[2 bytes] Length (big-endian, includes opcode + data)
[1 byte]  Opcode
[N bytes] Data
```
- Used by: All legacy in-game packets sent via `clientStream.newPacket()`
- Problem: Server expects 2-byte opcodes, reads wrong data!

**NEW Format (`Buffer` class):**
```
[2 bytes] Length (big-endian, includes opcode + data)
[2 bytes] Opcode (big-endian short)
[N bytes] Data
```
- Used by: Session, Login, and NEW migrated packets
- Format produced by: `Buffer.toArrayWithLen()`
- Server expects: This format in `ClientHandler`

**MIGRATION STRATEGY (DO THIS FOR EVERY PACKET):**

1. **Client-side:** Create new `send*Packet()` method in `GameConnection.java`:
   ```java
   protected void sendWalkPacket(int targetX, int targetY, byte[] walkPath, int stepCount, boolean isAction) {
       Buffer out = new Buffer();
       out.putShort(Opcodes.Client.CL_WALK.value);  // 2-byte opcode!
       out.putShort((short) targetX);
       out.putShort((short) targetY);
       out.put(walkPath, 0, stepCount * 2);
       
       OutputStream outputStream = socket.getOutputStream();
       outputStream.write(out.toArrayWithLen());  // NEW format!
       outputStream.flush();
   }
   ```

2. **Client-side:** Replace OLD calls in `mudclient.java`:
   ```java
   // OLD (DELETE THIS):
   super.clientStream.newPacket(Opcodes.Client.CL_WALK.value);
   super.clientStream.putShort(targetX);
   super.clientStream.putShort(targetY);
   super.clientStream.sendPacket();
   
   // NEW (USE THIS):
   sendWalkPacket(targetX, targetY, walkPath, stepCount, false);
   ```

3. **Server-side:** Handler in `ServerSidePacketHandlers` works automatically!
   ```java
   // Already registered - no changes needed
   packetHandlers.put(Opcodes.Client.CL_WALK, new CL_WalkHandler()::handle);
   ```

**NEVER:**
- ❌ Mix old and new packet formats in same connection
- ❌ Try to "convert" packets in `ClientHandler` - migrate at source!
- ❌ Use `clientStream.newPacket()` for new code - use `Buffer` instead!
- ❌ Forget to flush output stream after sending

**DO:**
- ✅ Migrate one packet type at a time
- ✅ Test each migration thoroughly
- ✅ Eventually delete `ClientStream` and `Packet_` entirely
- ✅ Document each migrated packet

### 2. Client-Server Protocol

**Wire Format - All packets:**
```
[2 bytes] Total packet length (includes these 2 bytes)
[2 bytes] Opcode (packet type identifier)
[N bytes] Packet data
```

**CRITICAL:** Old packet handlers expect `pdata[0]` to contain the opcode byte!
- Modern handlers receive data WITHOUT opcode prefix
- Legacy `mudclient.handleIncomingPacket()` expects data WITH opcode at `pdata[0]`
- `GameConnection.handlePacket()` adds opcode byte for backward compatibility

### 2. Client-Server Protocol

**Wire Format - All packets:**
```
[2 bytes] Total packet length (includes these 2 bytes)
[2 bytes] Opcode (packet type identifier)
[N bytes] Packet data
```

**CRITICAL:** Old packet handlers expect `pdata[0]` to contain the opcode byte!
- Modern handlers receive data WITHOUT opcode prefix
- Legacy `mudclient.handleIncomingPacket()` expects data WITH opcode at `pdata[0]`
- `GameConnection.handlePacket()` adds opcode byte for backward compatibility

### 3. Dual Handler System (IMPORTANT!)

The codebase is transitioning from old to new packet handling:

**Old System (Legacy):**
- `mudclient.handleIncomingPacket()` - Monolithic method handling all packets
- Data format: `pdata[0]` = opcode, `pdata[1+]` = actual data

**New System (Target):**
- `ClientSidePacketHandlers` - Registry of individual packet handlers
- Each handler implements `IClientPacketHandler`
- Data format: Pure packet data (no opcode prefix)

**Migration Strategy:**
1. New handlers are registered in `ClientSidePacketHandlers`
2. Unregistered packets fall back to `mudclient.handleIncomingPacket()`
3. DO NOT modify old handlers - create new ones instead
4. Eventually, remove all old handler code

### 4. Bit-Packed Data (COMPLEX!)

Some packets use **bit-packing** instead of byte alignment:

**Example: SV_REGION_PLAYERS**
```java
// Server creates bit buffer starting at bit 0
byte[] bitData = new byte[5];
int bitOffset = 0;
NetHelper.setBitMask(bitData, bitOffset, 11, regionX);

// Client reads starting at bit 8 (pdata[0] is opcode!)
int k7 = 8;
localRegionX = Utility.getBitMask(pdata, k7, 11);
```

**CRITICAL:** Server writes at bit 0, client reads at bit 8 due to opcode byte!

### 5. Buffer Class Usage

**Server-side packet building:**
```java
Buffer out = new Buffer();
out.putShort(Opcodes.Server.SV_PACKET_NAME.value);
out.putInt(someValue);
out.putString("text");

// ALWAYS use toArrayWithLen() to add length prefix
player.getSocket().getOutputStream().write(out.toArrayWithLen());
player.getSocket().getOutputStream().flush();
```

**NEVER:**
- Send `out.toArray()` - missing length prefix!
- Forget to flush the output stream
- Reuse Buffer objects without resetting

### 6. Coordinate System

**World Coordinates:**
- Full world: 944 x 944 tiles
- Coordinates stored as absolute tile positions
- Example spawn: (1400, 1400)

**Section Coordinates:**
- World divided into 48x48 tile sections
- Section calculation: `sectionX = (worldX + 24) / 48`
- Used for map loading from JAG files

**Map Naming:**
```
Format: m{plane}{sectionX}{sectionY}
Examples:
  m04949 = Plane 0, Section 49, Section 49
  m14949 = Plane 1, Section 49, Section 49
```

### 7. Player Appearance System

**Equipment Slots (12 total):**
```
Slot 0:  Cape/back
Slot 1:  Hair/head (REQUIRED for visibility)
Slot 2:  Body/torso (REQUIRED for visibility)
Slot 3:  Legs (REQUIRED for visibility)
Slot 4:  Boots
Slot 5:  Gloves
Slot 6:  Right hand (weapon)
Slot 7:  Left hand (shield)
Slot 8-11: Additional equipment
```

**CRITICAL:** Players need at least slots 1, 2, 3 to be visible!
- If `character.colourBottom == 255`, player is INVISIBLE
- Equipment values are sprite/animation IDs, not item IDs
- Value 0 = nothing equipped in that slot

### 8. JAG File System

**Data Files:**
- `land63.jag` - Terrain/landscape data
- `maps63.jag` - Object placement data
- `media58.jag` - Sprites and animations
- `models36.jag` - 3D model data
- `entity24.jag` - Character models
- etc.

**JAG Format:**
- Custom archive format with compression
- Loaded via `GameData.loadJag()`
- Contains multiple entries indexed by name

### 9. Opcodes Enum

**Located in:** `src/common/Opcodes.java`

**Structure:**
```java
public enum Server {
    SV_WORLD_INFO((short) 25),
    SV_PLAYER_STAT_LIST((short) 156),
    // etc.
}

public enum Client {
    CL_SESSION((short) 0),
    CL_LOGIN((short) 0),
    // etc.
}
```

**ALWAYS:**
- Use enum names, never raw opcode numbers
- Access via `Opcodes.Server.SV_NAME.value`
- Check both client and server enums

---

## Common Pitfalls & Gotchas

### ❌ DON'T:
1. **Send packets without length prefix** - Use `toArrayWithLen()` always
2. **Modify old mudclient.java handlers** - Create new handlers instead
3. **Use raw opcode numbers** - Use `Opcodes` enum
4. **Forget bit offset mismatch** - Server at 0, client at 8
5. **Send appearance before region players** - Client crashes!
6. **Set colourBottom to 255** - Makes player invisible
7. **Send 0 equipment slots** - Player won't be visible
8. **Hardcode coordinates** - Use constants or config

### ✅ DO:
1. **Follow packet sequence** - See `protocol-implementation.md`
2. **Flush output streams** - Packets won't send otherwise
3. **Validate packet lengths** - Prevent buffer overflows
4. **Add debug logging** - Use `Logger.debug()` extensively
5. **Check null references** - Especially for `playerServer[]` array
6. **Test with clean build** - `ant clean jar-client jar-server`
7. **Document new packets** - Update `protocol-implementation.md`
8. **Handle exceptions properly** - Don't swallow errors silently

---

## File Organization

### Server (`src/server/`)
- `Server.java` - Main server entry point
- `CL_*Handler.java` - Client-to-server packet handlers
- `ServerSidePacketHandlers.java` - Packet handler registry
- `Player.java` - Player entity and state
- `GameWorld.java` - World state and player management
- `ClientHandler.java` - Individual client connection handler

### Client (`src/client/`)
- `mudclient.java` - Main client (7600+ lines, legacy code)
- `GameConnection.java` - Network connection handling
- `ClientSidePacketHandlers.java` - New packet handler registry
- `SV_*Handler.java` - Server-to-client packet handlers
- `GameData.java` - Game data loading (JAG files)
- `Scene.java` - 3D rendering engine
- `Surface.java` - 2D drawing and sprites
- `World.java` - World data and terrain

### Common (`src/common/`)
- `Buffer.java` - Byte buffer for packet building/reading
- `Opcodes.java` - Packet opcode definitions
- `Logger.java` - Logging system
- `Util.java` - Utility functions
- `NetHelper.java` - Network utilities (bit-packing)

### Documentation (`doc/`)
- `protocol-implementation.md` - **READ THIS FIRST** - Complete protocol docs
- `client-to-server.txt` - Client packet reference
- `server-to-client.txt` - Server packet reference
- `client-map.csv` - Opcode mappings

### Data (`data204/`)
- JAG archive files
- Game configuration
- Sprite and model data

---

## Build & Run Commands

### Build
```bash
ant clean           # Clean build artifacts
ant jar-client      # Build client JAR
ant jar-server      # Build server JAR
ant                 # Build both
```

### Run
```bash
ant run-server      # Start server (port 43594)
ant run-client      # Start client
```

### Development
```bash
ant -q jar-server   # Quick server rebuild (quiet)
ant -q jar-client   # Quick client rebuild (quiet)
```

---

## Debugging Guide

### Enable Debug Logging
```java
// In client main:
Logger.setLevel(Logger.LEVEL_DEBUG);

// In server:
Logger.setLevel(Logger.LEVEL_DEBUG);
```

### Common Debug Scenarios

**1. Packet Not Received:**
```java
// Server
Logger.debug("Sending SV_PACKET_NAME to " + player.getUsername());
out.write(buffer.toArrayWithLen());
out.flush();  // <- Don't forget!

// Client
Logger.debug("Received packet: " + opcode + " (length=" + length + ")");
```

**2. Player Not Visible:**
```java
Logger.debug("Rendering " + playerCount + " players");
Logger.debug("Player colourBottom=" + character.colourBottom);
Logger.debug("Equipped items: " + Arrays.toString(character.equippedItem));
```

**3. Connection Issues:**
```java
Logger.debug("Socket connected: " + socket.isConnected());
Logger.debug("Stream available: " + inputStream.available());
```

**4. Map Loading Problems:**
```java
Logger.debug("loadNextRegion: lx=" + lx + ", ly=" + ly);
Logger.debug("Loading section: x=" + sectionX + ", y=" + sectionY);
Logger.debug("Terrain data size: " + terrainData.length);
```

---

## Protocol Sequence Reference

### Login Flow (11 packets)
1. `SV_WORLD_INFO` → Sets world dimensions, player index
2. `SV_PLAYER_STAT_LIST` → 18 skills + experience
3. `SV_INVENTORY_ITEMS` → Player inventory
4. `SV_FRIEND_LIST` → Friends list
5. `SV_IGNORE_LIST` → Ignore list
6. `SV_PRIVACY_SETTINGS` → Privacy preferences
7. `SV_REGION_PLAYERS` → **Triggers map load** (bit-packed!)
8. `SV_REGION_OBJECTS` → World objects
9. `SV_REGION_WALL_OBJECTS` → Walls/fences
10. `SV_REGION_GROUND_ITEMS` → Ground items
11. `SV_REGION_NPCS` → NPCs
12. `SV_REGION_PLAYER_UPDATE` → **Player appearance** (must be LAST!)

**CRITICAL ORDER:** 
- `SV_REGION_PLAYERS` MUST come before appearance
- Appearance MUST come after all region packets
- Changing order causes crashes or invisible players

---

## Code Style Guidelines

### Naming Conventions
- Packets: `SV_PACKET_NAME` (server) or `CL_PACKET_NAME` (client)
- Handlers: `SV_PacketNameHandler.java`
- Constants: `UPPER_SNAKE_CASE`
- Variables: `camelCase`
- Classes: `PascalCase`

### Packet Handler Template
```java
public class SV_NewPacketHandler implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        // Read packet data
        int value = data.getInt();
        String text = data.getString();
        
        Logger.debug("SV_NEW_PACKET: value=" + value + ", text=" + text);
        
        // Process packet
        // ...
    }
}
```

### Server Packet Sending Template
```java
private void sendNewPacket(Player player, int value) throws IOException {
    Buffer out = new Buffer();
    out.putShort(Opcodes.Server.SV_NEW_PACKET.value);
    out.putInt(value);
    
    player.getSocket().getOutputStream().write(out.toArrayWithLen());
    player.getSocket().getOutputStream().flush();
    
    Logger.debug("Sent SV_NEW_PACKET to " + player.getUsername());
}
```

---

## Important Constants

### Network
```java
DEFAULT_PORT = 43594
SESSION_TIMEOUT = 30000 ms
MAX_PACKET_SIZE = 5000 bytes
```

### World
```java
WORLD_WIDTH = 944 tiles
WORLD_HEIGHT = 944 tiles
SECTION_SIZE = 48 tiles
DEFAULT_SPAWN_X = 1400
DEFAULT_SPAWN_Y = 1400
PLANE_COUNT = 4 (0-3)
```

### Skills
```java
SKILL_COUNT = 18
SKILL_ATTACK = 0
SKILL_DEFENSE = 1
SKILL_STRENGTH = 2
SKILL_HITPOINTS = 3
// etc. (see protocol-implementation.md)
```

### Player
```java
MAX_INVENTORY_SIZE = 30
MAX_EQUIPMENT_SLOTS = 12
MAX_FRIENDS = 100
MAX_IGNORE = 100
INVISIBLE_COLOR = 255  // colourBottom value for invisible
```

---

## Testing Checklist

When implementing new features:

- [ ] Server compiles without errors
- [ ] Client compiles without errors
- [ ] Server starts and listens on port 43594
- [ ] Client can connect and login
- [ ] Packets sent in correct order
- [ ] Packet lengths are correct
- [ ] Debug logging shows expected values
- [ ] No exceptions in server/client output
- [ ] Player appears in game world
- [ ] Features work as expected
- [ ] Documentation updated (`protocol-implementation.md`)

---

## Resources

### Key Documentation Files
- `doc/protocol-implementation.md` - **Primary reference**
- `doc/client-to-server.txt` - Client packet specs
- `doc/server-to-client.txt` - Server packet specs

### External References
- RuneScape Classic Wiki: https://classic.runescape.wiki/
- RSC Decompilation projects (for reference only)

---

## Version Information

- **Protocol Version:** RSC Build 203/204
- **JAG Version:** 63 (land63.jag, maps63.jag, etc.)
- **Java Version:** 11+
- **Client Version:** 203 (sent in CL_LOGIN packet)

---

## Quick Reference Commands

```bash
# Full rebuild
ant clean && ant && ant run-server

# Quick server test
ant -q jar-server && ant run-server

# Quick client test  
ant -q jar-client && ant run-client

# Check for compilation errors
ant jar-server jar-client
```

---

## When Things Go Wrong

### "Loading... Please wait" stuck
→ Check `SV_REGION_PLAYERS` packet bit-packing  
→ Verify coordinates have terrain in JAG files  
→ Enable debug logging in `loadNextRegion()`

### Player not visible
→ Check `colourBottom != 255`  
→ Verify equipment slots 1, 2, 3 have values  
→ Ensure appearance packet sent AFTER region players

### Client disconnects immediately
→ Check packet length calculations  
→ Verify `pdata[0]` contains opcode for old handlers  
→ Look for exceptions in server output

### Server crashes on login
→ Check for null player references  
→ Verify session ID validation  
→ Ensure all required fields initialized

---

**Last Updated:** 2025-10-10  
**Maintained By:** Runarius Development Team

For more details, always refer to `doc/protocol-implementation.md`
