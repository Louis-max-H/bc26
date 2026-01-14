package v02_myVersion.Robots;

import v02_myVersion.RobotPlayer;
import v02_myVersion.States.*;
import battlecode.common.*;

public class Baby extends Robot {
    State avoidCat;
    State attackCat;
    State collectCheese;
    State explore;

    @Override
    public void init(){
        this.avoidCat = new AvoidCat();
        this.attackCat = new AttackCat();
        this.collectCheese = new CollectCheese();
        this.explore = new Explore();
    }

    @Override
    public void updateState(Result resultBefore){
        // Priority-based state selection
        // 1. Check for cat danger first (safety)
        // 2. Try to attack cat if safe
        // 3. Collect cheese if available
        // 4. Default to exploring
        
        try {
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
                }
            }
            
            // Check shared array for cat
            if (catLoc == null) {
                try {
                    catLoc = v02_myVersion.Utils.Communication.readCatLocation(rc);
                    if (catLoc != null) {
                        minCatDist = myLoc.distanceSquaredTo(catLoc);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // Priority 1: Avoid cat if too close (within cat vision)
            if (catLoc != null && minCatDist < 25) { // 5^2, outside cat vision
                currentState = avoidCat;
                return;
            }
            
            // Priority 2: Attack cat if adjacent and safe
            if (catLoc != null && minCatDist <= 2) {
                currentState = attackCat;
                return;
            }
            
            // Priority 3: Collect cheese if we see cheese or have cheese to deliver
            MapInfo[] nearbyMap = rc.senseNearbyMapInfos();
            boolean hasCheeseNearby = false;
            for (MapInfo info : nearbyMap) {
                if (info.getCheeseAmount() > 0) {
                    hasCheeseNearby = true;
                    break;
                }
            }
            
            if (rc.getRawCheese() > 0 || hasCheeseNearby) {
                currentState = collectCheese;
                return;
            }
            
            // Priority 4: Default to exploring
            currentState = explore;
        } catch (Exception e) {
            // If anything goes wrong, default to explore
            currentState = explore;
        }
    }
}
