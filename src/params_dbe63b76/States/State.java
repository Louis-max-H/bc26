package params_dbe63b76.States;
import battlecode.common.GameActionException;
import params_dbe63b76.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
