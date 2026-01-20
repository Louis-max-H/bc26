package best_config_62165960.Robots;

import battlecode.common.GameActionException;
import battlecode.common.*;
import best_config_62165960.States.*;

public class Baby extends Robot {
    State cheeseToKing;
    State collectCheese;
    State explore;
    State attackCat;
    State attackEnemy;
    State placeTrap;
    State mineDirt;
    State placeDirt;


    @Override
    public void init() throws GameActionException {
        this.attackCat = new AttackCat();
        this.attackEnemy = new AttackEnemy();
        this.cheeseToKing = new CheeseToKing();
        this.collectCheese = new CollectCheese();
        this.explore = new Explore();
        this.placeTrap = new PlaceTrap();
        this.mineDirt = new MineDirt();
        this.placeDirt = new PlaceDirt();
    }

    @Override
    public void updateState(Result resultBefore){
        best_config_62165960State = switch (best_config_62165960State.name) {
            case "Init" -> avoidCat;
            case "AvoidCat" -> {
                if(rc.getRawCheese() > 30) {
                    yield cheeseToKing;
                }else {
                    yield attackEnemy;
                }
            }

            // Only if low on cheese
            case "AttackEnemy" -> attackCat;
            case "AttackCat" -> cheeseToKing;

            // Go back to normal mode
            case "CheeseToKing" -> placeTrap;
            case "PlaceTrap" -> collectCheese;
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
                Robot.err(best_config_62165960State.name + " don't match any states. Fallback to endTurn");
                yield endTurn;
            }
        };
    }
}
