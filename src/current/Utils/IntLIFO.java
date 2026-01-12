package current.Utils;

public class IntLIFO {
    public int[] content = new int[100];
    public int size = 0;

    public void add(int c){
        if(size == 100){reset();}
        content[size] = c;
        size++;
    }

    public int pop(){
        return content[--size];
    }

    public void reset(){
        size = 0;
    }
}
