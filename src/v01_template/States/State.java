package v01_template.States;
import battlecode.common.GameActionException;
import v01_template.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
