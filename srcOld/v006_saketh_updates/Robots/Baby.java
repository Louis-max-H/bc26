package v006_saketh_updates.Robots;

import battlecode.common.GameActionException;
import battlecode.common.*;
import v006_saketh_updates.States.*;

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
            case "Init" -> explore;
            case "Explore" -> {
                // Check for cat danger first
                if (nearestCat != null && myLoc.distanceSquaredTo(nearestCat) < 25) {
                    yield avoidCat;
                }
                // Check for enemy to attack
                if ((nearestEnemyRat != null || nearestEnemyKing != null) && 
                    myLoc.distanceSquaredTo(nearestEnemyRat != null ? nearestEnemyRat : nearestEnemyKing) <= 2) {
                    yield attackEnemy;
                }
                // Check if we can place trap (cat or enemy nearby)
                if (rc.isActionReady() && 
                    ((nearestCat != null && myLoc.distanceSquaredTo(nearestCat) <= 4 && rc.getAllCheese() >= 10) ||
                     ((nearestEnemyRat != null || nearestEnemyKing != null) && 
                      myLoc.distanceSquaredTo(nearestEnemyRat != null ? nearestEnemyRat : nearestEnemyKing) <= 4 && 
                      rc.getAllCheese() >= 30))) {
                    yield placeTrap;
                }
                // Check if we have cheese to transfer
                if (rc.getRawCheese() > 0) {
                    yield cheeseToKing;
                }
                yield explore;
            }
            case "AvoidCat" -> {
                // After avoiding, check if we can attack
                if (nearestCat != null && myLoc.distanceSquaredTo(nearestCat) <= 2) {
                    yield attackCat;
                }
                // Otherwise continue exploring
                yield explore;
            }
            case "AttackCat" -> explore;
            case "AttackEnemy" -> explore;
            case "PlaceTrap" -> explore;
            case "CheeseToKing" -> collectCheese;
            case "CollectCheese" -> {
                // If not in rush phase and have nothing urgent, can mine dirt or place dirt
                if(gamePhase > PHASE_START){
                    if(nearestDirt != null && rc.isActionReady() && rc.getAllCheese() >= 10){
                        yield mineDirt;
                    }
                    if(nearestWater != null && rc.isActionReady() && rc.getAllCheese() >= 10 && rc.getDirt() > 0){
                        yield placeDirt;
                    }
                }
                yield endTurn;
            }
            case "MineDirt" -> explore;
            case "PlaceDirt" -> explore;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
