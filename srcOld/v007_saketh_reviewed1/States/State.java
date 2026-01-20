package v007_saketh_reviewed1.States;
import battlecode.common.GameActionException;
import v007_saketh_reviewed1.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
