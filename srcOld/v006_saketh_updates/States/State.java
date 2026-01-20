package v006_saketh_updates.States;
import battlecode.common.GameActionException;
import v006_saketh_updates.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
