package v_potato.Communication;

import v_potato.Utils.IntLIFO;

public class MessageLIFO {
    public static IntLIFO buffer0 = new IntLIFO(); // Common
    public static IntLIFO buffer1 = new IntLIFO(); // Important
    public static IntLIFO buffer2 = new IntLIFO(); // Emergency

    public static void add(int msg, int priority){
        switch (priority){
            case 0: buffer0.add(msg); return;
            case 1: buffer1.add(msg); return;
            case 2: buffer2.add(msg); return;
            default:
                System.out.println("ERR: Message priority should be in [0, 3] 0 is LOW, 3 is CRIT");
        }
    }

    public static int pop(){
        if(buffer2.size > 0){return buffer2.pop();}
        if(buffer1.size > 0){return buffer1.pop();}
        if(buffer0.size > 0){return buffer0.pop();}
        return 0;
    }
}
