package starterbot;

import battlecode.common.*;

import java.util.Random;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what
 * we'll call once your robot
 * is created!
 */
public class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random(6147);
    static final Direction[] DIRS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    // Per-robot persistent state (not shared across your robots in Battlecode).
    static MapLocation wanderTarget = null;
    static int lastWanderTargetRound = -999999;
    static int lastKingBroadcastRound = -999999;
    static int lastKingBuildRound = -999999;
    static int lastEnemyKingSqueakRound = -999999;
    static int lastEnemyKingWriteRound = -999999;
    static MapLocation home = null;
    static int lastLoggedHealth = -1;

    // Shared-array layout (rat king writes, all rats can read):
    // 0 = kingX (0..63), 1 = kingY (0..63), 2 = lastUpdateRound mod 1024
    static final int SA_KING_X = 0;
    static final int SA_KING_Y = 1;
    static final int SA_KING_R = 2;

    // Enemy rat king last seen (rat king writes based on squeaks):
    // 3 = enemyKingX, 4 = enemyKingY, 5 = lastUpdateRound mod 1024
    static final int SA_EK_X = 3;
    static final int SA_EK_Y = 4;
    static final int SA_EK_R = 5;

    // Cheese mine memory (rat king writes based on squeaks):
    // 6 = mineCount (0..MAX_MINES)
    // 7..(7+2*MAX_MINES-1) = mine i: [x,y] stored as separate entries
    static final int SA_MINE_COUNT = 6;
    static final int SA_MINE_BASE = 7;
    static final int MAX_MINES = 10;

    // Squeak message types (stored in top byte)
    static final int MSG_ENEMY_KING = 1;
    static final int MSG_MINE = 2;
    static final int MSG_DEFEND = 3;

    static MapLocation lastMineSqueakLoc = null;
    static int lastMineSqueakRound = -999999;
    static int lastDefendSqueakRound = -999999;

    // Temporary rally target from DEFEND squeaks
    static MapLocation rallyTarget = null;
    static int rallyUntilRound = -999999;

    // Tuning knobs (easy iteration)
    static final int EARLY_GAME_ROUND = 450;
    static final int DEFENDER_MOD_EARLY = 3; // ~33% defenders early
    static final int DEFENDER_MOD_LATE = 5; // ~20% defenders later
    static final int MINER_MAX_DIST2_EARLY = 900; // miners won't go farther than this^0.5 from king early (~30 tiles)
    static final int RALLY_DURATION = 10;
    static final int CAT_THREAT_D2_TO_KING = 100; // if an enemy cat is within this of our king, treat as base threat
    static final int BAIT_MOD = 7; // ~1/7 rats act as sacrificial bait vs cat near king
    static final int BAIT_REMEMBER_ROUNDS = 25;
    static final int COOP_FORTIFY_ROUNDS = 45; // cooperation -> backstab rush window (from your screenshots)
    static final int COOP_TRAP_RADIUS_D2_FROM_KING = 36; // rats within this of king help place traps during
                                                         // coop-fortify

    static int baitUntilRound = -999999;
    static MapLocation baitTarget = null;

    // Navigation state (per-robot, since statics aren't shared between robots in
    // Battlecode)
    static MapLocation navTarget = null;
    static boolean bugging = false;
    static Direction bugDir = null;
    static int bugStartD2 = Integer.MAX_VALUE;
    static int bugStartRound = -999999;
    static boolean bugFollowRight = true; // initialized lazily from ID

    /**
     * run() is the method that is called when a robot is instantiated in the
     * Battlecode world.
     * It is like the main function for your robot. If this method returns, the
     * robot dies!
     *
     * @param rc The RobotController object. You use it to perform actions from this
     *           robot, and to get
     *           information on its current status. Essentially your portal to
     *           interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        rc.setIndicatorString("starterbot online");

        while (true) {
            try {
                turnCount++;

                switch (rc.getType()) {
                    case RAT_KING -> runRatKing(rc);
                    case BABY_RAT -> runBabyRat(rc);
                    case CAT -> runCat(rc);
                }
            } catch (GameActionException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    private static void runRatKing(RobotController rc) throws GameActionException {
        if (home == null)
            home = rc.getLocation();
        if (lastLoggedHealth < 0)
            lastLoggedHealth = rc.getHealth();
        int hp = rc.getHealth();
        if (hp < lastLoggedHealth) {
            System.out.println("[RK] took dmg: " + lastLoggedHealth + " -> " + hp + " @ " + rc.getLocation());
            lastLoggedHealth = hp;
        }
        if (rc.getRoundNum() % 250 == 0) {
            System.out.println("[RK] round=" + rc.getRoundNum() + " globalCheese=" + rc.getGlobalCheese() + " cost="
                    + rc.getCurrentRatCost());
        }

        // IMPORTANT: action is scarce. Never waste it on shared-array updates if we
        // could
        // attack/build/trap this turn.
        boolean usedAction = false;

        boolean coop = rc.isCooperation();
        boolean coopFortify = coop && rc.getRoundNum() <= COOP_FORTIFY_ROUNDS;

        // If enemies are near, broadcast a DEFEND rally so miners come back.
        // In cooperation mode, opponents can walk right up to you before backstab
        // flips,
        // so treat non-allies as threats even if we can't attack yet.
        RobotInfo nearbyEnemy = nearestNonAlly(rc, 64);
        if (nearbyEnemy != null && home != null) {
            trySqueakDefend(rc, home);
        }
        boolean panic = (nearbyEnemy != null) && !coop; // only "panic" when combat is enabled

        // Critical: keep king location fresh, especially during panic, so
        // defenders/miners can converge.
        // (Shared-array writes are allowed for rat kings; no action readiness
        // requirement in the API.)
        int roundNum = rc.getRoundNum();
        int broadcastEvery = panic ? 1 : 5;
        if (roundNum - lastKingBroadcastRound >= broadcastEvery) {
            tryWriteKingLocation(rc);
            lastKingBroadcastRound = roundNum;
        }

        // Basic combat: attack nearby enemies if possible.
        if (!coopFortify && !usedAction && rc.isActionReady()) {
            RobotInfo enemy = nearestEnemy(rc, 20);
            if (enemy != null && rc.canAttack(enemy.location)) {
                rc.attack(enemy.location);
                rc.setIndicatorString("RK: attack " + enemy.type + " @" + enemy.location);
                usedAction = true;
            }
        }

        // Panic mode: if we're under pressure, prioritize traps over everything else.
        // (Economy actions can wait; losing the king ends the game.)
        if (panic && !usedAction && rc.isActionReady()) {
            if (tryPlaceDefenseTrap(rc)) {
                rc.setIndicatorString("RK: panic trap");
                usedAction = true;
            }
        }

        // Cache map infos once per turn (costly). Reuse for pickup/targets.
        MapInfo[] infos = rc.senseNearbyMapInfos();

        // If there is cheese we can pick up, do it (helps stabilize the economy).
        // Allow during coop - we need cheese to spawn rats!
        if (!panic && !usedAction && rc.isActionReady()) {
            MapLocation cheeseLoc = bestCheesePickupFromInfos(rc, infos);
            if (cheeseLoc != null) {
                rc.pickUpCheese(cheeseLoc);
                rc.setIndicatorString("RK: pickup@" + cheeseLoc);
                usedAction = true;
            }
        }

        // BUILD BABY RATS - THIS IS CRITICAL!
        // Your screenshots show cerr or despair has 18 rats by round 20 while we have 0.
        // During coop/early game: spawn AGGRESSIVELY to build army before backstab.
        // After early game: be more conservative to avoid starvation.
        if (!panic && !usedAction && rc.isActionReady()) {
            int cost = rc.getCurrentRatCost();
            int cheese = rc.getGlobalCheese();
            int round = rc.getRoundNum();

            // Aggressive early spawning to match opponent's production
            boolean earlyGame = round <= 100;
            boolean midGame = round <= EARLY_GAME_ROUND;

            int reserveNeeded;
            int spawnCooldown;
            int costCap;

            if (earlyGame) {
                // HYPER-AGGRESSIVE: spawn as fast as possible in first 100 rounds
                reserveNeeded = 100;  // minimal reserve
                spawnCooldown = 2;    // spawn every 2 rounds
                costCap = 50;         // allow many rats
            } else if (midGame) {
                // Still aggressive but slightly more careful
                reserveNeeded = 400;
                spawnCooldown = 4;
                costCap = 40;
            } else {
                // Late game: conservative
                reserveNeeded = 1400;
                spawnCooldown = 12;
                costCap = 40;
            }

            boolean reserveOk = (cheese - cost) >= reserveNeeded;
            boolean tempoOk = (round - lastKingBuildRound) >= spawnCooldown;
            boolean countOk = cost <= costCap;

            if (reserveOk && tempoOk && countOk) {
                MapLocation spawn = pickRatSpawnLocation(rc);
                if (spawn != null && rc.canBuildRat(spawn)) {
                    rc.buildRat(spawn);
                    lastKingBuildRound = round;
                    rc.setIndicatorString("RK: build@" + spawn + " cost=" + cost + " cheese=" + cheese);
                    usedAction = true;
                }
            }
        }

        // Defensive setup:
        // In coopFortify, always attempt to place traps (enemy can pre-position a
        // surround).
        // Do NOT gate on cheese; rely on canPlace* to enforce resource limits.
        if (!usedAction && rc.isActionReady()
                && (coopFortify || nearbyEnemy != null || rc.getRoundNum() < EARLY_GAME_ROUND)) {
            boolean preferCat = (nearbyEnemy != null && nearbyEnemy.type == UnitType.CAT);
            if (tryPlaceDefenseTrapSmart(rc, infos, home, nearbyEnemy == null ? null : nearbyEnemy.location,
                    preferCat)) {
                rc.setIndicatorString("RK: trap");
                usedAction = true;
            }
        }

        // Lowest priority: update shared intel from squeaks (enemy king + mines).
        if (!usedAction && rc.isActionReady() && rc.getRoundNum() - lastEnemyKingWriteRound >= 5) {
            if (ingestSqueaks(rc)) {
                lastEnemyKingWriteRound = rc.getRoundNum();
                usedAction = true;
            }
        }

        // Movement: rat king is a high-value base. Avoid roaming far from home.
        // Only step to nearby cheese/mine targets within a small radius.
        // In panic mode, do NOT move away from home (stay behind defenders/traps).
        if (panic || coopFortify)
            return;
        MapLocation target = bestCheeseOrMineTargetFromInfos(rc, infos);
        // Tighten roaming radius; don't get dragged into fights.
        if (target != null && home != null && home.distanceSquaredTo(target) <= 9) {
            tryTurnToward(rc, target);
            if (tryMoveToward(rc, target)) {
                rc.setIndicatorString("RK: reposition " + target);
                return;
            }
        }

        // Otherwise, wander to explore the map.
        // Prefer staying near home rather than wandering into danger.
        if (home != null && rc.getLocation().distanceSquaredTo(home) > 9) {
            tryTurnToward(rc, home);
            if (tryMoveToward(rc, home)) {
                rc.setIndicatorString("RK: home");
                return;
            }
        }
    }

    private static void runBabyRat(RobotController rc) throws GameActionException {
        if (rc.isBeingCarried() || rc.isBeingThrown()) {
            rc.setIndicatorString("Rat: carried/thrown");
            return;
        }

        boolean coop = rc.isCooperation();
        boolean coopFortify = coop && rc.getRoundNum() <= COOP_FORTIFY_ROUNDS;

        // If we see an enemy rat king, broadcast it.
        RobotInfo enemyKing = nearestEnemyOfType(rc, UnitType.RAT_KING, rc.getType().visionConeRadiusSquared);
        if (enemyKing != null) {
            trySqueakEnemyKing(rc, enemyKing.location);
        }

        // Cache map infos once per turn (costly). Reuse for mine detection + cheese
        // target selection.
        MapInfo[] infos = rc.senseNearbyMapInfos();

        // If we see a cheese mine, broadcast it (helps the team converge on economy).
        MapLocation mine = visibleCheeseMineFromInfos(infos);
        if (mine != null) {
            trySqueakMine(rc, mine);
        }

        // Listen for DEFEND rally pings and temporarily prioritize returning to base.
        MapLocation rally = readRallyFromSqueaks(rc);
        if (rally != null) {
            rallyTarget = rally;
            rallyUntilRound = rc.getRoundNum() + RALLY_DURATION; // keep a short window
        }

        // Detect nearby enemy cat (key early-game threat).
        RobotInfo nearbyCat = nearestEnemyOfType(rc, UnitType.CAT, 36);

        // If we can bite something adjacent, do it (backstab mode).
        if (!coopFortify && rc.isActionReady()) {
            RobotInfo enemy = nearestEnemy(rc, 8);
            if (enemy != null && rc.canAttack(enemy.location)) {
                // If we have cheese, spend a bit when biting cats (they're tanky/dangerous).
                if (enemy.type == UnitType.CAT && rc.getRawCheese() >= 10 && rc.canAttack(enemy.location, 5)) {
                    rc.attack(enemy.location, 5);
                } else {
                    rc.attack(enemy.location);
                }
                rc.setIndicatorString("Rat: bite " + enemy.type);
                return;
            }
        }

        // If there's cheese we can pick up nearby, do it.
        // ALLOW during coop - we need cheese to spawn rats!
        if (rc.isActionReady()) {
            MapLocation cheeseLoc = bestCheesePickupFromInfos(rc, infos);
            if (cheeseLoc != null) {
                rc.pickUpCheese(cheeseLoc);
                rc.setIndicatorString("Rat: pickup@" + cheeseLoc);
                return;
            }
        }

        int carried = rc.getRawCheese();
        if (carried > 0) {
            MapLocation kingLoc = nearestAlliedRatKing(rc);
            if (kingLoc == null) {
                kingLoc = readKingLocation(rc);
            }

            if (kingLoc != null) {
                if (rc.isActionReady() && rc.canTransferCheese(kingLoc, carried)) {
                    rc.transferCheese(kingLoc, carried);
                    rc.setIndicatorString("Rat: deliver " + carried);
                    return;
                }
                tryTurnToward(rc, kingLoc);
                if (tryMoveToward(rc, kingLoc)) {
                    rc.setIndicatorString("Rat: to king " + kingLoc);
                    return;
                }
            }
        }

        // Role assignment so we don't abandon the king:
        // More defenders early to survive rushes; fewer later for economy.
        int mod = (rc.getRoundNum() <= EARLY_GAME_ROUND) ? DEFENDER_MOD_EARLY : DEFENDER_MOD_LATE;
        boolean isDefender = (rc.getID() % mod) == 0;
        boolean isBait = (rc.getID() % BAIT_MOD) == 1;

        MapLocation kingForRole = nearestAlliedRatKing(rc);
        if (kingForRole == null)
            kingForRole = readKingLocation(rc);

        // Cooperation-fortify: place traps near king to prepare for backstab.
        // But don't block mining/economy - we need cheese to spawn rats!
        if (coopFortify && kingForRole != null
                && rc.getLocation().distanceSquaredTo(kingForRole) <= COOP_TRAP_RADIUS_D2_FROM_KING) {
            if (rc.isActionReady()) {
                boolean preferCat = (nearbyCat != null
                        && nearbyCat.location.distanceSquaredTo(kingForRole) <= CAT_THREAT_D2_TO_KING);
                if (tryPlaceDefenseTrapSmart(rc, infos, kingForRole, nearbyCat == null ? null : nearbyCat.location,
                        preferCat)) {
                    rc.setIndicatorString("Rat: coop trap");
                    // DON'T return here - continue to mine cheese if possible!
                }
            }
        }

        // During coop, stay within a reasonable radius of king but still allow mining.
        // Expand the radius so rats can actually reach nearby cheese.
        if (coopFortify && kingForRole != null) {
            int d2ToKing = rc.getLocation().distanceSquaredTo(kingForRole);
            // Only pull back if very far (beyond 100 = ~10 tiles)
            if (d2ToKing > 100) {
                tryTurnToward(rc, kingForRole);
                if (tryMoveToward(rc, kingForRole)) {
                    rc.setIndicatorString("Rat: coop-return");
                    return;
                }
            }
            // Otherwise, continue to normal mining behavior below
        }

        // Cat pressure logic (inspired by Sprint-1 rush meta): when a cat is near our
        // king, do NOT have
        // everyone flee. Defenders fight/hold near base, while one bait rat tries to
        // kite the cat away.
        if (nearbyCat != null && kingForRole != null) {
            int catToKing = nearbyCat.location.distanceSquaredTo(kingForRole);
            if (catToKing <= CAT_THREAT_D2_TO_KING) {
                // Extend bait mode when we see a cat near base.
                baitUntilRound = Math.max(baitUntilRound, rc.getRoundNum() + BAIT_REMEMBER_ROUNDS);

                // Bait rat: move away from the king to drag cat away.
                if (isBait && rc.isMovementReady()) {
                    MapLocation me = rc.getLocation();
                    Direction out = kingForRole.directionTo(me);
                    if (out == Direction.CENTER)
                        out = me.directionTo(nearbyCat.location).opposite();
                    MapLocation far = new MapLocation(kingForRole.x + out.dx * 12, kingForRole.y + out.dy * 12);
                    baitTarget = clampToMap(rc, far);
                    tryTurnToward(rc, baitTarget);
                    if (tryMoveToward(rc, baitTarget)) {
                        rc.setIndicatorString("Rat: bait cat");
                        return;
                    }
                }

                // Defenders: stay close and move toward the cat to bite it (don't flee).
                if (isDefender) {
                    // Try to place a cat trap on an adjacent tile that the cat might step onto.
                    if (rc.isActionReady()) {
                        MapLocation trapLoc = bestTrapLocationNear(rc, nearbyCat.location, kingForRole);
                        if (trapLoc != null && rc.canPlaceCatTrap(trapLoc)) {
                            rc.placeCatTrap(trapLoc);
                            rc.setIndicatorString("Rat: place cat trap");
                            return;
                        }
                    }
                    tryTurnToward(rc, nearbyCat.location);
                    if (tryMoveToward(rc, nearbyCat.location)) {
                        rc.setIndicatorString("Rat: contest cat");
                        return;
                    }
                } else {
                    // Miners: if not currently baiting, rally back to king rather than fleeing
                    // outward.
                    if (rc.isMovementReady()) {
                        tryTurnToward(rc, kingForRole);
                        if (tryMoveToward(rc, kingForRole)) {
                            rc.setIndicatorString("Rat: return vs cat");
                            return;
                        }
                    }
                }
            }
        }

        // If we recently entered bait mode, keep baiting for a short time even if cat
        // isn't currently visible.
        if (isBait && baitTarget != null && rc.getRoundNum() <= baitUntilRound && rc.isMovementReady()) {
            tryTurnToward(rc, baitTarget);
            if (tryMoveToward(rc, baitTarget)) {
                rc.setIndicatorString("Rat: bait path");
                return;
            }
        }

        // Emergency fallback: if a cat is nearby and not a base-threat scenario, evade
        // it.
        if (nearbyCat != null && rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction away = nearbyCat.location.directionTo(me);
            if (away != Direction.CENTER) {
                Direction[] tries = { away, away.rotateLeft(), away.rotateRight(), away.rotateLeft().rotateLeft(),
                        away.rotateRight().rotateRight() };
                for (Direction d : tries) {
                    if (rc.canMove(d)) {
                        rc.move(d);
                        rc.setIndicatorString("Rat: flee cat");
                        return;
                    }
                }
            }
        }

        if (isDefender && kingForRole != null) {
            int d2 = rc.getLocation().distanceSquaredTo(kingForRole);
            // Tighter leash early so defenders are actually present at rush timing.
            int leash = (rc.getRoundNum() <= EARLY_GAME_ROUND) ? 16 : 25;
            if (d2 > leash) {
                tryTurnToward(rc, kingForRole);
                if (tryMoveToward(rc, kingForRole)) {
                    rc.setIndicatorString("Rat: defend (return)");
                    return;
                }
            }
        }

        // REMOVED: The "coopFortify halt" was causing us to have 0 rats while
        // cerr or despair had 18 by round 20. We NEED rats to mine during coop!

        // Rally overrides mining for a short time window: bring bodies back to protect
        // the king.
        if (rallyTarget != null && rc.getRoundNum() <= rallyUntilRound) {
            // Even miners should respond if they are reasonably close, or if a cat is
            // nearby.
            boolean shouldRally = isDefender
                    || (nearbyCat != null)
                    || rc.getLocation().distanceSquaredTo(rallyTarget) <= 400;
            if (shouldRally) {
                tryTurnToward(rc, rallyTarget);
                if (tryMoveToward(rc, rallyTarget)) {
                    rc.setIndicatorString("Rat: rally " + rallyTarget);
                    return;
                }
            }
        }

        // Otherwise: find cheese (prefer visible cheese piles, then mines).
        MapLocation target = bestCheeseOrMineTargetFromInfos(rc, infos);
        if (target == null) {
            // Only miners chase global mine intel. Defenders stick close to king.
            if (!isDefender) {
                target = readNearestKnownMine(rc);
            }
        }

        // Early game: don't let miners wander too far from the king/base (rush
        // defense).
        if (!isDefender && kingForRole != null && rc.getRoundNum() <= EARLY_GAME_ROUND) {
            if (rc.getLocation().distanceSquaredTo(kingForRole) > MINER_MAX_DIST2_EARLY) {
                target = kingForRole;
            }
        }

        // If no obvious cheese target exists, we can (optionally) move toward last
        // known enemy king late-game.
        // This is mostly to avoid totally aimless wandering when the map is quiet.
        if (target == null && rc.getRoundNum() > 800 && rc.getGlobalCheese() > 1500) {
            target = readEnemyKingLocation(rc);
        }
        if (target != null) {
            tryTurnToward(rc, target);
            if (tryMoveToward(rc, target)) {
                rc.setIndicatorString("Rat: to " + target);
                return;
            }
        }

        // Fallback: explore.
        wander(rc, "Rat");
    }

    private static void runCat(RobotController rc) throws GameActionException {
        // Simple hunter: move toward nearest enemy and attack if possible.
        RobotInfo enemy = nearestEnemy(rc, -1);
        if (enemy != null) {
            if (enemy.type == UnitType.RAT_KING) {
                trySqueakEnemyKing(rc, enemy.location);
            }
            if (rc.isActionReady() && rc.canAttack(enemy.location)) {
                rc.attack(enemy.location);
                rc.setIndicatorString("Cat: attack " + enemy.type);
                return;
            }
            tryTurnToward(rc, enemy.location);
            if (tryMoveToward(rc, enemy.location)) {
                rc.setIndicatorString("Cat: chase " + enemy.type);
                return;
            }
        }

        // If nothing is visible, wander.
        wander(rc, "Cat");
    }

    // -------------------- Sensing helpers --------------------

    private static RobotInfo nearestEnemy(RobotController rc, int radiusSquared) throws GameActionException {
        Team enemyTeam = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radiusSquared, enemyTeam);
        RobotInfo best = null;
        int bestD2 = Integer.MAX_VALUE;
        MapLocation me = rc.getLocation();
        for (RobotInfo e : enemies) {
            int d2 = me.distanceSquaredTo(e.location);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }

    private static RobotInfo nearestNonAlly(RobotController rc, int radiusSquared) throws GameActionException {
        RobotInfo[] bots = rc.senseNearbyRobots(radiusSquared);
        RobotInfo best = null;
        int bestD2 = Integer.MAX_VALUE;
        MapLocation me = rc.getLocation();
        Team my = rc.getTeam();
        for (RobotInfo b : bots) {
            if (b.team == my)
                continue;
            int d2 = me.distanceSquaredTo(b.location);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = b;
            }
        }
        return best;
    }

    private static RobotInfo nearestEnemyOfType(RobotController rc, UnitType type, int radiusSquared)
            throws GameActionException {
        Team enemyTeam = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radiusSquared, enemyTeam);
        RobotInfo best = null;
        int bestD2 = Integer.MAX_VALUE;
        MapLocation me = rc.getLocation();
        for (RobotInfo e : enemies) {
            if (e.type != type)
                continue;
            int d2 = me.distanceSquaredTo(e.location);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }

    private static MapLocation nearestAlliedRatKing(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        MapLocation me = rc.getLocation();
        MapLocation best = null;
        int bestD2 = Integer.MAX_VALUE;
        for (RobotInfo a : allies) {
            if (a.type != UnitType.RAT_KING)
                continue;
            int d2 = me.distanceSquaredTo(a.location);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = a.location;
            }
        }
        return best;
    }

    private static MapLocation bestCheeseOrMineTarget(RobotController rc) throws GameActionException {
        return bestCheeseOrMineTargetFromInfos(rc, rc.senseNearbyMapInfos());
    }

    private static MapLocation bestCheeseOrMineTargetFromInfos(RobotController rc, MapInfo[] infos)
            throws GameActionException {
        MapLocation me = rc.getLocation();
        MapLocation bestCheese = null;
        int bestCheeseD2 = Integer.MAX_VALUE;
        MapLocation bestMine = null;
        int bestMineD2 = Integer.MAX_VALUE;

        for (MapInfo mi : infos) {
            // Bytecode guard: if we're low, bail out with whatever we have so far.
            if (Clock.getBytecodesLeft() < 1200)
                break;
            MapLocation loc = mi.getMapLocation();
            int d2 = me.distanceSquaredTo(loc);

            if (mi.getCheeseAmount() > 0) {
                if (d2 < bestCheeseD2) {
                    bestCheeseD2 = d2;
                    bestCheese = loc;
                }
                continue;
            }

            if (mi.hasCheeseMine() && mi.isPassable()) {
                if (d2 < bestMineD2) {
                    bestMineD2 = d2;
                    bestMine = loc;
                }
            }
        }

        return bestCheese != null ? bestCheese : bestMine;
    }

    private static MapLocation bestCheesePickupFromInfos(RobotController rc, MapInfo[] infos)
            throws GameActionException {
        MapLocation me = rc.getLocation();
        MapLocation best = null;
        int bestD2 = Integer.MAX_VALUE;
        for (MapInfo mi : infos) {
            if (Clock.getBytecodesLeft() < 1200)
                break;
            int amt = mi.getCheeseAmount();
            if (amt <= 0)
                continue;
            MapLocation loc = mi.getMapLocation();
            if (!rc.canPickUpCheese(loc))
                continue;
            int d2 = me.distanceSquaredTo(loc);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = loc;
            }
        }
        return best;
    }

    // -------------------- Shared array helpers --------------------

    private static void tryWriteKingLocation(RobotController rc) {
        // Only rat kings can write the shared array.
        if (rc.getType() != UnitType.RAT_KING)
            return;
        MapLocation loc = rc.getLocation();
        if (loc.x > 63 || loc.y > 63)
            return; // map bounds are <= 60, but be safe.
        int roundMod = rc.getRoundNum() & 1023;
        try {
            rc.writeSharedArray(SA_KING_X, loc.x);
            rc.writeSharedArray(SA_KING_Y, loc.y);
            rc.writeSharedArray(SA_KING_R, roundMod);
        } catch (GameActionException ignored) {
        }
    }

    private static MapLocation readKingLocation(RobotController rc) {
        try {
            int x = rc.readSharedArray(SA_KING_X);
            int y = rc.readSharedArray(SA_KING_Y);
            int r = rc.readSharedArray(SA_KING_R);
            // Treat data as stale if not updated in a while.
            int now = rc.getRoundNum() & 1023;
            int age = (now - r) & 1023;
            if (age > 20)
                return null;
            MapLocation loc = new MapLocation(x, y);
            return rc.onTheMap(loc) ? loc : null;
        } catch (GameActionException e) {
            return null;
        }
    }

    private static MapLocation readEnemyKingLocation(RobotController rc) {
        try {
            int x = rc.readSharedArray(SA_EK_X);
            int y = rc.readSharedArray(SA_EK_Y);
            int r = rc.readSharedArray(SA_EK_R);
            int now = rc.getRoundNum() & 1023;
            int age = (now - r) & 1023;
            if (age > 30)
                return null;
            MapLocation loc = new MapLocation(x, y);
            return rc.onTheMap(loc) ? loc : null;
        } catch (GameActionException e) {
            return null;
        }
    }

    private static boolean ingestSqueaks(RobotController rc) {
        // Only rat kings can write shared array.
        if (rc.getType() != UnitType.RAT_KING)
            return false;
        Message[] msgs = rc.readSqueaks(-1);
        if (msgs == null || msgs.length == 0)
            return false;
        int nowMod = rc.getRoundNum() & 1023;

        for (Message m : msgs) {
            int raw = m.getBytes();
            int type = (raw >>> 24) & 0xFF;
            int x = (raw >>> 16) & 0xFF;
            int y = (raw >>> 8) & 0xFF;
            MapLocation loc = new MapLocation(x, y);
            if (!rc.onTheMap(loc))
                continue;

            if (type == MSG_ENEMY_KING) {
                try {
                    rc.writeSharedArray(SA_EK_X, x);
                    rc.writeSharedArray(SA_EK_Y, y);
                    rc.writeSharedArray(SA_EK_R, nowMod);
                } catch (GameActionException ignored) {
                }
                return true;
            }

            if (type == MSG_MINE) {
                // Store up to MAX_MINES unique mines.
                tryStoreMine(rc, loc);
                return true;
            }
        }
        return false;
    }

    private static void trySqueakEnemyKing(RobotController rc, MapLocation loc) {
        // Max 1 message per robot per turn; also avoid spamming every frame.
        int r = rc.getRoundNum();
        if (r - lastEnemyKingSqueakRound < 5)
            return;
        if (loc.x < 0 || loc.x > 255 || loc.y < 0 || loc.y > 255)
            return;
        int msg = (MSG_ENEMY_KING << 24) | ((loc.x & 0xFF) << 16) | ((loc.y & 0xFF) << 8);
        if (rc.squeak(msg)) {
            lastEnemyKingSqueakRound = r;
        }
    }

    private static void trySqueakMine(RobotController rc, MapLocation loc) {
        int r = rc.getRoundNum();
        if (lastMineSqueakLoc != null && lastMineSqueakLoc.equals(loc) && r - lastMineSqueakRound < 50)
            return;
        if (r - lastMineSqueakRound < 10)
            return;
        if (loc.x < 0 || loc.x > 255 || loc.y < 0 || loc.y > 255)
            return;
        int msg = (MSG_MINE << 24) | ((loc.x & 0xFF) << 16) | ((loc.y & 0xFF) << 8);
        if (rc.squeak(msg)) {
            lastMineSqueakLoc = loc;
            lastMineSqueakRound = r;
        }
    }

    private static void trySqueakDefend(RobotController rc, MapLocation loc) {
        int r = rc.getRoundNum();
        if (r - lastDefendSqueakRound < 5)
            return;
        if (loc.x < 0 || loc.x > 255 || loc.y < 0 || loc.y > 255)
            return;
        int msg = (MSG_DEFEND << 24) | ((loc.x & 0xFF) << 16) | ((loc.y & 0xFF) << 8);
        if (rc.squeak(msg)) {
            lastDefendSqueakRound = r;
        }
    }

    private static MapLocation readRallyFromSqueaks(RobotController rc) {
        Message[] msgs = rc.readSqueaks(-1);
        if (msgs == null || msgs.length == 0)
            return null;
        int bestRound = -1;
        MapLocation best = null;
        for (Message m : msgs) {
            int raw = m.getBytes();
            int type = (raw >>> 24) & 0xFF;
            if (type != MSG_DEFEND)
                continue;
            int x = (raw >>> 16) & 0xFF;
            int y = (raw >>> 8) & 0xFF;
            MapLocation loc = new MapLocation(x, y);
            if (!rc.onTheMap(loc))
                continue;
            if (m.getRound() > bestRound) {
                bestRound = m.getRound();
                best = loc;
            }
        }
        return best;
    }

    private static MapLocation visibleCheeseMineFromInfos(MapInfo[] infos) {
        for (MapInfo mi : infos) {
            if (mi.hasCheeseMine() && mi.isPassable())
                return mi.getMapLocation();
        }
        return null;
    }

    private static void tryStoreMine(RobotController rc, MapLocation mine) {
        // Only rat kings can write shared array; caller ensures that.
        if (rc.getType() != UnitType.RAT_KING)
            return;
        try {
            int count = rc.readSharedArray(SA_MINE_COUNT);
            if (count < 0)
                count = 0;
            if (count > MAX_MINES)
                count = MAX_MINES;

            // Dedup
            for (int i = 0; i < count; i++) {
                int x = rc.readSharedArray(SA_MINE_BASE + 2 * i);
                int y = rc.readSharedArray(SA_MINE_BASE + 2 * i + 1);
                if (x == mine.x && y == mine.y)
                    return;
            }

            if (count >= MAX_MINES)
                return;

            rc.writeSharedArray(SA_MINE_BASE + 2 * count, mine.x);
            rc.writeSharedArray(SA_MINE_BASE + 2 * count + 1, mine.y);
            rc.writeSharedArray(SA_MINE_COUNT, count + 1);
        } catch (GameActionException ignored) {
        }
    }

    private static MapLocation readNearestKnownMine(RobotController rc) {
        try {
            int count = rc.readSharedArray(SA_MINE_COUNT);
            if (count <= 0)
                return null;
            if (count > MAX_MINES)
                count = MAX_MINES;

            MapLocation me = rc.getLocation();
            MapLocation best = null;
            int bestD2 = Integer.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                int x = rc.readSharedArray(SA_MINE_BASE + 2 * i);
                int y = rc.readSharedArray(SA_MINE_BASE + 2 * i + 1);
                MapLocation loc = new MapLocation(x, y);
                if (!rc.onTheMap(loc))
                    continue;
                int d2 = me.distanceSquaredTo(loc);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    best = loc;
                }
            }
            return best;
        } catch (GameActionException e) {
            return null;
        }
    }

    // -------------------- Movement helpers --------------------

    private static void tryTurnToward(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isTurningReady() || !rc.canTurn())
            return;
        Direction d = rc.getLocation().directionTo(target);
        if (d == Direction.CENTER)
            return;
        if (rc.getDirection() == d)
            return;
        rc.turn(d);
    }

    private static boolean tryMoveToward(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isMovementReady())
            return false;
        if (target == null)
            return false;

        // Reset nav state when target changes.
        if (navTarget == null || !navTarget.equals(target)) {
            navTarget = target;
            bugging = false;
            bugDir = null;
            bugStartD2 = Integer.MAX_VALUE;
            bugStartRound = rc.getRoundNum();
            bugFollowRight = (rc.getID() & 1) == 0;
        }

        MapLocation me = rc.getLocation();
        Direction to = me.directionTo(target);
        if (to == Direction.CENTER)
            return false;

        // If we're bugging for too long, reset.
        if (bugging && rc.getRoundNum() - bugStartRound > 40) {
            bugging = false;
            bugDir = null;
        }

        // Exit wall-follow if we made progress and can go roughly toward target again.
        if (bugging) {
            int d2 = me.distanceSquaredTo(target);
            if (d2 < bugStartD2 && (rc.canMove(to) || rc.canMove(to.rotateLeft()) || rc.canMove(to.rotateRight()))) {
                bugging = false;
                bugDir = null;
            }
        }

        // Greedy attempt first (fast path).
        if (!bugging) {
            Direction[] tries = {
                    to,
                    to.rotateLeft(),
                    to.rotateRight(),
                    to.rotateLeft().rotateLeft(),
                    to.rotateRight().rotateRight(),
            };

            for (Direction d : tries) {
                if (rc.canMove(d)) {
                    rc.move(d);
                    return true;
                }
            }

            // Start bugging (wall-follow).
            bugging = true;
            bugDir = to;
            bugStartD2 = me.distanceSquaredTo(target);
            bugStartRound = rc.getRoundNum();
        }

        // Wall-follow: rotate around obstacles until we find a move.
        if (bugDir == null)
            bugDir = to;
        for (int i = 0; i < 8; i++) {
            if (rc.canMove(bugDir)) {
                rc.move(bugDir);
                // After moving, hug the wall by turning toward it.
                bugDir = bugFollowRight ? bugDir.rotateRight() : bugDir.rotateLeft();
                return true;
            }
            bugDir = bugFollowRight ? bugDir.rotateLeft() : bugDir.rotateRight();
        }
        return false;
    }

    private static void wander(RobotController rc, String label) throws GameActionException {
        int now = rc.getRoundNum();
        if (wanderTarget == null || now - lastWanderTargetRound > 60
                || rc.getLocation().distanceSquaredTo(wanderTarget) <= 4) {
            wanderTarget = randomMapLocation(rc);
            lastWanderTargetRound = now;
        }
        tryTurnToward(rc, wanderTarget);
        if (tryMoveToward(rc, wanderTarget)) {
            rc.setIndicatorString(label + ": wander " + wanderTarget);
        } else if (rc.isMovementReady()) {
            // If we got stuck, pick a new target quickly.
            wanderTarget = randomMapLocation(rc);
            lastWanderTargetRound = now;
        }
    }

    private static boolean tryPlaceDefenseTrap(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();

        // Prefer cat traps (bigger threat), then rat traps.
        for (Direction d : DIRS) {
            MapLocation loc = me.add(d);
            if (!rc.onTheMap(loc))
                continue;
            if (rc.canPlaceCatTrap(loc)) {
                rc.placeCatTrap(loc);
                return true;
            }
        }
        for (Direction d : DIRS) {
            MapLocation loc = me.add(d);
            if (!rc.onTheMap(loc))
                continue;
            if (rc.canPlaceRatTrap(loc)) {
                rc.placeRatTrap(loc);
                return true;
            }
        }
        return false;
    }

    // Place a trap on an adjacent tile (within build distance). Uses the
    // already-cached
    // vision MapInfo[] to avoid extra sensing calls.
    // If preferCatTrap is true, try cat trap first; otherwise rat trap first.
    private static boolean tryPlaceDefenseTrapSmart(
            RobotController rc,
            MapInfo[] visionInfos,
            MapLocation kingLoc,
            MapLocation threatLoc,
            boolean preferCatTrap) throws GameActionException {
        if (!rc.isActionReady())
            return false;

        MapLocation me = rc.getLocation();

        // Choose best candidate among tiles within BUILD_DISTANCE_SQUARED (2).
        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;

        for (MapInfo mi : visionInfos) {
            if (Clock.getBytecodesLeft() < 1000)
                break;
            MapLocation loc = mi.getMapLocation();
            if (me.distanceSquaredTo(loc) > 2)
                continue;
            if (!mi.isPassable())
                continue;
            if (mi.getTrap() != TrapType.NONE)
                continue; // our own trap already here (enemy traps are invisible anyway)

            int score = 0;
            if (kingLoc != null)
                score -= 3 * loc.distanceSquaredTo(kingLoc);
            if (threatLoc != null)
                score -= loc.distanceSquaredTo(threatLoc);

            if (best == null || score > bestScore) {
                bestScore = score;
                best = loc;
            }
        }

        if (best == null)
            return false;

        if (preferCatTrap) {
            if (rc.canPlaceCatTrap(best)) {
                rc.placeCatTrap(best);
                return true;
            }
            if (rc.canPlaceRatTrap(best)) {
                rc.placeRatTrap(best);
                return true;
            }
        } else {
            if (rc.canPlaceRatTrap(best)) {
                rc.placeRatTrap(best);
                return true;
            }
            if (rc.canPlaceCatTrap(best)) {
                rc.placeCatTrap(best);
                return true;
            }
        }

        return false;
    }

    private static MapLocation randomMapLocation(RobotController rc) {
        int w = rc.getMapWidth();
        int h = rc.getMapHeight();
        int x = rng.nextInt(Math.max(1, w));
        int y = rng.nextInt(Math.max(1, h));
        return new MapLocation(x, y);
    }

    private static MapLocation clampToMap(RobotController rc, MapLocation loc) {
        int x = Math.max(0, Math.min(rc.getMapWidth() - 1, loc.x));
        int y = Math.max(0, Math.min(rc.getMapHeight() - 1, loc.y));
        return new MapLocation(x, y);
    }

    // Choose a trap tile near the cat, biased toward the line between cat and king.
    private static MapLocation bestTrapLocationNear(RobotController rc, MapLocation cat, MapLocation king)
            throws GameActionException {
        MapLocation me = rc.getLocation();
        Direction toCat = me.directionTo(cat);
        Direction toKing = me.directionTo(king);

        // Candidate tiles adjacent to us that are also near the cat (trigger radius is
        // 2, so adjacency helps).
        Direction[] dirs = {
                toCat,
                toCat.rotateLeft(),
                toCat.rotateRight(),
                toKing, // also consider blocking line toward king
                toKing.rotateLeft(),
                toKing.rotateRight()
        };

        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Direction d : dirs) {
            if (d == Direction.CENTER)
                continue;
            MapLocation loc = me.add(d);
            if (!rc.onTheMap(loc))
                continue;
            // Prefer tiles closer to the cat and between cat/king.
            int score = 0;
            score -= loc.distanceSquaredTo(cat);
            score -= loc.distanceSquaredTo(king) / 3;
            if (best == null || score > bestScore) {
                bestScore = score;
                best = loc;
            }
        }
        return best;
    }

    private static MapLocation pickRatSpawnLocation(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        MapLocation target = bestCheeseOrMineTarget(rc);
        Direction prefer = (target == null) ? DIRS[rng.nextInt(8)] : me.directionTo(target);
        if (prefer == Direction.CENTER)
            prefer = DIRS[rng.nextInt(8)];

        // Try close tiles first, biased toward prefer direction.
        Direction[] tryDirs = {
                prefer,
                prefer.rotateLeft(),
                prefer.rotateRight(),
                prefer.rotateLeft().rotateLeft(),
                prefer.rotateRight().rotateRight(),
                prefer.opposite(),
                prefer.opposite().rotateLeft(),
                prefer.opposite().rotateRight(),
        };

        for (Direction d : tryDirs) {
            MapLocation loc = me.add(d);
            if (rc.canBuildRat(loc))
                return loc;
        }

        // If adjacent doesn't work, try the full build radius (<= 2).
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int d2 = dx * dx + dy * dy;
                if (d2 == 0 || d2 > 4)
                    continue;
                MapLocation loc = new MapLocation(me.x + dx, me.y + dy);
                if (rc.canBuildRat(loc))
                    return loc;
            }
        }
        return null;
    }
}
