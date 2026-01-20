package v005_saketh.States;
import battlecode.common.GameActionException;
import v005_saketh.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
