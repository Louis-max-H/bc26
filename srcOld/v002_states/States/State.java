package v002_states.States;
import battlecode.common.GameActionException;
import v002_states.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
