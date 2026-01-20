package v003_merge.States;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static v003_merge.States.Code.*;

public class Spawn extends State {
    public int summonCount = 0;
    public int reserveNeeded;
    public int costCap;
    public Spawn(){
        this.name = "Spawn";
    }

    @Override
    public Result run() throws GameActionException {
        // Spawn rats if it can and with basics conditions
        // TODOS: We can use the vector King -> Spawn location to indicate a direction for exploration of the spawned rat
        // TODOS: We may spawn rats the further we can to help them explore ?

        int cheeseStock = rc.getAllCheese();

        if(!rc.getType().isRatKingType()){
            return new Result(ERR, "Unit should be king to spawn rats.");
        }

        // Loading parameters
        if (gamePhase <= PHASE_START) {
            // HYPER-AGGRESSIVE: spawn as fast as possible in first 100 rounds
            reserveNeeded = 100;  // minimal reserve
            costCap = 50;         // allow many rats
        } else if (gamePhase <= PHASE_MIDLE) {
            // Still aggressive but slightly more careful
            reserveNeeded = 400;
            costCap = 40;
        } else {
            // Late game: conservative
            reserveNeeded = 1400;
            costCap = 40;
        }

        if(cheeseStock < 100){
            return new Result(OK, "Low on cheese, going to eco " + rc.getRawCheese());
        }

        if(rc.getCurrentRatCost() > cheeseStock){
            return new Result(OK, "Not enough cheese " + rc.getRawCheese() + ", " + rc.getCurrentRatCost() + "needed");
        }

        // !!!!!!!!!!!!! Only for debug purpose
        if(false && summonCount > 4 && !competitiveMode){
            return new Result(WARN, "Debug only!!, summon limited to 1");
        }

        // 10 base cost of rat
        // 10 cheese by 4 rats leaving => 2.5
        if(rc.getCurrentRatCost() > 10 + (2.5 * 2)){
            return new Result(OK, "Cost too hight " + rc.getCurrentRatCost());
        }

        for(MapLocation loc: rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 9)){
            if(rc.canBuildRat(loc)){
                rc.buildRat(loc);
                summonCount++;
                return new Result(OK, "Builind rat to " + loc);
            }
        }

        return new Result(WARN, "No location to spawn rats in radius 9");
    };
}
