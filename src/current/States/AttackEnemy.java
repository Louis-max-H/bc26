package current.States;

import battlecode.common.*;
import current.Utils.PathFinding;

import static current.States.Code.*;

/**
 * State to attack enemy rats with improved micro
 * - Move out of enemy view after attack
 * - Bonus for first attack
 * - Consider suicide if losing battle
 */
public class AttackEnemy extends State {
    public AttackEnemy() {
        this.name = "AttackEnemy";
    }

    // Baby rat vision radius squared: 5^2 = 25
    private static final int RAT_VISION_RADIUS_SQUARED = 25;
    // Safe distance to attack (adjacent only)
    private static final int ATTACK_DISTANCE_SQUARED = 2;
    // Distance to be out of enemy view
    private static final int OUT_OF_VIEW_DISTANCE_SQUARED = 25;

    @Override
    public Result run() throws GameActionException {
        // Prioritize enemy rat over enemy king
        MapLocation enemyLoc = nearestEnemyRat != null ? nearestEnemyRat : nearestEnemyKing;
        if (enemyLoc == null) {
            return new Result(OK, "No enemy");
        }

        int enemyDistance = myLoc.distanceSquaredTo(enemyLoc);
        if (enemyDistance > ATTACK_DISTANCE_SQUARED) {
            return new Result(OK, "No enemy to attack");
        }

        // Check if we can attack
        if (!rc.canAttack(enemyLoc)) {
            // If we can't attack but are adjacent, try to move out of view
            if (enemyDistance <= ATTACK_DISTANCE_SQUARED && rc.isMovementReady()) {
                Direction awayFromEnemy = myLoc.directionTo(enemyLoc).opposite();
                PathFinding.resetScores();
                PathFinding.modificatorOrientation(awayFromEnemy);
                // Boost directions that get us out of enemy view
                int[] escapeScores = new int[9];
                for (Direction dir : Direction.values()) {
                    if (dir != Direction.CENTER) {
                        MapLocation nextLoc = myLoc.add(dir);
                        int nextDist = nextLoc.distanceSquaredTo(enemyLoc);
                        if (nextDist >= OUT_OF_VIEW_DISTANCE_SQUARED) {
                            escapeScores[dir.ordinal()] = 1000;
                        } else {
                            escapeScores[dir.ordinal()] = -9999;
                        }
                    }
                }
                PathFinding.addScoresWithoutNormalization(escapeScores);
                return PathFinding.moveBest();
            }
            return new Result(OK, "Can't attack");
        }

        // Check enemy health to see if we're winning
        RobotInfo enemyInfo = null;
        if (rc.canSenseLocation(enemyLoc)) {
            enemyInfo = rc.senseRobotAtLocation(enemyLoc);
        }

        // Use some cheese for a stronger bite if we have it
        int cheeseToSpend = Math.min(Math.max(rc.getRawCheese(), rc.getGlobalCheese()), 25);
        
        // Attack the enemy
        rc.attack(enemyLoc, cheeseToSpend);
        
        // After attack, try to move out of enemy view if we can
        if (rc.isMovementReady() && enemyDistance < OUT_OF_VIEW_DISTANCE_SQUARED) {
            Direction awayFromEnemy = myLoc.directionTo(enemyLoc).opposite();
            PathFinding.resetScores();
            PathFinding.modificatorOrientation(awayFromEnemy);
            // Boost directions that get us out of enemy view
            int[] escapeScores = new int[9];
            for (Direction dir : Direction.values()) {
                if (dir != Direction.CENTER) {
                    MapLocation nextLoc = myLoc.add(dir);
                    int nextDist = nextLoc.distanceSquaredTo(enemyLoc);
                    if (nextDist >= OUT_OF_VIEW_DISTANCE_SQUARED) {
                        escapeScores[dir.ordinal()] = 500;
                    }
                }
            }
            PathFinding.addScoresWithoutNormalization(escapeScores);
            PathFinding.moveBest();
        }

        // Consider suicide if we're losing badly
        if (enemyInfo != null) {
            int ourHealth = rc.getHealth();
            int enemyHealth = enemyInfo.getHealth();
            // If enemy has 3x our health and we're low, might be better to suicide
            if (ourHealth < 30 && enemyHealth > ourHealth * 3) {
                return new Result(OK, "Attacked but losing badly, avoiding further engagement");
            }
        }

        return new Result(OK, "Attacked enemy with " + cheeseToSpend + " cheese");
    }
}
