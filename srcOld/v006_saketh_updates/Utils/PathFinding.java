package v006_saketh_updates.Utils;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import v006_saketh_updates.Robots.Robot;
import v006_saketh_updates.States.Result;

import static v006_saketh_updates.States.Code.CANT;
import static v006_saketh_updates.States.Code.OK;

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


    // BugNav pathfinding inspired by Battlecode 2024 chenyx512 (US_QUAL)
    // Uses wall-following algorithm to navigate around obstacles
    private static MapLocation bugNavTarget = null;
    private static DirectionStack bugNavStack = new DirectionStack();
    private static int bugNavTurnDir = 0; // 0 = left, 1 = right
    private static final int MAX_BUGNAV_DEPTH = 8;

    // Simple direction stack for BugNav
    static class DirectionStack {
        int size = 0;
        Direction[] dirs = new Direction[20];

        void clear() {
            size = 0;
        }

        void push(Direction d) {
            if (size < dirs.length) {
                dirs[size++] = d;
            }
        }

        Direction top() {
            return size > 0 ? dirs[size - 1] : null;
        }

        Direction top(int n) {
            return size >= n ? dirs[size - n] : null;
        }

        void pop() {
            if (size > 0) size--;
        }

        void pop(int n) {
            size = Math.max(0, size - n);
        }
    }

    // Reset BugNav when target changes
    private static void resetBugNav(MapLocation newTarget) {
        if (bugNavTarget == null || !newTarget.equals(bugNavTarget)) {
            bugNavTarget = newTarget;
            bugNavStack.clear();
            bugNavTurnDir = 0; // Default to left turn
        }
    }

    // Check if we can move in a direction (simplified for BC26)
    private static boolean canMoveDir(Direction dir) throws GameActionException {
        if (dir == Direction.CENTER) return false;
        return Robot.rc.canMove(dir);
    }

    // BugNav algorithm - returns direction to move, or null if stuck
    private static Direction bugNavGetMoveDir(MapLocation target) throws GameActionException {
        resetBugNav(target);
        
        MapLocation myLoc = Robot.myLoc;
        Direction directDir = myLoc.directionTo(target);
        
        // If we can move directly toward target, do it
        if (canMoveDir(directDir)) {
            bugNavStack.clear(); // Clear stack if we can move directly
            return directDir;
        }
        
        // Try adjacent directions
        Direction dirL = directDir.rotateLeft();
        Direction dirR = directDir.rotateRight();
        if (canMoveDir(dirL)) {
            bugNavStack.clear();
            return dirL;
        }
        if (canMoveDir(dirR)) {
            bugNavStack.clear();
            return dirR;
        }
        
        // Obstacle encountered - use BugNav wall-following
        if (bugNavStack.size == 0) {
            // Start wall-following: rotate around obstacle
            Direction dir = directDir;
            while (!canMoveDir(dir) && bugNavStack.size < MAX_BUGNAV_DEPTH) {
                MapLocation nextLoc = myLoc.add(dir);
                if (!Robot.rc.onTheMap(nextLoc)) {
                    bugNavTurnDir ^= 1; // Switch turn direction
                    bugNavStack.clear();
                    return null; // Can't move
                }
                bugNavStack.push(dir);
                dir = bugNavTurnDir == 0 ? dir.rotateLeft() : dir.rotateRight();
            }
            if (bugNavStack.size < MAX_BUGNAV_DEPTH) {
                return dir;
            }
        } else {
            // Continue wall-following
            // If we can move in direction 2 steps ahead, pop 2 (optimization)
            if (bugNavStack.size > 1 && canMoveDir(bugNavStack.top(2))) {
                bugNavStack.pop(2);
            }
            // Pop directions we can now move in
            while (bugNavStack.size > 0 && canMoveDir(bugNavStack.top())) {
                bugNavStack.pop();
            }
            if (bugNavStack.size == 0) {
                // Try direct path again
                if (canMoveDir(directDir)) return directDir;
                if (canMoveDir(dirL)) return dirL;
                if (canMoveDir(dirR)) return dirR;
                bugNavStack.push(directDir);
            }
            // Continue rotating around obstacle
            Direction curDir;
            while (bugNavStack.size > 0 && !canMoveDir(curDir = (bugNavTurnDir == 0 ? bugNavStack.top().rotateLeft() : bugNavStack.top().rotateRight()))) {
                MapLocation nextLoc = myLoc.add(curDir);
                if (!Robot.rc.onTheMap(nextLoc)) {
                    bugNavTurnDir ^= 1;
                    bugNavStack.clear();
                    return null;
                }
                bugNavStack.push(curDir);
                if (bugNavStack.size >= MAX_BUGNAV_DEPTH) {
                    bugNavStack.clear();
                    return null;
                }
            }
            Direction moveDir = bugNavStack.size == 0 ? directDir : (bugNavTurnDir == 0 ? bugNavStack.top().rotateLeft() : bugNavStack.top().rotateRight());
            if (canMoveDir(moveDir)) {
                return moveDir;
            }
        }
        return null;
    }

    // Try to move in a direction, with fallback to adjacent directions
    // Inspired by chenyx512's tryMoveDir
    private static Result tryMoveDirWithFallback(Direction dir) throws GameActionException {
        if (!Robot.rc.isMovementReady() || dir == Direction.CENTER) {
            return new Result(CANT, "Can't move");
        }
        if (canMoveDir(dir)) {
            return moveDir(dir);
        } else if (canMoveDir(dir.rotateRight())) {
            return moveDir(dir.rotateRight());
        } else if (canMoveDir(dir.rotateLeft())) {
            return moveDir(dir.rotateLeft());
        }
        return new Result(CANT, "Can't move in any direction");
    }

    public static Direction dirTo(MapLocation loc){
        return Robot.myLoc.directionTo(loc);
    }

    // Smart movement using BugNav when direct path is blocked
    // Uses BugNav algorithm inspired by Battlecode 2024 chenyx512 (US_QUAL)
    public static Result smartMoveTo(MapLocation loc) throws GameActionException {
        if (loc == null) {
            return new Result(CANT, "No target location");
        }
        
        // Try BugNav first for obstacle avoidance
        Direction bugNavDir = bugNavGetMoveDir(loc);
        if (bugNavDir != null) {
            return tryMoveDirWithFallback(bugNavDir);
        }
        
        // Fallback to orientation-based movement
        modificatorOrientation(dirTo(loc));
        return moveBest();
    }

    public static Result moveBest() throws GameActionException {
        return moveDir(bestDir());
    }

    public static Result moveDir(Direction dir) throws GameActionException {
        RobotController rc = Robot.rc;
        if(rc.canMove(dir)){
            Robot.lastLocation = Robot.myLoc;
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
