package v004_cat.Robots;

import battlecode.common.GameActionException;
import v004_cat.States.*;

public class Baby extends Robot {
    State cheeseToKing;
    State collectCheese;
    State explore;
    State avoidCat;
    State attackCat;


    @Override
    public void init() throws GameActionException {
        this.attackCat = new AttackCat();
        this.avoidCat = new AvoidCat();
        this.cheeseToKing = new CheeseToKing();
        this.collectCheese = new CollectCheese();
        this.explore = new Explore();
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
