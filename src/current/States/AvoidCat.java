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
    private static final int SAFE_DISTANCE_SQUARED = 25; // 5^2, outside cat vision
    private static final int SAFE_DISTANCE_KING_SQUARED = 50; // 7^2 for kings
    
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
        int[] scores = new int[9];
        for (Direction dir : Direction.values()) {
            int distance = myLoc.add(dir).distanceSquaredTo(nearestCat);
            if (distance < safetyDist) {

                int level = 4 - (distance * 4) / safetyDist; // Level 4 is when very cloose of cat
                scores[dir.ordinal()] = - level * 100_000; // High score when close to cat
            }
        }
        PathFinding.addScoresWithoutNormalization(scores);
        
        return new Result(OK, "Add scores to avoid cat");
    }
}
