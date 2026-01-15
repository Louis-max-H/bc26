package v006_saketh_updates.States;

import static v006_saketh_updates.States.Code.OK;

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
