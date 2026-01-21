package current.Communication;

import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
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

        // Late game rush coordination: Send enemy king location as rush target after round 1500
        if(round >= 1500 && nearestEnemyKing != null && !rc.isCooperation()){
            // In late game backstab, prioritize enemy king as rush target
            // This message will be sent with high priority to coordinate attack
            Communication.addMessageEnemyKing(nearestEnemyKing, nearestEnemyKingID);
            // Mark as "found something nice" - enemy king in late game is high priority target
        }

        // Mark "found something nice" for interesting discoveries:
        // - Large cheese amounts (>20)
        // - Enemy king (always interesting)
        // - Multiple enemies nearby
        boolean foundSomethingNice = false;
        if(nearestEnemyKing != null){
            foundSomethingNice = true; // Enemy king is always interesting
        }
        if(nearestCheese != null && rc.canSenseLocation(nearestCheese)){
            MapInfo cheeseInfo = rc.senseMapInfo(nearestCheese);
            if(cheeseInfo.getCheeseAmount() > 20){
                foundSomethingNice = true; // Large cheese amount is interesting
            }
        }
        // Multiple enemies nearby is also interesting
        if(nearestEnemyRat != null){
            int enemyCount = 0;
            for(RobotInfo enemy : rc.senseNearbyRobots(-1, rc.getTeam().opponent())){
                if(enemy.getType() != UnitType.RAT_KING){
                    enemyCount++;
                }
            }
            if(enemyCount >= 3){
                foundSomethingNice = true; // Multiple enemies is interesting
            }
        }

        // Store "found something nice" flag for use in squeak encoding
        // (This could be added as a bit flag in the message if needed)
        if(foundSomethingNice){
            // For now, we'll just ensure important messages are sent with higher priority
            // The actual bit encoding would require modifying the message format
        }
    }
}
