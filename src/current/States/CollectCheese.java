package current.States;

import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import current.Utils.PathFinding;

import static current.States.Code.*;

public class CollectCheese extends State {
    public CollectCheese(){this.name = "CollectCheese";}

    public MapLocation cheeseLoc;

    @Override
    public Result run() throws GameActionException {
        // Check existing cheeseLoc
        if(cheeseLoc != null && rc.canSenseLocation(cheeseLoc) && rc.senseMapInfo(cheeseLoc).getCheeseAmount() > 0){
            print("Cheese at " + cheeseLoc + " have disappear :'(");
            cheeseLoc = null;
        }

        // Check for new cheese
        if(cheeseLoc == null){
            for(MapInfo infos: rc.senseNearbyMapInfos()){

                // We can access cheese
                if(infos.isWall() || infos.getCheeseAmount() == 0){
                    continue;
                }

                // Check if it's the nearest cheese
                if(cheeseLoc != null && rc.getLocation().distanceSquaredTo(cheeseLoc) < rc.getLocation().distanceSquaredTo(infos.getMapLocation())){
                    continue;
                }

                cheeseLoc = infos.getMapLocation();
            }
        }

        // Did we have a target ?
        if(cheeseLoc == null){
            return new Result(OK, "");
        }

        // Can I collect ?
        if(rc.canPickUpCheese(cheeseLoc)){
            rc.pickUpCheese(cheeseLoc);
            cheeseLoc = null;
            return new Result(OK, "Cheese picked up, miam");
        }

        print("Moving to cheese " +  cheeseLoc);
        PathFinding.smartMoveTo(cheeseLoc);

        // Can I collect?
        if(rc.canPickUpCheese(cheeseLoc)){
            rc.pickUpCheese(cheeseLoc);
            cheeseLoc = null;
            return new Result(OK, "Cheese picked up, miam");
        }

        return new Result(OK, "Can't pickup");
    };
}
