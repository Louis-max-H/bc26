package best_config_62165960.States;
import battlecode.common.GameActionException;
import best_config_62165960.Robots.Robot;

public abstract class State extends Robot {
    public String name;
    public abstract Result run() throws GameActionException;
}
