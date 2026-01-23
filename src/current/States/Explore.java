package current.States;

import battlecode.common.*;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;

import java.util.Random;

import static current.States.Code.*;

public class Explore extends State {
    /**
     * Explore use a init in Init state.
     * We have a big array of interrest, and we move/turn to the zone with the biggest score.
     * */
    MapLocation target;
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

        UnitType unitType = rc.getType();
        // For each nearby cells, add their heuristic to the direction that lead to this cell
        PathFinding.printScores("At begining explore");

        // Heuristic based on Vision
        long[] scores = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        long[] lookScores = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        for(Direction dir : Direction.values()){
            if(dir != Direction.CENTER){
                scores[dir.ordinal()] = VisionUtils.getScoreInView(myLoc.add(dir), dir, unitType);
                lookScores[dir.ordinal()] = VisionUtils.getScoreInView(myLoc, dir, unitType);
            }
        }
        PathFinding.addScoresWithNormalization(scores, 1);


        // Take best look direction
        Direction bestLookDir = Direction.CENTER;
        long bestLookScore = 0;
        for(Direction dir : directions){
            long score = lookScores[dir.ordinal()];
            if(score > bestLookScore){
                bestLookScore = score;
                bestLookDir = dir;
            }
        }

        // Take best move direction
        long bestMoveScore = 0;
        for(Direction dir : directions){
            long score = scores[dir.ordinal()];
            if(score > bestMoveScore){
                bestMoveScore = score;
            }
        }

        // Turn to direction
        if(rc.canTurn() && bestLookDir != Direction.CENTER && bestLookScore > bestMoveScore * 12 / 10){
            rc.turn(bestLookDir);
        }

        // Move to best dir
        Result result = PathFinding.moveBest();
        Result resultTurn = VisionUtils.smartLook();
        return new Result(OK, "Move result : " + result.msg + " Turn result : " + resultTurn.msg);

        // TODOS: Maybe turn, and then, according to new infos, restart from beginning ?
        // TODOS: Check if you need to move after turning
        // TODOS: Check if second score parameters is pertinent
        // TODOS: Check if not moving when second direction is nice, is good choice (can allow us to just tourn arround and then move)
    };
}
