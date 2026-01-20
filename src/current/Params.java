package current;

import current.Robots.Robot;

public class Params {

    // Aggressivity, depending on MAP_ and phase of the game
    public static int PARAMS_aggressivityMAP_SMALL1 = 50; // [1, 100]
    public static int PARAMS_aggressivityMAP_SMALL2 = 70; // [1, 100]
    public static int PARAMS_aggressivityMAP_SMALL3 = 85; // [1, 100]
    public static int PARAMS_aggressivityMAP_SMALL4 = 95; // [1, 100]
    public static int PARAMS_aggressivityMAP_MEDIUM1 = 40; // [1, 100]
    public static int PARAMS_aggressivityMAP_MEDIUM2 = 60; // [1, 100]
    public static int PARAMS_aggressivityMAP_MEDIUM3 = 80; // [1, 100]
    public static int PARAMS_aggressivityMAP_MEDIUM4 = 90; // [1, 100]
    public static int PARAMS_aggressivityMAP_LARGE1 = 30; // [1, 100]
    public static int PARAMS_aggressivityMAP_LARGE2 = 50; // [1, 100]
    public static int PARAMS_aggressivityMAP_LARGE3 = 70; // [1, 100]
    public static int PARAMS_aggressivityMAP_LARGE4 = 85; // [1, 100]
    public static int[] aggresivity;

    public static int PARAMS_DANGER_OUT_OF_ENEMY_VIEW      = -3; // [-30, -1]
    public static int PARAMS_DANGER_IN_ENEMY_VIEW          =  6; // [1, 30]
    public static int PARAMS_DANGER_IN_REACH               = 22; // [1, 30]
    public static int PARAMS_DANGER_IN_REACH_WITH_MOVEMENT = 20; // [1, 30]
    public static int PARAMS_ATTACK_BONUS_FIRSTMOVE        = 14; // [1, 30]
    public static int PARAMS_ATTACK_BONUS_DONT_MOVE        = 17; // [1, 30]
    public static int PARAMS_ATTACK_BONUS_CANT_SEE         = 10; // [1, 30]

    public static void init(){
        aggresivity = switch (Robot.mapType){
            case 0 -> new int[]{PARAMS_aggressivityMAP_SMALL1,PARAMS_aggressivityMAP_SMALL2,PARAMS_aggressivityMAP_SMALL3,PARAMS_aggressivityMAP_SMALL4};
            case 1 -> new int[]{PARAMS_aggressivityMAP_MEDIUM1,PARAMS_aggressivityMAP_MEDIUM2,PARAMS_aggressivityMAP_MEDIUM3,PARAMS_aggressivityMAP_MEDIUM4};
            case 2 -> new int[]{PARAMS_aggressivityMAP_LARGE1,PARAMS_aggressivityMAP_LARGE2,PARAMS_aggressivityMAP_LARGE3,PARAMS_aggressivityMAP_LARGE4};
            default -> throw new IllegalStateException("Unexpected value: MAP_type " + Robot.mapType);
        };
    }
}
