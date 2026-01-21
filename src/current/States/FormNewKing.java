package current.States;

import battlecode.common.*;
import current.Utils.PathFinding;

import static current.States.Code.*;

/**
 * State for low health rats to go to nearest king and try to form a new king
 * Requires 7+ allied rats in 3x3 area, 50 cheese, and valid space
 */
public class FormNewKing extends State {
    public FormNewKing() {
        this.name = "FormNewKing";
    }

    private static final int LOW_HEALTH_THRESHOLD = 30; // If health below this, try to form king
    private static final int KING_FORMATION_COST = 50;

    @Override
    public Result run() throws GameActionException {
        // Only baby rats can form new kings
        if(isKing){
            return new Result(ERR, "Already a king");
        }

        // Check if we have low health
        int health = rc.getHealth();
        if(health >= LOW_HEALTH_THRESHOLD){
            return new Result(OK, "Health too high: " + health);
        }

        // Check if we have enough cheese
        if(rc.getAllCheese() < KING_FORMATION_COST){
            return new Result(OK, "Not enough cheese: " + rc.getAllCheese() + " < " + KING_FORMATION_COST);
        }

        // Check if we can become a king (7+ rats in 3x3 area)
        if(rc.canBecomeRatKing()){
            rc.becomeRatKing();
            return new Result(OK, "Became a new king!");
        }

        // If we can't form king here, go to nearest king to gather with other rats
        if(nearestKing != null){
            // Check if we're close to king
            int distToKing = myLoc.distanceSquaredTo(nearestKing);
            if(distToKing <= 9){ // Within 3 tiles
                // We're near the king, try to gather other rats
                // Count nearby allied rats
                int nearbyRats = 0;
                for(RobotInfo info : rc.senseNearbyRobots(-1, rc.getTeam())){
                    if(info.getType() == UnitType.BABY_RAT && info.getLocation().distanceSquaredTo(myLoc) <= 9){
                        nearbyRats++;
                    }
                }
                
                // If we have 7+ rats nearby, try to form king
                if(nearbyRats >= 7 && rc.canBecomeRatKing()){
                    rc.becomeRatKing();
                    return new Result(OK, "Became king with " + nearbyRats + " nearby rats!");
                }
                
                // Otherwise, just wait/stay near king
                return new Result(OK, "Waiting near king with " + nearbyRats + " rats (need 7)");
            } else {
                // Move toward king
                PathFinding.smartMoveTo(nearestKing);
                return new Result(END_OF_TURN, "Moving to king to form new king");
            }
        }

        return new Result(WARN, "No king found to gather near");
    }
}
