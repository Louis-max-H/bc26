package current.States;

import battlecode.common.*;
import current.Robots.Robot;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;

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
        int catDist = myLoc.distanceSquaredTo(nearestCat);

        // Set scores: only allow left/right of away direction, penalize all others
        long[] scores = new long[9];
        for (Direction dir : Direction.values()) {
            long distance = myLoc.add(dir).distanceSquaredTo(nearestCat);
            if (distance < safetyDist) {

                long level = 4 - (distance * 4) / safetyDist; // Level 4 is when very cloose of cat
                scores[dir.ordinal()] = - level; // High score when close to cat
            }
        }

        int coefBase = rc.isCooperation() ? 10 : 50;
        if(isKing){
            coefBase *= 2;
        }
        PathFinding.addScoresWithNormalization(scores, coefBase);

        // If attacked from behind without seeing enemy, add vision scores behind us
        if(Robot.wasAttackedFromBehind){
            Direction currentDir = rc.getDirection();
            Direction behind = currentDir.opposite();
            Direction behindLeft = behind.rotateLeft();
            Direction behindRight = behind.rotateRight();
            
            // Add high scores for directions that let us look behind
            long[] backVisionScores = new long[9];
            backVisionScores[behind.ordinal()] = 1000;
            backVisionScores[behindLeft.ordinal()] = 800;
            backVisionScores[behindRight.ordinal()] = 800;
            
            // Also add to PathFinding scores for movement
            PathFinding.addScoresWithNormalization(backVisionScores, 20);
            
            // Try to turn to look behind
            if(rc.isTurningReady()){
                VisionUtils.smartLookAt(myLoc.add(behind));
            }
            
            Robot.wasAttackedFromBehind = false; // Reset flag
        }
        
        return new Result(OK, "Add scores to avoid cat");
    }
}
