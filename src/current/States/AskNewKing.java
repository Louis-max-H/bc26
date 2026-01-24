package current.States;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import current.Communication.Communication;
import current.Params;

import java.util.Map;

import static current.States.Code.*;
import static java.lang.Math.min;

public class AskNewKing extends State {
    public int consecutiveNoKing = 0;
    public AskNewKing(){
        this.name = "AskNewKing";
    }


    // Position where we can ask for new king
    public static int shiftX[] = {03, 03, 03, -3, -3, -3, 00, 02, -2, 00, 02, -2};
    public static int shiftY[] = {00, 02, -2, 00, 02, -2, 03, 03, 03, -3, -3, -3};


    @Override
    public Result run() throws GameActionException {
        if(kings.size >= 2){
            consecutiveNoKing = 0;
            return new Result(OK, "Already have enough kings");
        }

        if(rc.getRoundNum() % 75 <= 30 && nearestMine != null){
            Communication.addMessageCreateKing(nearestMine, PRIORITY_CRIT);
        }

        // If another king exist
        for (int i = 0; i < kings.size; i++) {
            if(kings.ids[i] != rc.getID() % 4096){
                consecutiveNoKing = 0;
                return new Result(OK, "Already have another king " + kings.ids[i]);
            }
        }

        consecutiveNoKing++;
        if(consecutiveNoKing < 2){
            return new Result(OK, "Maybe create new king : consectiveNoKing: " + consecutiveNoKing);
        }

        // Wait a little because when spawn, kings array is not initialized
        if(spawnRound + 4 > rc.getRoundNum()){
            return new Result(OK, "Wait after spawn before asking new king");
        }

        // Check placement for new king
        MapLocation newKingCenter = null;
        labelLookForKingCenter:
        for (int i = 0; i < 12; i++) {
            newKingCenter = myLoc.translate(shiftX[i], shiftY[i]);

            // Check if all cells are passable
            for(Direction dir: Direction.values()){
                // Out of the map
                if(!rc.onTheMap(newKingCenter.add(dir))){
                    newKingCenter = null;
                    break;
                }

                // Location passable
                if(!rc.senseMapInfo(newKingCenter.add(dir)).isPassable()){
                    newKingCenter = null;
                    break;
                }
            }

            if(newKingCenter != null){
                break;
            }
        }
        
        // If no placement found
        if(newKingCenter == null){
            return new Result(WARN, "No placement found for new king");
        }

        // Else, call for king
        Communication.addMessageCreateKing(newKingCenter, PRIORITY_CRIT);
        rc.setIndicatorLine(rc.getLocation(), newKingCenter, 0, 255, 0);
        return new Result(WARN, "Ask for king at " + newKingCenter);
    };
}
