package current;

import current.Robots.Robot;

public class Params {

    // Aggressivity, depending on MAP_ and phase of the game
    public static int PARAMS_aggressivityMAP_SMALL1 = 50; // [1, 100]
    public static int PARAMS_aggressivityMAP_MEDIUM1 = 40; // [1, 100]
    public static int PARAMS_aggressivityMAP_LARGE1 = 30; // [1, 100]
    public static int[] aggresivity;

    public static int PARAMS_DANGER_OUT_OF_ENEMY_VIEW      = -3; // [-30, -1]
    public static int PARAMS_DANGER_IN_ENEMY_VIEW          =  6; // [1, 30]
    public static int PARAMS_DANGER_IN_REACH               = 22; // [1, 30]
    public static int PARAMS_DANGER_IN_REACH_WITH_MOVEMENT = 20; // [1, 30]
    public static int PARAMS_ATTACK_BONUS_FIRSTMOVE        = 14; // [1, 30]
    public static int PARAMS_ATTACK_BONUS_DONT_MOVE        = 17; // [1, 30]
    public static int PARAMS_ATTACK_BONUS_CANT_SEE         = 10; // [1, 30]

    public static int PARAMS_MAX_RATS_MAP_SMALL  = 20; // [15, 40]
    public static int PARAMS_MAX_RATS_MAP_MEDIUM = 20; // [15, 60]
    public static int PARAMS_MAX_RATS_MAP_LARGE  = 30; // [15, 60]
    public static int maxRats;

    // Not tested since we haven't prefix P A R A M S
    public static int MAX_CHEESE_MAP_SMALL = 35; // [15, 40]
    public static int MAX_CHEESE_MAP_MEDIUM = 40; // [15, 60]
    public static int MAX_CHEESE_MAP_LARGE = 45; // [15, 60]
    public static int maxCheese;

    public static void init(){
        maxCheese = switch (Robot.mapType){
            case 0 -> MAX_CHEESE_MAP_SMALL;
            case 1 -> MAX_CHEESE_MAP_MEDIUM;
            case 2 -> MAX_CHEESE_MAP_LARGE;
            default -> throw new IllegalStateException("Unexpected value: MAP_type " + Robot.mapType);
        };

        maxRats = switch (Robot.mapType){
            case 0 -> PARAMS_MAX_RATS_MAP_SMALL;
            case 1 -> PARAMS_MAX_RATS_MAP_MEDIUM;
            case 2 -> PARAMS_MAX_RATS_MAP_LARGE;
            default -> throw new IllegalStateException("Unexpected value: MAP_type " + Robot.mapType);
        };
        // maxRats = 4;

        aggresivity = switch (Robot.mapType){
            case 0 -> new int[]{PARAMS_aggressivityMAP_SMALL1,PARAMS_aggressivityMAP_SMALL1,PARAMS_aggressivityMAP_SMALL1*10,PARAMS_aggressivityMAP_SMALL1*10};
            case 1 -> new int[]{PARAMS_aggressivityMAP_MEDIUM1,PARAMS_aggressivityMAP_MEDIUM1,PARAMS_aggressivityMAP_MEDIUM1*10,PARAMS_aggressivityMAP_MEDIUM1*10};
            case 2 -> new int[]{PARAMS_aggressivityMAP_LARGE1,PARAMS_aggressivityMAP_LARGE1,PARAMS_aggressivityMAP_LARGE1*10,PARAMS_aggressivityMAP_LARGE1*10};
            default -> throw new IllegalStateException("Unexpected value: MAP_type " + Robot.mapType);
        };
    }
}
