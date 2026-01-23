package current;

import battlecode.common.*;
import current.Robots.*;
import current.States.*;

public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        while(true) {
            try {
                // Instantiate
                Robot robot = switch (rc.getType()) {
                    case BABY_RAT -> new Baby();
                    case RAT_KING -> new King();
                    default -> null;
                };

                // Run
                while(true) {robot.run(rc);}
            } catch (Exception e) {
                e.printStackTrace(System.out);
                if(!rc.getType().isRatKingType()){
                    Robot.rc.setTimelineMarker(e.getMessage(), 255, 0, 0);
                }
            }

            // Resign if not competitive and not king
            if (!Robot.competitiveMode && rc.getTeam() == Team.A && !rc.getType().isRatKingType()) {
                System.out.println("\nNot in competitive mode. We have found an error, I will surrender to be sure the error is seen by the debugger ;)");
                rc.resign();
            }
        }
    }
}