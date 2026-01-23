package current.States;

import battlecode.common.*;
import current.Robots.Robot;
import current.Utils.PathFinding;

import static current.States.Code.*;

public class MoveKing extends State {
    private static final int SAFE_DISTANCE_KING_SQUARED = 25;
    private static final int MINE_THREAT_DISTANCE_SQUARED = 144;
    private static final int ENEMY_THREAT_DISTANCE_SQUARED = 25;
    private static final int MINE_ENEMY_DISTANCE_SQUARED = 36;
    /**
     * Explore use a init in Init state.
     * We have a big array of interrest, and we move/turn to the zone with the biggest score.
     * */

    public MoveKing(){
        this.name = "MoveKing";
    }

    @Override
    public Result run() throws GameActionException {

        // If only one king, dont move
        if(Robot.kings.size <= 1){
            return new Result(OK, "Only one king, playing safe");
        }

        // Move only if we have the lower id in kings
        boolean canMove = false;
        int myId = rc.getID() % 4096;
        for(int i = 0; i < Robot.kings.size; i++){
            if(Robot.kings.ids[i] < myId){
                canMove = true;
                break;
            }
        }
        if(!canMove){
            return new Result(OK, "I am not the lowest ID, not moving");
        }

        // Check if we can move and turn
        if(!rc.isMovementReady()){
            return new Result(CANT, "Can't move");
        }

        boolean onMine = nearestMine != null && nearestMine.equals(myLoc);
        if(nearestCat != null){
            int catDist = myLoc.distanceSquaredTo(nearestCat);
            int safetyDist = onMine ? MINE_THREAT_DISTANCE_SQUARED : SAFE_DISTANCE_KING_SQUARED;
            if(catDist <= safetyDist){
                long[] scores = new long[9];
                for(Direction dir : Direction.values()){
                    if(dir == Direction.CENTER){
                        scores[dir.ordinal()] = 0;
                        continue;
                    }
                    scores[dir.ordinal()] = myLoc.add(dir).distanceSquaredTo(nearestCat);
                }
                PathFinding.addScoresWithNormalization(scores, 12);
                Result result = PathFinding.moveBest();
                return new Result(result.code, "Avoiding cat at " + nearestCat + " (" + result.msg + ")");
            }
        }

        MapLocation nearestThreat = null;
        int nearestThreatDist = Integer.MAX_VALUE;
        if(nearestEnemyRat != null){
            int dist = myLoc.distanceSquaredTo(nearestEnemyRat);
            if(dist < nearestThreatDist){
                nearestThreatDist = dist;
                nearestThreat = nearestEnemyRat;
            }
        }
        if(nearestEnemyKing != null){
            int dist = myLoc.distanceSquaredTo(nearestEnemyKing);
            if(dist < nearestThreatDist){
                nearestThreatDist = dist;
                nearestThreat = nearestEnemyKing;
            }
        }
        if(nearestThreat != null && nearestThreatDist <= ENEMY_THREAT_DISTANCE_SQUARED){
            long[] scores = new long[9];
            for(Direction dir : Direction.values()){
                if(dir == Direction.CENTER){
                    scores[dir.ordinal()] = 0;
                    continue;
                }
                scores[dir.ordinal()] = myLoc.add(dir).distanceSquaredTo(nearestThreat);
            }
            PathFinding.addScoresWithNormalization(scores, 12);
            Result result = PathFinding.moveBest();
            return new Result(result.code, "Avoiding enemy at " + nearestThreat + " (" + result.msg + ")");
        }

        MapLocation targetMine = nearestMine;
        if(cheeseMines.size > 0 && (nearestCat != null || nearestEnemyRat != null || nearestEnemyKing != null)){
            MapLocation safeMine = null;
            int bestDist = Integer.MAX_VALUE;
            for(char i = 0; i < cheeseMines.size; i++){
                MapLocation mine = cheeseMines.locs[i];
                if(mine == null){
                    continue;
                }
                if(nearestCat != null && mine.distanceSquaredTo(nearestCat) <= MINE_THREAT_DISTANCE_SQUARED){
                    continue;
                }
                if(nearestEnemyRat != null && mine.distanceSquaredTo(nearestEnemyRat) <= MINE_ENEMY_DISTANCE_SQUARED){
                    continue;
                }
                if(nearestEnemyKing != null && mine.distanceSquaredTo(nearestEnemyKing) <= MINE_ENEMY_DISTANCE_SQUARED){
                    continue;
                }
                int dist = myLoc.distanceSquaredTo(mine);
                if(dist < bestDist){
                    bestDist = dist;
                    safeMine = mine;
                }
            }
            if(safeMine != null){
                targetMine = safeMine;
            } else if(targetMine != null && nearestCat != null && targetMine.distanceSquaredTo(nearestCat) <= MINE_THREAT_DISTANCE_SQUARED){
                targetMine = null;
            }
        }
        if(targetMine != null){
            if(nearestEnemyRat != null && targetMine.distanceSquaredTo(nearestEnemyRat) <= MINE_ENEMY_DISTANCE_SQUARED){
                targetMine = null;
            } else if(nearestEnemyKing != null && targetMine.distanceSquaredTo(nearestEnemyKing) <= MINE_ENEMY_DISTANCE_SQUARED){
                targetMine = null;
            }
        }

        if(targetMine != null && targetMine.equals(myLoc)){
            targetMine = null;
        }

        // Prefer moving toward known cheese mines.
        if(targetMine != null){
            Result result = PathFinding.smartMoveTo(targetMine);
            return new Result(result.code, "Moving to mine " + targetMine + " (" + result.msg + ")");
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
