package controllers.multiPlayer.heuristics;

import controllers.multiPlayer.heuristics.StateHeuristicMulti;
import core.game.Observation;
import core.game.StateObservation;
import core.game.StateObservationMulti;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 11/02/14
 * Time: 15:44
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class SimpleStateHeuristic extends StateHeuristicMulti {

    double initialNpcCounter = 0;

    int state = STATE_EXPLORE;

    static final int STATE_EXPLORE = 0; //penalize lack of change of position / returning to last different position
    static final int STATE_COLLECT = 1; //gather resource (big weight)
    static final int STATE_ATTACK = 2; //chase avatar (big weight) && gather resource (small weight)
    static final int STATE_DEFEND = 3; //flee from avatar (big weight) && gather resource (medium weight)

    static final double BIG_WEIGHT = 2;
    static final double MEDIUM_WEIGHT = 1.5;
    static final double SMALL_WEIGHT = 1.2;


    static final int PENALTY = 50, BIG_PENALTY = -999999999, BONUS = 100000000;

    double distMoved;
    StateObservationMulti firstState;
    Vector2d targetPos;

    public SimpleStateHeuristic(StateObservationMulti stateObs) {
        if (firstState == null) firstState = stateObs;
    }

    public void setDistMoved(double distMoved) {
        this.distMoved = distMoved;
    }
    public void setFirstState(StateObservationMulti state) {
        this.firstState = state;
    }

    @Override
    public void setTargetPos(Vector2d pos) {
        this.targetPos = pos;
    }

    public double evaluateState(StateObservationMulti stateObs, int playerID, boolean changed, int state) {
        this.state = state;
        Vector2d avatarPosition = stateObs.getAvatarPosition(playerID);
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPosition);
        ArrayList<Observation>[] npcPositionsF = firstState.getNPCPositions(avatarPosition);
        ArrayList<Observation>[] resPostitions = stateObs.getResourcesPositions(avatarPosition);
        HashMap<Integer, Integer> resources = stateObs.getAvatarResources(playerID);
//        double distToImmov = stateObs.getImmovablePositions(avatarPosition)[0].get(0).sqDist;

        int resourceCount = 0;
        for (Object o : resources.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            resourceCount += (int) pair.getValue();
        }

        double won = 0;
        int oppID = (playerID + 1) % stateObs.getNoPlayers();
        Types.WINNER[] winners = stateObs.getMultiGameWinner();

        boolean bothWin = (winners[playerID] == Types.WINNER.PLAYER_WINS) && (winners[oppID] == Types.WINNER.PLAYER_WINS);
        boolean meWins  = (winners[playerID] == Types.WINNER.PLAYER_WINS) && (winners[oppID] == Types.WINNER.PLAYER_LOSES);
        boolean meLoses = (winners[playerID] == Types.WINNER.PLAYER_LOSES) && (winners[oppID] == Types.WINNER.PLAYER_WINS);
        boolean bothLose = (winners[playerID] == Types.WINNER.PLAYER_LOSES) && (winners[oppID] == Types.WINNER.PLAYER_LOSES);

        if(meWins || bothWin)
            won = BONUS;
        else if (meLoses)
            return BIG_PENALTY;


        double minDistanceNPC = Double.POSITIVE_INFINITY;
        double minDistanceRes = Double.POSITIVE_INFINITY;
//        Vector2d minObject = null;
//        int minNPC_ID = -1;
//        int minNPCType = -1;

        int npcCounter = 0;
        if (npcPositions != null) {
            for (ArrayList<Observation> npcs : npcPositions) {
                if(npcs.size() > 0)
                {
//                    minObject   = npcs.get(0).position; //This is the closest guy
//                    minDistance = npcs.get(0).sqDist;   //This is the (square) distance to the closest NPC.
//                    minNPC_ID   = npcs.get(0).obsID;    //This is the id of the closest NPC.
//                    minNPCType  = npcs.get(0).itype;    //This is the type of the closest NPC.
                    npcCounter += npcs.size();
                    for (Observation o : npcs) {
                        if (o.itype == 54)
                            minDistanceNPC = o.sqDist;
                    }
                }
            }
        }

        int npcCounterF = 0;
        if (npcPositionsF != null) {
            for (ArrayList<Observation> npcs : npcPositionsF) {
                if(npcs.size() > 0)
                {
                    npcCounterF += npcs.size();
                }
            }
        }

        if (resPostitions != null) {
            for (ArrayList<Observation> res : resPostitions) {
                if(res.size() > 0)
                {
                    minDistanceRes = res.get(0).sqDist;
                }
            }
        }

        double distanceToOpponent = avatarPosition.dist(stateObs.getAvatarPosition(oppID));
        double distance = Math.min(distanceToOpponent, minDistanceNPC);
        double distanceToTargetPos;
        if (targetPos != null)
            distanceToTargetPos = avatarPosition.dist(targetPos)/stateObs.getBlockSize();
        else distanceToTargetPos = 0;
        int hp = stateObs.getAvatarHealthPoints(playerID);

        double score = BIG_WEIGHT * firstState.getGameScore(playerID);
        score += won * BONUS;
        score -= SMALL_WEIGHT * (npcCounter - npcCounterF);
        score -= (avatarPosition.equals(stateObs.getAvatarPosition(oppID)) ? 1 : 0) * PENALTY;

        switch (this.state) {
            case STATE_COLLECT: score += MEDIUM_WEIGHT * resourceCount + hp * MEDIUM_WEIGHT + minDistanceRes; break;
            case STATE_ATTACK: score += - MEDIUM_WEIGHT * distance + SMALL_WEIGHT * resourceCount; break;
            case STATE_DEFEND: score += MEDIUM_WEIGHT * distance + SMALL_WEIGHT * resourceCount + hp * SMALL_WEIGHT; break;
            default: score += resourceCount -
                    (stateObs.getAvatarLastPosition(playerID).equals(avatarPosition)
                            || !changed ? 1 : 0) * PENALTY + distMoved
                    - distanceToTargetPos * MEDIUM_WEIGHT + hp * SMALL_WEIGHT;

        }

        return score;
    }


}


