package current.Robots;

import battlecode.common.GameActionException;
import current.States.*;

public class King extends Robot {
    State moveKing;
    State spawn;
    State avoidCat;

    @Override
    public void init() throws GameActionException {
        this.init = new Init();
        this.moveKing = new MoveKing();
        this.endTurn = new EndTurn();
        this.spawn = new Spawn();
        this.avoidCat = new AvoidCat();
    }

    @Override
    public void updateState(Result resultBefore){
        currentState = switch (currentState.name) {
            case "Init" -> moveKing;
            case "MoveKing" -> avoidCat;
            case "AvoidCat" -> spawn;
            case "Spawn" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
