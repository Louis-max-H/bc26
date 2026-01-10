package starter;
import java.util.Random;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Util {
    static int clamp(int v, int lo, int hi){
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    static boolean inMap(RobotController rc, MapLocation m) {
        return m.x >= 0 && m.y >= 0 && m.x < rc.getMapWidth() && m.y < rc.getMapHeight();
    }

    static int cheb(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    static MapLocation away(RobotController rc, MapLocation from, MapLocation threat, int step) {
        int dx = Integer.compare(from.x, threat.x);
        int dy = Integer.compare(from.y, threat.y);
        if (dx == 0 && dy == 0) dx = 1;
        int x = clamp(from.x + dx * step, 0, rc.getMapWidth() - 1);
        int y = clamp(from.y + dy * step, 0, rc.getMapHeight() - 1);
        return new MapLocation(x, y);
    }

    static MapLocation extendLine(RobotController rc, MapLocation a, MapLocation b, int stepBeyond) {
        int dx = Integer.compare(b.x, a.x);
        int dy = Integer.compare(b.y, a.y);
        int x = clamp(b.x + dx * stepBeyond, 0, rc.getMapWidth() - 1);
        int y = clamp(b.y + dy * stepBeyond, 0, rc.getMapHeight() - 1);
        return new MapLocation(x, y);
    }

    static MapLocation randomInSector(RobotController rc, Random rng, int sector) {
        int w = rc.getMapWidth();
        int h = rc.getMapHeight();
        int cx = w / 2;
        int cy = h / 2;
        int x0 = (sector == 0 || sector == 2) ? 0 : cx;
        int x1 = (sector == 0 || sector == 2) ? cx - 1 : w - 1;
        int y0 = (sector == 0 || sector == 1) ? 0 : cy;
        int y1 = (sector == 0 || sector == 1) ? cy - 1 : h - 1;
        int x = x0 + rng.nextInt(Math.max(1, x1 - x0 + 1));
        int y = y0 + rng.nextInt(Math.max(1, y1 - y0 + 1));
        return new MapLocation(x, y);
    }
}
