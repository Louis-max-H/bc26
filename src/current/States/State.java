package current.States;
import battlecode.common.GameActionException;
import current.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
