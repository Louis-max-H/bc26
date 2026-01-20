package v000_starter;

import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            try {
                UnitType t = rc.getType();
                if (t == UnitType.RAT_KING) King.run(rc);
                if (t == UnitType.BABY_RAT) Baby.run(rc);
            }
            catch (Exception ignored) {
            } finally {
                Clock.yield();
            }
        }
    }
}