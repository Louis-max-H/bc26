package v007_saketh_reviewed1.States;

import static v007_saketh_reviewed1.States.Code.*;

public class HelloWorld extends State {
    public HelloWorld(){
        this.name = "HelloWorld";
    }

    @Override
    public Result run(){
        print("Hello World!");
        return new Result(OK, "");
    };
}
