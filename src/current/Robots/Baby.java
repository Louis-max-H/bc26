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
    State throwToWalls;


    @Override
    public void init() throws GameActionException {
        this.attackCat = new AttackCat();
        this.attackEnemy = new AttackEnemy();
        this.cheeseToKing = new CheeseToKing();
        this.collectCheese = new CollectCheese();
        this.throwToWalls = new ThrowToWalls();
        this.explore = new Explore();
        this.placeTrap = new PlaceTrap();
    }

    @Override
    public void updateState(Result resultBefore){
        currentState = switch (currentState.name) {
            case "Init" -> avoidCat;
            case "AvoidCat" -> {
                if(rc.getRawCheese() > Params.maxCheese) {
                    yield cheeseToKing;
                }else {
                    yield attackEnemy;
                }
            }

            // Only if low on cheese
            case "AttackEnemy" -> throwToWalls;
            case "ThrowToWalls" -> attackCat;
            case "AttackCat" -> cheeseToKing;

            // Go back to normal mode
            case "CheeseToKing" -> placeTrap;
            case "PlaceTrap" -> collectCheese;
            case "CollectCheese" -> explore;
            case "Explore" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Resign to force dev to fix me");
                rc.resign();
                yield endTurn;
            }
        };
    }
}
