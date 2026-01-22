package current.States;

import battlecode.common.*;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;

import static current.States.Code.*;

public class Explore extends State {
    /**
     * Explore by moving toward a random destination biased toward a corner/border.
     * Corners are distributed by baby ID to reduce clumping. 
     * */
    private MapLocation target;

    private static final int TARGET_REACHED_DIST_SQ = 9;
    private static final int CORNER_BAND_DIVISOR = 2;

    private static final int CORNER_BOTTOM_LEFT = 0;
    private static final int CORNER_BOTTOM_RIGHT = 1;
    private static final int CORNER_TOP_LEFT = 2;
    private static final int CORNER_TOP_RIGHT = 3;

    public Explore(){
        this.name = "Explore";
    }

    @Override
    public Result run() throws GameActionException {
        // Check if we can move and turn
        if(rc.getMovementCooldownTurns() != 0){
            return new Result(CANT, "Can't move");
        }

        if(rc.getTurningCooldownTurns() != 0){
            return new Result(CANT, "Can't turn");
        }

        if(target == null || myLoc.distanceSquaredTo(target) <= TARGET_REACHED_DIST_SQ){
            target = pickRandomDestination();
        }

        Result result = PathFinding.smartMoveTo(target);
        Result resultTurn = VisionUtils.smartLook();
        rc.setIndicatorLine(myLoc, target, 0, 200, 200);
        return new Result(OK, "Exploring toward " + target + " move: " + result.msg + " turn: " + resultTurn.msg);

        // TODOS: Maybe turn, and then, according to new infos, restart from beginning ?
        // TODOS: Check if you need to move after turning
        // TODOS: Check if second score parameters is pertinent
        // TODOS: Check if not moving when second direction is nice, is good choice (can allow us to just tourn arround and then move)
    };

    private MapLocation pickRandomDestination(){
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        int corner = pickCorner(width, height);
        int bandX = Math.max(1, (width + CORNER_BAND_DIVISOR - 1) / CORNER_BAND_DIVISOR);
        int bandY = Math.max(1, (height + CORNER_BAND_DIVISOR - 1) / CORNER_BAND_DIVISOR);

        boolean towardMaxX = corner == CORNER_BOTTOM_RIGHT || corner == CORNER_TOP_RIGHT;
        boolean towardMaxY = corner == CORNER_TOP_LEFT || corner == CORNER_TOP_RIGHT;

        // Restrict target to a border band near the chosen corner.
        int minX = towardMaxX ? Math.max(0, width - bandX) : 0;
        int maxX = towardMaxX ? width - 1 : Math.max(0, bandX - 1);
        int minY = towardMaxY ? Math.max(0, height - bandY) : 0;
        int maxY = towardMaxY ? height - 1 : Math.max(0, bandY - 1);

        int x = biasedCoord(minX, maxX, towardMaxX);
        int y = biasedCoord(minY, maxY, towardMaxY);
        return new MapLocation(x, y);
    }

    private int pickCorner(int width, int height){
        int currentCorner = quadrantOf(myLoc, width, height);
        int corner = (rc.getID() + (round / 200)) & 3;
        if(corner == currentCorner){
            corner = (corner + 2) & 3;
        }
        return corner;
    }

    private int quadrantOf(MapLocation loc, int width, int height){
        int midX = width / 2;
        int midY = height / 2;
        boolean onLeft = loc.x < midX;
        boolean onBottom = loc.y < midY;

        if(onLeft && onBottom){
            return CORNER_BOTTOM_LEFT;
        }
        if(onLeft){
            return CORNER_TOP_LEFT;
        }
        if(onBottom){
            return CORNER_BOTTOM_RIGHT;
        }
        return CORNER_TOP_RIGHT;
    }

    private int biasedCoord(int min, int max, boolean towardMax){
        int span = max - min;
        if(span <= 0){
            return min;
        }
        // Bias toward the chosen edge by squaring the random pick.
        int pick = rng.nextInt(span + 1);
        pick = (pick + rc.getID()) % (span + 1);
        int biased = (pick * pick) / (span + 1);
        return towardMax ? max - biased : min + biased;
    }
}
