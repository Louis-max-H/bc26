package current.Robots;

import battlecode.common.*;
import current.States.*;
import current.Utils.MapLocations;
import current.Utils.MapLocationsWithId;
import current.Utils.Tools;

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
    public static boolean isKing;
    public static MapLocation myLoc;
    public static int round;

    // Meta variables
    public static boolean moveRandom = true;
    public static boolean competitiveMode = true;

    // Nearest locations
    public static MapLocation nearestCat;
    public static MapLocation nearestRat;
    public static MapLocation nearestKing;
    public static MapLocation nearestEnemyRat;
    public static MapLocation nearestEnemyKing;
    public static MapLocation nearestMine;
    public static MapLocation nearestDirt;
    public static MapLocation nearestWater;
    public static MapLocation nearestCheese;

    // Nearest ID, ID are mod 4096
    public static int nearestEnemyRatID;
    public static int nearestCatID;
    public static int nearestKingID;
    public static int nearestEnemyKingID;

    // Memory
    public static MapLocations rats = new MapLocations((char) 100);
    public static MapLocations cheeseMines = new MapLocations((char) 150);
    public static MapLocations cheeseMinesFromArray = new  MapLocations((char) 150);
    public static MapLocationsWithId allyRats = new MapLocationsWithId((char) 15, true);
    public static MapLocationsWithId enemiesRats = new MapLocationsWithId((char) 30, true);
    public static MapLocationsWithId cats = new MapLocationsWithId((char) 100, true);
    public static MapLocationsWithId kings = new MapLocationsWithId((char) 100, true);
    public static MapLocationsWithId enemiesKings = new MapLocationsWithId((char) 100, true);

    // Direction of units
    public static char[] directionAllyRats = Tools.arrayOf4096Chars();
    public static char[] directionEnemyRats = Tools.arrayOf4096Chars();

    // Messages
    public static int PRIORITY_CRIT   = 3; // King being attacked, rush order
    public static int PRIORITY_HIGH   = 2; // Enemy in view, cat position
    public static int PRIORITY_NORMAL = 1; // King position, cheese mine

    // Phases
    public static int gamePhase;
    public static int mapType; // 0 Small, 1 Medium, 2 Large
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
    public State avoidCat;
    public State endTurn;
    public State currentState;
    public void onNewTurn(){};
    public void updateState(Result previousResult){}
    public void init() throws GameActionException {}

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
        header("Starting at round " + Clock.getBytecodeNum());
        init = new Init();
        avoidCat = new AvoidCat();
        endTurn = new EndTurn();
        currentState = init;
        init();
        header("Done init at bytecode " + Clock.getBytecodeNum());

        // Playing
        while (true) {
            if(!competitiveMode && round > 400) {
                rc.resign();
            }

            // Playing state
            header("\t" + currentState.name + "\t" + Clock.getBytecodeNum());
            Result result = currentState.run();
            if(!result.msg.isEmpty()) {
                print("<= " + result.code.name() + " " + result.msg);
                print("");
            }

            // Continue according to return state
            switch(result.code){
                case LOCK:
                    print("Lock: End turn and resume to state.");
                    rc.setIndicatorString("LOCK " + currentState.name);
                    endTurn.run();
                    init.run();
                    avoidCat.run();
                    break;

                case END_OF_TURN:
                    rc.setIndicatorString("END_OF_TURN " + currentState.name);
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
        if(round < 400 && rc.getTeam() == Team.A) {
            System.out.println(msg);
        }
    }
    public static void header(String msg)    {_debug(msg);}      // Important informations (state name, round, energy used)
    public static void print (String msg)    {_debug("\t\t" + msg);}     // Print inside state (target, action, etc.)
    public static void printBytecode(String msg)    {_debug("\t\t" + msg + " \t" + Clock.getBytecodeNum());}     // Print inside state (target, action, etc.)
    public static void debug (String msg)    {_debug("\t\t\t" + msg);}   // Debug stuff
    public static void ddebug(String msg)    {_debug("\t\t\t\t" + msg);} // Debug stuff

    public static void warn  (String msg)     {_debug(" WW: " + msg);}
    public static void err(String msg)     {_debug(" EE: " + msg);}
}
