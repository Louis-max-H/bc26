package current.Utils;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import current.Robots.Robot;
import current.States.Result;

import static current.States.Code.CANT;
import static current.States.Code.OK;

public class PathFinding {
    static int scores[]; // Contain score for each direction

    //////////////////////////////////// public functions //////////////////////////////////////////////////////////////
    /// All functions you may need

    public static void resetScores(){
        scores = new int[9];
    }

    public static Direction bestDir() {
        // Take best score, 0 if can't move
        int bestScore = 0;
        Direction bestDir = Direction.CENTER;
        for(Direction dir: Direction.values()){
            if(!Robot.rc.canMove(dir)){
                scores[dir.ordinal()] = 0;
                continue;
            }

            if(bestScore < scores[dir.ordinal()]){
                bestScore = scores[dir.ordinal()];
                bestDir = dir;
            }
        }

        // Debug
        Robot.debug("Exploring:");
        Robot.debug(" ⬆️ " + scores[Direction.NORTH.ordinal()]);
        Robot.debug(" ↗️ " + scores[Direction.NORTHEAST.ordinal()]);
        Robot.debug(" ➡️ " + scores[Direction.EAST.ordinal()]);
        Robot.debug(" ↘️ " + scores[Direction.SOUTHEAST.ordinal()]);
        Robot.debug(" ⬇️ " + scores[Direction.SOUTH.ordinal()]);
        Robot.debug(" ↙️ " + scores[Direction.SOUTHWEST.ordinal()]);
        Robot.debug(" ⬅️ " + scores[Direction.WEST.ordinal()]);
        Robot.debug(" ↖️ " + scores[Direction.NORTHWEST.ordinal()]);
        Robot.debug(" ⏹️ " + scores[Direction.CENTER.ordinal()]);

        // move
        return bestDir;
    }


    // TODOS: Use Bugnav
    public static Direction dirTo(MapLocation loc){
        return Robot.rc.getLocation().directionTo(loc);
    }

    public static Result smartMoveTo(MapLocation loc) throws GameActionException {
        modificatorOrientation(PathFinding.dirTo(loc));
        return moveBest();
    }

    public static Result moveBest() throws GameActionException {
        return moveDir(bestDir());
    }

    public static Result moveDir(Direction dir) throws GameActionException {
        RobotController rc = Robot.rc;
        if(rc.canMove(dir)){
            Robot.lastLocation = rc.getLocation();
            Robot.lastDirection = dir;

            rc.move(dir);
            return new Result(OK, "Moved to " + dir.toString());
        } else {
            return new Result(CANT, "Can't move to " + dir.toString());
        }
    }

    //////////////////////////////////// Modificator ///////////////////////////////////////////////////////////////////

    public static void modificatorOrientation(Direction dir){
        // Boost score toward direction. Set to 0 if not toward.
        scores[dir.rotateLeft().ordinal()] /= 2;
        scores[dir.ordinal()] *= 1;
        scores[dir.rotateRight().ordinal()] /= 2;

        Direction opposite = dir.opposite();
        scores[opposite.rotateLeft().rotateLeft().ordinal()] = 0;
        scores[opposite.rotateLeft().ordinal()] = 0;
        scores[opposite.ordinal()] = 0;
        scores[opposite.rotateRight().ordinal()] = 0;
        scores[opposite.rotateRight().rotateRight().ordinal()] = 0;
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

    public static void addScoresWithoutNormalization(int[] newScores, int coef){
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
