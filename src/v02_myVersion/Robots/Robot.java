package v02_myVersion.Robots;

import battlecode.common.*;
import v02_myVersion.States.*;
import v02_myVersion.Utils.Communication;

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
    public void updateState(Result previousResult){}
    public void init(){}



    /////////////////////////////////////// Run ///////////////////////////////////////

    public void run(RobotController rc) throws GameActionException {
        Robot.rc = rc;

        // Initialize communication system (only once)
        try {
            Communication.init(rc);
        } catch (Exception e) {
            err("Init exception: " + e.getMessage());
        }

        // Init states (only once)
        try {
            header("Starting at round " + Clock.getBytecodeNum() + " bytecode " + Clock.getBytecodeNum());
            init = new Init();
            endTurn = new EndTurn();
            currentState = init;
            init();
            header("Done init at bytecode " + Clock.getBytecodeNum());
        } catch (Exception e) {
            err("State init exception: " + e.getMessage());
        }

        // Main game loop - must never return or robot freezes
        while (true) {
            try {
                // Update shared array state at start of turn
                Communication.updateSharedArrayState(rc);
                
                // Rat kings ingest squeaks and update shared array
                if (rc.getType().isRatKingType()) {
                    Communication.ingestSqueaks(rc);
                }

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
                        // Don't update state, stay in current state
                        break;

                    case END_OF_TURN:
                        print("Skipping to end of turn");
                        currentState = endTurn;
                        // EndTurn will be called next iteration
                        break;

                    default: // Ok, Err, Warn, CANT
                        updateState(result);
                        break;
                }
            } catch (GameActionException e) {
                // Single catch at root - avoid 500 bytecode penalty from unhandled exceptions
                err("GameActionException: " + e.getMessage());
                // Continue loop after yielding
            } catch (Exception e) {
                // Catch any other exceptions to prevent robots from freezing
                err("Exception: " + e.getMessage());
                // Continue loop after yielding
            } catch (Throwable t) {
                // Catch even errors to be extra safe
                err("Throwable: " + t.getMessage());
                // Continue loop after yielding
            } finally {
                // Always yield at end of turn to prevent freezing
                // This ensures the robot never returns from run() method
                Clock.yield();
            }
        }
    }


    /////////////////////////////////////// Debug ///////////////////////////////////////
    public static void _debug(String msg){
        if(rc.getRoundNum() < 400 && rc.getTeam() == Team.A) {
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
