package v006_saketh_updates.States;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static v006_saketh_updates.States.Code.*;

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
        // Conservative approach: always keep cheese buffer for 200 rounds
        // Rat kings consume 2 cheese per round, so minimum buffer = 200 * 2 = 400 cheese
        // Plus some extra for safety and potential spawn costs

        int cheeseStock = rc.getAllCheese();

        if(!isKing){
            return new Result(ERR, "Unit should be king to spawn rats.");
        }

        // Calculate minimum cheese needed for 200 rounds
        // Rat king consumes 2 cheese per round, so 200 rounds = 400 cheese minimum
        // Add buffer for potential spawn costs (current cost can be up to ~50+)
        int minCheeseFor200Rounds = 400 + 100; // 400 for consumption + 100 buffer = 500 minimum
        
        // More conservative: calculate based on current spawn cost too
        int currentSpawnCost = rc.getCurrentRatCost();
        int conservativeBuffer = minCheeseFor200Rounds + currentSpawnCost; // Always have enough for one spawn + 200 rounds

        // Check if we have enough cheese for 200 rounds
        if(cheeseStock < conservativeBuffer){
            return new Result(OK, "Conserving cheese for 200 rounds. Have: " + cheeseStock + ", Need: " + conservativeBuffer);
        }

        // Only spawn if we can afford it AND still have buffer
        int cheeseAfterSpawn = cheeseStock - currentSpawnCost;
        if(cheeseAfterSpawn < minCheeseFor200Rounds){
            return new Result(OK, "Can't spawn - would drop below 200-round buffer. After spawn: " + cheeseAfterSpawn + ", Need: " + minCheeseFor200Rounds);
        }

        // Additional cost cap check - don't spawn if cost is too high
        // 10 base cost of rat, but costs increase with more rats
        // Be more conservative with cost cap
        int maxAcceptableCost = 15; // Lower cap to be more conservative
        if(currentSpawnCost > maxAcceptableCost){
            return new Result(OK, "Cost too high: " + currentSpawnCost + " (max: " + maxAcceptableCost + ")");
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
