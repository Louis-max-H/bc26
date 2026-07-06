package bytecode;

import battlecode.common.*;
import current.Utils.MapLocationsWithId;

public class RobotPlayer {
    public static String msg;
    public static int start;

    // Benchmarking
    public int someInt = 1;
    public static int someStaticInt = 2;
    private static int somePrivateStaticInt = 3;
    private static int[] staticArray = new int[10];

    public static void start(String msg){
        RobotPlayer.msg = msg;
        RobotPlayer.start = Clock.getBytecodeNum();
    }

    public static void endCompare(int base) {
        int count = Clock.getBytecodeNum() - start - 3; // Minus 3 because we pass one parameter to the function
        int delta = count - base;

        if(count - base == 0){
        System.out.println(
            String.format("%-35s : %5d (===)",
                    msg, count
            )
        );
        }else{
            String signe = "+";
            if(count - base < 0){
                signe = "-";
                delta *= -1;
            }
            System.out.println(
                String.format("%-35s : %5d (%s%2d)",
                    msg, count,
                    signe, delta
                )
            );
        }
    }

    public static int end(){
        int count = Clock.getBytecodeNum() - start - 2;
        System.out.println(String.format("%-35s : %5d", msg, count));
        return count;
    }

    public static void foo(int i){
        // Do nothing
    }

    public static void foo(MapLocation loc){
        // Do nothing
    }

