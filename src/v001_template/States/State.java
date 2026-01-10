package v001_template.States;
import battlecode.common.GameActionException;
import v001_template.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
