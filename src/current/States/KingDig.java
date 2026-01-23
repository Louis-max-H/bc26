package current.States;

import battlecode.common.*;
import current.Robots.Robot;
import current.Utils.PathFinding;

import static current.States.Code.*;

public class KingDig extends State {
    public KingDig(){
        this.name = "KingDig";
    }

    @Override
    public Result run() throws GameActionException {
        // Action ready
        if(!rc.isActionReady()){
            return new Result(OK, "Action not ready");
        }

        // Dig somewhere ?
        MapLocation dirt = null;
        for(MapInfo infos: rc.senseNearbyMapInfos(8)){
            if(infos.isDirt() && rc.canRemoveDirt(infos.getMapLocation())){
                rc.removeDirt(infos.getMapLocation());
                return new Result(OK, "Dirt removed at " + infos.getMapLocation());
            }
        }

        return new Result(OK, "Can't remove dirt");
    };
}
