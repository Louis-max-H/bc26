package current.States;

import battlecode.common.*;
import current.Robots.Robot;
import current.Utils.PathFinding;

import static current.States.Code.*;

public class CollectCheese extends State {
    public CollectCheese(){this.name = "CollectCheese";}

    public MapLocation cheeseLoc;
    public MapLocation targetMine; // Target cheese mine to stay near
    private static final int MINE_STAY_RADIUS_SQUARED = 9; // Stay within 3 tiles of mine

    @Override
    public Result run() throws GameActionException {
        // Priority 1: If we have cheese, transfer to king
        if(rc.getRawCheese() > 0){
            return new Result(OK, "Have cheese, should transfer");
        }

        // Priority 2: Find nearest cheese mine if we don't have a target
        if(targetMine == null && Robot.cheeseMines.size > 0){
            int minDist = Integer.MAX_VALUE;
            for(char i = 0; i < Robot.cheeseMines.size; i++){
                MapLocation mine = Robot.cheeseMines.locs[i];
                int dist = myLoc.distanceSquaredTo(mine);
                if(dist < minDist){
                    minDist = dist;
                    targetMine = mine;
                }
            }
        }

        // If we have a target mine, stay nearby
        if(targetMine != null){
            int distToMine = myLoc.distanceSquaredTo(targetMine);
            
            // If too far from mine, move closer
            if(distToMine > MINE_STAY_RADIUS_SQUARED){
                PathFinding.resetScores();
                Direction dirToMine = myLoc.directionTo(targetMine);
                PathFinding.modificatorOrientation(dirToMine);
                // Penalize directions that move away from mine using negative scores
                int[] penaltyScores = new int[9];
                for(Direction dir : Direction.values()){
                    if(dir != Direction.CENTER){
                        MapLocation nextLoc = myLoc.add(dir);
                        int nextDist = nextLoc.distanceSquaredTo(targetMine);
                        if(nextDist > distToMine + 2){ // Moving away
                            penaltyScores[dir.ordinal()] = -9999;
                        }
                    }
                }
                PathFinding.addScoresWithoutNormalization(penaltyScores, 1);
                return PathFinding.moveBest();
            }
        }

        // Check for nearby cheese to collect
        if(cheeseLoc == null){
            for(MapInfo infos: rc.senseNearbyMapInfos()){
                if(infos.isWall() || infos.getCheeseAmount() == 0){
                    continue;
                }
                if(cheeseLoc == null || myLoc.distanceSquaredTo(cheeseLoc) > myLoc.distanceSquaredTo(infos.getMapLocation())){
                    cheeseLoc = infos.getMapLocation();
                }
            }
        }

        // Collect cheese if available
        if(cheeseLoc != null){
            if(rc.canPickUpCheese(cheeseLoc)){
                rc.pickUpCheese(cheeseLoc);
                cheeseLoc = null;
                return new Result(OK, "Cheese picked up");
            }
            PathFinding.smartMoveTo(cheeseLoc);
            if(rc.canPickUpCheese(cheeseLoc)){
                rc.pickUpCheese(cheeseLoc);
                cheeseLoc = null;
                return new Result(OK, "Cheese picked up after moving");
            }
        }

        // If no cheese nearby and we're at mine, explore around mine
        if(targetMine != null && myLoc.distanceSquaredTo(targetMine) <= MINE_STAY_RADIUS_SQUARED){
            return new Result(OK, "Staying near mine, waiting for cheese");
        }

        return new Result(OK, "No cheese to collect");
    };
}
