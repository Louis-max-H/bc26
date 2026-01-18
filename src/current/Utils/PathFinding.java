package current.Utils;

import battlecode.common.*;
import current.Robots.Robot;
import current.States.Result;

import static current.States.Code.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class PathFinding {
    static int scores[]; // Contain score for each direction

    //////////////////////////////////// public functions //////////////////////////////////////////////////////////////
    /// All functions you may need

    public static void resetScores(){
        // Default score of 1 everywhere
        scores = new int[]{100_000, 100_000, 100_000, 100_000, 100_000, 100_000, 100_000, 100_000, 0};
    }
    public static boolean digEnable = true;

    public static void addCanMoveConstraint(){
        RobotController rc = Robot.rc;
        MapLocation myLoc = Robot.rc.getLocation();

        // Check if we can dig
        if(rc.getActionCooldownTurns() > 0 || rc.getAllCheese() < GameConstants.DIG_DIRT_CHEESE_COST){
            digEnable = false;
        }

        // Without dig : King, no action, or not enough cheese to mine
        if(Robot.isKing || !digEnable){
            for(Direction dir: Direction.values()){
                if(!rc.canMove(dir)){
                    scores[dir.ordinal()] = 0;
                }
            }
            return;
        }

        // Check if we can move or dig
        for(Direction dir : Direction.values()){
            // If we have a wall, can't dig
            MapLocation loc = myLoc.add(dir);
            if(BugNavLmx.mapCosts[loc.x + loc.y * 60] == BugNavLmx.SCORE_CELL_WALL) {
                scores[dir.ordinal()] = 0;
            }
        }
    }

    public static Direction bestDir() {
        // Best dir where we can move
        addCanMoveConstraint();

        // Take best score
        int bestScore = scores[Direction.NORTH.ordinal()] - 100_000; // Small malus
        Direction bestDir = Direction.CENTER;
        for(Direction dir: Direction.values()){
            if(bestScore < scores[dir.ordinal()]){
                bestScore = scores[dir.ordinal()];
                bestDir = dir;
            }
        }

        // Debug
        Robot.debug("Exploring:");
        Robot.debug("              " +  scores[Direction.NORTH.ordinal()]);
        Robot.debug(String.format("%10s  ↖️⬆️↗️  %10s", scores[Direction.NORTHWEST.ordinal()], scores[Direction.NORTHEAST.ordinal()]));
        Robot.debug(String.format("%10s  ⬅️⏹️➡️  %10s", scores[Direction.WEST.ordinal()], scores[Direction.EAST.ordinal()]));
        Robot.debug(String.format("%10s  ↙️⬇️↘️  %10s", scores[Direction.SOUTHWEST.ordinal()], scores[Direction.SOUTHEAST.ordinal()]));
        Robot.debug("              " +  scores[Direction.SOUTH.ordinal()] + "     CENTER : " + scores[Direction.CENTER.ordinal()]);

        // move
        return bestDir;
    }

    // Smart movement using BugNav when direct path is blocked
    // Uses BugNav algorithm inspired by Battlecode 2024 chenyx512 (US_QUAL)
    public static Result smartMoveTo(MapLocation loc) throws GameActionException {
        if (loc == null) {
            return new Result(ERR, "No target location");
        }
        
        // First try, bugnav of Louis-Max
        Direction bugNavDir = BugNavLmx.pathTo(
            Robot.rc.getLocation(), loc,
            null, // mapScore = null to use memory
            BugNavLmx.SCORE_CELL_PASSABLE * 30, // Max 30 cells
                (digEnable) ? BugNavLmx.SCORE_CELL_IF_DIG : BugNavLmx.SCORE_CELL_PASSABLE, // Allow digging
            max(1000, min(Clock.getBytecodesLeft() - 1000, 6000)) // Number bytecode used
        );

        if(bugNavDir == null || bugNavDir == Direction.CENTER){
            System.out.println("BugNavLmx return null or center, trying BugNavChenyx512");
            bugNavDir = BugNavChenyx512.bugNavGetMoveDir(loc);
        }

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
            return new Result(ERR, "Can't move to center");
        }

        // If dirt, turn to the direction and remove dirt
        int xy = locMove.x + 60 * locMove.y;
        if(VisionUtils.scores[xy] == BugNavLmx.SCORE_CELL_IF_DIG){
            if(rc.canTurn()){
                rc.turn(dir);
                rc.removeDirt(locMove);
            }else{
                return new Result(WARN, "Need to see " + dir + " to remove dirt and move to this cell");
            }
        }

        // Try move
        if(rc.canMove(dir)){
            Robot.lastLocation = Robot.myLoc;
            Robot.lastDirection = dir;

            rc.move(dir);
            Robot.myLoc = Robot.myLoc.add(dir);
            return new Result(OK, "Moved to " + dir.toString());
        } else {
            return new Result(CANT, "Can't move to " + dir.toString());
        }
    }

    //////////////////////////////////// Modificator ///////////////////////////////////////////////////////////////////

    public static void modificatorOrientation(Direction dir){
        // Boost score toward direction. Set to 0 if not toward.
        scores[dir.rotateLeft().ordinal()] *= 4;
        scores[dir.ordinal()] += 100_000;
        scores[dir.ordinal()] *= 8;
        scores[dir.rotateRight().ordinal()] *= 4;

        Direction opposite = dir.opposite();
        scores[opposite.rotateLeft().rotateLeft().ordinal()] *= 2;
        scores[opposite.rotateLeft().ordinal()] *= 1;
        scores[opposite.ordinal()] /= 2;
        scores[opposite.rotateRight().ordinal()] *= 1;
        scores[opposite.rotateRight().rotateRight().ordinal()] *= 2;

        scores[Direction.CENTER.ordinal()] = 0;
    }

    public static void modificatorHortogonal(){
        scores[Direction.NORTH.ordinal()] *= 2;
        scores[Direction.EAST.ordinal()]  *= 2;
        scores[Direction.SOUTH.ordinal()] *= 2;
        scores[Direction.WEST.ordinal()]  *= 2;
    }

    //////////////////////////////////// Heuristic  ////////////////////////////////////////////////////////////////////

    // No heuristic for now
    // Maybe move the one from Explore here ?

    // You don't need to go further this point
    //////////////////////////////////// Scoring ///////////////////////////////////////////////////////////////////////

    public static void addScoresWithNormalization(int[] newScores, int coef){
        int max = Tools.maxInt9(newScores);
        if(max == 0){return;}

        /** Normalize to    200.000
         *  Max int is 2147.483.647
         *               20.000.000 with coef of 100
         *             2000.000.000 with 100 different coefs
         *
         *  Values should not exceed coef*normalize/2, or we will have normalization = 0
         * */
        int normalize = 200_000 * coef / max;
        Robot.print("Normalize coef : " + normalize);
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

    public static void addScoresWithoutNormalization(int[] newScores){
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
