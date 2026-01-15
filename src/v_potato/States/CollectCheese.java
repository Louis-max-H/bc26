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

public class CollectCheese extends State {
    public CollectCheese(){this.name = "CollectCheese";}

    private static final int MINE_STAY_RADIUS_SQ = 32;
    private static final int MINE_CROWD_RADIUS_SQ = 9;
    private static final int MAX_RATS_PER_MINE = 3;
    private static final int CAT_DANGER_RADIUS_SQ = 16;
    private static final int CAT_POUNCE_RADIUS_SQ = 9;
    private static final int CAT_NEAR_KING_RADIUS_SQ = 100;
    private static final int BABY_DEFEND_RADIUS_SQ = 16;
    private static final int RETURN_TO_KING_CHEESE = 1;
    private static final int KING_CLEAR_RADIUS_SQ = 8;
    private static final int WANDER_TARGET_TTL = 30;
    private static final int WANDER_REACH_RADIUS_SQ = 4;
    private static final int CENTER_BIAS_ROUNDS = 250;
    private static final int CENTER_BIAS_CHANCE = 70;
    private static final int CENTER_JITTER = 6;
    private static final int MIN_GLOBAL_CHEESE_FOR_BABY_TRAP = 150;
    private static final int GLOBAL_CHEESE_EMERGENCY = 600;
    private static final int MAX_DEFENDERS = 3;
    private static final int DEFENDER_COUNT_RADIUS_SQ = 16;
    private static final int FEED_EMERGENCY_GLOBAL = 250;
    private static final int FEED_EMERGENCY_HP = 200;
    private static final int ATTACK_MIN_GLOBAL_CHEESE = 800;
    private static final int TARGET_STALL_LIMIT = 6;
    private static final int ENEMY_NEAR_KING_RADIUS_SQ = 36;
    private static final int ENEMY_DEFEND_RADIUS_SQ = 36;
    private static final int MAX_ENEMY_DEFENDERS = 4;
    private static final int RAT_TRAP_GLOBAL_MIN = 300;
    private static final int RAT_TRAP_COOLDOWN_ROUNDS = 8;

    public MapLocation cheeseLoc;
    public MapLocation mineLoc;
    private MapLocation avoidMineLoc;
    private int avoidMineUntilRound;
    private MapLocation wanderTarget;
    private int wanderTargetUntilRound;
    private MapLocation lastTarget;
    private int lastTargetDist;
    private int targetStallTurns;
    private int lastRatTrapRound = -999;

