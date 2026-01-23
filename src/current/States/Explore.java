package current.States;

import battlecode.common.*;
import current.Utils.BugNavLmx;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;

import java.util.Random;

import static current.States.Code.*;
import static java.lang.Math.sqrt;

public class Explore extends State {
    MapLocation targetExplore;
    int lastTargetRoundSet;
    int initialDistance;


    MapLocation target;
    public Explore(){
        this.name = "Explore";
    
    
        // Init target explore using orientation given at spawn
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        targetExplore = switch(rc.getDirection()){
            case Direction.NORTHEAST -> new MapLocation(height - 1, width - 1);
            case Direction.SOUTHEAST -> new MapLocation(0, width - 1);
            case Direction.SOUTHWEST -> new MapLocation(0, 0);
            case Direction.NORTHWEST -> new MapLocation(0, height / 2);
            case Direction.NORTH     -> new MapLocation(width / 2, height - 1);
            case Direction.SOUTH     -> new MapLocation(width / 2, 0);
            case Direction.EAST      -> new MapLocation(width - 1, height / 2);
            case Direction.WEST      -> new MapLocation(height - 1, 0);
            case Direction.CENTER    -> null;
        };

        initialDistance = (int)sqrt(rc.getLocation().distanceSquaredTo(targetExplore));
        lastTargetRoundSet = rc.getRoundNum();
    }
    
    @Override
    public Result run() throws GameActionException {
        // Check if we can move
        if(rc.getMovementCooldownTurns() != 0){
            return new Result(CANT, "Can't move");
        }

        // Check if we can turn
        if(rc.getTurningCooldownTurns() != 0){
            return new Result(CANT, "Can't turn");
        }

        if(targetExplore == null || lastTargetRoundSet + 100 < rc.getRoundNum()){
            for (int i = 0; i < cheeseMines.size; i++) {
                MapLocation loc = cheeseMines.locs[i];

                // Not depleted ?
                if(isMineDepleted[loc.x + loc.y * 60] <= rc.getRoundNum()){
                    // Nearest ?
                    if(targetExplore == null || myLoc.distanceSquaredTo(loc) < myLoc.distanceSquaredTo(targetExplore)){
                        targetExplore = loc;
                    }
                }
            }
        }

        // Add bonus to target
        if (targetExplore != null) {
            Direction dir = PathFinding.BugNavLmx(targetExplore);
            if(BugNavLmx.resultCode <= -10 || dir == null){
                if(targetExplore != null){
                    isMineDepleted[targetExplore.x + targetExplore.y * 60] = (char) (rc.getRoundNum() + 150);
                }
                targetExplore = null;
                print("BugNavLmx return " + BugNavLmx.resultCode + ", targetExplore set to null");
            }else{
                PathFinding.modificatorOrientationSoft(dir, 5); // Coef 5
            }
        }

        // For each nearby cells, add their heuristic to the direction that lead to this cell
        UnitType unitType = rc.getType();
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

        // Move to best dir
        Result result = PathFinding.moveBest();

        // Turn to direction
        if(rc.canTurn() && bestLookDir != Direction.CENTER && bestLookScore > bestMoveScore * 12 / 10){
            rc.turn(bestLookDir);
        }
        Result resultTurn = VisionUtils.smartLook();
        return new Result(OK, "Move result : " + result.msg + " Turn result : " + resultTurn.msg);

        // TODOS: Maybe turn, and then, according to new infos, restart from beginning ?
        // TODOS: Check if you need to move after turning
        // TODOS: Check if second score parameters is pertinent
        // TODOS: Check if not moving when second direction is nice, is good choice (can allow us to just tourn arround and then move)
    };
}
