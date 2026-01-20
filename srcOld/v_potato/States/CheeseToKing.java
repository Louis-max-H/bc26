package v_potato.States;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.TrapType;
import battlecode.common.UnitType;
import v_potato.Utils.PathFinding;

import static v_potato.States.Code.*;

public class CheeseToKing extends State {
    public CheeseToKing(){
        this.name = "CheeseToKing";
    }

    private static final int CAT_DANGER_RADIUS_SQ = 25;
    private static final int CAT_POUNCE_RADIUS_SQ = 9;
    private static final int RETURN_TO_KING_CHEESE = 1;
    private static final int KING_GUARD_RADIUS_SQ = 8;
    private static final int KING_GUARD_RING_MIN = 5;
    private static final int KING_GUARD_RING_MAX = 13;
    private static final int CAT_NEAR_KING_RADIUS_SQ = 25;
    private static final int BABY_DEFEND_RADIUS_SQ = 36;
    private static final int GUARD_TARGET_TTL = 5;
    private static final int MIN_GLOBAL_CHEESE_FOR_BABY_TRAP = 150;
    private static final int KING_RING_RADIUS = 2;
    private static final int KING_HOLD_RADIUS = 3;

    private MapLocation guardTarget;
    private int guardTargetUntilRound;

    @Override
    public Result run() throws GameActionException {
        // Check if we have cheese
        int carriedCheese = rc.getRawCheese();
        if(carriedCheese < RETURN_TO_KING_CHEESE){
            return new Result(OK, "");
        }

        MapLocation nearbyKing = findNearbyAlliedKing();
        if(nearbyKing != null){
            nearestKing = nearbyKing;
        }
        if(nearestKing == null){
            nearestKing = findKnownKingFromMemory();
        }

        // Check if we have a king to go
        boolean kingFromFallback = false;
        if(nearestKing == null){
            nearestKing = spawnLoc;
            kingFromFallback = true;
        }
        if(nearestKing == null){
            return new Result(WARN, "I have no king to drop cheese");
        }

        if(rc.canSenseLocation(nearestKing)){
            if(rc.canSenseRobotAtLocation(nearestKing)){
                RobotInfo unit = rc.senseRobotAtLocation(nearestKing);
                if(unit.type != UnitType.RAT_KING || unit.team != rc.getTeam()){
                    nearestKing = spawnLoc;
                    kingFromFallback = true;
                }
            }else{
                MapLocation known = findKnownKingFromMemory();
                if(known != null){
                    nearestKing = known;
                    kingFromFallback = false;
                }else{
                    nearestKing = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
                    kingFromFallback = true;
                }
            }
        }

        maybeTurnToward(nearestKing);
        if(tryTransferToKing(carriedCheese)){
            return new Result(END_OF_TURN, "Cheese transferred!");
        }
        Result transferHold = handleTransferHold(kingFromFallback, carriedCheese);
        if(transferHold != null){
            return transferHold;
        }

        if(shouldAvoidCatWhileCarrying()){
            return avoidCat();
        }

        if(stuckTurns >= 3){
            Result dig = tryDigBlockingDirt(nearestKing);
            if(dig != null){
                return dig;
            }
        }

        MapLocation moveTarget = nearestKing;
        if(!kingFromFallback){
            MapLocation approachTarget = pickKingApproachTarget(nearestKing);
            if(approachTarget != null){
                moveTarget = approachTarget;
            }else{
                MapLocation holdTarget = pickKingHoldTarget(nearestKing);
                if(holdTarget != null){
                    moveTarget = holdTarget;
                }
            }
        }

        maybeTurnToward(moveTarget);
        print("Moving to king at " + nearestKing);
        moveToward(moveTarget);

        if(stuckTurns >= 4 && rc.isMovementReady()){
            if(tryUnstickMove(nearestKing)){
                return new Result(END_OF_TURN, "Unsticking near king");
            }
        }

        maybeTurnToward(nearestKing);
        if(tryTransferToKing(carriedCheese)){
            return new Result(END_OF_TURN, "Cheese transferred!");
        }
        transferHold = handleTransferHold(kingFromFallback, carriedCheese);
        if(transferHold != null){
            return transferHold;
        }

        return new Result(END_OF_TURN, "Can't transfer cheese");
    }

