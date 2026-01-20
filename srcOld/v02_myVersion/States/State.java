package v02_myVersion.States;
import battlecode.common.GameActionException;
import v02_myVersion.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
