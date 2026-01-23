package current.States;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import current.Communication.Communication;
import current.Params;

import java.util.Map;

import static current.States.Code.*;

public class KingCheese extends State {
    public KingCheese(){
        this.name = "KingCheese";
    }


    // Position where we can ask for new king
    public static int shiftX[] = {03, 03, 03, -3, -3, -3, 00, 03, -3, 00, 03, -3};
    public static int shiftY[] = {00, 03, -3, 00, 03, -3, 03, 03, 03, -3, -3, -3};


    @Override
    public Result run() throws GameActionException {
        for(MapInfo info: rc.senseNearbyMapInfos(8)){
            if(info.getCheeseAmount() > 0){
                if(rc.canPickUpCheese(info.getMapLocation())){
                    rc.pickUpCheese(info.getMapLocation());
                }
                return new Result(OK, "Cheese picked up");
            }
        }
        return new Result(OK, "No cheese nearby");
    };
}
