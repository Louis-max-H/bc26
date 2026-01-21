package current.Robots;

import battlecode.common.GameActionException;
import battlecode.common.*;
import current.Params;
import current.States.*;

public class Baby extends Robot {
    State cheeseToKing;
    State collectCheese;
    State explore;
    State attackCat;
    State attackEnemy;
    State placeTrap;
    State formNewKing;


    @Override
    public void init() throws GameActionException {
        this.attackCat = new AttackCat();
        this.attackEnemy = new AttackEnemy();
        this.cheeseToKing = new CheeseToKing();
        this.collectCheese = new CollectCheese();
        this.explore = new Explore();
        this.placeTrap = new PlaceTrap();
        this.formNewKing = new FormNewKing();
    }

    @Override
    public void updateState(Result resultBefore){
        currentState = switch (currentState.name) {
            case "Init" -> avoidCat;
            case "AvoidCat" -> {
                // Check if low health - prioritize forming new king
                if(rc.getHealth() < 30 && rc.getAllCheese() >= 50){
                    yield formNewKing;
                }
                if(rc.getRawCheese() > Params.maxCheese) {
                    yield cheeseToKing;
                }else {
                    yield attackEnemy;
                }
            }

            // Only if low on cheese
            case "AttackEnemy" -> attackCat;
            case "AttackCat" -> cheeseToKing;
            case "FormNewKing" -> {
                // After trying to form king, continue normal flow
                if(rc.getRawCheese() > Params.maxCheese) {
                    yield cheeseToKing;
                }else {
                    yield attackEnemy;
                }
            }

            // Go back to normal mode
            case "CheeseToKing" -> placeTrap;
            case "PlaceTrap" -> collectCheese;
            case "CollectCheese" -> explore;
            case "Explore" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Fallback to endTurn");
                yield endTurn;
            }
        };
    }
}
