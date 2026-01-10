package starter;

import battlecode.common.*;

import java.util.Random;

public class Baby {
    static boolean init = false;
    static Random rng = new Random(0);

    static int baseRole = -1;
    static int sector = 0;

    static boolean placedTrap = false;

    static MapLocation enemyKingSeen = null;
    static boolean enemyKingReported = false;

    static MapLocation exploreTarget = null;
    static int exploreRound = -9999;

    static final int KEEP_RAW_FIGHT = 10;
    static final int RAW_RETURN_COOP = 10;
    static final int RAW_RETURN_BACKSTAB = 20;

    static final int REPORT_HOME_D2 = 16;
    static final int REPORT_COOLDOWN = 20;
    static int lastReportRound = -9999;

    static void setup(RobotController rc) {
        if (init) return;
        rng.setSeed((long) rc.getID() * 0x9E3779B97F4A7C15L);
        baseRole = Roles.initialRole(rc);
        sector = Roles.sector(rc);
        init = true;
    }

    public static void run(RobotController rc) throws GameActionException {
        setup(rc);

        int round = rc.getRoundNum();
        MapLocation home = Comms.readHome(rc);

        boolean coop = rc.isCooperation();
        boolean commit = Roles.commit(rc);

        MapLocation threat = Comms.readThreat(rc, round, 6);
        int threatN = (threat == null) ? 0 : Comms.readThreatCount(rc);

        if (rc.isBeingThrown()) return;

        Mining.observe(rc);

        RobotInfo cat = CatDefense.nearestCat(rc);
        if (!commit && baseRole != Roles.GUARD) {
            if (CatDefense.babyDodge(rc, cat)) return;
        }

        //keep king alive first
        if (Comms.readNeed(rc) && rc.getRawCheese() > 0 && home != null) {
            deliverKeep(rc, home, 0);
            return;
        }

        //deposit less during backstab so we can spend raw on damage
        int raw = rc.getRawCheese();
        int thresh = coop ? RAW_RETURN_COOP : RAW_RETURN_BACKSTAB;
        if (raw >= thresh && home != null) {
            deliverKeep(rc, home, commit ? KEEP_RAW_FIGHT : 0);
            return;
        }

        if (rc.isActionReady()) Combat.tryPickUpCheeseNearMe(rc);

        //coop: don't start backstab by accident
        if (coop && !commit) {
            Combat.tryAttackCat(rc);
        }

        //defend home when rushed (even in coop, we just don't bite unless commit)
        if (threat != null && home != null) {
            int meHomeD2 = Nav.dist2(rc.getLocation(), home);
            boolean mustDefend = (baseRole == Roles.GUARD)
                    || meHomeD2 <= 900
                    || threatN >= 4;

            if (mustDefend) {
                if (commit) {
                    Combat.tryPlaceRatTrapToward(rc, threat);
                    Combat.tryAttackEnemyPriority(rc);
                }
                Nav.goTo(rc, threat);
                return;
            }
        }

        //after commit/backstab, more bodies should actually fight
        int effRole = baseRole;
        if (commit && baseRole == Roles.MINER && (rc.getID() % 3 == 0)) effRole = Roles.ATTACKER;

        if (commit) {
            Combat.tryAttackEnemyPriority(rc);
        }

        if (effRole == Roles.GUARD) {
            runGuard(rc, home, cat, round, commit);
            return;
        }

        if (effRole == Roles.SCOUT) {
            runScout(rc, home, round, commit);
            return;
        }

        if (effRole == Roles.ATTACKER) {
            runAttacker(rc, home, commit);
            return;
        }

        //miner
        Mining.runMiner(rc, sector, rng);
    }

