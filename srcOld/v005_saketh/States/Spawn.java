package v005_saketh.States;

import battlecode.common.*;
import v005_saketh.Robots.Robot;
import v005_saketh.Utils.PathFinding;

import static v005_saketh.States.Code.*;

/**
 * State for rat kings to spawn baby rats
 */
public class Spawn extends State {
    
    public Spawn() {
        this.name = "Spawn";
    }
    
    @Override
    public Result run() throws GameActionException {
        // Only rat kings can spawn
        if (!rc.getType().isRatKingType()) {
            return new Result(ERR, "Not a rat king");
        }
        
        // Check if we have enough cheese and action is ready
        if (!rc.isActionReady()) {
            return new Result(CANT, "Action not ready");
        }
        
        int spawnCost = rc.getCurrentRatCost();
        if (rc.getAllCheese() < spawnCost) {
            return new Result(CANT, "Not enough cheese: " + rc.getAllCheese() + " < " + spawnCost);
        }
        
        // Rat kings can spawn within radius squared 4 (distance 2) from their center
        // Try adjacent first, then distance 2
        MapLocation myLoc = rc.getLocation();
        
        // First try adjacent locations (distance 1)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip center
                MapLocation spawnLoc = new MapLocation(myLoc.x + dx, myLoc.y + dy);
                if (rc.canBuildRat(spawnLoc)) {
                    rc.buildRat(spawnLoc);
                    return new Result(OK, "Spawned rat at " + spawnLoc);
                }
            }
        }
        
        // Then try distance 2 locations (radius squared 4)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int distSq = dx * dx + dy * dy;
                if (distSq == 0 || distSq > 4) continue; // Skip center and too far
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) continue; // Already tried
                MapLocation spawnLoc = new MapLocation(myLoc.x + dx, myLoc.y + dy);
                if (rc.canBuildRat(spawnLoc)) {
                    rc.buildRat(spawnLoc);
                    return new Result(OK, "Spawned rat at " + spawnLoc);
                }
            }
        }
        
        // No valid spawn location found
        return new Result(CANT, "No valid spawn location");
    }
}
