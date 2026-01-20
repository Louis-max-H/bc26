package v_potato.States;

import static v_potato.States.Code.OK;

public class Result {
    public Code code;
    public String msg;

    public Result(Code code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public Boolean isOk(){return code == OK;}
    public Boolean notOk(){return code != OK;}
}
