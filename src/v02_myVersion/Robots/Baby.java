package v02_myVersion.Robots;

import v02_myVersion.RobotPlayer;
import v02_myVersion.States.*;

public class Baby extends Robot {
    State explore;
    State helloWorld;

    @Override
    public void init(){
        this.explore = new Explore();
        this.helloWorld = new HelloWorld();
    }

    @Override
    public void updateState(Result resultBefore){
        currentState = switch (currentState.name) {
            case "Init" -> explore;
            case "Explore" -> helloWorld;
            case "HelloWorld" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
