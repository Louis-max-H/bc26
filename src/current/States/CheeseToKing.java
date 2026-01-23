package current.States;

import battlecode.common.*;
import current.Robots.King;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;

import static current.States.Code.*;

public class CheeseToKing extends State {
    public CheeseToKing(){
        this.name = "CheeseToKing";
    }

    private Direction bestDirectionCovering(MapLocation target){
        MapLocation from = rc.getLocation();
        UnitType unitType = rc.getType();
        Direction bestDir = rc.getDirection();
        int bestScore = Integer.MIN_VALUE;
        for(Direction dir : VisionUtils.directionsToSeeTarget(target, from)){
            if(dir == Direction.CENTER){
                continue;
            }
            int score = VisionUtils.getScoreInView(from, dir, unitType);
            if(score > bestScore){
                bestScore = score;
                bestDir = dir;
            }
        }
        return bestDir;
    }

    @Override
    public Result run() throws GameActionException {

        // Check if we have cheese
        if(rc.getRawCheese() == 0){
            return new Result(OK, "");
        }

        // Check if we have a king to go
        if(nearestKing == null){
            return new Result(WARN, "I have no king to drop cheese");
        }

        // Check if we can sense location, and if so, check if king
        // -> It's part of nearestKing function to check correct value

        // Turn to king to transfert
        if(rc.getLocation().distanceSquaredTo(nearestKing) <= GameConstants.CHEESE_TRANSFER_RADIUS_SQUARED){
            if(rc.canTurn()){
                Direction lookDir = bestDirectionCovering(nearestKing);
                if(lookDir != Direction.CENTER && rc.canTurn(lookDir)){
                    rc.turn(lookDir);
                }else{
                    Direction directDir = rc.getLocation().directionTo(nearestKing);
                    if(rc.canTurn(directDir)){
                        rc.turn(directDir);
                    }
                }
            }
        }

        // Try to transfer
        if(rc.canTransferCheese(nearestKing, rc.getRawCheese())){
            rc.transferCheese(nearestKing, rc.getRawCheese());
            return new Result(OK, "Cheese transferred!");
        }

        print("Moving to king at " + nearestKing);
        PathFinding.smartMoveTo(nearestKing);

        // Try to transfer
        if(rc.canTransferCheese(nearestKing, rc.getRawCheese())){
            rc.transferCheese(nearestKing, rc.getRawCheese());
            return new Result(OK, "Cheese transferred!");
        }

        return new Result(END_OF_TURN, "Can't transfer cheese");
    }
}
