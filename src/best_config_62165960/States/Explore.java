package best_config_62165960.States;

import battlecode.common.*;
import best_config_62165960.Utils.PathFinding;
import best_config_62165960.Utils.VisionUtils;

import static best_config_62165960.States.Code.*;

public class Explore extends State {
    /**
     * Explore use a init in Init state.
     * We have a big array of interrest, and we move/turn to the zone with the biggest score.
     * */

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

        // For each nearby cells, add their heuristic to the direction that lead to this cell
        long[] scores = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        for(Direction dir : Direction.values()){
            if(dir != Direction.CENTER){
                scores[dir.ordinal()] = VisionUtils.getScoreInView(myLoc.add(dir), dir, rc.getType());
            }
        }

        // Add bias toward map center for exploration
        MapLocation mapCenter = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        Direction dirToCenter = myLoc.directionTo(mapCenter);

        // Add bias to center
        int centerBias = 50; // Small bias to encourage exploration toward center
        scores[dirToCenter.ordinal()] += centerBias;
        scores[dirToCenter.rotateLeft().ordinal()] += centerBias;
        scores[dirToCenter.rotateRight().ordinal()] += centerBias;

        // Add scores and move to best dir
        PathFinding.addScoresWithNormalization(scores, 1);
        Result result = PathFinding.moveBest();
        Result resultTurn = VisionUtils.smartLook();
        return new Result(OK, "Move result : " + result.msg + " Turn result : " + resultTurn.msg);

        // TODOS: Maybe turn, and then, according to new infos, restart from beginning ?
        // TODOS: Check if you need to move after turning
        // TODOS: Check if second score parameters is pertinent
        // TODOS: Check if not moving when second direction is nice, is good choice (can allow us to just tourn arround and then move)
    };
}
