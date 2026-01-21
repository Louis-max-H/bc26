package v008_saketh.States;
import battlecode.common.GameActionException;
import v008_saketh.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
