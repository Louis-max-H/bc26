package current.States;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.TrapType;

import static current.States.Code.*;

public class Spawn extends State {
    public int summonCount = 0;
    public int reserveNeeded;
    public int costCap;
    private int lastTrapRound = -999;
    private static final int TRAP_COOLDOWN_ROUNDS = 1;
    private static final int TRAP_MIN_BUFFER = 0;
    private static final int TRAP_EMERGENCY_RADIUS_SQ = 100;
    private static final int TRAP_EARLY_ROUNDS = 10;
    private static final int RELOCATE_ROUNDS = 120;
    private static final int EDGE_BUFFER = 2;
    private static final int MIN_TRAPS_AROUND_KING = 8;
    private static final int FORTIFY_ROUNDS = 120;
    private static final int TRAP_STOCK_BUFFER = 100;
    private static final int GLOBAL_CHEESE_EMERGENCY = 300;
    private static final int MIN_PASSABLE_RING_TILES = 2;
    public Spawn(){
        this.name = "Spawn";
    }

    @Override
    public Result run() throws GameActionException {
        // Spawn rats if it can and with basics conditions
        // TODOS: We can use the vector King -> Spawn location to indicate a direction for exploration of the spawned rat
        // TODOS: We may spawn rats the further we can to help them explore ?

        int cheeseStock = rc.getAllCheese();
        int globalCheese = rc.getGlobalCheese();

        if(!rc.getType().isRatKingType()){
            return new Result(ERR, "Unit should be king to spawn rats.");
        }

        // Loading parameters
        if (gamePhase <= PHASE_START) {
            reserveNeeded = 200;  // keep more cheese for king survival
            costCap = 60;
        } else if (gamePhase <= PHASE_MIDLE) {
            reserveNeeded = 400;
            costCap = 80;
        } else {
            // Late game: conservative
            reserveNeeded = 800;
            costCap = 100;
        }

        boolean emergency = nearestCat != null
            && rc.getLocation().distanceSquaredTo(nearestCat) <= TRAP_EMERGENCY_RADIUS_SQ;

        if(globalCheese < GLOBAL_CHEESE_EMERGENCY && !emergency){
            return new Result(OK, "Economy hold");
        }

        if(!emergency && tryClearTransferLane()){
            return new Result(OK, "Clearing transfer lane");
        }

        if(round <= TRAP_EARLY_ROUNDS && tryPlaceDefenseTrap(Integer.MAX_VALUE, true)){
            return new Result(OK, "Early trap down");
        }

        if(shouldFortify(emergency, cheeseStock) && tryPlaceDefenseTrap(cheeseStock, emergency)){
            return new Result(OK, "Placed defense trap");
        }
        if(emergency){
            if(tryStepAwayFromCat()){
                return new Result(END_OF_TURN, "Stepping away from cat");
            }
            return new Result(END_OF_TURN, "Emergency defense hold");
        }
        if(shouldRelocate()){
            if(tryMoveTowardCenter()){
                return new Result(END_OF_TURN, "Relocating king");
            }
        }

        if(rc.getCurrentRatCost() > cheeseStock){
            return new Result(OK, "Not enough cheese " + rc.getRawCheese() + ", " + rc.getCurrentRatCost() + "needed");
        }

        // !!!!!!!!!!!!! Only for debug purpose
        if(false && summonCount > 4 && !competitiveMode){
            return new Result(WARN, "Debug only!!, summon limited to 1");
        }

        if(cheeseStock - rc.getCurrentRatCost() < reserveNeeded){
            return new Result(OK, "Keeping reserve " + reserveNeeded);
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

    private boolean tryClearTransferLane() throws GameActionException {
        if(!rc.isActionReady()){
            return false;
        }
        int passable = countPassableRingTiles();
        if(passable > MIN_PASSABLE_RING_TILES){
            return false;
        }
        MapLocation kingLoc = rc.getLocation();
        MapLocation best = null;
        int bestDist = Integer.MAX_VALUE;
        for(int dx = -2; dx <= 2; dx++){
            for(int dy = -2; dy <= 2; dy++){
                if(Math.max(Math.abs(dx), Math.abs(dy)) != 2){
                    continue;
                }
                MapLocation loc = new MapLocation(kingLoc.x + dx, kingLoc.y + dy);
                if(!rc.onTheMap(loc)){
                    continue;
                }
                MapInfo info = rc.senseMapInfo(loc);
                if(!info.isDirt()){
                    continue;
                }
                if(!rc.canRemoveDirt(loc)){
                    continue;
                }
                int dist = loc.distanceSquaredTo(kingLoc);
                if(dist < bestDist){
                    bestDist = dist;
                    best = loc;
                }
            }
        }
        if(best == null){
            return false;
        }
        rc.removeDirt(best);
        return true;
    }

    private int countPassableRingTiles() throws GameActionException {
        MapLocation kingLoc = rc.getLocation();
        int count = 0;
        for(int dx = -2; dx <= 2; dx++){
            for(int dy = -2; dy <= 2; dy++){
                if(Math.max(Math.abs(dx), Math.abs(dy)) != 2){
                    continue;
                }
                MapLocation loc = new MapLocation(kingLoc.x + dx, kingLoc.y + dy);
                if(!rc.onTheMap(loc)){
                    continue;
                }
                MapInfo info = rc.senseMapInfo(loc);
                if(info.isPassable()){
                    count++;
                }
            }
        }
        return count;
    }

    private boolean shouldFortify(boolean emergency, int cheeseStock) throws GameActionException {
        if(emergency){
            return true;
        }
        if(round <= TRAP_EARLY_ROUNDS){
            return cheeseStock >= 10;
        }
        if(round <= FORTIFY_ROUNDS){
            return cheeseStock >= reserveNeeded;
        }
        return cheeseStock >= reserveNeeded + TRAP_STOCK_BUFFER
            && countTrapsAroundKing() < MIN_TRAPS_AROUND_KING;
    }

    private int countTrapsAroundKing() throws GameActionException {
        int count = 0;
        MapLocation kingLoc = rc.getLocation();
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(kingLoc, 2)){
            if(!rc.onTheMap(loc)){
                continue;
            }
            MapInfo mi = rc.senseMapInfo(loc);
            if(mi.getTrap() != TrapType.NONE){
                count++;
            }
        }
        return count;
    }

    private boolean shouldRelocate(){
        if(round > RELOCATE_ROUNDS){
            return false;
        }
        MapLocation loc = rc.getLocation();
        int maxX = rc.getMapWidth() - 1;
        int maxY = rc.getMapHeight() - 1;
        return loc.x <= EDGE_BUFFER || loc.y <= EDGE_BUFFER
            || loc.x >= maxX - EDGE_BUFFER || loc.y >= maxY - EDGE_BUFFER;
    }

    private boolean tryMoveTowardCenter() throws GameActionException {
        if(rc.getMovementCooldownTurns() != 0){
            return false;
        }
        MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        Direction dir = rc.getLocation().directionTo(center);
        if(dir != Direction.CENTER && rc.canMove(dir)){
            rc.move(dir);
            return true;
        }
        Direction left = dir.rotateLeft();
        Direction right = dir.rotateRight();
        if(left != Direction.CENTER && rc.canMove(left)){
            rc.move(left);
            return true;
        }
        if(right != Direction.CENTER && rc.canMove(right)){
            rc.move(right);
            return true;
        }
        return false;
    }

    private boolean tryPlaceDefenseTrap(int cheeseStock, boolean emergency) throws GameActionException {
        if(round - lastTrapRound < TRAP_COOLDOWN_ROUNDS){
            return false;
        }
        if(cheeseStock < 10){
            return false;
        }
        if(cheeseStock < reserveNeeded + TRAP_MIN_BUFFER && nearestCat == null && round > TRAP_EARLY_ROUNDS){
            return false;
        }
        if(!rc.isActionReady()){
            return false;
        }

        MapLocation kingLoc = rc.getLocation();
        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(kingLoc, 2)){
            if(!rc.onTheMap(loc)){
                continue;
            }
            MapInfo mi = rc.senseMapInfo(loc);
            if(!mi.isPassable()){
                continue;
            }
            if(mi.getTrap() != TrapType.NONE){
                continue;
            }

            int distToKing = kingLoc.distanceSquaredTo(loc);
            int score = -distToKing;
            if(nearestCat != null){
                score -= 2 * loc.distanceSquaredTo(nearestCat);
            }

            if(best == null || score > bestScore){
                bestScore = score;
                best = loc;
            }
        }

        if(best == null){
            return false;
        }

        if(rc.canPlaceCatTrap(best)){
            rc.placeCatTrap(best);
            lastTrapRound = round;
            return true;
        }
        if(rc.canPlaceRatTrap(best)){
            rc.placeRatTrap(best);
            lastTrapRound = round;
            return true;
        }

        return false;
    }

    private boolean tryStepAwayFromCat() throws GameActionException {
        if(nearestCat == null || rc.getMovementCooldownTurns() != 0){
            return false;
        }
        Direction away = rc.getLocation().directionTo(nearestCat).opposite();
        if(away != Direction.CENTER && rc.canMove(away)){
            rc.move(away);
            return true;
        }
        Direction left = away.rotateLeft();
        Direction right = away.rotateRight();
        if(left != Direction.CENTER && rc.canMove(left)){
            rc.move(left);
            return true;
        }
        if(right != Direction.CENTER && rc.canMove(right)){
            rc.move(right);
            return true;
        }
        return false;
    }
}