    private boolean tryPlaceKingTrap() throws GameActionException {
        if(nearestKing == null || nearestCat == null){
            return false;
        }
        boolean emergency = nearestKing.distanceSquaredTo(nearestCat) <= CAT_POUNCE_RADIUS_SQ;
        if(!emergency && rc.getGlobalCheese() < MIN_GLOBAL_CHEESE_FOR_BABY_TRAP){
            return false;
        }
        if(!emergency && rc.getRawCheese() > 0){
            return false;
        }
        if(!rc.isActionReady()){
            return false;
        }
        MapLocation myLoc = rc.getLocation();
        if(myLoc.distanceSquaredTo(nearestKing) > KING_GUARD_RADIUS_SQ){
            return false;
        }
        if(nearestKing.distanceSquaredTo(nearestCat) > CAT_DANGER_RADIUS_SQ){
            return false;
        }

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
            if(loc.distanceSquaredTo(nearestKing) > 2){
                continue;
            }

            int score = -loc.distanceSquaredTo(nearestKing) - loc.distanceSquaredTo(nearestCat);
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
            return true;
        }
        if(rc.canPlaceRatTrap(best)){
            rc.placeRatTrap(best);
            return true;
        }

        return false;
    }

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

    private boolean moveGuardRing() throws GameActionException {
        if(nearestKing == null){
            return false;
        }
        MapLocation myLoc = rc.getLocation();
        int distToKing = myLoc.distanceSquaredTo(nearestKing);
        if(distToKing >= KING_GUARD_RING_MIN && distToKing <= KING_GUARD_RING_MAX){
            if(guardTarget == null || round >= guardTargetUntilRound){
                guardTarget = chooseGuardTarget();
                guardTargetUntilRound = round + GUARD_TARGET_TTL;
            }
            if(guardTarget != null && myLoc.distanceSquaredTo(guardTarget) <= 2){
                return true;
            }
        }

        if(guardTarget == null || round >= guardTargetUntilRound){
            guardTarget = chooseGuardTarget();
            guardTargetUntilRound = round + GUARD_TARGET_TTL;
        }
        if(guardTarget != null){
            moveToward(guardTarget);
            return true;
        }
        return false;
    }

    private MapLocation chooseGuardTarget() throws GameActionException {
        if(nearestKing == null){
            return null;
        }
        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(nearestKing, KING_GUARD_RING_MAX)){
            int distToKing = nearestKing.distanceSquaredTo(loc);
            if(distToKing < KING_GUARD_RING_MIN || distToKing > KING_GUARD_RING_MAX){
                continue;
            }
            if(!rc.canSenseLocation(loc)){
                continue;
            }
            MapInfo mi = rc.senseMapInfo(loc);
            if(!mi.isPassable()){
                continue;
            }
            int score = -distToKing;
            if(nearestCat != null){
                score -= loc.distanceSquaredTo(nearestCat);
            }
            if(best == null || score > bestScore){
                bestScore = score;
                best = loc;
            }
        }
        return best;
    }

    private Result avoidCat() throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        int currentDist = myLoc.distanceSquaredTo(nearestCat);

        Direction bestDir = Direction.CENTER;
        int bestScore = Integer.MIN_VALUE;
        Direction secondDir = Direction.CENTER;
        int secondScore = Integer.MIN_VALUE;
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

    private MapLocation findNearbyAlliedKing() throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        MapLocation closest = null;
        int bestDist = Integer.MAX_VALUE;
        for(RobotInfo info : rc.senseNearbyRobots(8, rc.getTeam())){
            if(!info.type.isRatKingType()){
                continue;
            }
            int dist = myLoc.distanceSquaredTo(info.location);
            if(dist < bestDist){
                bestDist = dist;
                closest = info.location;
            }
        }
        return closest;
    }

    private MapLocation findKnownKingFromMemory(){
        MapLocation myLoc = rc.getLocation();
        MapLocation closest = null;
        int bestDist = Integer.MAX_VALUE;
        for(char i = 0; i < kings.size; i++){
            MapLocation loc = kings.locs[i];
            if(loc == null){
                continue;
            }
            int dist = myLoc.distanceSquaredTo(loc);
            if(dist < bestDist){
                bestDist = dist;
                closest = loc;
            }
        }
        return closest;
    }

    private boolean isAdjacentToKingCenter(MapLocation kingLoc){
        if(kingLoc == null){
            return false;
        }
        MapLocation myLoc = rc.getLocation();
        int dx = Math.abs(myLoc.x - kingLoc.x);
        int dy = Math.abs(myLoc.y - kingLoc.y);
        return Math.max(dx, dy) == 2;
    }

    private MapLocation pickKingApproachTarget(MapLocation kingLoc) throws GameActionException {
        if(kingLoc == null){
            return null;
        }
        MapLocation myLoc = rc.getLocation();
        MapLocation[] ring = getRingSlots(kingLoc, KING_RING_RADIUS);
        if(ring == null || ring.length == 0){
            return null;
        }

        MapLocation preferred = ring[rc.getID() % ring.length];
        if(isRingSlotFree(preferred)){
            return preferred;
        }

        MapLocation best = null;
        int bestDist = Integer.MAX_VALUE;
        for(MapLocation loc : ring){
            if(!isRingSlotFree(loc)){
                continue;
            }
            int dist = myLoc.distanceSquaredTo(loc);
            if(dist < bestDist){
                bestDist = dist;
                best = loc;
            }
        }
        return best;
    }

    private MapLocation pickKingHoldTarget(MapLocation kingLoc) throws GameActionException {
        MapLocation[] ring = getRingSlots(kingLoc, KING_HOLD_RADIUS);
        if(ring == null || ring.length == 0){
            return null;
        }
        return ring[rc.getID() % ring.length];
    }

    private MapLocation[] getRingSlots(MapLocation center, int radius) {
        if(center == null){
            return null;
        }
        MapLocation[] slots = new MapLocation[16];
        int count = 0;
        for(int dx = -radius; dx <= radius; dx++){
            for(int dy = -radius; dy <= radius; dy++){
                if(Math.max(Math.abs(dx), Math.abs(dy)) != radius){
                    continue;
                }
                MapLocation loc = new MapLocation(center.x + dx, center.y + dy);
                slots[count++] = loc;
            }
        }
        MapLocation[] trimmed = new MapLocation[count];
        for(int i = 0; i < count; i++){
            trimmed[i] = slots[i];
        }
        return trimmed;
    }

    private boolean isRingSlotFree(MapLocation loc) throws GameActionException {
        if(loc == null || !rc.onTheMap(loc)){
            return false;
        }
        if(!rc.canSenseLocation(loc)){
            return true;
        }
        MapInfo mi = rc.senseMapInfo(loc);
        if(!mi.isPassable()){
            return false;
        }
        if(rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc) != null){
            return false;
        }
        return true;
    }

    private boolean tryTransferToKing(int amount) throws GameActionException {
        if(amount <= 0 || !rc.isActionReady() || nearestKing == null){
            return false;
        }
        if(rc.canTransferCheese(nearestKing, amount)){
            rc.transferCheese(nearestKing, amount);
            return true;
        }
        return false;
    }

    private Result handleTransferHold(boolean kingFromFallback, int amount) throws GameActionException {
        if(kingFromFallback || nearestKing == null){
            return null;
        }
        if(!isAdjacentToKingCenter(nearestKing)){
            return null;
        }
        maybeTurnToward(nearestKing);
        if(rc.isActionReady() && rc.canTransferCheese(nearestKing, amount)){
            rc.transferCheese(nearestKing, amount);
            return new Result(END_OF_TURN, "Cheese transferred!");
        }
        return new Result(END_OF_TURN, "Waiting to transfer");
    }

    private boolean tryShiftOnRing() throws GameActionException {
        if(nearestKing == null){
            return false;
        }
        MapLocation myLoc = rc.getLocation();
        int dx = Math.abs(myLoc.x - nearestKing.x);
        int dy = Math.abs(myLoc.y - nearestKing.y);
        if(Math.max(dx, dy) != 2){
            return false;
        }
        Direction toKing = myLoc.directionTo(nearestKing);
        Direction[] options = new Direction[]{
            toKing.rotateLeft(),
            toKing.rotateRight(),
            toKing.rotateLeft().rotateLeft(),
            toKing.rotateRight().rotateRight(),
        };
        for(Direction dir : options){
            if(dir == Direction.CENTER){
                continue;
            }
            if(!rc.canMove(dir)){
                continue;
            }
            MapLocation next = myLoc.add(dir);
            int ndx = Math.abs(next.x - nearestKing.x);
            int ndy = Math.abs(next.y - nearestKing.y);
            if(Math.max(ndx, ndy) != 2){
                continue;
            }
            PathFinding.moveDir(dir);
            return true;
        }
        return false;
    }

    private boolean stepOutFromKing() throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        int currentDist = myLoc.distanceSquaredTo(nearestKing);
        Direction bestDir = Direction.CENTER;
        int bestDist = Integer.MAX_VALUE;
        for(Direction dir : directions){
            if(!rc.canMove(dir)){
                continue;
            }
            MapLocation next = myLoc.add(dir);
            int dist = next.distanceSquaredTo(nearestKing);
            if(dist <= currentDist){
                continue;
            }
            if(dist < bestDist){
                bestDist = dist;
                bestDir = dir;
            }
        }
        if(bestDir != Direction.CENTER){
            PathFinding.moveDir(bestDir);
            return true;
        }
        return false;
    }

    private boolean tryUnstickMove(MapLocation anchor) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        int currentDist = anchor != null ? myLoc.distanceSquaredTo(anchor) : 0;
        int start = rng.nextInt(directions.length);
        for(int i = 0; i < directions.length; i++){
            Direction dir = directions[(start + i) % directions.length];
            if(!rc.canMove(dir)){
                continue;
            }
            if(anchor != null){
                int nextDist = myLoc.add(dir).distanceSquaredTo(anchor);
                if(nextDist > currentDist + 2){
                    continue;
                }
            }
            PathFinding.moveDir(dir);
            return true;
        }
        return false;
    }

    private void maybeTurnToward(MapLocation target) throws GameActionException {
        if(target == null){
            return;
        }
        if(rc.getTurningCooldownTurns() >= 10){
            return;
        }
        Direction dir = rc.getLocation().directionTo(target);
        if(dir != Direction.CENTER && rc.canTurn(dir)){
            rc.turn(dir);
        }
    }

    private boolean shouldAvoidCatWhileCarrying() throws GameActionException {
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
        return rc.getLocation().distanceSquaredTo(nearestCat) <= CAT_POUNCE_RADIUS_SQ;
    }

    private void moveToward(MapLocation target) throws GameActionException {
        if(target == null){
            return;
        }
        if(PathFinding.tryMoveToward(target)){
            return;
        }

        Direction fallback = pickFallbackMoveToward(target);
        if(fallback != Direction.CENTER){
            PathFinding.moveDir(fallback);
        }
    }

    private Direction pickFallbackMoveToward(MapLocation target){
        MapLocation myLoc = rc.getLocation();
        Direction reverse = lastDirection != null ? lastDirection.opposite() : null;
        Direction bestDir = Direction.CENTER;
        int bestDist = Integer.MAX_VALUE;
        Direction firstLegal = Direction.CENTER;

        for(Direction dir : directions){
            if(!rc.canMove(dir)){
                continue;
            }
            if(firstLegal == Direction.CENTER){
                firstLegal = dir;
            }
            if(reverse != null && dir == reverse){
                continue;
            }
            int dist = target != null ? myLoc.add(dir).distanceSquaredTo(target) : 0;
            if(dist < bestDist){
                bestDist = dist;
                bestDir = dir;
            }
        }

        if(bestDir != Direction.CENTER){
            return bestDir;
        }
        if(reverse != null && rc.canMove(reverse)){
            return reverse;
        }
        return firstLegal;
    }

    private Result tryDigBlockingDirt(MapLocation target) throws GameActionException {
        if(target == null || !rc.isActionReady()){
            return null;
        }
        MapLocation myLoc = rc.getLocation();
        MapLocation best = null;
        int bestDist = Integer.MAX_VALUE;
        for(Direction dir : directions){
            MapLocation loc = myLoc.add(dir);
            if(!rc.onTheMap(loc)){
                continue;
            }
            if(!rc.canRemoveDirt(loc)){
                continue;
            }
            int dist = loc.distanceSquaredTo(target);
            if(dist < bestDist){
                bestDist = dist;
                best = loc;
            }
        }
        if(best == null){
            return null;
        }
        rc.removeDirt(best);
        return new Result(END_OF_TURN, "Clearing dirt to king");
    }
}
