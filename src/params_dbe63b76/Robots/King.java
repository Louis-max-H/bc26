package params_dbe63b76.Robots;

import battlecode.common.GameActionException;
import params_dbe63b76.States.*;

public class King extends Robot {
    State moveKing;
    State spawn;

    @Override
    public void init() throws GameActionException {
        this.init = new Init();
        this.moveKing = new MoveKing();
        this.endTurn = new EndTurn();
        this.spawn = new Spawn();
    }

    @Override
    public void updateState(Result resultBefore){
        params_dbe63b76State = switch (params_dbe63b76State.name) {
            case "Init" -> moveKing;
            case "MoveKing" -> avoidCat;
            case "AvoidCat" -> spawn;
            case "Spawn" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(params_dbe63b76State.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