    public static void run(RobotController rc) throws GameActionException {
        start("Empty benchmark");
        end();

        start("Empty benchmark, saving result");
        int base = end();

        start("Empty benchmark, with compare");
        endCompare(base);


        /////////// Reading values ///////////////////////////////////////
        System.out.println("\n\nReading values:");
        int a;
        int b = 2;

        start("Affecting local variable");
        a = b;
        base = end();


        start("Affecting static");
        a = someStaticInt;
        endCompare(base);

        start("Affecting private static");
        a = somePrivateStaticInt;
        endCompare(base);

        start("Affecting int");
        a = 1;
        endCompare(base);


        /////////// Array operations ///////////////////////////////////////
        System.out.println("\n\nArray operations");
        char[] array;

        start("20 elements using new");
        array = new char[20];
        base = end();

        start("20 elements using toCharArray");
        array = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000".toCharArray();
        endCompare(base);

        String myString = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000";
        start("20 elements using StringBuilder");
        StringBuilder stringBuilder = new StringBuilder(myString);
        endCompare(base);


        start("Reading item []");
        a = array[2];
        base = end();

        start("Reading charAt");
        a = stringBuilder.charAt(2);
        endCompare(base);

        start("Writing item []");
        array[2] = 1;
        base = end();

        start("Writing charAt");
        stringBuilder.setCharAt(2, '\u0001');
        endCompare(base);


        /////////// Loops ///////////////////////////////////////
        System.out.println("\n\nLoops");
        // Cf https://cory.li/bytecode-hacking/ for more infos !

        start("Fori 10");
        for (int i = 0; i < 10; i++) {
            foo(i);
        }
        base = end();

        start("Fori reversed 10");
        for (int i = 9; i >= 0; i--) {
            foo(i);
        }
        endCompare(base);

        int size = 10;
        start("Switch case");
        switch(size){
            case 15: foo(15);
            case 14: foo(14);
            case 13: foo(13);
            case 12: foo(12);
            case 11: foo(11);
            case 10: foo(10);
            case  9: foo( 9);
            case  8: foo( 8);
            case  7: foo( 7);
            case  6: foo( 6);
            case  5: foo( 5);
            case  4: foo( 4);
            case  3: foo( 3);
            case  2: foo( 2);
            case  1: foo( 1);
            case 0: break;
            default:
                for (int i = 0; i < size; i++) {
                    foo(i);
                }
                break;
        }
        endCompare(base);

        /////////// Iterate on array ///////////////////////////////////////
        System.out.println("\n\nIterate on array");
        start("Fori reversed");
        for (int i = 9; i >= 0; i--) {
            foo(staticArray[i]);
        }
        base = end();

        int[] localArray = staticArray;
        start("Fori reversed + local array");
        for (int i = 9; i >= 0; i--) {
            foo(localArray[i]);
        }
        endCompare(base);

        start("Foreach + local array");
        for (int i: localArray) {
            foo(i);
        }
        endCompare(base);


        /////////// FastIterableLocSet ///////////////////////////////////////
        System.out.println("\n\nFastIterableLocSet");

        start("new FastIterableLocSet(100)");
        FastIterableLocSet fastIterableLocSet = new FastIterableLocSet(100);
        int baseInstanciation = end();

        start("Add MapLocation");
        fastIterableLocSet.add(new MapLocation(0, 0));
        int baseAddMapLocation = end();

        start("Add      x, y");
        fastIterableLocSet.add(1, 2);
        int baseAddXandY = end();

        start("Add      x, y already existing");
        fastIterableLocSet.add(1, 2);
        int baseAddXandYAlreadyExisting = end();

        start("Contains x, y");
        fastIterableLocSet.contains(1, 2);
        int baseContainsXandY = end();

        start("Remove   x, y");
        fastIterableLocSet.remove(1, 2);
        int baseRemoveXandY = end();

        // Setup 10 elements
        fastIterableLocSet.clear();
        for (int i = 0; i < 10; i++) {
            fastIterableLocSet.add(i, i);
        }

        start("Update iterable on 10 elements");
        fastIterableLocSet.updateIterable();
        int abseUpdate = end();

        start("Iterate 10 items, Fori reversed");
        for (int i = fastIterableLocSet.size - 1; i >= 0; i--) {
            foo(fastIterableLocSet.locs[i]);
        }
        int baseIterateOn10Elements = end();


        start("Clear");
        fastIterableLocSet.clear();
        int baseClear = end();


        /////////// MapLocations ///////////////////////////////////////
        System.out.println("\n\nMapLocations");
        start("new MapLocations(100)");
        MapLocations mapLocations = new MapLocations((char) 100);
        endCompare(baseInstanciation);

        MapLocation loc = new MapLocation(0, 0);
        start("Add MapLocation");
        mapLocations.add(loc);
        endCompare(baseAddMapLocation);

        System.out.println();
        start("Add      x, y");
        mapLocations.add(1, 2);
        endCompare(baseAddXandY);

        start("Add      x, y already existing");
        mapLocations.add(1, 2);
        endCompare(baseAddMapLocation);

        start("Contains x, y");
        mapLocations.contains(1, 2);
        endCompare(baseContainsXandY);

        start("Remove   x, y");
        mapLocations.remove(1, 2);
        endCompare(baseRemoveXandY);

        System.out.println();
        start("Add      xy");
        mapLocations.add(122); // 2 + 60*2
        endCompare(baseAddXandY);

        start("Add      xy already exist");
        mapLocations.add(122); // 2 + 60*2
        endCompare(baseAddXandY);

        start("Contains xy");
        mapLocations.contains(122); // 2 + 60*2
        endCompare(baseContainsXandY);

        start("Remove   xy");
        mapLocations.remove(122); // 2 + 60*2
        endCompare(baseRemoveXandY);

        System.out.println();

        /////////// MapLocationsWithID ///////////////////////////////////////
        System.out.println("\n\nMapLocationsWithID");
        start("new MapLocationsWithID(100)");
        MapLocationsWithId mapLocationsWithId = new MapLocationsWithId((char)100, true); // MaxSize, FlushWhenTrue
        end();

        MapLocation loc = new MapLocation(0, 0);
        int id = 13;
        start("Add MapLocation");
        mapLocationsWithId.add(loc, id);
        endCompare(baseAddMapLocation);

        System.out.println();
        start("Add      x, y");
        mapLocations.add(1, 2);
        endCompare(baseAddXandY);

        start("Add      x, y already existing");
        mapLocations.add(1, 2);
        endCompare(baseAddMapLocation);

        start("Contains x, y");
        mapLocations.contains(1, 2);
        endCompare(baseContainsXandY);

        start("Remove   x, y");
        mapLocations.remove(1, 2);
        endCompare(baseRemoveXandY);

        System.out.println();
        start("Add      xy");
        mapLocations.add(122); // 2 + 60*2
        endCompare(baseAddXandY);

        start("Add      xy already exist");
        mapLocations.add(122); // 2 + 60*2
        endCompare(baseAddXandY);

        start("Contains xy");
        mapLocations.contains(122); // 2 + 60*2
        endCompare(baseContainsXandY);

        start("Remove   xy");
        mapLocations.remove(122); // 2 + 60*2
        endCompare(baseRemoveXandY);

        System.out.println();


        // Setup 10 elements
        mapLocations.clear();
        for (int i = 0; i < 10; i++) {
            mapLocations.add(i + 60*i);
        }

        start("Update iterable on 10 elements");
        // Not needed
        endCompare(abseUpdate);


        start("Iterate 10 items, Fori reversed");
        for (int i = mapLocations.size - 1; i >= 0; i--) {
            foo(mapLocations.locs[i]);
        }
        endCompare(baseIterateOn10Elements);

        start("Clear");
        mapLocations.clear();
        endCompare(baseClear);

        /////////// Operations ///////////////////////////////////////
        System.out.println("\n\nOperations:");
        int x = 1;
        int y = 2;

        start("x << 6");
        a = x << 6;
        base = end();
        // System.out.println("Result : " + a);

        start("x * 65536");
        a = x * 65536;
        endCompare(base);
        // System.out.println("Result : " + a);

        start("x++");
        x++;
        base = end();

        start("++x");
        ++x;
        endCompare(base);

        x = 2;
        start("array[x] = 1; x++");
        array[x] = 1;
        x++;
        base = end();

        start("array[x++] = 1");
        array[x++] = 1;
        endCompare(base);

        start("int variable = 2;");
        int variable = 2;
        base = end();

        start("variable = 2;");
        variable = 2;
        endCompare(base);


        x = 0;
        start("f(a[x]); f(a[x]);");
        foo(array[x]);
        foo(array[x]);
        base = end();

        start("i=a[x]; f(i) f(i)");
        int i = array[x];
        foo(i);
        foo(i);
        endCompare(base);



        System.out.println("\n\n\n\n\n\n\n");
        rc.resign();

    }
}
