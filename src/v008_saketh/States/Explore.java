package v008_saketh.States;

import battlecode.common.*;
import v008_saketh.Robots.Robot;
import v008_saketh.Utils.PathFinding;
import v008_saketh.Utils.VisionUtils;

import static v008_saketh.States.Code.*;

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

        // Add stronger bias to center (increased from 50 to 150)
        int centerBias = 150; // Stronger bias to encourage exploration toward center
        scores[dirToCenter.ordinal()] += centerBias;
        scores[dirToCenter.rotateLeft().ordinal()] += centerBias * 2 / 3;
        scores[dirToCenter.rotateRight().ordinal()] += centerBias * 2 / 3;
        
        // Also add some random exploration: pick a random direction occasionally
        if(Robot.rng.nextInt(10) < 2){ // 20% chance
            Direction randomDir = Robot.directions[Robot.rng.nextInt(8)];
            scores[randomDir.ordinal()] += 100; // Add random exploration bonus
        }

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
