package v005_saketh.States;

import battlecode.common.*;
import v005_saketh.Robots.Robot;
import v005_saketh.Utils.Communication;
import v005_saketh.Utils.PathFinding;

import static v005_saketh.States.Code.*;
import static v005_saketh.Utils.Communication.*;

/**
 * State to attack cats when safe
 * Only attacks if cat is adjacent and we're not in immediate danger
 */
public class AttackCat extends State {
    
    // Cat vision radius squared: sqrt(17) ≈ 4.12, so radius squared = 17
    private static final int CAT_VISION_RADIUS_SQUARED = 17;
    // Safe distance to attack (adjacent only)
    private static final int ATTACK_DISTANCE_SQUARED = 2; // Adjacent = 1 or 2
    
    public AttackCat() {
        this.name = "AttackCat";
    }
    
    @Override
    public Result run() throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        MapLocation catLoc = null;
        int minCatDist = Integer.MAX_VALUE;
        
        // Check for cats in vision
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
        
        // If we found a cat and it's adjacent, attack it
        if (catLoc != null && minCatDist <= ATTACK_DISTANCE_SQUARED) {
            // Check if we can attack
            if (rc.canAttack(catLoc)) {
                // Use some cheese for stronger bite if we have it
                int cheeseToSpend = 0;
                if (rc.getRawCheese() > 0) {
                    cheeseToSpend = Math.min(rc.getRawCheese(), 25); // Spend up to 25 cheese
                } else if (rc.getGlobalCheese() > 0) {
                    cheeseToSpend = Math.min(rc.getGlobalCheese(), 25);
                }
                
                if (cheeseToSpend > 0) {
                    rc.attack(catLoc, cheeseToSpend);
                } else {
                    rc.attack(catLoc);
                }
                return new Result(OK, "Attacked cat with " + cheeseToSpend + " cheese");
            }
            
            // Can't attack yet, might need to move closer or wait for cooldown
            return new Result(CANT, "Cat adjacent but can't attack");
        }
        
        // No cat nearby or not in attack range
        return new Result(OK, "No cat to attack");
    }
}
