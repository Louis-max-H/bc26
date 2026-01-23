package current.Utils;

import battlecode.common.*;
import current.Robots.Robot;
import current.States.Result;

import java.util.Random;

import static current.States.Code.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class PathFinding {
    public static long scores[]; // Contain score for each direction
    public static long SCORE_BASIS = 100_000;
    //////////////////////////////////// public functions //////////////////////////////////////////////////////////////
    /// All functions you may need

    public static int numberMove = 0;
    public static boolean isLmxBugnav = false; // is lmxBugNav used ?

    public static void resetScores(){
        // Default score of 1 everywhere
        scores = new long[]{100_000, 100_000, 100_000, 100_000, 100_000, 100_000, 100_000, 100_000, 0};
    }
    public static boolean digEnable = true;

    public static void addCanMoveConstraint(){
        RobotController rc = Robot.rc;
        MapLocation myLoc = Robot.rc.getLocation();

        // TODO: Consider we can always dig
        // Check if we can dig
        // if(rc.getGlobalCheese() < GameConstants.DIG_DIRT_CHEESE_COST){
        //    digEnable = false;
        // }


        // Check all direction
        // printScores("Before move constrainte");
        for(Direction dir : Direction.values()){
            MapLocation loc = myLoc.add(dir);
            int cellType = BugNavLmx.mapCosts[loc.x + (loc.y<<7) + 129];

            if (cellType == BugNavLmx.SCORE_CELL_WALL) {
                scores[dir.ordinal()] = 0;
                continue;
            }

            if(!rc.canMove(dir)) {
                char cellType = BugNavLmx.mapCosts[loc.x + (loc.y<<7) + 128];

                // If we have a wall, can't dig
                if (cellType >= BugNavLmx.SCORE_CELL_WALL) {
                    scores[dir.ordinal()] = 0;

                // Dirt
                } else if (cellType == BugNavLmx.SCORE_CELL_IF_DIG) {
                    if(!digEnable) {
                        scores[dir.ordinal()] = 0;
                    }

                // Other (units)
                } else {
                    try{
                        rc.move(dir); // Will fail
                    }catch (GameActionException e){
                        if(e.getMessage().contains("is occupied by a different robot")){
                            System.out.println("Can't move because robot " + dir + " : " + e.getMessage());
                            scores[dir.ordinal()] = 0;
                        }
                    }
                }
            }
        }
    }

    public static void printScores(String msg){
        Robot.debug(msg + ":");
        Robot.debug("              " +  scores[Direction.NORTH.ordinal()]);
        Robot.debug(String.format("%10s  ↖️⬆️↗️  %10s", scores[Direction.NORTHWEST.ordinal()], scores[Direction.NORTHEAST.ordinal()]));
        Robot.debug(String.format("%10s  ⬅️⏹️➡️  %10s", scores[Direction.WEST.ordinal()], scores[Direction.EAST.ordinal()]));
        Robot.debug(String.format("%10s  ↙️⬇️↘️  %10s", scores[Direction.SOUTHWEST.ordinal()], scores[Direction.SOUTHEAST.ordinal()]));
        Robot.debug("              " +  scores[Direction.SOUTH.ordinal()] + "     CENTER : " + scores[Direction.CENTER.ordinal()]);

    }

    public static Direction bestDir() {
        // Best dir where we can move
        addCanMoveConstraint();

        // Take best score
        Direction backDir = Direction.CENTER;
        if (Robot.lastDirection != null) {
            backDir = Robot.lastDirection.opposite();
        } else if (Robot.lastLocation != null) {
            backDir = Robot.myLoc.directionTo(Robot.lastLocation);
        }

        long bestScore = scores[Direction.CENTER.ordinal()];
        Direction bestDir = Direction.CENTER;
        long secondBestScore = Long.MIN_VALUE;
        Direction secondBestDir = Direction.CENTER;
        for(Direction dir: Direction.values()){
            long score = scores[dir.ordinal()];
            if(score > bestScore){
                secondBestScore = bestScore;
                secondBestDir = bestDir;
                bestScore = score;
                bestDir = dir;
            } else if (dir != bestDir && score > secondBestScore){
                secondBestScore = score;
                secondBestDir = dir;
            }
        }

        // Avoid immediate backtracking unless it's clearly the best option.
        if (backDir != Direction.CENTER && bestDir == backDir) {
            if (secondBestDir != Direction.CENTER && secondBestScore > 0 && secondBestScore * 13 >= bestScore * 10) {
                bestDir = secondBestDir;
                bestScore = secondBestScore;
            }
        }

        // Prefer continuing in the same direction when scores are close.
        if (Robot.lastDirection != null && Robot.lastDirection != Direction.CENTER) {
            long continueScore = scores[Robot.lastDirection.ordinal()];
            if (continueScore > 0 && continueScore * 10 >= bestScore * 9) {
                bestDir = Robot.lastDirection;
                bestScore = continueScore;
            }
        }

        // Debug
        Robot.debug("Best direction:");
        Robot.debug("              " +  scores[Direction.NORTH.ordinal()]);
        Robot.debug(String.format("%10s  ↖️⬆️↗️  %10s", scores[Direction.NORTHWEST.ordinal()], scores[Direction.NORTHEAST.ordinal()]));
        Robot.debug(String.format("%10s  ⬅️⏹️➡️  %10s", scores[Direction.WEST.ordinal()], scores[Direction.EAST.ordinal()]));
        Robot.debug(String.format("%10s  ↙️⬇️↘️  %10s", scores[Direction.SOUTHWEST.ordinal()], scores[Direction.SOUTHEAST.ordinal()]));
        Robot.debug("              " +  scores[Direction.SOUTH.ordinal()] + "     CENTER : " + scores[Direction.CENTER.ordinal()]);

        // move
        return bestDir;
    }

    public static Direction BugNavLmx(MapLocation loc) throws GameActionException {
        // SCORE_CELL_WALL : 60000
        // SCORE_CELL_IF_DIG :2001
        // If unit : rc.getRound

        return BugNavLmx.pathTo(
                Robot.rc.getLocation(), loc,
                BugNavLmx.SCORE_CELL_IF_DIG * 30, // Max 30 cells
                BugNavLmx.SCORE_CELL_IF_DIG, // Avoid units
                max(1000, min(Clock.getBytecodesLeft() - 2000, 6000)) // Number bytecode used
        );
    }

    // Smart movement using BugNav when direct path is blocked
    // Uses BugNav algorithm inspired by Battlecode 2024 chenyx512 (US_QUAL)
    public static Result smartMoveTo(MapLocation loc) throws GameActionException {
        if (loc == null) {
            return new Result(ERR, "No target location");
        }
        if (loc.equals(Robot.rc.getLocation())) {
            return new Result(OK, "Already at target");
        }

        // First try, bugnav of Louis-Max
        Direction bugNavDir = BugNavLmx.pathTo(
            Robot.rc.getLocation(), loc,
            BugNavLmx.SCORE_CELL_PASSABLE * 30, // Max 30 cells
                BugNavLmx.SCORE_CELL_IF_DIG, // Avoid units
            max(1000, min(Clock.getBytecodesLeft() - 2000, 6000)) // Number bytecode used
        );
        isLmxBugnav = true;

        // Fallback to Chenyx512 if failed
        if(bugNavDir == null || bugNavDir == Direction.CENTER){
            System.out.println("BugNavLmx return null or center, trying BugNavChenyx512");
            Robot.rc.setIndicatorLine(Robot.rc.getLocation(), loc, 255, 0, 255);
            isLmxBugnav = false;
            bugNavDir = BugNavChenyx512.bugNavGetMoveDir(loc);
        }

        // Fallback to direction if still fail
        if (bugNavDir == null) {
            System.out.println("BugNavChenyx512 return null, taking direct direction");
            bugNavDir = Robot.myLoc.directionTo(loc);
        }

        modificatorOrientation(bugNavDir);
        return moveBest();
    }

    public static Result moveBest() throws GameActionException {
        return moveDir(bestDir());
    }

    public static Result moveDir(Direction dir) throws GameActionException {
        RobotController rc = Robot.rc;
        MapLocation locMove = rc.getLocation().add(dir);

        // Can't move center
        if(dir == Direction.CENTER){
            return new Result(WARN, "Can't move to center");
        }

        // If dirt, turn to the direction and remove dirt
        int xy = locMove.x + (locMove.y<<7) + 129;
        Robot.print("Score at loc is " + (int)BugNavLmx.mapCosts[xy]);
        if(BugNavLmx.mapCosts[xy] == BugNavLmx.SCORE_CELL_IF_DIG){
            Robot.print("Try diging dirt at " + locMove);

            VisionUtils.smartLookAt(locMove);
            if(rc.canRemoveDirt(locMove)){
                Robot.print("Dirt removed ! ");
                rc.removeDirt(locMove);
            }else{
                Robot.print("Can't :'( ");
            }
        }

        return move(dir);
    }

    public static Result move(Direction dir) throws GameActionException {
        numberMove++;
        if(Robot.moveRandom && numberMove % 15 == 0){
            // use math random to get a random direction
            int randomDir = (int) (Math.random() * 8);
            dir = Direction.values()[randomDir];
        }

        if(!Robot.rc.canMove(dir)){
            if(Robot.rc.canTurn()){
                Robot.rc.turn(dir);
            }
        }

        if(Robot.rc.canMove(dir)){
            Robot.lastLocation = Robot.myLoc;
            Robot.lastDirection = dir;

            Robot.rc.move(dir);
            Robot.myLoc = Robot.myLoc.add(dir);
            return new Result(OK, "Moved to " + dir.toString());
        } else {
            return new Result(CANT, "Can't move to " + dir.toString());
        }
    }

    //////////////////////////////////// Modificator ///////////////////////////////////////////////////////////////////

    public static void modificatorOrientation(Direction dir){
        // Add bonus for moving
        scores[0] += 100_000;
        scores[1] += 100_000;
        scores[2] += 100_000;
        scores[3] += 100_000;
        scores[4] += 100_000;
        scores[5] += 100_000;
        scores[6] += 100_000;
        scores[7] += 100_000;

        // Boost score toward direction
        scores[dir.rotateLeft().ordinal()] *= 4;
        scores[dir.ordinal()] *= 8;
        scores[dir.rotateRight().ordinal()] *= 4;

        Direction opposite = dir.opposite();
        scores[opposite.rotateLeft().rotateLeft().ordinal()] *= 2;
        scores[opposite.rotateLeft().ordinal()] *= 1;
        scores[opposite.ordinal()] /= 2;
        scores[opposite.rotateRight().ordinal()] *= 1;
        scores[opposite.rotateRight().rotateRight().ordinal()] *= 2;
    }

    public static void modificatorOrientationSoft(Direction dir){
        // Add bonus for moving
        scores[0] += 100_000;
        scores[1] += 100_000;
        scores[2] += 100_000;
        scores[3] += 100_000;
        scores[4] += 100_000;
        scores[5] += 100_000;
        scores[6] += 100_000;
        scores[7] += 100_000;

        // Boost score toward direction
        scores[dir.rotateLeft().ordinal()] += 350_000 ;
        scores[dir.ordinal()] += 400_000;
        scores[dir.rotateRight().ordinal()] += 350_000;

        Direction opposite = dir.opposite();
        scores[opposite.rotateLeft().rotateLeft().ordinal()] += 250_000;
        scores[opposite.rotateLeft().ordinal()] += 150_000;
        scores[opposite.ordinal()] += 50_000;
        scores[opposite.rotateRight().ordinal()] += 150_000;
        scores[opposite.rotateRight().rotateRight().ordinal()] += 250_000;
    }

    //////////////////////////////////// Heuristic  ////////////////////////////////////////////////////////////////////

    // No heuristic for now
    // Maybe move the one from Explore here ?

    // You don't need to go further this point
    //////////////////////////////////// Scoring ///////////////////////////////////////////////////////////////////////

    public static void addScoresWithNormalization(long[] newScores, long coef){
        long max = Tools.maxLong9(newScores);
        if(max == 0){return;}

        /** Normalize to 100.000.000
         * */
        printScores("Before adding score WithNormalization");
        long normalize = 100_000_000 * coef / max;
        // Robot.print("Noramize with coef : " + coef + " and max : " + max + " -> " + (normalize));
        // Robot.print("Adding : i=" + 0 + " " + newScores[0] * normalize);
        // Robot.print("Adding : i=" + 1 + " " + newScores[1] * normalize);
        // Robot.print("Adding : i=" + 2 + " " + newScores[2] * normalize);
        // Robot.print("Adding : i=" + 3 + " " + newScores[3] * normalize);
        // Robot.print("Adding : i=" + 4 + " " + newScores[4] * normalize);
        // Robot.print("Adding : i=" + 5 + " " + newScores[5] * normalize);
        // Robot.print("Adding : i=" + 6 + " " + newScores[6] * normalize);
        // Robot.print("Adding : i=" + 7 + " " + newScores[7] * normalize);
        // Robot.print("Adding : i=" + 8 + " " + newScores[8] * normalize);
        scores[0] += newScores[0] * normalize;
        scores[1] += newScores[1] * normalize;
        scores[2] += newScores[2] * normalize;
        scores[3] += newScores[3] * normalize;
        scores[4] += newScores[4] * normalize;
        scores[5] += newScores[5] * normalize;
        scores[6] += newScores[6] * normalize;
        scores[7] += newScores[7] * normalize;
        scores[8] += newScores[8] * normalize;
    }

    public static void addScoresWithoutNormalization(long[] newScores){
        printScores("Before adding score Without Normalization");
        scores[0] += newScores[0];
        scores[1] += newScores[1];
        scores[2] += newScores[2];
        scores[3] += newScores[3];
        scores[4] += newScores[4];
        scores[5] += newScores[5];
        scores[6] += newScores[6];
        scores[7] += newScores[7];
        scores[8] += newScores[8];
    }
}
