package params_dbe63b76.States;

import battlecode.common.*;
import params_dbe63b76.Utils.PathFinding;

import static params_dbe63b76.States.Code.*;

/**
 * State to attack cats with improved micro
 * - Move out of cat view after attack
 * - Bonus for first attack
 * - Consider suicide if losing battle
 */
public class AttackCat extends State {
    public AttackCat() {
        this.name = "AttackCat";
    }

    // Cat vision radius squared: sqrt(17) ≈ 4.12, so radius squared = 17
    private static final int CAT_VISION_RADIUS_SQUARED = 17;
    // Safe distance to attack (adjacent only)
    private static final int ATTACK_DISTANCE_SQUARED = 2;
    // Distance to be out of cat view
    private static final int OUT_OF_VIEW_DISTANCE_SQUARED = 25; // 5^2, outside cat vision

    @Override
    public Result run() throws GameActionException {
        if (nearestCat == null) {
            return new Result(OK, "No cat");
        }

        int catDistance = myLoc.distanceSquaredTo(nearestCat);
        if (catDistance > ATTACK_DISTANCE_SQUARED) {
            return new Result(OK, "No cat to attack");
        }

        // Check if we can attack
        if (!rc.canAttack(nearestCat)) {
            // If we can't attack but are adjacent, try to move out of view
            if (catDistance <= ATTACK_DISTANCE_SQUARED && rc.isMovementReady()) {
                // Move away from cat to get out of view
                Direction awayFromCat = myLoc.directionTo(nearestCat).opposite();
                PathFinding.resetScores();
                PathFinding.modificatorOrientation(awayFromCat);
                // Boost directions that get us out of cat view
                long[] escapeScores = new long[9];
                for (Direction dir : Direction.values()) {
                    if (dir != Direction.CENTER) {
                        MapLocation nextLoc = myLoc.add(dir);
                        int nextDist = nextLoc.distanceSquaredTo(nearestCat);
                        if (nextDist >= OUT_OF_VIEW_DISTANCE_SQUARED) {
                            escapeScores[dir.ordinal()] = 1000; // High score for escape
                        } else {
                            escapeScores[dir.ordinal()] = -9999; // Low score if still in view
                        }
                    }
                }
                PathFinding.addScoresWithoutNormalization(escapeScores);
                return PathFinding.moveBest();
            }
            return new Result(OK, "Can't attack");
        }

        // Check cat health to see if we're winning
        RobotInfo catInfo = null;
        if (rc.canSenseLocation(nearestCat)) {
            catInfo = rc.senseRobotAtLocation(nearestCat);
        }

        // Use some cheese for a stronger bite if we have it
        int cheeseToSpend = Math.min(Math.max(rc.getRawCheese(), rc.getGlobalCheese()), 25);
        
        // Attack the cat
        rc.attack(nearestCat, cheeseToSpend);
        
        // After attack, try to move out of cat view if we can
        if (rc.isMovementReady() && catDistance < OUT_OF_VIEW_DISTANCE_SQUARED) {
            Direction awayFromCat = myLoc.directionTo(nearestCat).opposite();
            PathFinding.resetScores();
            PathFinding.modificatorOrientation(awayFromCat);
            // Boost directions that get us out of cat view
            long[] escapeScores = new long[9];
            for (Direction dir : Direction.values()) {
                if (dir != Direction.CENTER) {
                    MapLocation nextLoc = myLoc.add(dir);
                    int nextDist = nextLoc.distanceSquaredTo(nearestCat);
                    if (nextDist >= OUT_OF_VIEW_DISTANCE_SQUARED) {
                        escapeScores[dir.ordinal()] = 500; // Medium score for escape after attack
                    }
                }
            }
            PathFinding.addScoresWithoutNormalization(escapeScores);
            PathFinding.moveBest(); // Try to move, but don't fail if can't
        }

        // Consider suicide if we're losing badly (cat has much more health than us)
        if (catInfo != null) {
            int ourHealth = rc.getHealth();
            int catHealth = catInfo.getHealth();
            // If cat has 3x our health and we're low, might be better to suicide
            // to avoid giving enemy more points by dealing damage
            if (ourHealth < 30 && catHealth > ourHealth * 3) {
                // Don't attack again, just move away or accept death
                return new Result(OK, "Attacked but losing badly, avoiding further engagement");
            }
        }

        return new Result(OK, "Attacked cat with " + cheeseToSpend + " cheese");
    }
}
