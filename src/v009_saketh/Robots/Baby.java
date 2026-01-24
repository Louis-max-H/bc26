package v009_saketh.Robots;

import battlecode.common.GameActionException;
import battlecode.common.*;
import v009_saketh.Params;
import v009_saketh.States.*;

public class Baby extends Robot {
    State cheeseToKing;
    State collectCheese;
    State explore;
    State attackCat;
    State attackEnemy;
    State placeTrap;
    State throwToWalls;
    State becomeKing;
    State moveKing;
    State spawn;
    State rushKing;


    @Override
    public void init() throws GameActionException {
        this.attackCat = new AttackCat();
        this.attackEnemy = new AttackEnemy();
        this.cheeseToKing = new CheeseToKing();
        this.collectCheese = new CollectCheese();
        this.throwToWalls = new ThrowToWalls();
        this.explore = new Explore();
        this.placeTrap = new PlaceTrap();
        this.becomeKing = new BecomeKing();
        this.moveKing = new MoveKing();
        this.spawn = new Spawn();
        this.rushKing = new RushKing();
    }

    @Override
    public void updateState(Result resultBefore){
        currentState = switch (currentState.name) {
            case "Init" -> avoidCat;
            case "AvoidCat" -> {
                // After round 1500, prioritize rushing to enemy king location
                if (gamePhase >= PHASE_FINAL) {
                    yield rushKing;
                }
                yield attackEnemy;
            }

            // Rush king state transitions
            case "RushKing" -> {
                // After rushing, continue with normal flow
                yield attackEnemy;
            }

            // Only if low on cheese
            case "AttackEnemy" -> throwToWalls;
            case "ThrowToWalls" -> becomeKing;
            case "BecomeKing" -> attackCat;
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
