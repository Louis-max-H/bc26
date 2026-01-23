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
    State becomeKing;
    State moveKing;
    State spawn;


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
    }

    @Override
    public void updateState(Result resultBefore){
        boolean nowKing = isKing || rc.getType().isRatKingType();
        if (nowKing) {
            Robot robot = new King();
            Robot.rc.setTimelineMarker("King creation", 236, 153, 73);
            while(true) {
                try {
                    robot.run(rc);
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    Robot.rc.setTimelineMarker(e.getMessage(), 255, 0, 0);
                }
            }
        }

        currentState = switch (currentState.name) {
            case "Init" -> avoidCat;
            case "AvoidCat" -> attackEnemy;

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
