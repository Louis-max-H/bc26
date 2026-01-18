package current.Utils;

import battlecode.common.*;
import current.RobotPlayer;
import current.States.Result;

import java.util.Map;

import static java.lang.Math.max;
import static current.States.Code.*;

public class Tools {
    static int maxInt9(int[] array){
        /**
         * Max of an array of 8 elements
         * */
        return max(
            max(
                max(
                    max(array[0], array[1]),
                    max(array[2], array[3])
                ),
                max(
                    max(array[4], array[5]),
                    max(array[6], array[7])
                )
            ), array[8]
        );
    }

    public static Direction bestDirOfInt9(int[] array){
        int max = 0;
        Direction bestDir = Direction.CENTER;
        for(Direction dir: Direction.values()){
            if(array[dir.ordinal()] > max){
                max = array[dir.ordinal()];
                bestDir = dir;
            }
        }
        return bestDir;
        
    }

    public static int[] toInt9(char[] array){
        /**
         * convert char[8] to int[8]
         * */
        return new int[]{
            array[0],
            array[1],
            array[2],
            array[3],
            array[4],
            array[5],
            array[6],
            array[7],
            array[8]
        };
    }
}
