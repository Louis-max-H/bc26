package v004_cat.States;
import battlecode.common.GameActionException;
import v004_cat.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
