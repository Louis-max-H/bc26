package v003_merge.States;

import battlecode.common.*;
import v003_merge.Robots.Robot;
import v003_merge.Utils.PathFinding;
import v003_merge.Utils.VisionUtils;

import static battlecode.common.GameConstants.INDICATOR_STRING_MAX_LENGTH;
import static v003_merge.States.Code.*;

public class Init extends State {
    public Init(){
        this.name = "Init";
        Robot.spawnLoc = rc.getLocation();
        Robot.spawnRound = rc.getRoundNum();
        Robot.isKing = rc.getType().isRatKingType();

        VisionUtils.initScore(rc.getMapWidth(), rc.getMapHeight());
    }


    @Override
    public Result run() throws GameActionException {
        // Called on new turns

        print("Init classic variables");
        lastInitRound = rc.getRoundNum();
        PathFinding.resetScores();
        isKing = rc.getType().isRatKingType();


        print("Initializing Explore state");
        /**
         * We want to give a score of interest to each cell. Default is 700
         * When the unit can see a cell, we give it a score according to the actual turn : score(t).
         * We want to have score(t1) > score(t2) iff t1 < t2   (the score is bigger if last visit is a turn from long ago)
         *
         * King can view 9*9 cells, with max char of 65536, max values is around 800, taking default value of 600
         *
         * We use score(t) = (2000 - t) / 28
         * score(0) ~ 71 << 700 and score(2000) = 0
         * */
        char scoreTurn = (char)((2000 - rc.getRoundNum()) / 28);

        for(MapLocation loc  : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 99)){
            if(rc.canSenseLocation(loc)) {
                // x + y*(60+gap) + gap + gap*(60+gap)
                VisionUtils.scores[loc.x + 68 * loc.y + 552] = scoreTurn;
            }
        }


        if(isKing){
            Robot.nearestKing = rc.getLocation();
        }else{
            for(RobotInfo info : rc.senseNearbyRobots(99, rc.getTeam())){
                if(rc.getType().isRatKingType()){
                    nearestKing = info.location;
                    break; // Only one will be enough
                }
            }
        }


        return new Result(OK, "");
    }
}
