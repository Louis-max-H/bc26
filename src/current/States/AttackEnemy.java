package current.States;

import battlecode.common.*;
import current.Params;
import current.Robots.Robot;
import current.Utils.*;

import static current.States.Code.*;
import static current.Utils.Micro.addMicroScore;

/**
 * State to attack enemy rats with improved micro
 * - Move out of enemy view after attack
 * - Bonus for first attack
 * - Consider suicide if losing battle
 */
public class AttackEnemy extends State {
    public AttackEnemy() {
        this.name = "AttackEnemy";
    }

    @Override
    public Result run() throws GameActionException {

        // Check if enemy
        if(nearestEnemyRat == null || myLoc.distanceSquaredTo(nearestEnemyRat) > 18){
            return new Result(OK, "No enemy or too far");
        }

        // Reset scores
        Micro.reset();

        // For all enemies, add attack and danger
        int i = 0;
        while(i < enemiesRats.size){
            debug("Rat " + enemiesRats.ids[i] + " at " + enemiesRats.locs[i]);

            // Add micro score
            MapLocation targetLoc = enemiesRats.locs[i];
            char targetDir = directionEnemyRats[enemiesRats.ids[i]];
            int damage = 10;
            addMicroScore(myLoc, targetLoc, targetDir, damage);

            // Add vision score (on init, we use (2000 - round) * 100, lets take 2000*100)
            VisionUtils.addScoreArroundUnit(targetLoc, 200000);
            i++;
        }

        // Calculates scores
        long scoresAttack[] = Micro.scoresAttack; // Sum of danger of enemy units
        long scoresDanger[] = Micro.scoresDanger; // Max amount of damage I can deal
        char attackDirection[] = Micro.attackDirection; // Direction of the best attack
        long mixedScore[] = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0}; // Mix of attack and danger

        // Calculate scores : Score = attack * coefAttack - danger
        int coefAttack = Params.aggresivity[gamePhase];
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
            /*if(rc.canMove(Direction.CENTER) */mixedScore[8] = scoresAttack[8] * coefAttack - scoresDanger[8];

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

        print(String.format("%10s | %6s | %6s | %6s", "Directions", "Danger", "Attack", "Mixed coef " + coefAttack) );
        for (Direction dir : Direction.values()) {
            int k = dir.ordinal();
            print(String.format("%10s | %6s | %6s | %6s ", dir.name(), scoresDanger[k], scoresAttack[k], mixedScore[k]) + directions[attackDirection[k]].name());
            if(rc.onTheMap(myLoc.add(dir))){
                rc.setIndicatorDot(myLoc.add(dir), (int)(scoresDanger[k] * 30), rc.getTeam().ordinal() * 200, (int)(scoresAttack[k] * 30));
            }
        }

        // take best direction
        Direction bestDir = Tools.bestDirOfLong9(mixedScore);

        if(mixedScore[bestDir.ordinal()] == 0){
            PathFinding.moveDir(bestDir);
            return new Result(LOCK, "Mixed attack score is zero, I am too far or can't move");
            // TODO: Add cooldown, if can"t attack 5 turn, exit mode
        }

        // If the best attack is on the current cell, attack now
        Direction atteckDirection = directions[attackDirection[bestDir.ordinal()]];
        MapLocation target = myLoc.add(atteckDirection);
        if(scoresAttack[8] >= scoresAttack[bestDir.ordinal()]){
            if(!rc.canSenseLocation(target)){
                print("Can't sense target, smartlook : " + VisionUtils.smartLookAt(target).msg);
            }

            if(rc.canAttack(target)){
                rc.setIndicatorLine(myLoc, target, 255, 0, 0);
                print("Attacking " + target + " " + atteckDirection);
                rc.attack(target);
            }else{
                print("Warn: Can't attack " + target + " " + atteckDirection);
            }
        }

        // Else, move to cell
        if(rc.canMove(bestDir)){
            PathFinding.move(bestDir);
        }else{
            print("Can't move, try to look in the direction : " + VisionUtils.smartLookAt(myLoc.add(bestDir)));
            return new Result(LOCK, "Can't move to " + bestDir);
        }

        // Then, attack from new cell
        atteckDirection = directions[attackDirection[bestDir.ordinal()]];
        target = myLoc.add(atteckDirection);
        if(rc.isActionReady()){
            if(!rc.canSenseLocation(target)){
                print("Can't sense target, smartlook : " + VisionUtils.smartLookAt(target).msg);
            }

            if(rc.canAttack(target)){
                rc.setIndicatorLine(myLoc, target, 255, 0, 0);
                print("Attacking " + target + " " + atteckDirection);
                rc.attack(target);
            }else{
                print("Warn: Can't attack " + target + " " + atteckDirection);
            }
        }

        return new Result(LOCK, "Stay in micro state");
    }
}
