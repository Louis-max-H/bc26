package current.States;

import battlecode.common.*;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;

import static current.States.Code.*;

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
        int[] scores = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        MapLocation myLoc = rc.getLocation();
        for(Direction dir : Direction.values()){
            if(dir != Direction.CENTER){
                scores[dir.ordinal()] = VisionUtils.getScoreInView(myLoc.add(dir), dir, rc.getType());
            }
        }

        PathFinding.addScoresWithoutNormalization(scores, 1);
        Direction bestDir = PathFinding.bestDir();

        // Turn and move to this direction
        rc.turn(bestDir);
        if(PathFinding.moveDir(bestDir).notOk()){
            print("Can't move to best direction.");
        }

        // TODOS: Maybe turn, and then, according to new infos, restart from beginning ?
        // TODOS: Check if you need to move after turning
        // TODOS: Check if second score parameters is pertinent
        // TODOS: Check if not moving when second direction is nice, is good choice (can allow us to just tourn arround and then move)

        return new Result(OK, "Turning and moving to " + bestDir.name());
    };
}
