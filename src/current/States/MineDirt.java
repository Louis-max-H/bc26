package current.States;

import battlecode.common.*;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;

import static current.States.Code.*;

/**
 * State to mine dirt when not in rush phase
 * Only collects dirt when we have nothing else to do
 */
public class MineDirt extends State {
    public MineDirt(){
        this.name = "MineDirt";
    }

    @Override
    public Result run() throws GameActionException {
        // Only mine dirt if not in rush phase (not PHASE_START)
        if(gamePhase == PHASE_START){
            return new Result(OK, "Rush phase, skip mining dirt");
        }

        // Check if we can mine dirt (need action ready and 10 cheese)
        if(!rc.isActionReady() || rc.getAllCheese() < 10){
            return new Result(OK, "Can't mine dirt (action not ready or not enough cheese)");
        }

        // Find nearest dirt
        if(nearestDirt == null){
            return new Result(OK, "No dirt nearby");
        }

        // If close enought, force looking to dirt (required to dig it)
        if(rc.getLocation().distanceSquaredTo(nearestDirt) <= 2){
            VisionUtils.smartLookAt(nearestDirt);
        }

        // Try to remove it
        if(rc.canRemoveDirt(nearestDirt)){
            rc.removeDirt(nearestDirt);
            nearestDirt = null;
            return new Result(OK, "Removed dirt");
        }

        // Move toward dirt
        PathFinding.smartMoveTo(nearestDirt);
        return new Result(OK, "Moving to dirt");
    }
}
