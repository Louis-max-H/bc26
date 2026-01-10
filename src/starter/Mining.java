package starter;
import battlecode.common.*;
import java.util.Random;

public class Mining {
    static final int MAX_MINES = 8;
    static MapLocation[] mines = new MapLocation[MAX_MINES];
    static int mineCount = 0;
    static MapLocation mineTarget = null;
    static MapLocation minePatrol = null;
    static int minePatrolRound = -9999;
    static MapLocation cheeseTarget = null;
    static int cheeseRound = -9999;
    static final int CHEESE_TTL = 12;
    static final int PATROL_RETARGET = 25;

    static void observe(RobotController rc) throws GameActionException {
        MapInfo[] infos = rc.senseNearbyMapInfos();
        MapLocation me = rc.getLocation();

        MapLocation bestCheese = null;
        int bestCheeseD2 = Integer.MAX_VALUE;

        for (MapInfo mi : infos) {
            MapLocation loc = mi.getMapLocation();

            if (mi.hasCheeseMine()) addMine(loc);

            int amt = mi.getCheeseAmount();
            if (amt > 0) {
                int d2 = Nav.dist2(me, loc);
                if (d2 < bestCheeseD2) {
                    bestCheeseD2 = d2;
                    bestCheese = loc;
                }
            }
        }

        if (bestCheese != null) {
            cheeseTarget = bestCheese;
            cheeseRound = rc.getRoundNum();
        }
    }

    static void addMine(MapLocation loc) {
        for (int i = 0; i < mineCount; i++) {
            if (mines[i].equals(loc)) return;
        }
        if (mineCount < MAX_MINES) mines[mineCount++] = loc;
    }

    static MapLocation getCheeseTarget(int round) {
        if (cheeseTarget == null) return null;
        if (round - cheeseRound > CHEESE_TTL) return null;
        return cheeseTarget;
    }

    static void ensureMineTarget(RobotController rc, int sector, Random rng) throws GameActionException {
        if (mineTarget != null) return;

        //pick a mine in our sector if possible
        MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        for (int i = 0; i < mineCount; i++) {
            MapLocation m = mines[i];
            if (inSector(m, sector, center)) {
                mineTarget = m;
                return;
            }
        }

        if (mineCount > 0) mineTarget = mines[rng.nextInt(mineCount)];
    }

    static boolean inSector(MapLocation loc, int sector, MapLocation c){
        boolean left = loc.x < c.x;
        boolean up = loc.y < c.y;

        return switch (sector) {
            case 0 -> left && up;     //nw
            case 1 -> !left && up;    //ne
            case 2 -> left && !up;    //sw
            default -> !left && !up;  //se
        };
    }

    static void maybeDropCrowdedMine(RobotController rc) throws GameActionException {
        if (mineTarget == null) return;
        if (Util.cheb(rc.getLocation(), mineTarget) > 4) return;

        //too many allied babies in 9x9 -> leave
        RobotInfo[] bots = rc.senseNearbyRobots();
        int allies = 0;
        for (RobotInfo ri : bots) {
            if (ri.team != rc.getTeam()) continue;
            if (ri.type != UnitType.BABY_RAT) continue;
            if (Nav.dist2(ri.location, mineTarget) <= 16) allies++;
        }

        if (allies >= 8) {
            mineTarget = null;
            minePatrol = null;
        }
    }

    static void runMiner(RobotController rc, int sector, Random rng) throws GameActionException {
        int round = rc.getRoundNum();
        MapLocation cheese = getCheeseTarget(round);

        
        if (cheese != null){
            Nav.goTo(rc, cheese);
            return;
        }

        ensureMineTarget(rc, sector, rng);
        maybeDropCrowdedMine(rc);

        if (mineTarget != null) {
            if (Util.cheb(rc.getLocation(), mineTarget) > 4) {
                Nav.goTo(rc, mineTarget);
                return;
            }

            if (minePatrol == null || round - minePatrolRound > PATROL_RETARGET || rc.getLocation().equals(minePatrol)) {
                minePatrol = randomAround(rc, rng, mineTarget, 4);
                minePatrolRound = round;
            }
            Nav.goTo(rc, minePatrol);
            return;
        }

        Nav.goTo(rc, Util.randomInSector(rc, rng, sector));
    }

    static MapLocation randomAround(RobotController rc, Random rng, MapLocation c, int chebRadius) {
        int dx = rng.nextInt(2 * chebRadius + 1) - chebRadius;
        int dy = rng.nextInt(2 * chebRadius + 1) - chebRadius;
        int x = Util.clamp(c.x + dx, 0, rc.getMapWidth() - 1);
        int y = Util.clamp(c.y + dy, 0, rc.getMapHeight() - 1);
        return new MapLocation(x, y);
    }
}