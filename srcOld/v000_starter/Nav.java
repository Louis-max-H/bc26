package v000_starter;
import battlecode.common.*;

public class Nav {
    public static final Direction[] DIRS = new Direction[] {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    public static int dist2(MapLocation a, MapLocation b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    public static Direction dirTo(MapLocation from, MapLocation to) {
        int dx = Integer.compare(to.x, from.x);
        int dy = Integer.compare(to.y, from.y);

        if (dx == 0 && dy == 0) return Direction.CENTER;
        if (dx == 0 && dy == 1) return Direction.NORTH;
        if (dx == 1 && dy == 1) return Direction.NORTHEAST;
        if (dx == 1 && dy == 0) return Direction.EAST;
        if (dx == 1 && dy == -1) return Direction.SOUTHEAST;
        if (dx == 0 && dy == -1) return Direction.SOUTH;
        if (dx == -1 && dy == -1) return Direction.SOUTHWEST;
        if (dx == -1 && dy == 0) return Direction.WEST;
        return Direction.NORTHWEST;
    }

    public static void goTo(RobotController rc, MapLocation target) throws GameActionException {
        if (target == null) return;

        MapLocation here = rc.getLocation();
        Direction want = dirTo(here, target);
        if (want == Direction.CENTER) return;

        if (tryStep(rc, want)) return;

        int idx = dirIndex(want);
        for (int k = 1; k <= 3; k++) {
            Direction left = DIRS[(idx + 8 - k) % 8];
            Direction right = DIRS[(idx + k) % 8];
            if (tryStep(rc, left)) return;
            if (tryStep(rc, right)) return;
        }

        if (rc.isTurningReady() && rc.getDirection() != want) rc.turn(want);
    }

    static boolean tryStep(RobotController rc, Direction dir) throws GameActionException {
        if (dir == Direction.CENTER) return false;

        if (!rc.isMovementReady()) {
            if (rc.isTurningReady() && rc.getDirection() != dir) {
                rc.turn(dir);
                return true;
            }
            return false;
        }

        if (rc.getDirection() != dir && rc.isTurningReady()) rc.turn(dir);

        if (rc.getDirection() == dir && rc.canMoveForward()) {
            rc.moveForward();
            return true;
        }

        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        return false;
    }

    static int dirIndex(Direction d) {
        for (int i = 0; i < 8; i++) if (DIRS[i] == d) return i;
        return 0;
    }
}