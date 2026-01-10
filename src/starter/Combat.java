package starter;

import battlecode.common.*;

public class Combat {
    static void tryPickUpCheeseNearMe(RobotController rc) throws GameActionException {
        MapLocation here = rc.getLocation();
        if (rc.canPickUpCheese(here)) {
            rc.pickUpCheese(here);
            return;
        }
        for (Direction d : Nav.DIRS) {
            MapLocation adj = here.add(d);
            if (rc.canPickUpCheese(adj)) {
                rc.pickUpCheese(adj);
                return;
            }
        }
    }

    static void tryAttackCat(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;

        RobotInfo[] bots = rc.senseNearbyRobots();
        RobotInfo best = null;
        int bestD2 = Integer.MAX_VALUE;
        MapLocation me = rc.getLocation();

        for (RobotInfo ri : bots) {
            if (ri.type != UnitType.CAT) continue;
            if (!rc.canAttack(ri.location)) continue;
            int d2 = Nav.dist2(me, ri.location);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = ri;
            }
        }

        if (best != null) rc.attack(best.location);
    }

    // backstab/commit combat
    static void tryAttackEnemyPriority(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;

        RobotInfo[] bots = rc.senseNearbyRobots();
        RobotInfo best = null;
        int bestScore = Integer.MIN_VALUE;

        for (RobotInfo ri : bots) {
            if (ri.team == rc.getTeam()) continue;
            if (ri.type == UnitType.CAT) continue;
            if (!rc.canAttack(ri.location)) continue;

            int typeBonus = (ri.type == UnitType.RAT_KING) ? 10000 : 3000;
            int score = typeBonus - ri.health;
            if (score > bestScore) {
                bestScore = score;
                best = ri;
            }
        }

        if (best == null) return;

        int raw = rc.getRawCheese();
        int spend = (best.type == UnitType.RAT_KING) ? Math.min(raw, 20) : Math.min(raw, 10);

        if (spend > 0 && rc.canAttack(best.location, spend)) rc.attack(best.location, spend);
        else rc.attack(best.location);
    }

    static void tryPlaceRatTrapToward(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isActionReady()) return;
        if (target == null) return;

        MapLocation me = rc.getLocation();
        MapLocation best = null;
        int bestD2 = Integer.MAX_VALUE;

        for (Direction d : Nav.DIRS) {
            MapLocation m = me.add(d);
            if (!Util.inMap(rc, m)) continue;
            if (!rc.canPlaceRatTrap(m)) continue;

            int d2 = Nav.dist2(m, target);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = m;
            }
        }

        if (best != null) rc.placeRatTrap(best);
    }

    static void tryCarryThrow(RobotController rc, MapLocation enemyKing) throws GameActionException {
        if (enemyKing == null) return;

        RobotInfo carried = rc.getCarrying();
        if (carried != null) {
            if (rc.isTurningReady()) {
                Direction dir = Nav.dirTo(rc.getLocation(), enemyKing);
                if (dir != Direction.CENTER) rc.turn(dir);
            }
            if (rc.isActionReady() && rc.canThrowRat()) rc.throwRat();
            return;
        }

        if (!rc.isActionReady()) return;

        RobotInfo[] bots = rc.senseNearbyRobots();
        MapLocation me = rc.getLocation();
        for (RobotInfo ri : bots) {
            if (ri.team == rc.getTeam()) continue;
            if (ri.type != UnitType.BABY_RAT) continue;
            if (Math.abs(ri.location.x - me.x) <= 1 && Math.abs(ri.location.y - me.y) <= 1) {
                if (rc.canCarryRat(ri.location)) rc.carryRat(ri.location);
                return;
            }
        }
    }
}