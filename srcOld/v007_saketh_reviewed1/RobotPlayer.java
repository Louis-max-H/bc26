package v007_saketh_reviewed1;

import battlecode.common.*;
import v007_saketh_reviewed1.Robots.*;
import v007_saketh_reviewed1.States.*;

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