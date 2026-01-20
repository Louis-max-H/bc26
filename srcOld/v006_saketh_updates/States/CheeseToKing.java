package v006_saketh_updates.States;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;
import v006_saketh_updates.Robots.King;
import v006_saketh_updates.Utils.PathFinding;

import static v006_saketh_updates.States.Code.*;

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

        // Check if we can sense king
        if(rc.canSenseLocation(nearestKing)){
            if(!rc.canSenseRobotAtLocation(nearestKing)){
                nearestKing = null;
                return new Result(OK, "No unit at nearestKing");
            }

            RobotInfo unit = rc.senseRobotAtLocation(nearestKing);
            if(unit.type != UnitType.RAT_KING || unit.team != rc.getTeam()){
                nearestKing = null;
                return new Result(OK, "Unit at nearestKing is not a king");
            }
        }

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
