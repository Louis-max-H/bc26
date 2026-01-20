package v005_saketh.States;

import static v005_saketh.States.Code.*;

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
