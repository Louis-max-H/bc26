package v02_myVersion.States;

import battlecode.common.*;
import static v02_myVersion.States.Code.*;

public class EndTurn extends State {
    public EndTurn(){
        this.name = "EndTurn";
    }

    @Override
    public Result run(){
        if(lastInitRound != rc.getRoundNum()){
            // Clock.yield() - We are one round behind ! Not skipping it.
            return new Result(WARN, "Turn start at round " + lastInitRound + " end at " + rc.getRoundNum());
        }

        Clock.yield();
        return new Result(OK, "Ending turn gracefully.");
    };
}
