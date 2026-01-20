package current;

import current.Robots.Robot;

public class Params {

    // Aggressivity, depending on map and phase of the game
    public static int PARAMS_aggressivitySmall1 = 50; // [1, 100]
    public static int PARAMS_aggressivitySmall2 = 70; // [1, 100]
    public static int PARAMS_aggressivitySmall3 = 85; // [1, 100]
    public static int PARAMS_aggressivitySmall4 = 95; // [1, 100]
    public static int PARAMS_aggressivityMedium1 = 40; // [1, 100]
    public static int PARAMS_aggressivityMedium2 = 60; // [1, 100]
    public static int PARAMS_aggressivityMedium3 = 80; // [1, 100]
    public static int PARAMS_aggressivityMedium4 = 90; // [1, 100]
    public static int PARAMS_aggressivityLarge1 = 30; // [1, 100]
    public static int PARAMS_aggressivityLarge2 = 50; // [1, 100]
    public static int PARAMS_aggressivityLarge3 = 70; // [1, 100]
    public static int PARAMS_aggressivityLarge4 = 85; // [1, 100]
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
            case 0 -> new int[]{PARAMS_aggressivitySmall1,PARAMS_aggressivitySmall2,PARAMS_aggressivitySmall3,PARAMS_aggressivitySmall4};
            case 1 -> new int[]{PARAMS_aggressivityMedium1,PARAMS_aggressivityMedium2,PARAMS_aggressivityMedium3,PARAMS_aggressivityMedium4};
            case 2 -> new int[]{PARAMS_aggressivityLarge1,PARAMS_aggressivityLarge2,PARAMS_aggressivityLarge3,PARAMS_aggressivityLarge4};
            default -> throw new IllegalStateException("Unexpected value: Maptype " + Robot.mapType);
        };
    }
}
