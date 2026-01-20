package params_dbe63b76.States;

import battlecode.common.*;
import params_dbe63b76.Utils.PathFinding;

import static params_dbe63b76.States.Code.*;

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

        // Add a score to move to rats with cheese
        long[] scores = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        for(RobotInfo info: rc.senseNearbyRobots(-1, rc.getTeam())){
            scores[myLoc.directionTo(info.location).ordinal()] += info.cheeseAmount;
        }

        PathFinding.addScoresWithNormalization(scores, 5);
        PathFinding.moveBest();
        return new Result(OK, "Done updating scores");
    };
}
