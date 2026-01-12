package v004_cat.States;

import battlecode.common.*;
import v004_cat.Robots.Robot;
import v004_cat.Utils.PathFinding;
import v004_cat.Utils.VisionUtils;

import static v004_cat.States.Code.*;

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
        round = rc.getRoundNum();
        lastInitRound = rc.getRoundNum();
        isKing = rc.getType().isRatKingType();
        PathFinding.resetScores();

        if      (round <  100) { gamePhase = PHASE_START;}
        else if (round <  500) { gamePhase = PHASE_EARLY;}
        else if (round < 1500) { gamePhase = PHASE_MIDLE;}
        else                   { gamePhase = PHASE_FINAL;}

        print("Update vision score state");
        /**
         * We want to give a score of interest to each cell. Default is 700
         * When the unit can see a cell, we give it a score according to the actual turn : score(t).
         * We want to have score(t1) > score(t2) iff t1 < t2   (the score is bigger if last visit is a turn from long ago)
         *
         * King can view 9*9 cells, with max char of 65536, max values is around 800, taking default value of 600
         *
         * We use score(t) = (2000 - t) / 7
         * score(0) ~ 285 << 700 and score(2000) = 0
         * */
        char scoreTurn = (char)((2000 - rc.getRoundNum()) / 28);

        // Reset score if we can view the cell
        for(MapLocation loc  : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 99)){
            if(rc.canSenseLocation(loc)) {
                // x + y*(60+gap) + gap + gap*(60+gap)
                VisionUtils.scores[loc.x + 68 * loc.y + 552] = scoreTurn;
            }
        }

        // Add penalty if some rats are already looking in this direction
        char penalty;
        if(gamePhase <= PHASE_START){penalty = 300;
        }else                       {penalty = 100;}

        for(RobotInfo info: rc.senseNearbyRobots(-1, rc.getTeam())){
            if(info.type == UnitType.RAT_KING){continue;}

            for(MapLocation loc: VisionUtils.getAllLocationsVisibleFrom(info.getLocation(), info.getDirection(), UnitType.BABY_RAT)){
                int cell = loc.x + 68 * loc.y + 552;
                if(VisionUtils.scores[cell] >= penalty){
                    VisionUtils.scores[cell] = (char)(VisionUtils.scores[cell] - penalty);
                }else{
                    VisionUtils.scores[cell] = 0;
                }
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
