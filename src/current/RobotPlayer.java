package current;

import battlecode.common.*;
import current.Robots.*;
import current.States.*;

public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        Robot robot = switch(rc.getType()){
            case BABY_RAT -> new Baby();
            case RAT_KING -> new King();
            default -> null;
        };

        robot.run(rc);
    }
}