package v007_saketh_reviewed1.Robots;

import battlecode.common.GameActionException;
import battlecode.common.*;
import v007_saketh_reviewed1.RobotPlayer;
import v007_saketh_reviewed1.States.*;

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
        v007_saketh_reviewed1State = switch (v007_saketh_reviewed1State.name) {
            case "Init" -> explore;
            case "Explore" -> {
                // Check for cat danger first
                if (nearestCat != null && myLoc.distanceSquaredTo(nearestCat) < 50) {
                    yield avoidCat;
                }
                // Check if we can spawn (conservative: need buffer for 200 rounds)
                // Rat kings consume 2 cheese per round, so 200 rounds = 400 cheese minimum
                // Plus spawn cost + buffer = ~500+ cheese needed
                if (rc.isActionReady()) {
                    int cheeseStock = rc.getAllCheese();
                    int spawnCost = rc.getCurrentRatCost();
                    int minCheeseFor200Rounds = 400 + 100; // 400 for consumption + 100 buffer
                    int conservativeBuffer = minCheeseFor200Rounds + spawnCost;
                    
                    // Only spawn if we have enough cheese for 200 rounds after spawning
                    if (cheeseStock >= conservativeBuffer && (cheeseStock - spawnCost) >= minCheeseFor200Rounds) {
                        yield spawn;
                    }
                }
                yield explore;
            }
            case "AvoidCat" -> explore;
            case "Spawn" -> endTurn;
            case "EndTurn" -> init;
            default -> {
                Robot.err(v007_saketh_reviewed1State.name + " don't match any states. Fallback to init");
                yield init;
            }
        };
    }
}
