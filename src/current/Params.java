package current;

import current.Robots.Robot;

public class Params {

    // Aggressivity, depending on map and phase of the game
    public static int PARAMS_aggressivitySmall1 = 0; // [1, 100]
    public static int PARAMS_aggressivitySmall2 = 0; // [1, 100]
    public static int PARAMS_aggressivitySmall3 = 0; // [1, 100]
    public static int PARAMS_aggressivitySmall4 = 0; // [1, 100]
    public static int PARAMS_aggressivityMedium1 = 0; // [1, 100]
    public static int PARAMS_aggressivityMedium2 = 0; // [1, 100]
    public static int PARAMS_aggressivityMedium3 = 0; // [1, 100]
    public static int PARAMS_aggressivityMedium4 = 0; // [1, 100]
    public static int PARAMS_aggressivityLarge1 = 0; // [1, 100]
    public static int PARAMS_aggressivityLarge2 = 0; // [1, 100]
    public static int PARAMS_aggressivityLarge3 = 0; // [1, 100]
    public static int PARAMS_aggressivityLarge4 = 0; // [1, 100]
    public static int[] aggresivity;

    public static int PARAMS_DANGER_OUT_OF_ENEMY_VIEW      = -1; // [1, 30]
    public static int PARAMS_DANGER_IN_ENEMY_VIEW          =  1; // [1, 30]
    public static int PARAMS_DANGER_IN_REACH               = 15; // [1, 30]
    public static int PARAMS_DANGER_IN_REACH_WITH_MOVEMENT = 15; // [1, 30]
    public static int PARAMS_ATTACK_BONUS_FIRSTMOVE        = 10; // [1, 30]
    public static int PARAMS_ATTACK_BONUS_DONT_MOVE        = 10; // [1, 30]
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
