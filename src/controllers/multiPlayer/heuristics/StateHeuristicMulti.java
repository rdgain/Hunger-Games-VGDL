package controllers.multiPlayer.heuristics;

import core.game.StateObservationMulti;
import tools.Vector2d;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 11/02/14
 * Time: 15:43
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public abstract class StateHeuristicMulti {

    abstract public double evaluateState(StateObservationMulti stateObs, int playerID, boolean changed, int state);
    abstract public void setDistMoved(double distMoved);
    abstract public void setFirstState(StateObservationMulti state);
    abstract public void setTargetPos(Vector2d pos);
}
