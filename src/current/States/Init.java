package current.States;

import battlecode.common.*;
import current.Communication.SenseForComs;
import current.Robots.Robot;
import current.Utils.BugNavLmx;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;
import current.Communication.Communication;

import static current.Communication.Communication.TYPE_KING;
import static current.Robots.Robot.rats;
import static current.States.Code.*;
import static current.Communication.Communication.TYPE_CAT;

public class Init extends State {
    public Init() throws GameActionException {
        this.name = "Init";
        Robot.spawnLoc = rc.getLocation();
        Robot.spawnRound = rc.getRoundNum();
        Robot.isKing = rc.getType().isRatKingType();

        // Init utils
        VisionUtils.initScore(rc.getMapWidth(), rc.getMapHeight());
        BugNavLmx.init(rc.getMapWidth(), rc.getMapHeight());
    }

    public boolean isInformationCorrect(MapLocation loc, int shortId) throws GameActionException {
        // If null loc
        if (loc == null){
            return false;
        }

        // If we can sense, trust the information
        if (!rc.canSenseRobotAtLocation(loc)){
            return true;
        }

        return rc.senseRobotAtLocation(loc).getID() % 4096 == shortId;
    }

    @Override
    public Result run() throws GameActionException {
        // Called on new turns

        print("Update classic variables");
        round = rc.getRoundNum();
        lastInitRound = round;
        isKing = rc.getType().isRatKingType();
        PathFinding.resetScores();
        myLoc = rc.getLocation();


        if      (round <  100) { gamePhase = PHASE_START;}
        else if (round <  500) { gamePhase = PHASE_EARLY;}
        else if (round < 1500) { gamePhase = PHASE_MIDLE;}
        else                   { gamePhase = PHASE_FINAL;}

        printBytecode("Update communications");
        Communication.readMessages(); // Read messsages

        printBytecode("Update pathFinding");
        VisionUtils.updatePathfindingCost(rc.getLocation(), rc.getDirection(), rc.getType());

        printBytecode("Update vision score state");
        /**
         * We want to give a score of interest to each cell. Default is 700
         * When the unit can see a cell, we give it a score according to the actual turn : score(t).
         * We want to have score(t1) > score(t2) iff t1 < t2   (the score is bigger if last visit is a turn from long ago)
         *
         * King can view 9*9 cells, with max char of 65536, max values is around 800, taking default value of 600
         *
         * We use score(t) = (2000 - t) / 7
         * score(0) ~ 285 << 700 and score(2000) = 0
         * */

        // Reset score if we can view the cell
        char scoreTurn = (char)((2000 - round) / 28);
        VisionUtils.setScoreInRatVision(myLoc, rc.getDirection(), scoreTurn);

        // Add penalty if some rats are already looking in this direction
        for(RobotInfo info: rc.senseNearbyRobots(-1, rc.getTeam())){
            if(info.type == UnitType.RAT_KING){continue;}
            VisionUtils.divideScoreBy2InRatVision(info.getLocation(), info.getDirection());
            break; // Only one rat
        }


        printBytecode("Init with sensing");
        // Clear data
        enemiesRats.clear();
        rats.clear();

        debug("Sensing: ally");
        for(RobotInfo info : rc.senseNearbyRobots(-1, rc.getTeam())){
            if(info.type == UnitType.RAT_KING) {
                kings.add(info.location, info.getID());
            } else {
                rats.add(info.location);
            }
        }

        // Sensing enemy
        debug("Sensing: Update enemy position (king, rats)");
        for (RobotInfo info : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
            if(info.type == UnitType.RAT_KING) {
                enemiesKings.add(info.location, info.getID());
            } else {
                enemiesRats.add(info.location);
            }
        }

        // Sensing Neutral (cat
        debug("Sensing: Neutral (cat)");
        for (RobotInfo info : rc.senseNearbyRobots(-1, Team.NEUTRAL)) {
            cats.add(info.location, info.getID());
        }

        debug("Sensing: mapinfo (mines, cheese, dirt, ...)");
        for(MapInfo info : rc.senseNearbyMapInfos()){
            // Dirt
            if(info.isDirt() && (nearestDirt == null || nearestDirt.distanceSquaredTo(info.getMapLocation()) > nearestDirt.distanceSquaredTo(myLoc))){
                nearestDirt = info.getMapLocation();
            }

            // Cheese
            if(info.getCheeseAmount() > 0 && (nearestCheese == null || nearestCheese.distanceSquaredTo(info.getMapLocation()) > nearestCheese.distanceSquaredTo(myLoc))){
                nearestCheese = info.getMapLocation();
            }

            // Cheese mine
            if(info.hasCheeseMine()){
                cheeseMines.add(info.getMapLocation());
            }
        }

        int i;
        int bestDistance;
        printBytecode("Update nearest units");

        debug("Nearest: king");
        // Update nearest king if can see it but not here
        if(!isInformationCorrect(nearestKing, nearestKingID)){
            print("King info not correct");
            nearestKing = null;
            nearestKingID = -1;
        }

        // Update nearest king
        i = 0;
        bestDistance = 99999;
        while (i < kings.size) {
            // TODO: lmx, move this code directly to struct to save bytecode
            if(!isInformationCorrect(kings.locs[i], kings.ids[i])){
                kings.remove(kings.ids[i]);
                continue;
            }

            if(myLoc.distanceSquaredTo(kings.locs[i]) < bestDistance){
                nearestKing = kings.locs[i];
                nearestKingID = kings.ids[i];
                bestDistance = myLoc.distanceSquaredTo(kings.locs[i]);
            }

            i++;
        }

        debug("Nearest: enemy king");
        // Update nearest enemy king if can see it but not here
        if(!isInformationCorrect(nearestEnemyKing, nearestEnemyKingID)){
            nearestEnemyKing = null;
            nearestEnemyKingID = -1;
        }

        // Update nearest
        i = 0;
        bestDistance = 99999;   
        while (i < enemiesKings.size) {
            if(!isInformationCorrect(enemiesKings.locs[i], enemiesKings.ids[i])){
                enemiesKings.remove(enemiesKings.ids[i]);
                continue;
            }

            if(myLoc.distanceSquaredTo(enemiesKings.locs[i]) < bestDistance){
                nearestEnemyKing = enemiesKings.locs[i];
                nearestEnemyKingID = enemiesKings.ids[i];
                bestDistance = myLoc.distanceSquaredTo(enemiesKings.locs[i]);
            }

            i++;
        }

        debug("Nearest: cat");
        // Update nearest enemy king if can see it but not here
        if(!isInformationCorrect(nearestCat, nearestCatID)){
            nearestCat = null;
            nearestCatID = -1;
        }

        // Update nearest
        i = 0;
        bestDistance = 99999;
        while (i < cats.size) {
            if(!isInformationCorrect(cats.locs[i], cats.ids[i])){
                cats.remove(cats.ids[i]);
                continue;
            }

            if(myLoc.distanceSquaredTo(cats.locs[i]) < bestDistance){
                nearestCat = cats.locs[i];
                nearestCatID = cats.ids[i];
                bestDistance = myLoc.distanceSquaredTo(cats.locs[i]);
            }

            i++;
        }


        if(nearestKing != null){
            rc.setIndicatorLine(rc.getLocation(), nearestKing, 0, 10, 10);
        }
        if(nearestCat != null){
            rc.setIndicatorLine(rc.getLocation(), nearestCat, 45, 105, 199);
        }
        if(nearestEnemyKing != null){
            rc.setIndicatorLine(rc.getLocation(), nearestEnemyKing, 50, 0, 0);
        }
        if(nearestMine != null){
            rc.setIndicatorLine(rc.getLocation(), nearestMine, 255, 228, 181);
        }
        if(nearestEnemyRat != null){
            rc.setIndicatorLine(rc.getLocation(), nearestEnemyRat, 20, 0, 0);
        }


        return new Result(OK, "");
    }
}
