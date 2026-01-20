package v000_starter;

import battlecode.common.*;

import java.util.Random;

public class King {
    static boolean init = false;
    static Random rng = new Random(0);
    static int spawnsMade = 0;
    static final int SPAWN_CAP = 14;
    static final int SPAWN_RESERVE = 1000;
    static final int NEED_CHEESE_AT = 800;

    static void setup(RobotController rc) {
        if (init) return;
        rng.setSeed((long) rc.getID() * 0x9E3779B97F4A7C15L);
        init = true;
    }

    public static void run(RobotController rc) throws GameActionException {
        setup(rc);

        int round = rc.getRoundNum();
        MapLocation me = rc.getLocation();

        Comms.kingWriteHome(rc, me);

        MapLocation report = Comms.kingReadEnemyReport(rc);
        if (report != null) Comms.kingWriteEnemyKing(rc, report);

        int g = rc.getGlobalCheese();
        boolean need = g < NEED_CHEESE_AT;
        Comms.kingWriteNeed(rc, need);

        //detect enemy rush near home
        Threat th = senseThreat(rc);
        if (th.n > 0) Comms.kingWriteThreat(rc, th.loc, th.n, round);
        else Comms.kingClearThreat(rc);

        boolean commit = !rc.isCooperation();
        if (!commit) {
            MapLocation ek = Comms.readEnemyKing(rc);
            if (ek != null && g > 1400 && spawnsMade >= 10 && round > 200) commit = true;
            if (th.n >= 3 && g > 900 && round > 60) commit = true;
            if (g > 1800 && round > 650) commit = true;
        }
        Comms.kingWriteMode(rc, commit ? Comms.MODE_COMMIT : Comms.MODE_ECON);

        RobotInfo cat = CatDefense.nearestCat(rc);
        if (cat != null) {
            Comms.kingWriteCat(rc, cat.location, round);
            CatDefense.kingReact(rc, cat);
        }

        //if we're committing and the rush is in range, start the fight first
        if (commit && rc.isCooperation() && th.enemyInAttackRange && rc.isActionReady()) {
            rc.attack(th.loc);
        }

        //king fights during backstab too
        if (!rc.isCooperation() && th.enemyInAttackRange && rc.isActionReady()) {
            rc.attack(th.loc);
        }

        //defensive rat trap when being rushed
        if (commit && th.n > 0 && rc.isActionReady() && g > 200) {
            Combat.tryPlaceRatTrapToward(rc, th.loc);
        }

        //spawn control
        if (rc.isActionReady() && !need && spawnsMade < SPAWN_CAP && g > SPAWN_RESERVE) {
            if (trySpawnOnRing(rc)) spawnsMade++;
        }

        rc.setIndicatorString("g=" + g + " sp=" + spawnsMade + " c=" + (commit ? 1 : 0) + " th=" + th.n);
    }

    static class Threat {
        MapLocation loc;
        int n;
        boolean enemyInAttackRange;
        Threat(MapLocation loc, int n, boolean inRange) {
            this.loc = loc;
            this.n = n;
            this.enemyInAttackRange = inRange;
        }
    }

    static Threat senseThreat(RobotController rc) throws GameActionException {
        RobotInfo[] bots = rc.senseNearbyRobots();
        MapLocation me = rc.getLocation();

        RobotInfo best = null;
        int bestD2 = Integer.MAX_VALUE;
        int n = 0;

        for (RobotInfo ri : bots) {
            if (ri.team == rc.getTeam()) continue;
            if (ri.type == UnitType.CAT) continue;
            if (ri.type != UnitType.BABY_RAT && ri.type != UnitType.RAT_KING) continue;

            n++;
            int d2 = Nav.dist2(me, ri.location);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = ri;
            }
        }

        if (best == null) return new Threat(me, 0, false);

        boolean inRange = rc.canAttack(best.location);
        return new Threat(best.location, n, inRange);
    }

    static boolean trySpawnOnRing(RobotController rc) throws GameActionException {
        MapLocation c = rc.getLocation();
        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;

        int sdx = Integer.compare(rc.getMapWidth() / 2, c.x);
        int sdy = Integer.compare(rc.getMapHeight() / 2, c.y);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) continue;
                MapLocation m = new MapLocation(c.x + dx, c.y + dy);
                if (!Util.inMap(rc, m)) continue;
                if (!rc.canBuildRat(m)) continue;

                int score = dx * sdx + dy * sdy;
                if (score > bestScore) {
                    bestScore = score;
                    best = m;
                }
            }
        }

        if (best != null) {
            rc.buildRat(best);
            return true;
        }
        return false;
    }
}