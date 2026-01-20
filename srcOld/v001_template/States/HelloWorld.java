package v001_template.States;

import static v001_template.States.Code.*;

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
