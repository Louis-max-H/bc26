package current.States;

import battlecode.common.*;
import current.Communication.SenseForComs;
import current.Params;
import current.Robots.Robot;
import current.Utils.BugNavLmx;
import current.Utils.Micro;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;
import current.Communication.Communication;

import static current.Communication.Communication.TYPE_KING;
import static current.States.Code.*;
import static current.Communication.Communication.TYPE_CAT;

public class Init extends State {
    public Init() throws GameActionException {
        this.name = "Init";
        Robot.spawnLoc = rc.getLocation();
        Robot.spawnRound = rc.getRoundNum();
        Robot.isKing = rc.getType().isRatKingType();
        Robot.isFirstKing = rc.getType().isRatKingType();

        Robot.mapType = 0; // Small
        if(rc.getMapWidth() * rc.getMapHeight() > 32*32){
            mapType = 1; // Medium
        }
        if(rc.getMapWidth() * rc.getMapHeight() > 45*45){
            mapType = 2; // Big
        }

        // Init utils
        VisionUtils.initScore(rc.getMapWidth(), rc.getMapHeight());
        BugNavLmx.init(rc.getMapWidth(), rc.getMapHeight());
        Micro.init(rc);
    }

    @Override
    public Result run() throws GameActionException {
        // Called on new turns
        print("Update classic variables");
        round = rc.getRoundNum();
        lastInitRound = round;
        isKing = rc.getType().isRatKingType();
        myLoc = rc.getLocation();
        forceMovingEndOfTurn = true;

        // Utils
        PathFinding.resetScores();
        VisionUtils.resetDirections();

        // Reset kings every 10 turn, like that, if one die, it will be cleared
        if(rc.getRoundNum() % 10 == 0){
            kings.clear();
        }

        // Game phase
        if      (round <  100) { gamePhase = PHASE_START;}
        else if (round <  500) { gamePhase = PHASE_EARLY;}
        else if (round < 1500) { gamePhase = PHASE_MIDLE;}
        else                   { gamePhase = PHASE_FINAL;}
        Params.init();

        printBytecode("Update communications");
        Communication.readMessages(); // Read messsages

        printBytecode("Update pathFinding costs");
        VisionUtils.updatePathfindingCost(rc.getLocation(), rc.getDirection(), rc.getType());

        printBytecode("Update vision score state");
        /**
         * We want to give a score of interest to each cell. Default is 21000 = 2000 * 10 + 1000 = score(turn = 0) + 1000
         *
         * When the unit can see a cell, we give it a score according to the actual turn : score(t).
         * We want to have score(t1) > score(t2) iff t1 < t2   (the score is bigger if last visit is a turn from long ago)
         *
         * Imagine, it's 2D we can have
         * [t-4][t-3][ t ] < rat [t-1][000]
         * We want the rat to go checkout the cells at t-3 rather than checking again t-1.
         * Thus, 7 * score(t-3) + 22 * score(t) > 29 score(t-1)
         * Thus score(t) > 29 score(t-1) - 7 score(t-3)
         * */

        // Reset score if we can view the cell
        int scoreTurn = (2000 - round) * 100;
        VisionUtils.setScoreInRatVision(rc.getLocation(), rc.getDirection(), scoreTurn);

        // Add penalty if some rats are already looking in this direction
        for(RobotInfo info: rc.senseNearbyRobots(-1, rc.getTeam())){
            if(info.type == UnitType.RAT_KING){continue;}
            VisionUtils.divideScoreBy2InRatVision(info.getLocation(), info.getDirection());
        }


        printBytecode("Init with sensing");

        // debug("Sensing: ally");
        for(RobotInfo info : rc.senseNearbyRobots(-1, rc.getTeam())){
            if(info.type == UnitType.RAT_KING) {
                kings.add(info.location, info.getID());
            }

            // Baby rats are added using squeaks
        }

        // Sensing enemy
        // debug("Sensing: Update enemy position (king, rats)");
        for (RobotInfo info : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
            if(info.type == UnitType.RAT_KING) {
                enemiesKings.add(info.location, info.getID());
            } else {
                enemiesRats.add(info.location, info.getID());
            }
        }

        // Sensing Neutral (cat
        // debug("Sensing: Neutral (cat)");
        for (RobotInfo info : rc.senseNearbyRobots(-1, Team.NEUTRAL)) {
            cats.add(info.location, info.getID());
        }

        // Update sensing item
        if(nearestCheese != null && rc.canSenseLocation(nearestCheese) && rc.senseMapInfo(nearestCheese).getCheeseAmount() == 0){
            nearestCheese = null;
        }
        if(nearestDirt != null && rc.canSenseLocation(nearestDirt) && !rc.senseMapInfo(nearestDirt).isDirt()){
            nearestDirt = null;
        }

        // Sensing map info
        // debug("Sensing: mapinfo (mines, cheese, dirt, ...)");
        for(MapInfo info : rc.senseNearbyMapInfos()){
            // Dirt
            if(info.isDirt() && (nearestDirt == null || myLoc.distanceSquaredTo(info.getMapLocation()) < myLoc.distanceSquaredTo(nearestDirt))){
                nearestDirt = info.getMapLocation();
            }

            // Cheese
            if(info.getCheeseAmount() > 0 && (nearestCheese == null || myLoc.distanceSquaredTo(info.getMapLocation()) < myLoc.distanceSquaredTo(nearestCheese))){
                nearestCheese = info.getMapLocation();
            }

            // Cheese mine
            if(info.hasCheeseMine()){
                cheeseMines.add(info.getMapLocation());
            }
        }

        printBytecode("Update nearest units");
        int maxDistance = 10800;


        // debug("Nearest: king");
        int i = kings.nearestAndClear(myLoc, maxDistance);
        if(i == -1){
            nearestKing = null;
        }else{
            nearestKing = kings.locs[i];
            nearestKingID = kings.ids[i];
        }


        // debug("Nearest: enemy king");
        i = enemiesKings.nearestAndClear(myLoc, maxDistance);
        if(i == -1){
            nearestEnemyKing = null;
        }else{
            nearestEnemyKing = enemiesKings.locs[i];
            nearestEnemyKingID = enemiesKings.ids[i];
        }

        // debug("Nearest: cat");
        i = cats.nearestAndClear(myLoc, 200);
        if(i == -1){
            nearestCat = null;
        }else{
            nearestCat = cats.locs[i];
            nearestCatID = cats.ids[i];
        }

        // debug("Nearest: rat");
        i = alliesRats.nearestAndClear(myLoc, 100);
        if(i == -1){
            nearestAllyRat = null;
        }else{
            nearestAllyRat = enemiesRats.locs[i];
            nearestAllyRatID = enemiesRats.ids[i];
        }

        // debug("Nearest: enemy rat");
        i = enemiesRats.nearestAndClear(myLoc, 100);
        if(i == -1){
            nearestEnemyRat = null;
        }else{
            nearestEnemyRat = enemiesRats.locs[i];
            nearestEnemyRatID = enemiesRats.ids[i];
        }

        // debug("Nearest: cheese mine");
        i = cheeseMines.nearest(myLoc);
        if(i == -1){
            nearestMine = null;
        }else{
            nearestMine = cheeseMines.locs[i];
        }

        /// // debug lines
        /*if(nearestKing != null){
            rc.setIndicatorLine(rc.getLocation(), nearestKing, 0, 10, 10);
        }*/
        /*if(nearestCat != null){
            rc.setIndicatorLine(rc.getLocation(), nearestCat, 45, 105, 199);
        }*/
        /*
        if(nearestEnemyKing != null){
            rc.setIndicatorLine(rc.getLocation(), nearestEnemyKing, 50, 0, 0);
        }*/

        /*if(nearestMine != null){
            rc.setIndicatorLine(rc.getLocation(), nearestMine, 0, 255, 0);
        }*/
        /*
        if(nearestEnemyRat != null){
            rc.setIndicatorLine(rc.getLocation(), nearestEnemyRat, 255, 0, 0);
        }*/

        /*if(isKing){
            for (int j = 0; j < cheeseMines.size; j++) {
                rc.setIndicatorLine(rc.getLocation(), cheeseMines.locs[j], 0, 0, 255);
            }
        }*/

        /*
        for (int j = 0; j < kings.size; j++) {
            rc.setIndicatorLine(rc.getLocation(), kings.locs[j], 0, 255, 0);
        }*/

        return new Result(OK, "");
    }
}
