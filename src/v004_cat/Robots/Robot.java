package v004_cat.Robots;

import battlecode.common.*;
import v004_cat.States.*;

import java.util.Random;

public class Robot {
    /////////////////////////////////////// Configuration ///////////////////////////////////////

    // All variables need to be static to be accessible from different states
    public static RobotController rc;
    public static MapLocation spawnLoc;
    public static MapLocation lastLocation;
    public static Direction lastDirection;
    public static int lastInitRound = 0;
    public static int spawnRound = 0;
    public static MapLocation nearestKing;
    public static boolean isKing;
    public static int round;

    public static boolean competitiveMode = false;

    // Phases
    public static int gamePhase;
    public static final int PHASE_START = 0;
    public static final int PHASE_EARLY = 1;
    public static final int PHASE_MIDLE = 2;
    public static final int PHASE_FINAL = 3;

    // Directions
    public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    public static final Random rng = new Random(6147);


    // State Machine
    public State init;
    public State endTurn;
    public State currentState;
    public void onNewTurn(){};
    public void updateState(Result previousResult){}
    public void init(){}

    /////////////////////////////////////// Run ///////////////////////////////////////

    public void run(RobotController rc) throws GameActionException {
        Robot.rc = rc;
        if(!competitiveMode && rc.getRoundNum() <= 2) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("   This bot is running with competitiveMode set to false    ");
            System.out.println("            Edit the flag before submitting it !            ");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            if(competitiveMode && rc.getTeam() == Team.A) {
                competitiveMode = false;
                System.out.println("Competitive mode set to false because we are in teamA.");
            }
        }

        // Init states
        header("Starting at round " + Clock.getBytecodeNum() + " bytecode " + Clock.getBytecodeNum());
        init = new Init();
        endTurn = new EndTurn();
        currentState = init;
        init();
        header("Done init at bytecode " + Clock.getBytecodeNum());

        // Playing
        while (true) {

            // Playing state
            header("\t" + currentState.name + "\t" + (int)(Clock.getBytecodeNum() * 10000 / 17500));
            Result result = currentState.run();
            if(!result.msg.isEmpty()) {
                print("<= " + result.code.name() + " " + result.msg);
                print("");
            }

            // Continue according to return state
            switch(result.code){
                case LOCK:
                    print("Lock: End turn and resume to state.");
                    endTurn.run();
                    init.run();
                    break;

                case END_OF_TURN:
                    print("Skipping to end of turn");
                    currentState = endTurn;
                    break;

                default: // Ok, Err, Warn
                    updateState(result);
                    break;
            }
        }
    }


    /////////////////////////////////////// Debug ///////////////////////////////////////
    public static void _debug(String msg){
        if(rc.getRoundNum() < 100 && rc.getTeam() == Team.A) {
            System.out.println(msg);
        }
    }
    public static void header(String msg)    {_debug(msg);}      // Important informations (state name, round, energy used)
    public static void print (String msg)    {_debug("\t" + msg);}     // Print inside state (target, action, etc.)
    public static void debug (String msg)    {_debug("\t\t" + msg);}   // Debug stuff
    public static void ddebug(String msg)    {_debug("\t\t\t" + msg);} // Debug stuff

    public static void warn  (String msg)     {_debug(" WW: " + msg);}
    public static void err(String msg)     {_debug(" EE: " + msg);}
}
