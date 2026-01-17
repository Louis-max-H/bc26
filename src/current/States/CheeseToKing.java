package current.States;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;
import current.Robots.King;
import current.Utils.PathFinding;

import static current.States.Code.*;

public class CheeseToKing extends State {
    public CheeseToKing(){
        this.name = "CheeseToKing";
    }

    @Override
    public Result run() throws GameActionException {

        // Check if we have cheese
        if(rc.getRawCheese() < 5){
            return new Result(OK, "");
        }

        // Check if we have a king to go
        if(nearestKing == null){
            return new Result(WARN, "I have no king to drop cheese");
        }

        // Check if we can sense location, and if so, check if king
        // -> It's part of nearestKing function to check correct value

        // Try to transfer
        if(rc.canTransferCheese(nearestKing, rc.getAllCheese())){
            rc.transferCheese(nearestKing, rc.getAllCheese());
            return new Result(OK, "Cheese transferred!");
        }

        print("Moving to king at " + nearestKing);
        PathFinding.smartMoveTo(nearestKing);

        // Try to transfer
        if(rc.canTransferCheese(nearestKing, rc.getAllCheese())){
            rc.transferCheese(nearestKing, rc.getAllCheese());
            return new Result(OK, "Cheese transferred!");
        }

        return new Result(END_OF_TURN, "Can't transfer cheese");
    }
}
