package current.States;

import battlecode.common.*;

import static current.States.Code.*;

/**
 * State to avoid cats
 * Checks for cats in vision and from shared array, then moves away
 */
public class AvoidCat extends State {
    
    // Cat vision radius squared: sqrt(17) ≈ 4.12, so radius squared = 17
    private static final int CAT_VISION_RADIUS_SQUARED = 17;
    // Safe distance from cat (want to be outside cat vision)
    private static final int SAFE_DISTANCE_SQUARED = 25; // 5^2, outside cat vision
    
    public AvoidCat() {
        this.name = "AvoidCat";
    }
    
    @Override
    public Result run() throws GameActionException {
        // TODO: lmx
        return new Result(OK, "Need to be implemented");
    }
}
