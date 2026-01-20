package v004_cat.Utils;

import battlecode.common.*;
import v004_cat.Robots.Robot;

/**
 * Communication system for Battlecode 2026
 * 
 * Message encoding:
 * - Bits 28-31: Message type (4 bits)
 * - Bits 0-11: Position encoding (12 bits: x + y * 60)
 * 
 * Shared Array:
 * - Even indices: x coordinates
 * - Odd indices: y coordinates
 * - Only write to indices that haven't changed (optimization)
 */
public class Communication extends Robot {
    
    /////////////////////////////////////// Message Type Masks ///////////////////////////////////////
    
    //                                        0b111111112222223333333344444444
    public static final int MASK_TYPE       = 0b111100000000000000000000000000; // Bits 28-31
    public static final int TYPE_EMPTY      = 0b000000000000000000000000000000;
    public static final int TYPE_ENEMY_KING = 0b000100000000000000000000000000;
    public static final int TYPE_MINE       = 0b001000000000000000000000000000;
    public static final int TYPE_CAT        = 0b001100000000000000000000000000;
    public static final int TYPE_ENEMY_RAT  = 0b010000000000000000000000000000;
    public static final int TYPE_CHEESE     = 0b010100000000000000000000000000;
    
    public static final int MASK_POSITION   = 0b000000000000000000111111111111; // Bits 0-11 (12 bits)
    
    /////////////////////////////////////// Shared Array Indices ///////////////////////////////////////
    
    // Enemy King tracking (indices 0-2)
    public static final int SA_EK_X = 0;  // Enemy King X coordinate
    public static final int SA_EK_Y = 1;  // Enemy King Y coordinate
    public static final int SA_EK_R = 2;   // Enemy King last seen round (mod 1023)
    
    // Cheese Mine tracking (indices 3-62, pairs for up to 30 mines)
    // Even indices: mine X, Odd indices: mine Y
    public static final int SA_MINE_START = 3;
    public static final int SA_MINE_COUNT = 30; // Max 30 mines (60 indices)
    
    // Cat tracking (indices 63)
    public static final int SA_CAT_X = 63;
    public static final int SA_CAT_Y = 62; // Note: using 62,63 for cat (backwards for clarity)
    public static final int SA_CAT_R = 61; // Cat last seen round
    
    /////////////////////////////////////// Shared Array State Tracking ///////////////////////////////////////
    
    // Store previous turn's shared array to detect changes
    private static int[] lastSharedArray = new int[64];

    /**
     * Initialize communication system - call once at start
     */
    public static void init(RobotController rc) throws GameActionException {}
    
    /**
     * Update shared array state - call at start of each turn
     */
    public static void updateSharedArrayState() throws GameActionException {
        for (int i = 0; i < 64; i++) {
            lastSharedArray[i] = rc.readSharedArray(i);
        }
    }
    
    /**
     * Write to shared array only if value has changed (optimization)
     * Only rat kings can write to shared array
     */
    public static void writeSharedArrayIfChanged(RobotController rc, int index, int value) throws GameActionException {
        if (lastSharedArray[index] != value) {
            rc.writeSharedArray(index, value);
            lastSharedArray[index] = value;
        }
    }
    
    /////////////////////////////////////// Message Encoding ///////////////////////////////////////
    
    /**
     * Encode a message with type and position
     * @param type Message type (TYPE_ENEMY_KING, TYPE_MINE, etc.)
     * @param loc MapLocation to encode
     * @return Encoded message (int)
     */
    public static int encodeMessage(int type, MapLocation loc) {
        // Encode position: x + y * 60 (max map size is 60x60)
        return type | (loc.x + loc.y * 60);
    }
    
    /**
     * Decode position from message
     * @param message Encoded message
     * @return MapLocation or null if invalid
     */
    public static MapLocation decodePosition(int message) {
        int xyEncoded = message & MASK_POSITION;
        return new MapLocation(
            xyEncoded % 60, // x
            xyEncoded / 60     // y
        );
    }

    /////////////////////////////////////// Squeak Sending ///////////////////////////////////////

    /**
     * Send a squeak message (baby rats only)
     * Each rat can squeak at most once per turn
     * @param type Message type
     * @param loc Location to encode
     * @return true if message was sent
     */
    public static boolean sendSqueak(int type, MapLocation loc) throws GameActionException {
        // Only baby rats can squeak
        if (!isKing) {
            return false;
        }
        
        // Check if we've already squeaked this turn
        if (hasSqueakedThisTurn) {
            return false;
        }

        rc.squeak(encodeMessage(type, loc));
        lastSqueakRound = round;
        hasSqueakedThisTurn = true;
        return true;
    }
    
    /**
     * Send a squeak with just a type
     */
    public static boolean sendSqueak(int type) throws GameActionException {
        return sendSqueak(type, null);
    }
    
    /////////////////////////////////////// Squeak Ingesting ///////////////////////////////////////
    
