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

        try {
            robot.run(rc);
        } catch (Exception e) {
            System.out.println(e.getMessage());

            if(!Robot.competitiveMode && rc.getTeam() == Team.A){
                System.out.println("Not in competitive mode. We have found an error, I will surrender to be sure the error is seen by the debugger ;)");
                rc.resign();
            }
        }

    }
}