package current.Robots;

import battlecode.common.GameActionException;
import current.States.*;

public class King extends Robot {
    State moveKing;
    State spawn;
    State askForKing;
    State kingDig;
    State kingCheese;
    State attackCat;
    State placeTrap;

    @Override
    public void init() throws GameActionException {
        this.init = new Init();
        this.moveKing = new MoveKing();
        this.askForKing = new AskNewKing();
        this.endTurn = new EndTurn();
        this.spawn = new Spawn();
        this.kingDig = new KingDig();
        this.kingCheese = new KingCheese();
        this.attackCat = new AttackCat();
        this.placeTrap = new PlaceTrap();
    }

    @Override
    public void updateState(Result resultBefore){
        currentState = switch (currentState.name) {
            case "Init" -> moveKing;
            case "MoveKing" -> avoidCat;
            case "AvoidCat" -> attackCat;
            case "AttackCat" -> placeTrap;
            case "PlaceTrap" -> askForKing;
            case "AskNewKing" -> spawn;
            case "Spawn" -> kingDig;
            case "KingDig" -> kingCheese;
            case "KingCheese" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
