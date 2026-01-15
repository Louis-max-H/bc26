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
        
        if (catDist >= safetyDist) {
            return new Result(OK, "Cat detected but safe distance");
        }
        
        // Calculate direction away from cat
        Direction awayFromCat = myLoc.directionTo(nearestCat).opposite();
        Direction left = awayFromCat.rotateLeft();
        Direction right = awayFromCat.rotateRight();
        
        // Set scores: only allow left/right of away direction, penalize all others
        PathFinding.resetScores();
        int[] escapeScores = new int[9];
        for (Direction dir : Direction.values()) {
            if (dir == left || dir == right) {
                escapeScores[dir.ordinal()] = 50 * 100_000; // High score for escape directions
            } else {
                escapeScores[dir.ordinal()] = -50 * 100_000; // Very low score for other directions
            }
        }
        PathFinding.addScoresWithoutNormalization(escapeScores, 1);
        
        return new Result(OK, "Add scores to avoid cat");
    }
}
