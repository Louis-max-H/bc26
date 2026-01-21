package v008_saketh.States;

import battlecode.common.*;
import v008_saketh.Communication.SenseForComs;
import v008_saketh.Params;
import v008_saketh.Robots.Robot;
import v008_saketh.Utils.BugNavLmx;
import v008_saketh.Utils.PathFinding;
import v008_saketh.Utils.VisionUtils;
import v008_saketh.Communication.Communication;

import static v008_saketh.Communication.Communication.TYPE_KING;
import static v008_saketh.Robots.Robot.rats;
import static v008_saketh.States.Code.*;
import static v008_saketh.Communication.Communication.TYPE_CAT;
import v008_saketh.Communication.Communication;

public class Init extends State {
    public Init() throws GameActionException {
        this.name = "Init";
        Robot.spawnLoc = rc.getLocation();
        Robot.spawnRound = rc.getRoundNum();
        Robot.isKing = rc.getType().isRatKingType();

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

        // For kings: Place 3-4 cat traps around spawn location at game start
        // Use spawnRound to check if it's early game (round is not initialized in constructor)
        if(isKing && Robot.spawnRound == rc.getRoundNum() && rc.isActionReady()){
            int trapsPlaced = 0;
            int maxTraps = 4;
            MapLocation spawn = Robot.spawnLoc;
            
            // Place cat traps in a pattern around spawn (only during cooperation)
            if(rc.isCooperation()){
                for(int dx = -2; dx <= 2 && trapsPlaced < maxTraps; dx++){
                    for(int dy = -2; dy <= 2 && trapsPlaced < maxTraps; dy++){
                        if(dx == 0 && dy == 0) continue; // Skip center
                        if(Math.abs(dx) + Math.abs(dy) > 2) continue; // Only within distance 2
                        
                        MapLocation trapLoc = new MapLocation(spawn.x + dx, spawn.y + dy);
                        if(rc.canSenseLocation(trapLoc) && rc.canPlaceCatTrap(trapLoc) && rc.getAllCheese() >= 10){
                            rc.placeCatTrap(trapLoc);
                            trapsPlaced++;
                        }
                    }
                }
            }
        }
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
        myLoc = rc.getLocation();

        // Track health changes to detect attacks from behind
        int currentHealth = rc.getHealth();
        if(currentHealth < Robot.lastHealth && !isKing){
            // We took damage - check if we can see any enemy that could have attacked
            boolean canSeeAttacker = false;
            for(RobotInfo enemy : rc.senseNearbyRobots(-1, rc.getTeam().opponent())){
                if(enemy.getType() != UnitType.RAT_KING && myLoc.distanceSquaredTo(enemy.getLocation()) <= 2){
                    canSeeAttacker = true;
                    break;
                }
            }
            // Also check for cats
            if(!canSeeAttacker){
                for(RobotInfo cat : rc.senseNearbyRobots(-1, Team.NEUTRAL)){
                    if(myLoc.distanceSquaredTo(cat.getLocation()) <= 2){
                        canSeeAttacker = true;
                        break;
                    }
                }
            }
            Robot.wasAttackedFromBehind = !canSeeAttacker;
        } else {
            Robot.wasAttackedFromBehind = false;
        }
        Robot.lastHealth = currentHealth;

        // Utils
        PathFinding.resetScores();
        VisionUtils.resetDirections();


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
            } else {
                rats.add(info.location);
            }
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

        // Track king count to detect new king creation
        if(isKing){
            int currentKingCount = 0;
            MapLocation newKingLoc = null;
            for(RobotInfo info : rc.senseNearbyRobots(-1, rc.getTeam())){
                if(info.getType() == UnitType.RAT_KING){
                    currentKingCount++;
                    // If this is a new king (not our own location), record it
                    if(!info.getLocation().equals(myLoc) && !info.getLocation().equals(nearestKing)){
                        newKingLoc = info.getLocation();
                    }
                }
            }
            // If we see more kings than before, a new king was created
            if(currentKingCount > Robot.lastKingCount && newKingLoc != null){
                // Send message about new king creation
                Communication.addMessageKing(newKingLoc, 0); // ID 0 indicates new king
                Robot.lastKingCount = currentKingCount;
                print("New king detected at " + newKingLoc + ", sending message");
            } else if(currentKingCount != Robot.lastKingCount){
                Robot.lastKingCount = currentKingCount;
            }
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
        if(nearestEnemyRat != null){
            rc.setIndicatorLine(rc.getLocation(), nearestEnemyRat, 255, 0, 0);
        }


        return new Result(OK, "");
    }
}
