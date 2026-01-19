package current.States;

import battlecode.common.*;
import current.Utils.MapLocationsWithId;
import current.Utils.Tools;
import current.Utils.VisionUtils;
import current.Utils.PathFinding;

import static current.States.Code.*;
import static current.Utils.Micro.addMicroScore;

/**
 * State to attack enemy rats with improved micro
 * - Move out of enemy view after attack
 * - Bonus for first attack
 * - Consider suicide if losing battle
 */
public class AttackEnemy extends State {
    public static Direction[] dirs = new Direction[]{Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.CENTER};

    public AttackEnemy() {
        this.name = "AttackEnemy";
    }

    @Override
    public Result run() throws GameActionException {

        // Check if enemy
        if(nearestEnemyRat == null || myLoc.distanceSquaredTo(nearestEnemyRat) > 13){
            return new Result(OK, "No enemy");
        }

        // Calculates scores
        long scoresAttack[] = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0}; // Sum of danger of enemy units
        long scoresDanger[] = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0}; // Max amount of damage I can deal

        long mixedScore[] = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0}; // Mix of attack and danger
        char attackDirection[] = new char[]{0, 0, 0, 0, 0, 0, 0, 0, 0}; // Direction of the best attack

        // For all enemies
        int i = 0;
        while(i < enemiesRats.size){
            // Add micro score
            MapLocation targetLoc = enemiesRats.locs[i];
            char targetDir = directionEnemyRats[enemiesRats.ids[i]];
            int damage = 10;
            addMicroScore(myLoc, targetLoc, targetDir, scoresAttack, scoresDanger, attackDirection, damage);

            // Add vision score (or init, we use (2000 - round) * 100, lets take 2000*100)
            VisionUtils.addScoreArroundUnit(targetLoc, 200000);
            i++;
        }

        // Calculate scores : Score = attack * coefAttack - danger
        int coefAttack = 1;
        if(rc.isActionReady()) {

            // Compute score only if can move
            if(rc.canMove(Direction.NORTH)    ){mixedScore[0] = scoresAttack[0] * coefAttack - scoresDanger[0];}
            if(rc.canMove(Direction.NORTHEAST)){mixedScore[1] = scoresAttack[1] * coefAttack - scoresDanger[1];}
            if(rc.canMove(Direction.EAST)     ){mixedScore[2] = scoresAttack[2] * coefAttack - scoresDanger[2];}
            if(rc.canMove(Direction.SOUTHEAST)){mixedScore[3] = scoresAttack[3] * coefAttack - scoresDanger[3];}
            if(rc.canMove(Direction.SOUTH)    ){mixedScore[4] = scoresAttack[4] * coefAttack - scoresDanger[4];}
            if(rc.canMove(Direction.SOUTHWEST)){mixedScore[5] = scoresAttack[5] * coefAttack - scoresDanger[5];}
            if(rc.canMove(Direction.WEST)     ){mixedScore[6] = scoresAttack[6] * coefAttack - scoresDanger[6];}
            if(rc.canMove(Direction.NORTHWEST)){mixedScore[7] = scoresAttack[7] * coefAttack - scoresDanger[7];}
            if(rc.canMove(Direction.CENTER)   ){mixedScore[8] = scoresAttack[8] * coefAttack - scoresDanger[8];}

        }else{

            // If we can't attack, just take danger
            mixedScore[0] = -scoresDanger[0];
            mixedScore[1] = -scoresDanger[1];
            mixedScore[2] = -scoresDanger[2];
            mixedScore[3] = -scoresDanger[3];
            mixedScore[4] = -scoresDanger[4];
            mixedScore[5] = -scoresDanger[5];
            mixedScore[6] = -scoresDanger[6];
            mixedScore[7] = -scoresDanger[7];
            mixedScore[8] = -scoresDanger[8];
        }

        // take best direction
        Direction bestDir = Tools.bestDirOfLong9(mixedScore);

        // If the best attack is on the current cell, attack now
        MapLocation target = myLoc.add(dirs[attackDirection[bestDir.ordinal()]]);
        if(scoresAttack[8] >= scoresDanger[bestDir.ordinal()]){
            if(!rc.canSenseLocation(target)){
                print("Can't sense target, smartlook : " + VisionUtils.smartLookAt(target));
            }

            if(rc.canAttack(myLoc.add(bestDir))){
                rc.setIndicatorLine(myLoc, myLoc.add(bestDir), 255, 0, 0);
                print("Attacking " + myLoc.add(bestDir));
                rc.attack(myLoc.add(bestDir));
            }else{
                print("Warn: Can't attack " + myLoc.add(bestDir));
            }
        }

        // Else, move to cell
        if(rc.canMove(bestDir)){
            PathFinding.move(bestDir);
        }else{
            print("Can't move, try to look in the direction : " + VisionUtils.smartLookAt(myLoc.add(bestDir)));
            return new Result(WARN, "Can't move to " + bestDir);
        }

        // Then, attack from new cell
        target = myLoc.add(dirs[attackDirection[bestDir.ordinal()]]);
        if(rc.isActionReady()){
            if(!rc.canSenseLocation(target)){
                print("Can't sense target, smartlook : " + VisionUtils.smartLookAt(target));
            }

            if(rc.canAttack(myLoc.add(bestDir))){
                rc.setIndicatorLine(myLoc, myLoc.add(bestDir), 255, 0, 0);
                print("Attacking " + myLoc.add(bestDir));
                rc.attack(myLoc.add(bestDir));
            }else{
                print("Warn: Can't attack " + myLoc.add(bestDir));
            }
        }

        return new Result(LOCK, "Stay in micro state");
    }
}
