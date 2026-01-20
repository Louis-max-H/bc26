package testMapSize;

import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        int mapType = 0; // Small
        if(rc.getMapWidth() * rc.getMapHeight() > 32*32){
            mapType = 1; // Medium
        }
        if(rc.getMapWidth() * rc.getMapHeight() > 45*45){
            mapType = 2; // Big
        }

        switch(mapType){
            case 0:
                System.out.println("MAPTYPE: MAP_SMALL");
                break;
            case 1:
                System.out.println("MAPTYPE: MAP_MEDIUM");
                break;
            case 2:
                System.out.println("MAPTYPE: MAP_LARGE");
                break;
        }
        rc.resign();
    }
}
