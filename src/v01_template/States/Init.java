package v01_template.States;

import battlecode.common.*;
import v01_template.Robots.Robot;
import v01_template.Utils.PathFinding;

import static v01_template.States.Code.*;

public class Init extends State {
    public Init(){
        this.name = "Init";
        Robot.spawnLoc = rc.getLocation();
        Robot.spawnRound = rc.getRoundNum();
    }


    @Override
    public Result run(){
        // Called on new turns
        print("Init classic variables");
        lastInitRound = rc.getRoundNum();
        PathFinding.resetScores();

        print("Initializing Explore state");
        /**
         * We want to give a score of interest to each cell. Default is 400 (\u0190)
         * When the unit can see a cell, we give it a score according to the actual turn : score(t).
         * We want to have score(t1) > score(t2) iff t1 < t2   (the score is bigger if last visit is a turn from long ago)
         *
         * To avoid char overflow, we use score(t) = (2000 - t) / 20
         * score(0) = 100 << 400 and score(2000) = 0
         * Dividing by 10 is not a big issues, since we have a 10 round cooldown
         * */
        char scoreTurn = (char)((2000 - rc.getRoundNum()) / 20);

        for(MapInfo info  : rc.senseNearbyMapInfos()){
            MapLocation loc = info.getMapLocation();
            Explore.heuristic[loc.x + 60 * loc.y] = scoreTurn;
        }

        return new Result(OK, "");
    }
}
