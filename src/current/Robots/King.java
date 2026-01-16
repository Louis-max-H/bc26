package current.Robots;

import battlecode.common.GameActionException;
import current.States.*;

public class King extends Robot {
    State explore;
    State spawn;
    State avoidCat;

    @Override
    public void init() throws GameActionException {
        this.init = new Init();
        this.explore = new Explore();
        this.endTurn = new EndTurn();
        this.spawn = new Spawn();
        this.avoidCat = new AvoidCat();
    }

    @Override
    public void updateState(Result resultBefore){
        if(nearestCat != null && rc.getLocation().distanceSquaredTo(nearestCat) <= 36){
            currentState = avoidCat;
            return;
        }

        currentState = switch (currentState.name) {
            case "Init" -> spawn;
            case "Spawn" -> endTurn;
            case "AvoidCat" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(currentState.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