    @Override
    public Result run() throws GameActionException {
        boolean feedingPriority = isFeeder || isKingCheeseEmergency();
        if(rc.getRawCheese() >= RETURN_TO_KING_CHEESE && nearestKing != null){
            if(shouldAvoidCatWhileCarrying()){
                return avoidCat();
            }
            moveToward(nearestKing, null);
            return new Result(END_OF_TURN, "Carrying cheese, heading to king");
        }

        if(shouldDefendKingFromEnemyRat()){
            return defendKingFromEnemyRat();
        }

        if(shouldDefendKingFromCat()){
            return defendKingFromCat();
        }

        if(shouldAvoidCat()){
            return avoidCat();
        }

        if(!feedingPriority){
            Result trap = tryPlaceRatTrapForDefense();
            if(trap != null){
                return trap;
            }
        }

        if(!feedingPriority && rc.getRawCheese() == 0 && rc.getGlobalCheese() >= ATTACK_MIN_GLOBAL_CHEESE){
            Result attack = tryAttackEnemy();
            if(attack != null){
                return attack;
            }
        }

        if(shouldClearKingRing()){
            return clearKingRing();
        }

        if(stuckTurns >= 3){
            MapLocation digTarget = cheeseLoc != null ? cheeseLoc : mineLoc;
            if(digTarget == null){
                digTarget = wanderTarget;
            }
            Result dig = tryDigBlockingDirt(digTarget);
            if(dig != null){
                return dig;
            }
        }

        if(stuckTurns >= 4){
            cheeseLoc = null;
            mineLoc = null;
            avoidMineLoc = null;
            avoidMineUntilRound = 0;
            wanderTarget = null;
            return wanderForCheese();
        }

        // Check existing cheeseLoc
        if(cheeseLoc != null && rc.canSenseLocation(cheeseLoc) && rc.senseMapInfo(cheeseLoc).getCheeseAmount() == 0){
            print("Cheese at " + cheeseLoc + " have disappear :'(");
            cheeseLoc = null;
        }

        // Check existing mineLoc
        if(mineLoc != null && rc.canSenseLocation(mineLoc) && !rc.senseMapInfo(mineLoc).hasCheeseMine()){
            cheeseMines.remove(mineLoc);
            mineLoc = null;
        }

        // Pick nearest mine
        if(mineLoc == null){
            mineLoc = findNearestMine();
        }

        // If mine is crowded, abandon and explore
        if(!feedingPriority && mineLoc != null && isMineCrowded(mineLoc)){
            cheeseLoc = null;
            avoidMineLoc = mineLoc;
            avoidMineUntilRound = round + 20;
            mineLoc = null;
            return wanderForCheese();
        }

        // Check for new cheese
        if(cheeseLoc == null){
            if(nearestCheese != null){
                cheeseLoc = nearestCheese;
            }
            for(MapInfo infos: rc.senseNearbyMapInfos()){

                // We can access cheese
                if(infos.isWall() || infos.getCheeseAmount() == 0){
                    continue;
                }

                MapLocation infoLoc = infos.getMapLocation();
                if(mineLoc != null && infoLoc.distanceSquaredTo(mineLoc) > MINE_STAY_RADIUS_SQ){
                    continue;
                }

                // Check if it's the nearest cheese
                if(cheeseLoc != null && rc.getLocation().distanceSquaredTo(cheeseLoc) < rc.getLocation().distanceSquaredTo(infoLoc)){
                    continue;
                }

                cheeseLoc = infoLoc;
            }
        }

        // Did we have a target ?
        if(cheeseLoc == null && mineLoc == null){
            return wanderForCheese();
        }

        Result pickup = tryPickupCheese();
        if(pickup != null){
            return pickup;
        }

        MapLocation target = cheeseLoc != null ? cheeseLoc : mineLoc;
        if(target != null){
            int dist = rc.getLocation().distanceSquaredTo(target);
            if(lastTarget != null && lastTarget.equals(target)){
                if(dist >= lastTargetDist){
                    targetStallTurns++;
                }else{
                    targetStallTurns = 0;
                }
            }else{
                targetStallTurns = 0;
            }
            lastTarget = target;
            lastTargetDist = dist;
            if(targetStallTurns >= TARGET_STALL_LIMIT){
                cheeseLoc = null;
                mineLoc = null;
                avoidMineLoc = null;
                avoidMineUntilRound = 0;
                wanderTarget = null;
                targetStallTurns = 0;
                return wanderForCheese();
            }
        }
        print("Moving to target " +  target);
        moveToward(target, mineLoc);

        pickup = tryPickupCheese();
        if(pickup != null){
            return pickup;
        }

        return new Result(END_OF_TURN, "Can't pickup");
    };

    private MapLocation findNearestMine(){
        if(cheeseMines.size == 0){
            return nearestMine;
        }

        MapLocation closest = null;
        int bestDist = Integer.MAX_VALUE;
        MapLocation myLoc = rc.getLocation();
        for(char i = 0; i < cheeseMines.size; i++){
            MapLocation loc = cheeseMines.locs[i];
            if(avoidMineLoc != null && round < avoidMineUntilRound && loc.equals(avoidMineLoc)){
                continue;
            }
            int dist = myLoc.distanceSquaredTo(loc);
            if(dist < bestDist){
                bestDist = dist;
                closest = loc;
            }
        }
        return closest != null ? closest : nearestMine;
    }

    private boolean isMineCrowded(MapLocation mine) throws GameActionException {
        int rats = 0;
        MapLocation myLoc = rc.getLocation();
        if(myLoc.distanceSquaredTo(mine) <= MINE_CROWD_RADIUS_SQ){
            rats++;
        }
        for(RobotInfo info : rc.senseNearbyRobots(-1, rc.getTeam())){
            if(info.type != UnitType.BABY_RAT){
                continue;
            }
            if(info.location.distanceSquaredTo(mine) <= MINE_CROWD_RADIUS_SQ){
                rats++;
                if(rats > MAX_RATS_PER_MINE){
                    return true;
                }
            }
        }
        return false;
    }

