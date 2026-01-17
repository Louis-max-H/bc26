package current.Utils;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import current.Robots.Robot;

public class BugNavChenyx512 {

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
    public static Direction bugNavGetMoveDir(MapLocation target) throws GameActionException {
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
}
