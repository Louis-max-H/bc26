package v005_saketh.Robots;

import v005_saketh.RobotPlayer;
import v005_saketh.States.*;
import battlecode.common.*;

public class King extends Robot {
    State avoidCat;
    State spawn;
    State explore;

    @Override
    public void init(){
        // Note: init, endTurn are already set in Robot.run()
        this.avoidCat = new AvoidCat();
        this.spawn = new Spawn();
        this.explore = new Explore();
    }

    @Override
    public void updateState(Result resultBefore){
        // Priority-based state selection for rat kings
        // 1. Avoid cat if too close (safety)
        // 2. Spawn rats if we have enough cheese
        // 3. Default to exploring
        
        try {
            MapLocation myLoc = rc.getLocation();
            MapLocation catLoc = null;
            int minCatDist = Integer.MAX_VALUE;
            
            // Check for cats in vision (rat kings have 360 vision)
            RobotInfo[] nearby = rc.senseNearbyRobots();
            for (RobotInfo robot : nearby) {
                if (robot.getType() == UnitType.CAT) {
                    MapLocation loc = robot.getLocation();
                    int dist = myLoc.distanceSquaredTo(loc);
                    if (dist < minCatDist) {
                        minCatDist = dist;
                        catLoc = loc;
                    }
                }
            }
            
            // Check shared array for cat
            if (catLoc == null) {
                try {
                    catLoc = v005_saketh.Utils.Communication.readCatLocation(rc);
                    if (catLoc != null) {
                        minCatDist = myLoc.distanceSquaredTo(catLoc);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // Priority 1: Avoid cat if too close
            if (catLoc != null && minCatDist < 25) { // 5^2, outside cat vision
                currentState = avoidCat;
                return;
            }
            
            // Priority 2: Spawn rats if we have enough cheese and action is ready
            // Only spawn if we have reasonable amount of cheese (at least 100 to keep some buffer)
            if (rc.isActionReady() && rc.getAllCheese() >= 100) {
                int spawnCost = rc.getCurrentRatCost();
                if (rc.getAllCheese() >= spawnCost) {
                    currentState = spawn;
                    return;
                }
            }
            
            // Priority 3: Default to exploring
            currentState = explore;
        } catch (Exception e) {
            // If anything goes wrong, default to explore
            currentState = explore;
        }
    }
}
