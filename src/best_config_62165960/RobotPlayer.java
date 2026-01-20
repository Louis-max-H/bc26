package best_config_62165960;

import battlecode.common.*;
import best_config_62165960.Robots.*;
import best_config_62165960.States.*;

public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        while(true) {
            // Instantiate
            Robot robot = switch (rc.getType()) {
                case BABY_RAT -> new Baby();
                case RAT_KING -> new King();
                default -> null;
            };

            // Run
            try {
                robot.run(rc);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                Robot.rc.setTimelineMarker(e.getMessage(), 255, 0, 0);
            }

            // Resign if not competitive
            if (!Robot.competitiveMode && rc.getTeam() == Team.A) {
                System.out.println("\nNot in competitive mode. We have found an error, I will surrender to be sure the error is seen by the debugger ;)");
                rc.resign();
            }
        }
    }
}