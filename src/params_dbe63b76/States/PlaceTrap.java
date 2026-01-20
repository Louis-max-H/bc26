package params_dbe63b76.States;

import battlecode.common.*;
import params_dbe63b76.Utils.PathFinding;

import static params_dbe63b76.States.Code.*;

/**
 * State to place traps strategically when cat or enemy nearby
 * Places rat traps for enemies, cat traps for cats
 */
public class PlaceTrap extends State {
    public PlaceTrap(){
        this.name = "PlaceTrap";
    }

    private static final int RAT_TRAP_COST = 30;
    private static final int CAT_TRAP_COST = 10;
    private static final int TRAP_PLACEMENT_RADIUS_SQUARED = 4; // Place trap within 2 tiles

    @Override
    public Result run() throws GameActionException {
        // Need action ready to place trap
        if(!rc.isActionReady()){
            return new Result(OK, "Action not ready");
        }

        // Check for cat nearby - place cat trap
        if(nearestCat != null){
            int catDist = myLoc.distanceSquaredTo(nearestCat);
            // Place cat trap if cat is close enough and we have cheese
            if(catDist <= TRAP_PLACEMENT_RADIUS_SQUARED && rc.getAllCheese() >= CAT_TRAP_COST){
                // Find adjacent location to place trap
                for(Direction dir : Direction.values()){
                    if(dir == Direction.CENTER) continue;
                    MapLocation trapLoc = myLoc.add(dir);
                    if(rc.canSenseLocation(trapLoc) && rc.canPlaceCatTrap(trapLoc)){
                        rc.placeCatTrap(trapLoc);
                        return new Result(OK, "Placed cat trap at " + trapLoc);
                    }
                }
            }
        }

        // Check for enemy rat or king nearby - place rat trap
        if(nearestEnemyRat != null || nearestEnemyKing != null){
            MapLocation enemyLoc = nearestEnemyRat != null ? nearestEnemyRat : nearestEnemyKing;
            int enemyDist = myLoc.distanceSquaredTo(enemyLoc);
            // Place rat trap if enemy is close enough and we have cheese
            if(enemyDist <= TRAP_PLACEMENT_RADIUS_SQUARED && rc.getAllCheese() >= RAT_TRAP_COST){
                // Find adjacent location to place trap
                for(Direction dir : Direction.values()){
                    if(dir == Direction.CENTER) continue;
                    MapLocation trapLoc = myLoc.add(dir);
                    if(rc.canSenseLocation(trapLoc) && rc.canPlaceRatTrap(trapLoc)){
                        rc.placeRatTrap(trapLoc);
                        return new Result(OK, "Placed rat trap at " + trapLoc);
                    }
                }
            }
        }

        return new Result(OK, "No trap placement needed");
    }
}