    static void runGuard(RobotController rc, MapLocation home, RobotInfo cat, int round, boolean commit) throws GameActionException {
        if (home == null) {
            goExplore(rc);
            return;
        }

        if (Nav.dist2(rc.getLocation(), home) > 100) {
            Nav.goTo(rc, home);
            return;
        }

        //nly coop
        if (!commit && !placedTrap && CatDefense.guardPlaceTrapOnce(rc, home, cat == null ? null : cat.location)) {
            placedTrap = true;
            return;
        }

        //backstab: hit enemy, ignore cat unless it's on top of us
        if (commit) {
            Combat.tryAttackEnemyPriority(rc);
            MapLocation threat = Comms.readThreat(rc, rc.getRoundNum(), 6);
            if (threat != null) {
                Combat.tryPlaceRatTrapToward(rc, threat);
                Nav.goTo(rc, threat);
                return;
            }
        }

        if (cat != null) {
            if (rc.isActionReady() && rc.canAttack(cat.location)) rc.attack(cat.location);
            CatDefense.guardBait(rc, cat, home, round, rng);
            return;
        }

        if (rc.isActionReady()) Combat.tryAttackCat(rc);
        if (rng.nextInt(4) == 0) Nav.goTo(rc, home);
    }

    static void runScout(RobotController rc, MapLocation home, int round, boolean commit) throws GameActionException {
        RobotInfo[] bots = rc.senseNearbyRobots();
        for (RobotInfo ri : bots) {
            if (ri.team == rc.getTeam()) continue;
            if (ri.type != UnitType.RAT_KING) continue;
            enemyKingSeen = ri.location;
            enemyKingReported = false;
        }

        if (commit) {
            //if we already see a king, just fight it
            MapLocation tgt = enemyKingSeen;
            if (tgt == null && home != null) tgt = guessEnemyKing(rc, home);
            if (tgt != null) Nav.goTo(rc, tgt);
            Combat.tryAttackEnemyPriority(rc);
            return;
        }

        //coop: report by returning home
        if (enemyKingSeen != null && !enemyKingReported && home != null) {
            Nav.goTo(rc, home);

            if (Nav.dist2(rc.getLocation(), home) <= REPORT_HOME_D2
                    && round - lastReportRound >= REPORT_COOLDOWN) {

                RobotInfo cat = CatDefense.nearestCat(rc);
                if (cat == null || Nav.dist2(rc.getLocation(), cat.location) > 25) {
                    rc.squeak(Comms.packEnemyKing(enemyKingSeen));
                    enemyKingReported = true;
                    lastReportRound = round;
                }
            }
            return;
        }

        goExplore(rc);
    }

    static void runAttacker(RobotController rc, MapLocation home, boolean commit) throws GameActionException {
        if (!commit) {
            goExplore(rc);
            return;
        }

        MapLocation ek = Comms.readEnemyKing(rc);
        if (ek == null && home != null) ek = guessEnemyKing(rc, home);
        if (ek == null) {
            goExplore(rc);
            return;
        }

        Combat.tryCarryThrow(rc, ek);
        Combat.tryPlaceRatTrapToward(rc, ek);
        Combat.tryAttackEnemyPriority(rc);

        Nav.goTo(rc, ek);
    }

    static MapLocation guessEnemyKing(RobotController rc, MapLocation home) {
        return new MapLocation(rc.getMapWidth() - 1 - home.x, rc.getMapHeight() - 1 - home.y);
    }

    static void deliverKeep(RobotController rc, MapLocation home, int keep) throws GameActionException {
        int amt = rc.getRawCheese();
        int give = Math.max(0, amt - keep);
        if (rc.isActionReady() && give > 0 && rc.canTransferCheese(home, give)) {
            rc.transferCheese(home, give);
        }
        Nav.goTo(rc, home);
    }

    static void goExplore(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();
        MapLocation me = rc.getLocation();

        if (exploreTarget == null || round - exploreRound > 140 || me.equals(exploreTarget)) {
            exploreTarget = Util.randomInSector(rc, rng, sector);
            exploreRound = round;
        }
        Nav.goTo(rc, exploreTarget);
    }
}