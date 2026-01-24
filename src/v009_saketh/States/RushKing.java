package v009_saketh.States;

import battlecode.common.*;
import v009_saketh.Utils.PathFinding;

import static v009_saketh.States.Code.*;

/**
 * State to rush to the symmetric location of our king (where enemy king might be)
 * Only activates after round 1500 (PHASE_FINAL)
 */
public class RushKing extends State {
    public RushKing() {
        this.name = "RushKing";
    }

    /**
     * Calculate the symmetric location of a given location
     * Uses rotational symmetry (most common map symmetry type)
     */
    private MapLocation getSymmetricLocation(MapLocation loc) {
        int mapWidth = rc.getMapWidth();
        int mapHeight = rc.getMapHeight();
        // Rotational symmetry: (x, y) -> (width - 1 - x, height - 1 - y)
        int symX = mapWidth - 1 - loc.x;
        int symY = mapHeight - 1 - loc.y;
        return new MapLocation(symX, symY);
    }

    @Override
    public Result run() throws GameActionException {
        // Only activate after round 1500 (PHASE_FINAL)
        if (gamePhase < PHASE_FINAL) {
            return new Result(OK, "Not in final phase yet (gamePhase: " + gamePhase + ")");
        }

        // Get our king location
        MapLocation ourKingLoc = nearestKing;
        if (ourKingLoc == null) {
            // Try to get from kings collection if nearestKing is not set
            if (kings.size > 0) {
                ourKingLoc = kings.locs[0]; // Use first king found
            } else {
                return new Result(OK, "No king location found");
            }
        }

        // Calculate symmetric location (where enemy king might be)
        MapLocation targetLoc = getSymmetricLocation(ourKingLoc);

        // Check if target is on the map
        if (!rc.onTheMap(targetLoc)) {
            return new Result(WARN, "Symmetric location " + targetLoc + " is off map");
        }

        // Move toward the symmetric location
        if (rc.isMovementReady()) {
            Result result = PathFinding.smartMoveTo(targetLoc);
            return new Result(result.code, "Rushing to symmetric king location " + targetLoc + " (" + result.msg + ")");
        }

        // If we can't move, at least turn toward the target
        if (rc.canTurn()) {
            Direction dirToTarget = myLoc.directionTo(targetLoc);
            if (rc.canTurn(dirToTarget)) {
                rc.turn(dirToTarget);
                return new Result(OK, "Turning toward symmetric king location");
            }
        }

        return new Result(CANT, "Cannot move or turn");
    }
}
