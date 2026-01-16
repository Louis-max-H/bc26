package current.Robots;

import battlecode.common.GameActionException;
import current.States.*;

public class Baby extends Robot {
    State cheeseToKing;
    State collectCheese;
    State explore;
    State avoidCat;
    State attackCat;


    @Override
    public void init() throws GameActionException {
        this.attackCat = new AttackCat();
        this.avoidCat = new AvoidCat();
        this.cheeseToKing = new CheeseToKing();
        this.collectCheese = new CollectCheese();
        this.explore = new Explore();
        isFeeder = (rc.getID() & 1) == 0;
    }

    @Override
    public void updateState(Result resultBefore){
        if(resultBefore.code == Code.CANT){
            currentState = endTurn;
            return;
        }

        if(currentState.name.equals("EndTurn")){
            currentState = init;
            return;
        }

        if(rc.getRawCheese() > 0){
            currentState = cheeseToKing;
            return;
        }

        currentState = collectCheese;
    }
}
