package v000_starter;

import battlecode.common.*;

public class Roles {
    public static final int MINER = 0;
    public static final int GUARD = 1;
    public static final int SCOUT = 2;
    public static final int ATTACKER = 3;

    public static int initialRole(RobotController rc) {
        int r = rc.getID() % 10;
        if (r == 0) return SCOUT;
        if (r == 1 || r == 2) return ATTACKER;
        if (r == 3) return GUARD;
        return MINER;
    }

    public static int sector(RobotController rc) {
        return rc.getID() & 3;
    }

    public static boolean commit(RobotController rc) {
        return !rc.isCooperation() || Comms.readMode(rc) == Comms.MODE_COMMIT;
    }
}