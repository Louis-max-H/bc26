package current.Communication;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;
import current.Robots.Robot;

public class SenseForComs extends Robot {
    public static void senseForComs() throws GameActionException {
        // Will look around and add somes messages to debug
        // Could have been done in Communication.java but split apart to avoid big class

        // Give position of our king
        if(isKing){
            Communication.addMessageKing(myLoc, rc.getID(), PRIORITY_CRIT);
        }

        // MessageLIFO.buffer2 PRIORITY_CRIT   = 3; // King being attacked, rush order
        // MessageLIFO.buffer1 PRIORITY_HIGH   = 2; // Enemy in view, cat position
        // MessageLIFO.buffer0 PRIORITY_NORMAL = 1; // King position, cheese mine

        // If no messages, create news ones
        if(MessageLIFO.buffer1.size == 0 && MessageLIFO.buffer2.size == 0) {
            // Give position of cats
            if(nearestCat != null){
                Communication.addMessageCat(nearestCat, nearestCatID, PRIORITY_HIGH);
            }

            // Give position of nearest enemy king
            if (nearestEnemyKing != null) {
                Communication.addMessageEnemyKing(nearestEnemyKing, nearestEnemyKingID, PRIORITY_HIGH);
            }

            // Give position of enemy rat
            if (nearestEnemyRat != null) {
                Communication.addMessageEnemyRat(nearestEnemyRat, nearestEnemyRatID, PRIORITY_HIGH);
            }

            // Send mines infos
            for (char i = 0; i < cheeseMines.size; i++) {
                if (!cheeseMinesFromArray.contains(cheeseMines.locs[i])) {
                    Communication.addMessageMine(cheeseMines.locs[i], PRIORITY_HIGH);
                }
            }
        }
    }
}
