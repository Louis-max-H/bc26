package current.States;

import battlecode.common.*;
import battlecode.world.Trap;
import current.Params;
import current.Robots.Robot;
import current.Utils.*;

import static current.States.Code.*;
import static current.Utils.Micro.addMicroScore;
import static current.Utils.Micro.addThrowMicroScore;
import static java.lang.Math.max;
import static java.lang.Math.min;

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

    // Scoring
    public static long[] scoresAttack;
    public static long[] scoresDanger;
    public static char[] attackDirections;
    public static boolean[] isThrowAction;
    public static boolean throwEnable = true;
    private static int noAttackStreak = 0;
    private static final int MAX_NO_ATTACK_STREAK = 6;

    @Override
    public Result run() throws GameActionException {
        if(Robot.isCheeseEmergency() && !Robot.isKingThreatened()){
            return new Result(OK, "Cheese emergency, skip attack");
        }

        // Check if enemy
        if (nearestEnemyRat == null || myLoc.distanceSquaredTo(nearestEnemyRat) > 64) {
            noAttackStreak = 0;
            return new Result(OK, "No enemy or too far");
        }

        // Play
        throwEnable = true;
        boolean actionReadyAtStart = rc.isActionReady();
        Result result = play();
        if(actionReadyAtStart){
            if(!rc.isActionReady()){
                noAttackStreak = 0;
            }else{
                noAttackStreak++;
            }
        }

        if(noAttackStreak >= MAX_NO_ATTACK_STREAK){
            noAttackStreak = 0;
            return new Result(END_OF_TURN, "No attack for a while, cooling down");
        }

        return result;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////// SCORES /////////////////////////////////////////////////////////////////////
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void initScore() throws GameActionException {
        // Reset scores
        Micro.reset();

        // For all enemies, add attack, danger and is throw to them
        int i = 0;
        boolean canThrow = throwEnable & (rc.getCarrying() != null) && rc.isActionReady();
        int baseDamage = GameConstants.RAT_BITE_DAMAGE;
        int cheeseBonusDamage = Math.min(3, rc.getRawCheese());
        Team myTeam = rc.getTeam();
        while (i < enemiesRats.size) {
            debug("Rat " + enemiesRats.ids[i] + " at " + enemiesRats.locs[i]);

            // Add thrown score
            MapLocation targetLoc = enemiesRats.locs[i];
            if (canThrow) {
                addThrowMicroScore(myLoc, targetLoc, Micro.ATTACK_THROW); // Big score for throw
            }

            // Add micro score
            char targetDir = directionEnemyRats[enemiesRats.ids[i]];
            int damage = baseDamage + cheeseBonusDamage;
            if(rc.canSenseLocation(targetLoc)){
                RobotInfo info = rc.senseRobotAtLocation(targetLoc);
                if(info != null && info.getTeam() != myTeam && info.getHealth() <= damage){
                    damage += baseDamage;
                }
            }

            // Add bonus if can ratnap
            long bonusIfCanRatnap = (rc.getCarrying() == null) ? 100 : 0;
            addMicroScore(myLoc, targetLoc, targetDir, damage, bonusIfCanRatnap);

            // Add vision score, if dist < 18, we can be ratnap if we move in his direction and enemy move to our direction
            // Score on empty cell is 21000
            int score = (myLoc.distanceSquaredTo(enemiesRats.locs[i]) <= 18) ? 21000 * 10 : 21000;
            VisionUtils.addScoreArroundUnit(targetLoc, score);
            i++;
        }

        // For nearest cat
        /*
        if(nearestCat != null && myLoc.distanceSquaredTo(nearestCat) <= 20){
            for(MapLocation loc: rc.getAllLocationsWithinRadiusSquared(nearestCat, 2)){
                addThrowMicroScore(myLoc, loc, Micro.ATTACK_THROW + 10); // Slightly better score for cats
            }
        }
        */
    }


    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////// Update scores //////////////////////////////////////////////////////////////
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Direction updateScoresAndBestDir() throws GameActionException {
        scoresAttack = Micro.scoresAttack;        // Sum of danger of enemy units
        scoresDanger = Micro.scoresDanger;        // Max amount of damage I can deal
        attackDirections = Micro.attackDirection; // Direction of the best attack
        isThrowAction = Micro.isThrowAction;      // If the action is a throw action

        // If can't move
        if (!rc.isMovementReady()) {
            print("Can't move, keep only score for Direction.CENTER");
            scoresAttack = new long[]{0, 0, 0, 0, 0, 0, 0, 0, (rc.isActionReady()) ? scoresAttack[8] : 0};
            scoresDanger = new long[]{0, 0, 0, 0, 0, 0, 0, 0, scoresDanger[8]};
            return Direction.CENTER;
        }

        // If can't action
        if (!rc.isActionReady()) {
            print("Not action ready, scoresAttack set to 0, return lower of scoresDanger");
            scoresAttack = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
            return Tools.lowerDirOfLong9(Micro.scoresDanger);
        }


        // Calculate mixed scores : Score = attack * coefAttack - danger
        long[] mixedScore = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0}; // Mix of attack and danger
        int coefAttack = Params.aggresivity[gamePhase];

        // Compute score only if can move to cell
        for (int i = 0; i < 8; i++) {
            if (rc.canMove(directions[i])) {
                mixedScore[i] = max(scoresAttack[i], scoresAttack[8]) * coefAttack - scoresDanger[i];
            }
        }
        mixedScore[8] = scoresAttack[8] * coefAttack - scoresDanger[8];

        print(String.format("%10s | %6s | %6s | %6s", "Directions", "Danger", "Attack", "Mixed coef " + coefAttack));
        for (Direction dir : Direction.values()) {
            int k = dir.ordinal();
            print(String.format("%10s | %6s | %6s | %6s ", dir.name(), scoresDanger[k], scoresAttack[k], mixedScore[k]) + directions[attackDirections[k]].name());

            // Indicator stuff
            /*if (rc.onTheMap(myLoc.add(dir))) {
                rc.setIndicatorDot(myLoc.add(dir), (int) (scoresDanger[k] * 30), rc.getTeam().ordinal() * 200, (int) (scoresAttack[k] * 30));
            }*/
        }

        return Tools.bestDirOfLong9(mixedScore);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////// PLAY        ////////////////////////////////////////////////////////////////
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Result play() throws GameActionException {
        // init scores
        initScore();
        Direction bestDir = updateScoresAndBestDir();

        // If too much danger on one cell, try place trap
        Direction bestDanger = Tools.bestDirOfLong9(scoresDanger);
        MapLocation bestDangerLoc = myLoc.add(bestDanger);
        if(scoresDanger[bestDanger.ordinal()] >= Micro.DANGER_IN_REACH * 4 && (scoresAttack[bestDir.ordinal()] + scoresDanger[bestDir.ordinal()] < Micro.ATTACK_THROW) ){
            if(rc.canPlaceRatTrap(bestDangerLoc)) {
                print("Too much danger, placing trap at " + bestDangerLoc);
                rc.canPlaceRatTrap(bestDangerLoc);
            }
        }

        // Check if we have action available
        if (scoresAttack[bestDir.ordinal()] == 0 && scoresDanger[bestDir.ordinal()] == 0) {
            if (rc.isMovementReady()) {
                PathFinding.smartMoveTo(nearestEnemyRat);
            }
            return new Result(END_OF_TURN, "No scores, I am too far or can't move");
        }


        // If best action is throw, check if action is valid
        // Else, deactivate throw and update scores
        if(isThrowAction[bestDir.ordinal()]){
            Result resultCheckThrow = checkThrow(bestDir);
            print("resultCheckThrow : " + resultCheckThrow.msg);

            if(resultCheckThrow.code == CANT) {
                throwEnable = false;
                initScore();
                bestDir = updateScoresAndBestDir();
            }
        }


        // If the best combo is attacking from the current cell, attack now,
        if (scoresAttack[8] >= scoresAttack[bestDir.ordinal()] && scoresAttack[8] > 0) {
            Result result = (isThrowAction[8]) ? playThrow() : playAttack();
            print("Attack result : " + result.msg);
        }


        // Else, move
        if (bestDir != Direction.CENTER) {
            if (!rc.canMove(bestDir)) {
                return new Result(ERR, "Can't move to best dir ! (So, it's not the best dir, we have a bug in updateScore()");
            }

            PathFinding.move(bestDir);
        }


        // And attack when on bestScore
        if(rc.isActionReady()){
            return (isThrowAction[8]) ? playThrow() : playAttack();
        }

        return new Result(END_OF_TURN, "End of attack micro");
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////// PLAY ATTACK ////////////////////////////////////////////////////////////////
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public Result playAttack() throws GameActionException {
        // Loading informations
        Direction attackDirection = directions[attackDirections[8]];
        MapLocation target = myLoc.add(attackDirection);

        // Direction center ?
        if (attackDirection == Direction.CENTER) {
            return new Result(CANT, "Can't attack center");
        }

        // See location
        if (!rc.canSenseLocation(target)) {
            print("Can't sense target, smartlook : " + VisionUtils.smartLookAt(target).msg);

            if(!rc.canSenseLocation(target)){
                return new Result(CANT, "Can't sense target");
            }
        }

        // Try to ratnap
        if (rc.canCarryRat(target)) {
            rc.carryRat(target);
            roundRatnap = rc.getRoundNum();
            return new Result(OK, "Ratnaped " + target);
        }

        // Try to attack
        if (rc.canAttack(target)) {
            rc.attack(target, min(3, rc.getRawCheese()));
            return new Result(OK, "Attacked " + target);
        }

        return new Result(CANT, "Nothing to do !");
    }


    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////////  THROW     /////////////////////////////////////////////////////////////////
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Result checkThrow(Direction from) throws GameActionException {
        // Ok  : All good, we can continue
        // Warn: Maybe, maybe not
        // CANT: Refresh scores;
        Direction throwDirection = directions[attackDirections[from.ordinal()]];
        MapLocation cellToCheck = myLoc.add(from).add(throwDirection);


        // If can sense and passable or myself
        if( rc.canSenseLocation(cellToCheck)
        && (
                rc.senseMapInfo(cellToCheck).isPassable()
            ||  (rc.canSenseRobotAtLocation(cellToCheck) && rc.senseRobotAtLocation(cellToCheck).getID() == rc.getID())
            )
        ){
            return new Result(OK, "Passable or myself");
        }

        // Turn to direction of throw and try check again
        if(rc.getDirection() == throwDirection){
            ///  Is this case even possible ?
            return new Result(WARN, "Can't check direction and already oriented to throwDirection");
        }

        // If can't turn
        if(!rc.canTurn()){
            return new Result(CANT, "Can't turn to throw direction");
        }

        // Turn and check again
        rc.turn(throwDirection);
        return checkThrow(from);
    }

    public Result playThrow() throws GameActionException {
        // Loading information
        Direction attackDirection = directions[attackDirections[8]];

        // Direction center ?
        if (attackDirection == Direction.CENTER) {
            return new Result(CANT, "Can't attack center");
        }

        // Looking at direction ?
        if (rc.getDirection() != attackDirection) {
            if (!rc.canTurn()) {
                return new Result(CANT, "Can't turn to throw direction");
            }

            rc.turn(attackDirection);
        }

        // Can throw ?
        if (!rc.canThrowRat()) {
            return new Result(CANT, "Can't throw rat");
        }

        rc.throwRat();
        return new Result(CANT, "Rat throw :D " + attackDirection);
    }
}
