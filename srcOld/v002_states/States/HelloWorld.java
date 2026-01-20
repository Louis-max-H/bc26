package v002_states.States;

import static v002_states.States.Code.*;

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
