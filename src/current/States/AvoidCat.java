package current.States;

import battlecode.common.*;
import current.Utils.PathFinding;

import static current.States.Code.*;

/**
 * State to avoid cats
 * Uses PathFinding to set direction scores, allowing only left/right of away direction
 */
public class AvoidCat extends State {
    // Safe distance from cat (want to be outside cat vision)
    private static final int SAFE_DISTANCE_SQUARED = 10;
    private static final int SAFE_DISTANCE_KING_SQUARED = 20;
    
    public AvoidCat() {
        this.name = "AvoidCat";
    }
    
    @Override
    public Result run() throws GameActionException {
        if (nearestCat == null) {
            return new Result(OK, "No cat to avoid");
        }

        int safetyDist = isKing ? SAFE_DISTANCE_KING_SQUARED : SAFE_DISTANCE_SQUARED;

        // Set scores: only allow left/right of away direction, penalize all others
        long[] scores = new long[9];
        for (Direction dir : Direction.values()) {
            long distance = myLoc.add(dir).distanceSquaredTo(nearestCat);
            if (distance < safetyDist) {

                long level = 6 - (distance * 6) / safetyDist; // Level 6 is when very cloose of cat
                scores[dir.ordinal()] = - level; // High score when close to cat
            }
        }

        int coefBase = rc.isCooperation() ? 10 : 50;
        if(isKing){
            coefBase *= 2;
        }
        PathFinding.addScoresWithNormalization(scores, coefBase);
        
        return new Result(OK, "Add scores to avoid cat");
    }
}
