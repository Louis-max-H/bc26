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
            Communication.addMessageKing(myLoc, rc.getID());
        }

        // Give position of cats
        if(nearestCat != null && rc.canSenseLocation(nearestCat) && round % 2 == 0){
            RobotInfo infos = rc.senseRobotAtLocation(nearestCat);
            Communication.addMessageCat(infos.getLocation(), infos.getID());
        }

        // Give position of enemy king
        if(nearestEnemyKing != null && rc.canSenseLocation(nearestEnemyKing) && round % 2 == 0){
            RobotInfo infos = rc.senseRobotAtLocation(nearestKing);
            Communication.addMessageEnemyKing(infos.getLocation(), infos.getID());
        }

        // Give position of nearest enemy king
        if(nearestEnemyKing != null && rc.canSenseLocation(nearestEnemyKing) && round % 2 == 0){
            RobotInfo infos = rc.senseRobotAtLocation(nearestEnemyKing);
            Communication.addMessageEnemyKing(infos.getLocation(), infos.getID());
        }

        // Give position of enemy rat
        if(nearestEnemyRat != null && rc.canSenseLocation(nearestEnemyRat) && round % 2 == 0){
            RobotInfo infos = rc.senseRobotAtLocation(nearestEnemyRat);
            Communication.addMessageEnemyKing(infos.getLocation(), infos.getID());
        }

        // Send mines infos every 10 turn
        // TODO: Also send mines info if near king to give him the info
        // TODO: Will I send message of nearby mine already sended by the king ???
        if(MessageLIFO.buffer2.size == 0){ // Wait for MessageFIFO to be empty before repopulate it
            for (char i = 0; i < cheeseMines.size; i++) {
                Communication.addMessageMine(cheeseMines.locs[i]);
            }
        }
    }
}
