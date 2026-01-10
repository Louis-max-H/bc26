package v000_starter;

import battlecode.common.*;

public class Comms {
    static final int IDX_HOME_X = 0;
    static final int IDX_HOME_Y = 1;
    static final int IDX_ENEMY_KING_X = 2;
    static final int IDX_ENEMY_KING_Y = 3;

    static final int IDX_MODE = 4;//0 econ, 1 commit
    static final int IDX_NEED = 5;//0/1
    static final int IDX_CAT_X = 6;
    static final int IDX_CAT_Y = 7;
    static final int IDX_CAT_R = 8;
    static final int IDX_THREAT_X = 9;
    static final int IDX_THREAT_Y = 10;
    static final int IDX_THREAT_R = 11;
    static final int IDX_THREAT_N = 12;
    public static final int MODE_ECON = 0;
    public static final int MODE_COMMIT = 1;

    //squeak protocol (only for reporting enemy king near home)
    static final int MAGIC = 0xB;
    static final int OP_ENEMY_KING = 1;

    static int pack(int op, int x, int y) {
        return (MAGIC << 28) | ((op & 0xF) << 24) | ((x & 0x3FF) << 14) | ((y & 0x3FF) << 4);
    }

    static boolean ours(int msg) { return (msg >>> 28) == MAGIC; }
    static int op(int msg) { return (msg >>> 24) & 0xF; }
    static int x(int msg) { return (msg >>> 14) & 0x3FF; }
    static int y(int msg) { return (msg >>> 4) & 0x3FF; }

    public static int packEnemyKing(MapLocation loc) {
        return pack(OP_ENEMY_KING, loc.x, loc.y);
    }

    static void writeLoc(RobotController rc, int ix, int iy, MapLocation loc) throws GameActionException {
        rc.writeSharedArray(ix, loc.x + 1);
        rc.writeSharedArray(iy, loc.y + 1);
    }

    static MapLocation readLoc(RobotController rc, int ix, int iy) throws GameActionException {
        int vx = rc.readSharedArray(ix);
        int vy = rc.readSharedArray(iy);
        if (vx == 0 || vy == 0) return null;
        return new MapLocation(vx - 1, vy - 1);
    }

    public static void kingWriteHome(RobotController rc, MapLocation loc) {
        try { writeLoc(rc, IDX_HOME_X, IDX_HOME_Y, loc); } catch (GameActionException ignored) {}
    }

    public static MapLocation readHome(RobotController rc) {
        try { return readLoc(rc, IDX_HOME_X, IDX_HOME_Y); } catch (GameActionException e) { return null; }
    }

    public static void kingWriteEnemyKing(RobotController rc, MapLocation loc) {
        try { writeLoc(rc, IDX_ENEMY_KING_X, IDX_ENEMY_KING_Y, loc); } catch (GameActionException ignored) {}
    }

    public static MapLocation readEnemyKing(RobotController rc) {
        try { return readLoc(rc, IDX_ENEMY_KING_X, IDX_ENEMY_KING_Y); } catch (GameActionException e) { return null; }
    }

    public static void kingWriteMode(RobotController rc, int mode) {
        try { rc.writeSharedArray(IDX_MODE, mode); } catch (GameActionException ignored) {}
    }

    public static int readMode(RobotController rc) {
        try { return rc.readSharedArray(IDX_MODE); } catch (GameActionException e) { return MODE_ECON; }
    }

    public static void kingWriteNeed(RobotController rc, boolean need) {
        try { rc.writeSharedArray(IDX_NEED, need ? 1 : 0); } catch (GameActionException ignored) {}
    }

    public static boolean readNeed(RobotController rc) {
        try { return rc.readSharedArray(IDX_NEED) == 1; } catch (GameActionException e) { return false; }
    }

    public static void kingWriteCat(RobotController rc, MapLocation catLoc, int round) {
        try {
            writeLoc(rc, IDX_CAT_X, IDX_CAT_Y, catLoc);
            rc.writeSharedArray(IDX_CAT_R, Math.min(1023, round));
        } catch (GameActionException ignored) {}
    }

    public static MapLocation readCat(RobotController rc, int curRound, int ttl) {
        try {
            int r = rc.readSharedArray(IDX_CAT_R);
            if (r == 0) return null;
            if (curRound - r > ttl) return null;
            return readLoc(rc, IDX_CAT_X, IDX_CAT_Y);
        } catch (GameActionException e) {
            return null;
        }
    }

    //threat (enemy rats near home)
    public static void kingWriteThreat(RobotController rc, MapLocation loc, int n, int round) {
        try {
            writeLoc(rc, IDX_THREAT_X, IDX_THREAT_Y, loc);
            rc.writeSharedArray(IDX_THREAT_N, Math.min(1023, n));
            rc.writeSharedArray(IDX_THREAT_R, Math.min(1023, round));
        } catch (GameActionException ignored) {}
    }

    public static void kingClearThreat(RobotController rc) {
        try {
            rc.writeSharedArray(IDX_THREAT_X, 0);
            rc.writeSharedArray(IDX_THREAT_Y, 0);
            rc.writeSharedArray(IDX_THREAT_N, 0);
            rc.writeSharedArray(IDX_THREAT_R, 0);
        } catch (GameActionException ignored) {}
    }

    public static MapLocation readThreat(RobotController rc, int curRound, int ttl) {
        try {
            int r = rc.readSharedArray(IDX_THREAT_R);
            if (r == 0 || curRound - r > ttl) return null;
            return readLoc(rc, IDX_THREAT_X, IDX_THREAT_Y);
        } catch (GameActionException e) {
            return null;
        }
    }

    public static int readThreatCount(RobotController rc) {
        try { return rc.readSharedArray(IDX_THREAT_N); } catch (GameActionException e) { return 0; }
    }

    //king reads reports (scout has to squeak near home)
    public static MapLocation kingReadEnemyReport(RobotController rc){
        try {
            Message[] msgs = rc.readSqueaks(rc.getRoundNum());
            for (Message m : msgs) {
                int raw = m.getBytes();
                if (!ours(raw)) continue;
                if (op(raw) != OP_ENEMY_KING) continue;
                return new MapLocation(x(raw), y(raw));
            }
        } catch (Exception ignored) {}
        return null;
    }
}