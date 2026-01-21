package v008_saketh.States;

import battlecode.common.*;
import v008_saketh.Communication.SenseForComs;
import v008_saketh.Utils.PathFinding;
import v008_saketh.Utils.VisionUtils;
import v008_saketh.Communication.Communication;

import static v008_saketh.States.Code.*;
import static v008_saketh.Utils.Tools.bestDirOfLong9;

public class EndTurn extends State {
    public EndTurn(){
        this.name = "EndTurn";
    }

    @Override
    public Result run() throws GameActionException {
        // Force turn at end of turn
        if(rc.canTurn() && !isKing){
            VisionUtils.smartLook();
        }

        // Force move at end of turn
        if(!isKing && rc.getMovementCooldownTurns() == 0){
            print("Force moving at end of turn");
            PathFinding.moveBest();
        }

        // Communication
        SenseForComs.senseForComs();  // Generate messages to send
        Communication.sendMessages(); // Send messages (squeak or shared array)


        // End turn
        if(lastInitRound != round){
            // Clock.yield() - We are one round behind ! Not skipping it.
            return new Result(WARN, "Turn start at round " + lastInitRound + " end at " + round);
        }

        // Debug scores
        /*
        if(!competitiveMode && round <= 300) {
            int startX = myLoc.x - 6;
            int startY = myLoc.y - 6;
            int endX = myLoc.x + 7;
            int endY = myLoc.y + 7;
            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    MapLocation loc = new MapLocation(x, y);
                    if(Clock.getBytecodesLeft() < 2000){
                        Clock.yield();
                        return new Result(WARN, "Stop debug scores, not enough bytecode");
                    }
                    if (rc.onTheMap(loc)) {
                        int score = VisionUtils.scores[x + 68 * y + 552];
                        if(score == VisionUtils.SCORE_NOT_ALREADY_VIEWED){
                            rc.setIndicatorDot(loc, 0, 50, 0);
                        }else{
                            rc.setIndicatorDot(loc, score , 0, 0);
                        }
                    }
                }
            }
        }*/

        Clock.yield();
        return new Result(OK, "Ending turn gracefully.");
    };
}