    /**
     * Process all squeaks and update shared array
     * Only rat kings should call this
     * @param rc RobotController
     */
    public static void ingestSqueaks(RobotController rc) throws GameActionException {
        Message[] msgs = rc.readSqueaks(-1);
        int nowMod = rc.getRoundNum() & 1023; // Store round mod 1023 (fits in 10 bits)
        MapLocation loc;
        
        for (Message m : msgs) {
            int raw = m.getBytes();

            switch (raw & MASK_TYPE) {
                case TYPE_ENEMY_KING:
                    loc = decodePosition(raw);
                    writeSharedArrayIfChanged(rc, SA_EK_X, loc.x);
                    writeSharedArrayIfChanged(rc, SA_EK_Y, loc.y);
                    writeSharedArrayIfChanged(rc, SA_EK_R, nowMod);
                    break;
                    
                case TYPE_MINE:
                    loc = decodePosition(raw);
                    tryStoreMine(rc, loc);
                    break;
                    
                case TYPE_CAT:
                    loc = decodePosition(raw);
                    writeSharedArrayIfChanged(rc, SA_CAT_X, loc.x);
                    writeSharedArrayIfChanged(rc, SA_CAT_Y, loc.y);
                    writeSharedArrayIfChanged(rc, SA_CAT_R, nowMod);
                    break;
                    
                case TYPE_ENEMY_RAT:
                    // Could store enemy rat positions if needed
                    break;
                    
                case TYPE_CHEESE:
                    // Could store cheese locations if needed
                    break;
                    
                case TYPE_EMPTY:
                    break;
                    
                default:
                    Robot.err("ERR: Message can't be parsed: " + m + " not recognized.");
                    break;
            }
        }
    }
    
    /**
     * Try to store a cheese mine location in shared array
     * Stores in first available slot
     */
    private static void tryStoreMine(RobotController rc, MapLocation mineLoc) throws GameActionException {
        // Check if mine already stored
        for (int i = 0; i < SA_MINE_COUNT; i++) {
            int idxX = SA_MINE_START + i * 2;
            int idxY = idxX + 1;
            
            int storedX = rc.readSharedArray(idxX);
            int storedY = rc.readSharedArray(idxY);
            
            // Empty slot (0,0) or same location
            if ((storedX == 0 && storedY == 0) || (storedX == mineLoc.x && storedY == mineLoc.y)) {
                writeSharedArrayIfChanged(rc, idxX, mineLoc.x);
                writeSharedArrayIfChanged(rc, idxY, mineLoc.y);
                return;
            }
        }
        // All slots full, could implement replacement logic if needed
    }
    
    /////////////////////////////////////// Shared Array Reading ///////////////////////////////////////
    
    /**
     * Read enemy king location from shared array
     * @return MapLocation or null if not found/outdated
     */
    public static MapLocation readEnemyKingLocation(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum() & 1023;
        int lastSeenRound = rc.readSharedArray(SA_EK_R);
        
        // Check if info is recent (within last 100 rounds, accounting for mod wrap)
        int roundDiff = (round - lastSeenRound + 1024) % 1024;
        if (roundDiff > 100) {
            return null; // Info too old
        }
        
        int x = rc.readSharedArray(SA_EK_X);
        int y = rc.readSharedArray(SA_EK_Y);
        
        if (x == 0 && y == 0) {
            return null; // No enemy king stored
        }
        
        return new MapLocation(x, y);
    }
    
    /**
     * Read cat location from shared array
     * @return MapLocation or null if not found/outdated
     */
    public static MapLocation readCatLocation(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum() & 1023;
        int lastSeenRound = rc.readSharedArray(SA_CAT_R);
        
        // Check if info is recent (within last 50 rounds)
        int roundDiff = (round - lastSeenRound + 1024) % 1024;
        if (roundDiff > 50) {
            return null; // Info too old
        }
        
        int x = rc.readSharedArray(SA_CAT_X);
        int y = rc.readSharedArray(SA_CAT_Y);
        
        if (x == 0 && y == 0) {
            return null; // No cat stored
        }
        
        return new MapLocation(x, y);
    }
    
    /**
     * Read all stored cheese mine locations
     * @return Array of MapLocations (may contain nulls for empty slots)
     */
    public static MapLocation[] readMineLocations(RobotController rc) throws GameActionException {
        MapLocation[] mines = new MapLocation[SA_MINE_COUNT];
        int count = 0;
        
        for (int i = 0; i < SA_MINE_COUNT; i++) {
            int idxX = SA_MINE_START + i * 2;
            int idxY = idxX + 1;
            
            int x = rc.readSharedArray(idxX);
            int y = rc.readSharedArray(idxY);
            
            if (x != 0 || y != 0) {
                mines[count++] = new MapLocation(x, y);
            }
        }
        
        // Return only non-null locations
        MapLocation[] result = new MapLocation[count];
        System.arraycopy(mines, 0, result, 0, count);
        return result;
    }
}
