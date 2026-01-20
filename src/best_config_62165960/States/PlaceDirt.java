package best_config_62165960.States;

import battlecode.common.*;
import best_config_62165960.Utils.PathFinding;

import static best_config_62165960.States.Code.*;

/**
 * State to place dirt on water in checkerboard pattern
 * Places dirt at locations where x%2 == 0 and y%2 == 0
 */
public class PlaceDirt extends State {
    public PlaceDirt(){
        this.name = "PlaceDirt";
    }

    @Override
    public Result run() throws GameActionException {
        // Need action ready, 10 cheese, and dirt in team stash
        if(!rc.isActionReady() || rc.getAllCheese() < 10 || rc.getDirt() == 0){
            return new Result(OK, "Can't place dirt (need action, 10 cheese, and dirt)");
        }

        // Find nearby water
        if(nearestWater == null){
            return new Result(OK, "No water nearby");
        }

        // Check tiles around water for checkerboard pattern placement
        // Place dirt at x%2 == 0 && y%2 == 0 locations
        for(int dx = -2; dx <= 2; dx++){
            for(int dy = -2; dy <= 2; dy++){
                MapLocation loc = nearestWater.translate(dx, dy);
                if(!rc.onTheMap(loc) || !rc.canSenseLocation(loc)){
                    continue;
                }

                MapInfo info = rc.senseMapInfo(loc);
                // Check if it's passable (water) and matches checkerboard pattern
                // Water is passable but not dirt/wall
                if(!info.isWall() && !info.isDirt() && loc.x % 2 == 0 && loc.y % 2 == 0){
                    // Check if adjacent to place dirt
                    if(myLoc.distanceSquaredTo(loc) <= 2){
                        if(rc.canPlaceDirt(loc)){
                            rc.placeDirt(loc);
                            return new Result(OK, "Placed dirt on water at " + loc);
                        }
                    } else {
                        // Move toward the water location
                        PathFinding.smartMoveTo(loc);
                        return new Result(OK, "Moving to place dirt on water");
                    }
                }
            }
        }

        return new Result(OK, "No valid water location for checkerboard pattern");
    }
}
