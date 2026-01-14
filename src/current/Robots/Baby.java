package current.Robots;

import battlecode.common.GameActionException;
import battlecode.common.*;
import current.States.*;

public class Baby extends Robot {
    State cheeseToKing;
    State collectCheese;
    State explore;
    State avoidCat;
    State attackCat;
    State attackEnemy;
    State placeTrap;
    State mineDirt;
    State placeDirt;


    @Override
    public void init() throws GameActionException {
        this.attackCat = new AttackCat();
        this.attackEnemy = new AttackEnemy();
        this.avoidCat = new AvoidCat();
        this.cheeseToKing = new CheeseToKing();
        this.collectCheese = new CollectCheese();
        this.explore = new Explore();
        this.placeTrap = new PlaceTrap();
        this.mineDirt = new MineDirt();
        this.placeDirt = new PlaceDirt();
    }

    @Override
    public void updateState(Result resultBefore){
        currentState = switch (currentState.name) {
            case "Init" -> avoidCat;
            case "AvoidCat" -> {
                if(rc.getRawCheese() > 20) {
                    yield cheeseToKing;
                }else {
                    yield attackEnemy;
                }
            }
            case "AttackEnemy" -> attackCat;
            case "attackEnemy" -> placeTrap;
            case "CheeseToKing" -> collectCheese;
            case "CollectCheese" -> {
                // If not in rush phase and have nothing urgent, can mine dirt
                if(gamePhase > PHASE_START){
                    yield mineDirt;
                }else{
                    yield placeDirt;
                }
            }
            case "MineDirt" -> placeDirt;
            case "PlaceDirt" -> explore;
            case "Explore" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
