package v006_saketh_updates;

import battlecode.common.*;
import v006_saketh_updates.Robots.*;
import v006_saketh_updates.States.*;

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