package v004_cat.Utils;

import battlecode.common.*;
import v004_cat.RobotPlayer;
import v004_cat.States.Result;

import java.util.Map;

import static java.lang.Math.max;
import static v004_cat.States.Code.*;

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
