package v003_merge.States;

import battlecode.common.*;
import v003_merge.Utils.VisionUtils;

import static v003_merge.States.Code.*;

public class EndTurn extends State {
    public EndTurn(){
        this.name = "EndTurn";
    }

    @Override
    public Result run() throws GameActionException {

        /**
         * Debug scores
         * */
        if(rc.getRoundNum() <= 100 && !isKing) {
            int startX = rc.getLocation().x - 6;
            int startY = rc.getLocation().y - 6;
            int endX = rc.getLocation().x + 7;
            int endY = rc.getLocation().y + 7;
            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    MapLocation loc = new MapLocation(x, y);
                    if (rc.onTheMap(loc)) {
                        int score = VisionUtils.scores[x + 68 * y + 552];
                        if(score == VisionUtils.SCORE_NOT_ALREADY_VIEWED){
                            rc.setIndicatorDot(loc, 0, 20, 0);
                        }else{
                            rc.setIndicatorDot(loc, score , 0, 0);
                        }
                    }
                }
            }
        }

        if(lastInitRound != rc.getRoundNum()){
            // Clock.yield() - We are one round behind ! Not skipping it.
            return new Result(WARN, "Turn start at round " + lastInitRound + " end at " + rc.getRoundNum());
        }

        Clock.yield();
        return new Result(OK, "Ending turn gracefully.");
    };
}
