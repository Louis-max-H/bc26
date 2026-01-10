package v003_merge.States;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.util.Locale;

import static v003_merge.States.Code.*;

public class Spawn extends State {
    public int summonCount = 0;
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

        if(cheeseStock < 100){
            return new Result(OK, "Low on cheese, going to eco " + rc.getRawCheese());
        }

        if(rc.getCurrentRatCost() > cheeseStock){
            return new Result(OK, "Not enough cheese " + rc.getRawCheese() + ", " + rc.getCurrentRatCost() + "needed");
        }

        // !!!!!!!!!!!!!
        if(summonCount > 4){
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
