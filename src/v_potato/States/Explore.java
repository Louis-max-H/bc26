package v_potato.States;

import battlecode.common.*;
import v_potato.Utils.PathFinding;
import v_potato.Utils.VisionUtils;

import static v_potato.States.Code.*;

public class Explore extends State {
    /**
     * Explore use a init in Init state.
     * We have a big array of interrest, and we move/turn to the zone with the biggest score.
     * */

    public Explore(){
        this.name = "Explore";
    }

    private static final int CAT_DANGER_RADIUS_SQ = 25;
    private static final int CAT_POUNCE_RADIUS_SQ = 9;
    private static final int CAT_NEAR_KING_RADIUS_SQ = 25;
    private static final int BABY_DEFEND_RADIUS_SQ = 36;
    private static final int MIN_GLOBAL_CHEESE_FOR_BABY_TRAP = 150;

    @Override
    public Result run() throws GameActionException {
        if(shouldDefendKingFromCat()){
            return defendKingFromCat();
        }

        if(shouldAvoidCat()){
            return avoidCat();
        }

        // Check if we can move and turn
        if(!rc.isMovementReady() && rc.getTurningCooldownTurns() >= 10){
            return new Result(CANT, "Cooldown");
        }

        // For each nearby cells, add their heuristic to the direction that lead to this cell
        int[] scores = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        MapLocation myLoc = rc.getLocation();
        for(Direction dir : Direction.values()){
            if(dir != Direction.CENTER){
                scores[dir.ordinal()] = VisionUtils.getScoreInView(myLoc.add(dir), dir, rc.getType());
            }
        }

        PathFinding.addScoresWithoutNormalization(scores, 1);
        Direction bestDir = PathFinding.bestDir();

        // Turn and move to this direction
        if(bestDir != Direction.CENTER){
            if(rc.getTurningCooldownTurns() < 10 && rc.canTurn(bestDir)){
                rc.turn(bestDir);
            }
            if(rc.isMovementReady()){
                if(PathFinding.moveDir(bestDir).notOk()){
                    print("Can't move to best direction.");
                }
            }
            return new Result(END_OF_TURN, "Turning and moving to " + bestDir.name());
        }

        // TODOS: Maybe turn, and then, according to new infos, restart from beginning ?
        // TODOS: Check if you need to move after turning
        // TODOS: Check if second score parameters is pertinent
        // TODOS: Check if not moving when second direction is nice, is good choice (can allow us to just tourn arround and then move)

        return new Result(END_OF_TURN, "No move");
    };

    private boolean shouldAvoidCat() throws GameActionException {
        if(nearestCat == null){
            return false;
        }
        if(round - lastCatSeenRound > 5){
            return false;
        }
        if(!rc.canSenseLocation(nearestCat)){
            return false;
        }
        RobotInfo ri = rc.senseRobotAtLocation(nearestCat);
        if(ri == null || ri.getType() != UnitType.CAT){
            return false;
        }
        return rc.getLocation().distanceSquaredTo(nearestCat) <= CAT_DANGER_RADIUS_SQ;
    }

    private boolean shouldDefendKingFromCat() throws GameActionException {
        if(nearestKing == null || nearestCat == null){
            return false;
        }
        if(round - lastCatSeenRound > 5){
            return false;
        }
        if(!rc.canSenseLocation(nearestCat)){
            return false;
        }
        RobotInfo ri = rc.senseRobotAtLocation(nearestCat);
        if(ri == null || ri.getType() != UnitType.CAT){
            return false;
        }
        return nearestKing.distanceSquaredTo(nearestCat) <= CAT_NEAR_KING_RADIUS_SQ
            && rc.getLocation().distanceSquaredTo(nearestKing) <= BABY_DEFEND_RADIUS_SQ;
    }

    private Result defendKingFromCat() throws GameActionException {
        tryPlaceInterceptTrap();
        moveToward(nearestCat);
        return new Result(END_OF_TURN, "Defending king from cat");
    }

    private void moveToward(MapLocation target) throws GameActionException {
        if(target == null){
            return;
        }
        if(PathFinding.tryMoveToward(target)){
            return;
        }

        Direction fallback = pickFallbackMove();
        if(fallback != Direction.CENTER){
            PathFinding.moveDir(fallback);
        }
    }

    private Direction pickFallbackMove(){
        int start = rng.nextInt(directions.length);
        for(int i = 0; i < directions.length; i++){
            Direction dir = directions[(start + i) % directions.length];
            if(!rc.canMove(dir)){
                continue;
            }
            if(lastDirection != null && dir == lastDirection.opposite()){
                continue;
            }
            return dir;
        }
        return Direction.CENTER;
    }

    private void tryPlaceInterceptTrap() throws GameActionException {
        if(!rc.isActionReady() || nearestCat == null || nearestKing == null){
            return;
        }
        boolean emergency = nearestKing.distanceSquaredTo(nearestCat) <= CAT_POUNCE_RADIUS_SQ;
        if(!emergency && rc.getGlobalCheese() < MIN_GLOBAL_CHEESE_FOR_BABY_TRAP){
            return;
        }
        if(!emergency && rc.getRawCheese() > 0){
            return;
        }

        MapLocation myLoc = rc.getLocation();
        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(myLoc, 2)){
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
            if(loc.distanceSquaredTo(nearestKing) <= 2){
                continue;
            }

            int score = -loc.distanceSquaredTo(nearestCat) + loc.distanceSquaredTo(nearestKing);
            if(best == null || score > bestScore){
                bestScore = score;
                best = loc;
            }
        }

        if(best == null){
            return;
        }
        if(rc.canPlaceCatTrap(best)){
            rc.placeCatTrap(best);
            return;
        }
        if(rc.canPlaceRatTrap(best)){
            rc.placeRatTrap(best);
        }
    }

    private Result avoidCat() throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        int currentDist = myLoc.distanceSquaredTo(nearestCat);

        Direction bestDir = Direction.CENTER;
        int bestScore = Integer.MIN_VALUE;
        for(Direction dir : Direction.values()){
            if(dir == Direction.CENTER){
                continue;
            }
            if(!rc.canMove(dir)){
                continue;
            }
            MapLocation next = myLoc.add(dir);
            int nextDist = next.distanceSquaredTo(nearestCat);
            int score = (nextDist - currentDist) * 10;
            if(score > bestScore){
                bestScore = score;
                bestDir = dir;
            }
        }

        if(bestScore >= 0 && bestDir != Direction.CENTER){
            PathFinding.moveDir(bestDir);
            return new Result(END_OF_TURN, "Avoiding cat");
        }

        return new Result(END_OF_TURN, "Cat nearby but no safe move");
    }
}
