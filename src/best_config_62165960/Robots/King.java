package best_config_62165960.Robots;

import battlecode.common.GameActionException;
import best_config_62165960.States.*;

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
        best_config_62165960State = switch (best_config_62165960State.name) {
            case "Init" -> moveKing;
            case "MoveKing" -> avoidCat;
            case "AvoidCat" -> spawn;
            case "Spawn" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(best_config_62165960State.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
