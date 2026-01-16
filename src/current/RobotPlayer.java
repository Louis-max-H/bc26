package current;

import battlecode.common.*;
import current.Robots.*;

public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        Robot robot = switch(rc.getType()){
            case BABY_RAT -> new Baby();
            case RAT_KING -> new King();
            default -> throw new IllegalStateException("Unknown unit type: " + rc.getType());
        };
        robot.run(rc);
    }
}