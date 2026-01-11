package v02_myVersion.States;

import battlecode.common.*;
import v02_myVersion.Robots.Robot;
import v02_myVersion.Utils.Communication;
import v02_myVersion.Utils.PathFinding;

import static v02_myVersion.States.Code.*;
import static v02_myVersion.Utils.Communication.*;

/**
 * State to avoid cats
 * Checks for cats in vision and from shared array, then moves away
 */
public class AvoidCat extends State {
    
    // Cat vision radius squared: sqrt(17) ≈ 4.12, so radius squared = 17
    private static final int CAT_VISION_RADIUS_SQUARED = 17;
    // Safe distance from cat (want to be outside cat vision)
    private static final int SAFE_DISTANCE_SQUARED = 25; // 5^2, outside cat vision
    
    public AvoidCat() {
        this.name = "AvoidCat";
    }
    
    @Override
    public Result run() throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        MapLocation catLoc = null;
        int minCatDist = Integer.MAX_VALUE;
        
        // First, check for cats in vision
        RobotInfo[] nearby = rc.senseNearbyRobots();
        for (RobotInfo robot : nearby) {
            if (robot.getType() == UnitType.CAT) {
                MapLocation loc = robot.getLocation();
                int dist = myLoc.distanceSquaredTo(loc);
                if (dist < minCatDist) {
                    minCatDist = dist;
                    catLoc = loc;
                }
                
                // Report cat location via squeak
                if (rc.getType() == UnitType.BABY_RAT) {
                    Communication.sendSqueak(rc, TYPE_CAT, loc);
                }
            }
        }
        
        // If no cat in vision, check shared array
        if (catLoc == null) {
            catLoc = Communication.readCatLocation(rc);
            if (catLoc != null) {
                minCatDist = myLoc.distanceSquaredTo(catLoc);
            }
        }
        
        // If we found a cat, avoid it
        if (catLoc != null) {
            // Check if we're too close to cat
            if (minCatDist < SAFE_DISTANCE_SQUARED) {
                // Calculate direction away from cat
                Direction awayFromCat = myLoc.directionTo(catLoc).opposite();
                
                // Try to move away
                if (rc.isMovementReady()) {
                    // Try primary direction (directly away)
                    if (rc.canMove(awayFromCat)) {
                        rc.move(awayFromCat);
                        return new Result(OK, "Moved away from cat");
                    }
                    
                    // Try adjacent directions (left/right of away direction)
                    Direction left = awayFromCat.rotateLeft();
                    Direction right = awayFromCat.rotateRight();
                    
                    if (rc.canMove(left)) {
                        rc.move(left);
                        return new Result(OK, "Moved left away from cat");
                    }
                    if (rc.canMove(right)) {
                        rc.move(right);
                        return new Result(OK, "Moved right away from cat");
                    }
                }
                
                // If can't move, at least turn away
                if (rc.isTurningReady()) {
                    rc.turn(awayFromCat);
                    return new Result(OK, "Turned away from cat");
                }
                
                return new Result(CANT, "Cat nearby but can't move/turn");
            }
            
            // Cat is far enough away, we're safe
            return new Result(OK, "Cat detected but safe distance");
        }
        
        // No cat found, safe to continue
        return new Result(OK, "No cat detected");
    }
}
