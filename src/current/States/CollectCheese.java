package current.States;

import battlecode.common.*;
import current.Robots.Robot;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;

import static current.States.Code.*;

public class CollectCheese extends State {
    public CollectCheese(){this.name = "CollectCheese";}

    public MapLocation cheeseLoc;
    public MapLocation targetMine; // Target cheese mine to stay near
    private static final int MINE_STAY_RADIUS_SQUARED = 9; // Stay within 3 tiles of mine

    @Override
    public Result run() throws GameActionException {

        if(nearestCheese == null){
            if(targetMine == null && Robot.cheeseMines.size > 0){
                int minDist = Integer.MAX_VALUE;
                MapLocation anchor = Robot.isCheeseEmergency() && nearestKing != null ? nearestKing : myLoc;
                for(char i = 0; i < Robot.cheeseMines.size; i++){
                    MapLocation mine = Robot.cheeseMines.locs[i];
                    if(mine == null){
                        continue;
                    }
                    int dist = anchor.distanceSquaredTo(mine);
                    if(dist < minDist){
                        minDist = dist;
                        targetMine = mine;
                    }
                }
            }

            if(targetMine != null){
                int distToMine = myLoc.distanceSquaredTo(targetMine);
                if(distToMine > MINE_STAY_RADIUS_SQUARED){
                    PathFinding.smartMoveTo(targetMine);
                    return new Result(END_OF_TURN, "Moving to mine at " + targetMine);
                }
                return new Result(END_OF_TURN, "Holding near mine " + targetMine);
            }

            return new Result(OK, "No cheese nearby");
        }

        // Try pickup
        if(rc.getLocation().distanceSquaredTo(nearestCheese) <= 2){
            Result r = VisionUtils.smartLookAt(nearestCheese);
            if(r.notOk()){
                return new Result(r.code, "Looking at cheese : " + r.msg);
            }

            if(rc.canPickUpCheese(nearestCheese)){
                rc.pickUpCheese(nearestCheese);
                return new Result(END_OF_TURN, "Cheese picked up at "  + nearestCheese);
            }

            return new Result(WARN, "Can't pickupt cheese");
        }

        // Move to cheese
        PathFinding.smartMoveTo(nearestCheese);

        // Try pickup
        if(rc.getLocation().distanceSquaredTo(nearestCheese) <= 2){
            Result r = VisionUtils.smartLookAt(nearestCheese);
            if(r.notOk()){
                return new Result(r.code, "Looking at cheese : " + r.msg);
            }

            if(rc.canPickUpCheese(nearestCheese)){
                rc.pickUpCheese(nearestCheese);
                return new Result(END_OF_TURN, "Cheese picked up at "  + nearestCheese);
            }

            return new Result(WARN, "Can't pickupt cheese");
        }

        rc.setIndicatorLine(rc.getLocation(), nearestCheese, 184, 163, 51);
        return new Result(END_OF_TURN, "Moving to cheese at " + nearestCheese);

/*        
        if(true){
            return new Result(CANT, "Need to check this function");
        }

        // Priority 1: Find nearest cheese mine if we don't have a target
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
                PathFinding.addScoresWithoutNormalization(penaltyScores);
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
        */
    };
}
