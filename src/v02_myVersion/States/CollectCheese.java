package v02_myVersion.States;

import battlecode.common.*;
import v02_myVersion.Robots.Robot;
import v02_myVersion.Utils.Communication;
import v02_myVersion.Utils.PathFinding;

import static v02_myVersion.States.Code.*;
import static v02_myVersion.Utils.Communication.*;

/**
 * State for baby rats to collect cheese and transfer it to rat kings
 */
public class CollectCheese extends State {
    
    public CollectCheese() {
        this.name = "CollectCheese";
    }
    
    @Override
    public Result run() throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        
        // If we have raw cheese, try to transfer it to a rat king
        if (rc.getRawCheese() > 0) {
            // Find nearest allied rat king
            RobotInfo[] nearby = rc.senseNearbyRobots(rc.getType().getVisionRadiusSquared(), rc.getTeam());
            MapLocation nearestKing = null;
            int minDist = Integer.MAX_VALUE;
            
            for (RobotInfo robot : nearby) {
                if (robot.getType() == UnitType.RAT_KING) {
                    int dist = myLoc.distanceSquaredTo(robot.getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        nearestKing = robot.getLocation();
                    }
                }
            }
            
            // If we see a rat king, try to transfer cheese
            if (nearestKing != null) {
                if (rc.canTransferCheese(nearestKing, rc.getRawCheese())) {
                    rc.transferCheese(nearestKing, rc.getRawCheese());
                    return new Result(OK, "Transferred cheese to king");
                }
                
                // Move towards rat king if we can't transfer yet (need to be closer)
                if (rc.isMovementReady()) {
                    Direction dir = myLoc.directionTo(nearestKing);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        return new Result(OK, "Moving towards king");
                    }
                    // Try adjacent directions if direct path blocked
                    Direction left = dir.rotateLeft();
                    Direction right = dir.rotateRight();
                    if (rc.canMove(left)) {
                        rc.move(left);
                        return new Result(OK, "Moving left towards king");
                    }
                    if (rc.canMove(right)) {
                        rc.move(right);
                        return new Result(OK, "Moving right towards king");
                    }
                }
            } else {
                // No king in vision, explore to find one
                return new Result(OK, "Have cheese but no king in vision");
            }
        }
        
        // No cheese, look for cheese to pick up
        MapInfo[] nearbyMap = rc.senseNearbyMapInfos();
        MapLocation nearestCheese = null;
        int minCheeseDist = Integer.MAX_VALUE;
        
        for (MapInfo info : nearbyMap) {
            if (info.getCheeseAmount() > 0) {
                MapLocation loc = info.getMapLocation();
                int dist = myLoc.distanceSquaredTo(loc);
                if (dist < minCheeseDist) {
                    minCheeseDist = dist;
                    nearestCheese = loc;
                }
            }
            
            // Report cheese mines
            if (info.hasCheeseMine()) {
                Communication.sendSqueak(rc, TYPE_MINE, info.getMapLocation());
            }
        }
        
        // If we found cheese, try to pick it up
        if (nearestCheese != null) {
            if (rc.canPickUpCheese(nearestCheese)) {
                rc.pickUpCheese(nearestCheese);
                return new Result(OK, "Picked up cheese");
            }
            
            // Move towards cheese
            if (rc.isMovementReady()) {
                Direction dir = myLoc.directionTo(nearestCheese);
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    return new Result(OK, "Moving towards cheese");
                }
            }
        }
        
        // No cheese nearby, continue exploring
        return new Result(OK, "No cheese nearby");
    }
}
