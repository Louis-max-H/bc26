package v008_saketh.States;

import battlecode.common.*;
import v008_saketh.Robots.Robot;
import v008_saketh.Utils.PathFinding;

import static v008_saketh.States.Code.*;

public class MoveKing extends State {
    /**
     * Explore use a init in Init state.
     * We have a big array of interrest, and we move/turn to the zone with the biggest score.
     * */

    public MoveKing(){
        this.name = "MoveKing";
    }

    @Override
    public Result run() throws GameActionException {
        // Check if we can move and turn
        if(rc.getMovementCooldownTurns() != 0){
            return new Result(CANT, "Can't move");
        }

        // King move to nearest mine, if finds cat move back to spawn location
        if(nearestCat != null && myLoc.distanceSquaredTo(nearestCat) <= 25){
            // Cat nearby - retreat to spawn
            PathFinding.smartMoveTo(Robot.spawnLoc);
            return new Result(OK, "Retreating to spawn due to cat");
        }

        // Otherwise, move toward nearest mine if available
        if(nearestMine != null){
            PathFinding.smartMoveTo(nearestMine);
            return new Result(OK, "Moving to nearest mine");
        }

        // Fallback: Add a score to move to rats with cheese
        long[] scores = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        for(RobotInfo info: rc.senseNearbyRobots(-1, rc.getTeam())){
            scores[myLoc.directionTo(info.location).ordinal()] += info.cheeseAmount;
        }

        PathFinding.addScoresWithNormalization(scores, 5);
        PathFinding.moveBest();
        return new Result(OK, "Done updating scores");
    };
}
