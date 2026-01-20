package params_dbe63b76.States;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static params_dbe63b76.States.Code.*;

public class Spawn extends State {
    public int summonCount = 0;
    public int reserveNeeded;
    public int costCap;
    public Spawn(){
        this.name = "Spawn";
    }
    public int MAX_RATS = 30;

    @Override
    public Result run() throws GameActionException {
        // Spawn rats if it can and with basics conditions
        // Conservative approach: always keep cheese buffer for 200 rounds
        // Rat kings consume 2 cheese per round, so minimum buffer = 200 * 2 = 400 cheese
        // Plus some extra for safety and potential spawn costs

        int cheeseStock = rc.getAllCheese();

        if(!isKing){
            return new Result(ERR, "Unit should be king to spawn rats.");
        }

        // Calculate minimum cheese needed for 200 rounds
        // Rat king consumes 2 cheese per round, so 200 rounds = 400 cheese minimum
        // Add buffer for potential spawn costs (params_dbe63b76 cost can be up to ~50+)
        int minCheeseFor200Rounds = 400 + 100; // 400 for consumption + 100 buffer = 500 minimum
        
        // More conservative: calculate based on params_dbe63b76 spawn cost too
        int params_dbe63b76SpawnCost = rc.getCurrentRatCost();
        int conservativeBuffer = minCheeseFor200Rounds + params_dbe63b76SpawnCost; // Always have enough for one spawn + 200 rounds

        // Only spawn if we can afford it
        if(cheeseStock < conservativeBuffer){
            return new Result(OK, "Not enough cheese: " + cheeseStock + ", Need: " + conservativeBuffer);
        }

        // Don't spawn if costs is too high
        int maxAcceptableCost = 10 + (MAX_RATS / 4)*10 ; // 10 cheese per 4 rats
        if(params_dbe63b76SpawnCost >= maxAcceptableCost){
            return new Result(OK, "Cost too high: " + params_dbe63b76SpawnCost + " (max: " + maxAcceptableCost + ")");
        }

        // Try to spawn further away first (save one move), prefer toward center
        MapLocation mapCenter = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        MapLocation bestSpawnLoc = null;
        int bestDist = -1;
        int bestDistToCenter = Integer.MAX_VALUE;
        
        // First pass: find furthest spawn locations
        for(MapLocation loc: rc.getAllLocationsWithinRadiusSquared(myLoc, 4)){
            if(rc.canBuildRat(loc)){
                int distFromKing = myLoc.distanceSquaredTo(loc);
                int distToCenter = loc.distanceSquaredTo(mapCenter);
                if(distFromKing > bestDist || (distFromKing == bestDist && distToCenter < bestDistToCenter)){
                    bestDist = distFromKing;
                    bestDistToCenter = distToCenter;
                    bestSpawnLoc = loc;
                }
            }
        }
        
        if(bestSpawnLoc != null){
            rc.buildRat(bestSpawnLoc);
            summonCount++;
            return new Result(OK, "Building rat to " + bestSpawnLoc + " (furthest, toward center)");
        }
        
        // If no location at distance 2, try distance 1, still prefer toward center
        for(MapLocation loc: rc.getAllLocationsWithinRadiusSquared(myLoc, 1)){
            if(rc.canBuildRat(loc)){
                int distToCenter = loc.distanceSquaredTo(mapCenter);
                if(distToCenter < bestDistToCenter){
                    bestDistToCenter = distToCenter;
                    bestSpawnLoc = loc;
                }
            }
        }
        
        if(bestSpawnLoc != null){
            rc.buildRat(bestSpawnLoc);
            summonCount++;
            return new Result(OK, "Building rat to " + bestSpawnLoc);
        }

        return new Result(WARN, "No location to spawn rats in radius 4 or 1");
    };
}
