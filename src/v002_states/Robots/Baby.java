package v002_states.Robots;

import v002_states.States.*;

public class Baby extends Robot {
    State explore;
    State helloWorld;
    State collectCheese;
    State cheeseToKing;

    @Override
    public void init(){
        this.explore = new Explore();
        this.helloWorld = new CheeseToKing();
        this.cheeseToKing = new CheeseToKing();
        this.collectCheese = new CollectCheese();
    }

    @Override
    public void updateState(Result resultBefore){
        currentState = switch (currentState.name) {
            case "Init" ->  explore;
            case "Explore" -> cheeseToKing; // We want to drop cheese before collecting more,
            case "CheeseToKing" -> collectCheese;
            case "CollectCheese" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
