package current.States;

import battlecode.common.*;
import current.Communication.SenseForComs;
import current.Utils.PathFinding;
import current.Utils.VisionUtils;
import current.Communication.Communication;

import static current.States.Code.*;
import static current.Utils.Tools.bestDirOfInt9;

public class EndTurn extends State {
    public EndTurn(){
        this.name = "EndTurn";
    }

    @Override
    public Result run() throws GameActionException {
        // Force turn at end of turn
        if(rc.canTurn() && !isKing){
            int[] scores = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
            for(Direction dir : Direction.values()){
                if(dir != Direction.CENTER){
                    scores[dir.ordinal()] = VisionUtils.getScoreInView(myLoc.add(dir), dir, rc.getType());
                }
            }

            rc.turn(bestDirOfInt9(scores));
        }

        // Force move at end of turn
        if(rc.getMovementCooldownTurns() == 0){
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
        if(!competitiveMode && round <= 300 && isKing) {
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
                            rc.setIndicatorDot(loc, 0, 20, 0);
                        }else{
                            rc.setIndicatorDot(loc, score , 0, 0);
                        }
                    }
                }
            }
        }
        */

        Clock.yield();
        return new Result(OK, "Ending turn gracefully.");
    };
}
