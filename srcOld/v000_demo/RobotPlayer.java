package v000_demo;

import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            for(MapInfo info: rc.senseNearbyMapInfos()){
                if(info.hasCheeseMine()){
                    System.out.println("Mine found at " + info.getMapLocation());
                    rc.resign();
                }
            }

            try {
                rc.turn(Direction.WEST);
                rc.move(Direction.WEST);
            } catch (GameActionException e) {
            }

            Clock.yield();
        }
    }
}