    private void moveToward(MapLocation target, MapLocation mine) throws GameActionException {
        if(target == null){
            return;
        }

        MapLocation myLoc = rc.getLocation();
        int mineDist = mine != null ? myLoc.distanceSquaredTo(mine) : Integer.MAX_VALUE;
        MapLocation leashCenter = (mine != null && mineDist <= MINE_STAY_RADIUS_SQ) ? mine : null;

        if(PathFinding.tryMoveToward(target, leashCenter, MINE_STAY_RADIUS_SQ)){
            return;
        }

        Direction fallback = pickFallbackMoveToward(target, leashCenter, MINE_STAY_RADIUS_SQ);
        if(fallback != Direction.CENTER){
            PathFinding.moveDir(fallback);
        }
    }

    private Direction pickFallbackMoveToward(MapLocation target, MapLocation mine, int leashRadiusSq){
        MapLocation myLoc = rc.getLocation();
        Direction reverse = lastDirection != null ? lastDirection.opposite() : null;
        Direction bestDir = Direction.CENTER;
        int bestDist = Integer.MAX_VALUE;
        Direction firstLegal = Direction.CENTER;

        for(Direction dir : directions){
            if(!rc.canMove(dir)){
                continue;
            }
            if(mine != null && myLoc.add(dir).distanceSquaredTo(mine) > leashRadiusSq){
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
            if(mine == null || myLoc.add(reverse).distanceSquaredTo(mine) <= leashRadiusSq){
                return reverse;
            }
        }
        return firstLegal;
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
        int dangerRadius = isKingCheeseEmergency() ? CAT_POUNCE_RADIUS_SQ : CAT_DANGER_RADIUS_SQ;
        return rc.getLocation().distanceSquaredTo(nearestCat) <= dangerRadius;
    }

    private boolean shouldDefendKingFromCat() throws GameActionException {
        if(nearestKing == null || nearestCat == null){
            return false;
        }
        if(rc.getGlobalCheese() < GLOBAL_CHEESE_EMERGENCY
            && nearestKing.distanceSquaredTo(nearestCat) > CAT_POUNCE_RADIUS_SQ){
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
        int catToKing = nearestKing.distanceSquaredTo(nearestCat);
        if(catToKing > CAT_NEAR_KING_RADIUS_SQ){
            return false;
        }
        if(rc.getLocation().distanceSquaredTo(nearestKing) > BABY_DEFEND_RADIUS_SQ){
            return false;
        }
        if(isKingCheeseEmergency() && catToKing > CAT_POUNCE_RADIUS_SQ){
            return false;
        }
        if(catToKing > CAT_POUNCE_RADIUS_SQ && countDefendersNearKing() >= MAX_DEFENDERS){
            return false;
        }
        return true;
    }

    private boolean shouldClearKingRing() throws GameActionException {
        if(nearestKing == null){
            return false;
        }
        if(rc.getRawCheese() > 0){
            return false;
        }
        if(nearestCat != null && nearestKing.distanceSquaredTo(nearestCat) <= CAT_POUNCE_RADIUS_SQ){
            return false;
        }
        return rc.getLocation().distanceSquaredTo(nearestKing) <= KING_CLEAR_RADIUS_SQ;
    }

    private Result clearKingRing() throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        Direction bestDir = Direction.CENTER;
        int bestDist = myLoc.distanceSquaredTo(nearestKing);
        for(Direction dir : directions){
            if(!rc.canMove(dir)){
                continue;
            }
            int dist = myLoc.add(dir).distanceSquaredTo(nearestKing);
            if(dist > bestDist){
                bestDist = dist;
                bestDir = dir;
            }
        }
        if(bestDir != Direction.CENTER){
            PathFinding.moveDir(bestDir);
        }
        return new Result(END_OF_TURN, "Clearing king ring");
    }

    private Result defendKingFromCat() throws GameActionException {
        tryPlaceInterceptTrap();
        moveToward(nearestCat, null);
        return new Result(END_OF_TURN, "Defending king from cat");
    }

    private boolean shouldDefendKingFromEnemyRat() throws GameActionException {
        MapLocation threat = getEnemyThreatNearKing();
        if(nearestKing == null || threat == null){
            return false;
        }
        if(rc.getLocation().distanceSquaredTo(nearestKing) > ENEMY_DEFEND_RADIUS_SQ){
            return false;
        }
        if(nearestKing.distanceSquaredTo(threat) > ENEMY_NEAR_KING_RADIUS_SQ){
            return false;
        }
        if(countEnemyDefendersNearKing() >= MAX_ENEMY_DEFENDERS
            && rc.getLocation().distanceSquaredTo(threat) > 2){
            return false;
        }
        return true;
    }

    private Result defendKingFromEnemyRat() throws GameActionException {
        MapLocation threat = getEnemyThreatNearKing();
        if(threat == null){
            return new Result(OK, "No enemy threat");
        }
        if(rc.isActionReady() && rc.canAttack(threat)){
            rc.attack(threat, 0);
            return new Result(END_OF_TURN, "Attacked enemy threat");
        }
        Result trap = tryPlaceRatTrapForDefense();
        if(trap != null){
            return trap;
        }
        moveToward(threat, null);
        return new Result(END_OF_TURN, "Defending king from enemy");
    }

    private void tryPlaceInterceptTrap() throws GameActionException {
        if(!rc.isActionReady() || nearestCat == null || nearestKing == null){
            return;
        }
        boolean emergency = nearestKing.distanceSquaredTo(nearestCat) <= CAT_POUNCE_RADIUS_SQ;
        if(!emergency && rc.getGlobalCheese() < MIN_GLOBAL_CHEESE_FOR_BABY_TRAP){
            return;
        }
        if(!emergency && rc.getGlobalCheese() <= FEED_EMERGENCY_GLOBAL){
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

    private Result tryPlaceRatTrapForDefense() throws GameActionException {
        if(!rc.isActionReady()){
            return null;
        }
        if(round - lastRatTrapRound < RAT_TRAP_COOLDOWN_ROUNDS){
            return null;
        }
        if(rc.getRawCheese() > 0){
            return null;
        }
        if(rc.getGlobalCheese() < RAT_TRAP_GLOBAL_MIN || isKingCheeseEmergency()){
            return null;
        }
        MapLocation focus = getEnemyThreatNearKing();
        if(focus == null && nearestKing != null
            && rc.getLocation().distanceSquaredTo(nearestKing) <= KING_CLEAR_RADIUS_SQ){
            focus = nearestKing;
        }
        if(focus == null && mineLoc != null){
            focus = mineLoc;
        }
        if(focus == null){
            return null;
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
            int score = -loc.distanceSquaredTo(focus);
            if(nearestKing != null){
                score += loc.distanceSquaredTo(nearestKing);
            }
            if(best == null || score > bestScore){
                bestScore = score;
                best = loc;
            }
        }

        if(best == null){
            return null;
        }
        if(rc.canPlaceRatTrap(best)){
            rc.placeRatTrap(best);
            lastRatTrapRound = round;
            return new Result(END_OF_TURN, "Placed rat trap");
        }
        return null;
    }

    private MapLocation getEnemyThreatNearKing(){
        if(nearestKing == null){
            return null;
        }
        MapLocation threat = null;
        int bestDist = Integer.MAX_VALUE;
        if(nearestEnemyKing != null){
            int dist = nearestKing.distanceSquaredTo(nearestEnemyKing);
            if(dist <= ENEMY_NEAR_KING_RADIUS_SQ && dist < bestDist){
                bestDist = dist;
                threat = nearestEnemyKing;
            }
        }
        if(nearestEnemyRat != null){
            int dist = nearestKing.distanceSquaredTo(nearestEnemyRat);
            if(dist <= ENEMY_NEAR_KING_RADIUS_SQ && dist < bestDist){
                bestDist = dist;
                threat = nearestEnemyRat;
            }
        }
        return threat;
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

    private boolean isKingCheeseEmergency() throws GameActionException {
        if(rc.getGlobalCheese() <= FEED_EMERGENCY_GLOBAL){
            return true;
        }
        if(nearestKing == null){
            return false;
        }
        if(!rc.canSenseLocation(nearestKing) || !rc.canSenseRobotAtLocation(nearestKing)){
            return false;
        }
        RobotInfo info = rc.senseRobotAtLocation(nearestKing);
        if(info == null || info.getTeam() != rc.getTeam() || !info.getType().isRatKingType()){
            return false;
        }
        return info.getHealth() <= FEED_EMERGENCY_HP;
    }

    private int countDefendersNearKing() throws GameActionException {
        if(nearestKing == null){
            return 0;
        }
        int count = 0;
        for(RobotInfo info : rc.senseNearbyRobots(BABY_DEFEND_RADIUS_SQ, rc.getTeam())){
            if(info.type != UnitType.BABY_RAT){
                continue;
            }
            if(info.location.distanceSquaredTo(nearestKing) <= DEFENDER_COUNT_RADIUS_SQ){
                count++;
            }
        }
        return count;
    }

    private int countEnemyDefendersNearKing() throws GameActionException {
        if(nearestKing == null){
            return 0;
        }
        int count = 0;
        for(RobotInfo info : rc.senseNearbyRobots(BABY_DEFEND_RADIUS_SQ, rc.getTeam())){
            if(info.type != UnitType.BABY_RAT){
                continue;
            }
            if(info.location.distanceSquaredTo(nearestKing) <= DEFENDER_COUNT_RADIUS_SQ){
                count++;
            }
        }
        return count;
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

    private Result tryDigBlockingDirt(MapLocation target) throws GameActionException {
        if(target == null || !rc.isActionReady()){
            return null;
        }
        if(rc.getRawCheese() > 0){
            return null;
        }
        if(rc.getGlobalCheese() <= FEED_EMERGENCY_GLOBAL){
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
        return new Result(END_OF_TURN, "Clearing dirt");
    }

    private Result tryAttackEnemy() throws GameActionException {
        if(!rc.isActionReady()){
            return null;
        }
        MapLocation target = null;
        if(nearestEnemyKing != null && rc.canAttack(nearestEnemyKing)){
            target = nearestEnemyKing;
        }else if(nearestEnemyRat != null && rc.canAttack(nearestEnemyRat)){
            target = nearestEnemyRat;
        }
        if(target == null){
            return null;
        }
        int cheeseToSpend = Math.min(rc.getRawCheese(), 5);
        rc.attack(target, cheeseToSpend);
        return new Result(END_OF_TURN, "Attacked enemy");
    }

    private Result tryPickupCheese() throws GameActionException {
        if(cheeseLoc == null){
            return null;
        }
        MapLocation myLoc = rc.getLocation();
        if(myLoc.distanceSquaredTo(cheeseLoc) > 2){
            return null;
        }
        maybeTurnToward(cheeseLoc);
        if(rc.canPickUpCheese(cheeseLoc)){
            rc.pickUpCheese(cheeseLoc);
            cheeseLoc = null;
            return new Result(END_OF_TURN, "Cheese picked up, miam");
        }
        if(!rc.isActionReady()){
            return new Result(END_OF_TURN, "Waiting to pickup");
        }
        return null;
    }

    private Result wanderForCheese() throws GameActionException {
        if(wanderTarget == null || round >= wanderTargetUntilRound
            || rc.getLocation().distanceSquaredTo(wanderTarget) <= WANDER_REACH_RADIUS_SQ){
            wanderTarget = pickWanderTarget();
            wanderTargetUntilRound = round + WANDER_TARGET_TTL;
        }
        if(wanderTarget != null){
            PathFinding.tryMoveToward(wanderTarget);
        }
        return new Result(END_OF_TURN, "Exploring for cheese");
    }

    private MapLocation pickWanderTarget() throws GameActionException {
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        if(width <= 0 || height <= 0){
            return null;
        }
        boolean biasCenter = cheeseMines.size == 0
            || round < CENTER_BIAS_ROUNDS
            || rng.nextInt(100) < CENTER_BIAS_CHANCE;
        int x;
        int y;
        if(biasCenter){
            int centerX = width / 2;
            int centerY = height / 2;
            int dx = rng.nextInt(CENTER_JITTER * 2 + 1) - CENTER_JITTER;
            int dy = rng.nextInt(CENTER_JITTER * 2 + 1) - CENTER_JITTER;
            x = clamp(centerX + dx, 0, width - 1);
            y = clamp(centerY + dy, 0, height - 1);
        }else{
            x = rng.nextInt(width);
            y = rng.nextInt(height);
        }
        return new MapLocation(x, y);
    }

    private int clamp(int value, int min, int max){
        if(value < min){
            return min;
        }
        if(value > max){
            return max;
        }
        return value;
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
}
