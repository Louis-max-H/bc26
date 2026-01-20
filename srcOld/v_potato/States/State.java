package v_potato.States;
import battlecode.common.GameActionException;
import v_potato.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
