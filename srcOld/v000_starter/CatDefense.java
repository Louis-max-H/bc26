package v000_starter;

import battlecode.common.*;

import java.util.Random;

public class CatDefense {
    static final int BABY_FEAR_D2 = 36;  //6^2
    static final int KING_THREAT_D2 = 900;//30^2
    static final int CAT_TTL = 6;

    static RobotInfo nearestCat(RobotController rc) {
        RobotInfo[] bots = rc.senseNearbyRobots();
        RobotInfo best = null;
        int bestD2 = Integer.MAX_VALUE;
        MapLocation me = rc.getLocation();
        for (RobotInfo ri : bots) {
            if (ri.type != UnitType.CAT) continue;
            int d2 = Nav.dist2(me, ri.location);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = ri;
            }
        }
        return best;
    }

    static boolean babyDodge(RobotController rc, RobotInfo cat) throws GameActionException {
        if (cat == null) return false;
        if (Nav.dist2(rc.getLocation(), cat.location) > BABY_FEAR_D2) return false;
        Nav.goTo(rc, Util.away(rc, rc.getLocation(), cat.location, 3));
        return true;
    }

    static void kingReact(RobotController rc, RobotInfo cat) throws GameActionException {
        if (cat == null) return;
        int d2 = Nav.dist2(rc.getLocation(), cat.location);
        if (d2 > KING_THREAT_D2) return;

        //try trap on ring_use tiles
        if (rc.isCooperation() && rc.isActionReady() && rc.getGlobalCheese() > 200) {
            MapLocation best = bestRingTileToward(rc, cat.location);
            if (best != null && rc.canPlaceCatTrap(best)) {
                rc.placeCatTrap(best);
                return;
            }
        }

        //try dirt screen if we have any
        if (rc.isActionReady() && rc.getDirt() > 0) {
            Direction toCat = Nav.dirTo(rc.getLocation(), cat.location);
            MapLocation screen = rc.getLocation().add(toCat);
            if (Util.inMap(rc, screen) && rc.canPlaceDirt(screen)) {
                rc.placeDirt(screen);
                return;
            }
        }

        //kite
        Nav.goTo(rc, Util.away(rc, rc.getLocation(), cat.location, 6));
    }

    static MapLocation bestRingTileToward(RobotController rc, MapLocation target) {
        MapLocation c = rc.getLocation();
        MapLocation best = null;
        int bestD2 = Integer.MAX_VALUE;

        int dxs = Integer.compare(target.x, c.x);
        int dys = Integer.compare(target.y, c.y);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) continue;
                if (dxs != 0 && Integer.compare(dx, 0) != dxs) continue;
                if (dys != 0 && Integer.compare(dy, 0) != dys) continue;

                MapLocation m = new MapLocation(c.x + dx, c.y + dy);
                if (!Util.inMap(rc, m)) continue;

                int d2 = Nav.dist2(m, target);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    best = m;
                }
            }
        }
        return best;
    }

    static boolean guardPlaceTrapOnce(RobotController rc, MapLocation home, MapLocation catLoc) throws GameActionException {
        if (!rc.isCooperation()) return false;
        if (!rc.isActionReady()) return false;
        if (rc.getGlobalCheese() < 800) return false;

        MapLocation me = rc.getLocation();
        MapLocation best = null;
        int bestScore = Integer.MAX_VALUE;

        for (Direction d : Nav.DIRS) {
            MapLocation m = me.add(d);
            if (!Util.inMap(rc, m)) continue;
            if (!rc.canPlaceCatTrap(m)) continue;

            int score = (catLoc != null) ? Nav.dist2(m, catLoc) : Nav.dist2(m, home);
            if (score < bestScore) {
                bestScore = score;
                best = m;
            }
        }

        if (best != null) {
            rc.placeCatTrap(best);
            return true;
        }
        return false;
    }

    static void guardBait(RobotController rc, RobotInfo cat, MapLocation home, int round, Random rng) throws GameActionException {
        if (cat == null || home == null) return;

        int catHomeD2 = Nav.dist2(cat.location, home);
        if (catHomeD2 > KING_THREAT_D2) return;

        //noisy squeak (non-protocol)
        if (Nav.dist2(rc.getLocation(), cat.location) <= 16 && (round % 6 == 0)) {
            rc.squeak(0);
        }

        //pull cat away past its current position
        MapLocation pull = Util.extendLine(rc, home, cat.location, 12);
        if (Util.inMap(rc, pull)) {
            Nav.goTo(rc, pull);
            return;
        }

        //fallback
        Nav.goTo(rc, Util.away(rc, rc.getLocation(), cat.location, 3));
    }
}