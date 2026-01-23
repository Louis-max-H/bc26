package current.States;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import current.Communication.Communication;
import current.Utils.BugNavLmx;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;

import static current.States.Code.*;

public class BecomeKing extends State {
    private static final int LOW_KING_HEALTH = 150;
    private static final int KING_LOOK_RADIUS_SQUARED = 36;
    private static final int KING_RALLY_RADIUS_SQUARED = 64;

    public BecomeKing() {
        this.name = "BecomeKing";
    }

    @Override
    public Result run() throws GameActionException {
        // If I can create a king, make it
        if(rc.canBecomeRatKing()){
            rc.becomeRatKing();
        }


        // No one asking
        if(nearestCallForKing == null){
            return new Result(OK, "No call for king nearby");
        }

        // Already 2 kings
        if(kings.size >= 2){
            nearestCallForKing = null;
            return new Result(OK, "Already many kings");
        }

        // Outdated
        if(nearestCallForKingTurn + 10 < rc.getRoundNum()){
            nearestCallForKing = null;
            return new Result(OK, "Call for king outdated");
        }

       // Too far
        if(myLoc.distanceSquaredTo(nearestCallForKing) > 64){
            nearestCallForKing = null;
            return new Result(OK, "Call for king too far");
        }

        forceMovingEndOfTurn = false;

        // if not in the square, move to it
        if(!myLoc.isWithinDistanceSquared(nearestCallForKing, 2)){
            PathFinding.smartMoveTo(nearestCallForKing);
            return new Result(OK, "Moving to call for king at " + nearestCallForKing);
        }

        // Add score to stay in the square
        long[] scores = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (Direction dir: directions) {
            scores[dir.ordinal()] = (nearestCallForKing.isAdjacentTo(myLoc.add(dir)) ? 1 : -1);
        }
        PathFinding.addScoresWithNormalization(scores, 100);
        PathFinding.moveBest();
        return new Result(OK, "Staying in call for king at " + nearestCallForKing);

        /*

        if (rc.getType().isRatKingType()) {
            return new Result(OK, "Already a king");
        }

        MapLocation kingLoc = nearestKing;
        boolean kingLow = false;
        if (kingLoc == null) {
            kingLow = true;
        } else {
            int kingDist = myLoc.distanceSquaredTo(kingLoc);
            if (!rc.canSenseLocation(kingLoc) && kingDist <= KING_LOOK_RADIUS_SQUARED) {
                VisionUtils.smartLookAt(kingLoc);
            }
            if (rc.canSenseLocation(kingLoc)) {
                RobotInfo info = rc.senseRobotAtLocation(kingLoc);
                if (info == null || !info.getType().isRatKingType()) {
                    kingLow = true;
                } else if (info.getHealth() <= LOW_KING_HEALTH) {
                    kingLow = true;
                }
            }
        }

        if (!kingLow) {
            return new Result(OK, "No low king nearby");
        }

        if (rc.canBecomeRatKing()) {
            rc.becomeRatKing();
            isKing = true;
            return new Result(END_OF_TURN, "Became rat king");
        }

        if (kingLoc != null && myLoc.distanceSquaredTo(kingLoc) <= KING_RALLY_RADIUS_SQUARED && rc.getMovementCooldownTurns() == 0) {
            Result moveResult = PathFinding.smartMoveTo(kingLoc);
            return new Result(moveResult.code, "Rallying to king at " + kingLoc + " (" + moveResult.msg + ")");
        }

        return new Result(OK, "Cannot become rat king");
        */
    }
}
