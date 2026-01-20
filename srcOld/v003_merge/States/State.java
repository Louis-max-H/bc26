package v003_merge.States;
import battlecode.common.GameActionException;
import v003_merge.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
