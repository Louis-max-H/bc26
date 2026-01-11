package v02_myVersion.States;

import static v02_myVersion.States.Code.OK;

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
