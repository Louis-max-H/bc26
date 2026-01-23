package current.States;

import battlecode.common.*;
import current.Params;
import current.Robots.Robot;
import current.Utils.Tools;

import static current.States.Code.*;
import static java.lang.Math.sqrt;

public class Spawn extends State {
    public int summonCount = 0;
    public static int width;
    public static int height;
    
    public static MapLocation NORTH_CORNER;
    public static MapLocation SOUTH_CORNER;
    public static MapLocation EAST_CORNER;
    public static MapLocation WEST_CORNER;
    public static MapLocation NORTH_EAST_CORNER;
    public static MapLocation NORTH_WEST_CORNER;
    public static MapLocation SOUTH_EAST_CORNER;
    public static MapLocation SOUTH_WEST_CORNER;

    public static long maxDistancePlusMinCost = 10;
    public static long scoreSpawnDirection[] = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 99999999}; // Lower is best

    public static MapLocation getLocation(Direction dir){
        return switch(dir){
            case Direction.NORTHEAST -> NORTH_CORNER;
            case Direction.SOUTHEAST -> SOUTH_CORNER;
            case Direction.SOUTHWEST -> EAST_CORNER;
            case Direction.NORTHWEST -> WEST_CORNER;
            case Direction.NORTH     -> NORTH_EAST_CORNER;
            case Direction.SOUTH     -> NORTH_WEST_CORNER;
            case Direction.EAST      -> SOUTH_EAST_CORNER;
            case Direction.WEST      -> SOUTH_WEST_CORNER;
            case Direction.CENTER    -> null;
        };
    }

    public Spawn(){
        this.name = "Spawn";
        width = rc.getMapWidth();
        height = rc.getMapHeight();

        NORTH_CORNER = new MapLocation(width / 2, height - 1);
        SOUTH_CORNER = new MapLocation(width / 2, 0);
        EAST_CORNER = new MapLocation(width - 1, height / 2);
        WEST_CORNER = new MapLocation(0, height / 2);

        NORTH_EAST_CORNER = new MapLocation(height - 1, width - 1);
        NORTH_WEST_CORNER = new MapLocation(height - 1, 0);
        SOUTH_EAST_CORNER = new MapLocation(0, width - 1);
        SOUTH_WEST_CORNER = new MapLocation(0, 0);
    }

    @Override
    public Result run() throws GameActionException {
        int cheeseStock = rc.getAllCheese();

        if(!isKing){
            return new Result(ERR, "Unit should be king to spawn rats.");
        }

        // Calculate minimum cheese needed for 200 rounds
        // Rat king consumes 2 cheese per round, so 200 rounds = 400 cheese minimum
        // Add buffer for potential spawn costs (current cost can be up to ~50+)
        int minCheeseFor200Rounds = 400 * kings.size; // 400 for consumption + 100 buffer = 500 minimum
        if(minCheeseFor200Rounds > cheeseStock){
            return new Result(OK, "Not enough cheese: " + cheeseStock + " (min: " + minCheeseFor200Rounds + ")");
        }


        // Don't spawn if costs is too high
        int maxAcceptableCost = 10 + (Params.maxRats / 4)*10 ; // 10 cheese per 4 rats
        if(rc.getCurrentRatCost() >= maxAcceptableCost){
            return new Result(OK, "Cost too high: " + rc.getCurrentRatCost() + " (max: " + maxAcceptableCost + ")");
        }

        // Move myloc to local scop
        MapLocation myLoc = rc.getLocation();

        // Update score functions
        int maxDistance = 0;
        if(maxDistance < myLoc.distanceSquaredTo(NORTH_EAST_CORNER)){maxDistance = myLoc.distanceSquaredTo(NORTH_EAST_CORNER);}
        if(maxDistance < myLoc.distanceSquaredTo(SOUTH_EAST_CORNER)){maxDistance = myLoc.distanceSquaredTo(SOUTH_EAST_CORNER);}
        if(maxDistance < myLoc.distanceSquaredTo(SOUTH_WEST_CORNER)){maxDistance = myLoc.distanceSquaredTo(SOUTH_WEST_CORNER);}
        if(maxDistance < myLoc.distanceSquaredTo(NORTH_WEST_CORNER)){maxDistance = myLoc.distanceSquaredTo(NORTH_WEST_CORNER);}
        if(maxDistance < myLoc.distanceSquaredTo(NORTH_CORNER)){maxDistance = myLoc.distanceSquaredTo(NORTH_CORNER);}
        if(maxDistance < myLoc.distanceSquaredTo(SOUTH_CORNER)){maxDistance = myLoc.distanceSquaredTo(SOUTH_CORNER);}
        if(maxDistance < myLoc.distanceSquaredTo(EAST_CORNER)){maxDistance = myLoc.distanceSquaredTo(EAST_CORNER);}
        if(maxDistance < myLoc.distanceSquaredTo(WEST_CORNER)){maxDistance = myLoc.distanceSquaredTo(WEST_CORNER);}
        maxDistancePlusMinCost = maxDistance + 10;

        // Get minim direction
        Direction minDir = Tools.lowerDirOfLong9(scoreSpawnDirection);
        MapLocation targetExplore = getLocation(minDir);


        // Try to spawn the rat
        int minDistance = Integer.MAX_VALUE;
        MapLocation spawnLoc = null;
        for(MapInfo info: rc.senseNearbyMapInfos(GameConstants.BUILD_ROBOT_RADIUS_SQUARED)){
            if( rc.canBuildRat(info.getMapLocation())
            && !rc.canSenseRobotAtLocation(info.getMapLocation())
            && targetExplore.distanceSquaredTo(info.getMapLocation()) < minDistance
            ){
                spawnLoc = info.getMapLocation();
                minDistance = targetExplore.distanceSquaredTo(info.getMapLocation());
            }
        }

        // Check if can spawn
        if(spawnLoc == null){
            return new Result(OK, "No location to spawn rats");
        }

        // If can spawn
        if(!rc.canBuildRat(spawnLoc)){
            return new Result(OK, "Can't spawn rat at " + spawnLoc);
        }

        // Send direction to rat using his spawning direction
        rc.turn(minDir);
        rc.buildRat(spawnLoc);

        // Add score to scoreSpawnDirection (More far = less malus)
        scoreSpawnDirection[minDir.ordinal()] += 10 + maxDistancePlusMinCost - (long)sqrt(myLoc.distanceSquaredTo(targetExplore));
        return new Result(WARN, "Spawn a rat at " + spawnLoc);
    };
}
