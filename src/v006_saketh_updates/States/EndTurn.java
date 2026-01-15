package v006_saketh_updates.States;

import battlecode.common.*;
import v006_saketh_updates.Communication.SenseForComs;
import v006_saketh_updates.Utils.VisionUtils;
import v006_saketh_updates.Communication.Communication;

import static v006_saketh_updates.States.Code.*;

public class EndTurn extends State {
    public EndTurn(){
        this.name = "EndTurn";
    }

    @Override
    public Result run() throws GameActionException {
        System.out.println("Printing kings");
        print("Nearest kings :");
        for(char i=0; i< kings.size; i++){
            print("\t King at " + kings.locs[i]);
        }
        print("");


        // Communication
        SenseForComs.senseForComs();  // Generate messages to send
        Communication.sendMessages(); // Send messages (squeak or shared array)


        // End turn
        if(lastInitRound != round){
            // Clock.yield() - We are one round behind ! Not skipping it.
            return new Result(WARN, "Turn start at round " + lastInitRound + " end at " + round);
        }

        // Debug scores
        if(!competitiveMode && round <= 100) {
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

        Clock.yield();
        return new Result(OK, "Ending turn gracefully.");
    };
}
