package best_config_62165960.States;

import battlecode.common.*;
import best_config_62165960.Utils.PathFinding;

import static best_config_62165960.States.Code.*;

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
        int catDist = myLoc.distanceSquaredTo(nearestCat);

        // Set scores: only allow left/right of away direction, penalize all others
        long[] scores = new long[9];
        for (Direction dir : Direction.values()) {
            long distance = myLoc.add(dir).distanceSquaredTo(nearestCat);
            if (distance < safetyDist) {

                long level = 4 - (distance * 4) / safetyDist; // Level 4 is when very cloose of cat
                scores[dir.ordinal()] = - level * 100_000; // High score when close to cat
            }
        }
        PathFinding.addScoresWithoutNormalization(scores);
        
        return new Result(OK, "Add scores to avoid cat");
    }
}
