package params_dbe63b76.Communication;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;
import params_dbe63b76.Robots.Robot;

public class SenseForComs extends Robot {
    public static void senseForComs() throws GameActionException {
        // Will look around and add somes messages to debug
        // Could have been done in Communication.java but split apart to avoid big class

        // Give position of our king
        if(isKing){
            Communication.addMessageKing(myLoc, rc.getID());
        }

        // Give position of cats
        if(nearestCat != null){
            Communication.addMessageCat(nearestCat, nearestCatID);
        }

        // Give position of nearest enemy king
        if(nearestEnemyKing != null){
            Communication.addMessageEnemyKing(nearestEnemyKing, nearestEnemyKingID);
        }

        // Give position of enemy rat
        if(nearestEnemyRat != null){
            Communication.addMessageEnemyRat(nearestEnemyRat, nearestEnemyRatID);
        }

        // Send mines infos
        if(MessageLIFO.buffer2.size == 0){ // Wait for MessageFIFO to be empty before repopulate it
            for (char i = 0; i < cheeseMines.size; i++) {
                if(!cheeseMinesFromArray.contains(cheeseMines.locs[i])) {
                    Communication.addMessageMine(cheeseMines.locs[i]);
                }
            }
        }
    }
}
