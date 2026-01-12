package current.States;

import battlecode.common.*;
import current.Robots.Robot;
import current.Utils.Communication;
import current.Utils.PathFinding;

import static current.States.Code.*;
import static current.Utils.Communication.*;

/**
 * State to attack cats when safe
 * Only attacks if a cat is adjacent, and we're not in immediate danger
 */
public class AttackCat extends State {
    public AttackCat() {
        this.name = "AttackCat";
    }

    // Cat vision radius squared: sqrt(17) ≈ 4.12, so radius squared = 17
    private static final int CAT_VISION_RADIUS_SQUARED = 17;
    // Safe distance to attack (adjacent only)
    private static final int ATTACK_DISTANCE_SQUARED = 2;

    @Override
    public Result run() throws GameActionException {
        // If cat
        if (nearestCat == null) {
            return new Result(OK, "No cat");
        }

        int catDistance = myLoc.distanceSquaredTo(nearestCat);
        // Cat too far
        if (catDistance > ATTACK_DISTANCE_SQUARED) {
            return new Result(OK, "No cat to attack");
        }

        // Check if we can attack
        if (!rc.canAttack(nearestCat)) {
            return new Result(OK, "Can't attack");
        }

        // Use some cheese for a stronger bite if we have it
        int cheeseToSpend = Math.min(
            Math.max(
                rc.getRawCheese(),
                rc.getGlobalCheese()
            ), 25
        );

        rc.attack(nearestCat, cheeseToSpend);
        return new Result(OK, "Attacked cat with " + cheeseToSpend + " cheese");
    }
}
