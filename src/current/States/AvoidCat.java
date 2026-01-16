package current.States;

import battlecode.common.*;

import static current.States.Code.*;

/**
 * State to avoid cats
 * Checks for cats in vision and from shared array, then moves away
 */
public class AvoidCat extends State {
    
    private static final int CAT_DANGER_RADIUS_SQ = 36;
    
    public AvoidCat() {
        this.name = "AvoidCat";
    }
    
    @Override
    public Result run() throws GameActionException {
        if(nearestCat == null){
            return new Result(OK, "No cat nearby");
        }

        MapLocation myLoc = rc.getLocation();
        int currentDist = myLoc.distanceSquaredTo(nearestCat);
        if(currentDist > CAT_DANGER_RADIUS_SQ){
            return new Result(OK, "Cat far enough");
        }

        if(rc.getMovementCooldownTurns() != 0){
            return new Result(END_OF_TURN, "Can't move away yet");
        }

        Direction bestDir = Direction.CENTER;
        int bestScore = Integer.MIN_VALUE;
        for(Direction dir : Direction.values()){
            if(dir == Direction.CENTER){
                continue;
            }
            if(!rc.canMove(dir)){
                continue;
            }
            MapLocation next = myLoc.add(dir);
            int nextDist = next.distanceSquaredTo(nearestCat);
            int score = (nextDist - currentDist) * 10;
            if(score > bestScore){
                bestScore = score;
                bestDir = dir;
            }
        }

        if(bestScore > 0 && bestDir != Direction.CENTER){
            rc.move(bestDir);
            return new Result(END_OF_TURN, "Avoiding cat");
        }

        return new Result(OK, "Cat nearby but no safe move");
    }
}
