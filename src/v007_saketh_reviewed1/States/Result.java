package v007_saketh_reviewed1.States;

import static v007_saketh_reviewed1.States.Code.OK;

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
