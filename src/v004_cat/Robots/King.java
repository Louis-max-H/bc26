package v004_cat.Robots;

import v004_cat.RobotPlayer;
import v004_cat.States.*;

public class King extends Robot {
    State explore;
    State spawn;
    State collectCheese;

    @Override
    public void init(){
        this.init = new Init();
        this.explore = new Explore();
        this.endTurn = new EndTurn();
        this.spawn = new Spawn();
    }

    @Override
    public void updateState(Result resultBefore){
        currentState = switch (currentState.name) {
            case "Init" -> explore;
            case "Explore" -> spawn;
            case "Spawn" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
