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

    static final int BIG_WEIGHT = 3;
    static final int MEDIUM_WEIGHT = 2;
    static final int SMALL_WEIGHT = 1;


    static final int PENALTY = 100, BIG_PENALTY = -999999999, BONUS = 100000000;

    double distMoved;
    StateObservationMulti firstState;

    public SimpleStateHeuristic(StateObservationMulti stateObs) {

    }

    public void setDistMoved(double distMoved) {
        this.distMoved = distMoved;
    }
    public void setFirstState(StateObservationMulti state) {
        this.firstState = state;
    }

    public double evaluateState(StateObservationMulti stateObs, int playerID, boolean changed, int state) {
        if (firstState == null) firstState = stateObs;
        this.state = state;
        Vector2d avatarPosition = stateObs.getAvatarPosition(playerID);
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPosition);
        HashMap<Integer, Integer> resources = stateObs.getAvatarResources(playerID);
        double distToImmov = stateObs.getImmovablePositions(avatarPosition)[0].get(0).sqDist;

        int resourceCount = 0;
        for (Object o : resources.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            resourceCount += (int) pair.getValue();
        }


        ArrayList<Observation>[] npcPositionsNotSorted = stateObs.getNPCPositions();

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


        double minDistance = Double.POSITIVE_INFINITY;
        Vector2d minObject = null;
        int minNPC_ID = -1;
        int minNPCType = -1;

        int npcCounter = 0;
        if (npcPositions != null) {
            for (ArrayList<Observation> npcs : npcPositions) {
                if(npcs.size() > 0)
                {
                    minObject   = npcs.get(0).position; //This is the closest guy
                    minDistance = npcs.get(0).sqDist;   //This is the (square) distance to the closest NPC.
                    minNPC_ID   = npcs.get(0).obsID;    //This is the id of the closest NPC.
                    minNPCType  = npcs.get(0).itype;    //This is the type of the closest NPC.
                    npcCounter += npcs.size();
                }
            }
        }

        int npcCounterF = 0;
        if (npcPositions != null) {
            for (ArrayList<Observation> npcs : npcPositions) {
                if(npcs.size() > 0)
                {
                    minObject   = npcs.get(0).position; //This is the closest guy
                    minDistance = npcs.get(0).sqDist;   //This is the (square) distance to the closest NPC.
                    minNPC_ID   = npcs.get(0).obsID;    //This is the id of the closest NPC.
                    minNPCType  = npcs.get(0).itype;    //This is the type of the closest NPC.
                    npcCounterF += npcs.size();
                }
            }
        }

        double distance = stateObs.getAvatarPosition(playerID).dist(stateObs.getAvatarPosition(oppID));
        int hp = stateObs.getAvatarHealthPoints(playerID);

        double score = firstState.getGameScore(playerID);
        score += won * BONUS;
        score -= SMALL_WEIGHT * (npcCounter - npcCounterF);
        score -= (avatarPosition.equals(stateObs.getAvatarPosition(oppID)) ? 1 : 0) * PENALTY;

        switch (this.state) {
            case STATE_COLLECT: score += BIG_WEIGHT * resourceCount + hp * MEDIUM_WEIGHT; break;
            case STATE_ATTACK: score += - BIG_WEIGHT * distance + SMALL_WEIGHT * resourceCount; break;
            case STATE_DEFEND: score += BIG_WEIGHT * distance + MEDIUM_WEIGHT * resourceCount + hp * BIG_WEIGHT; break;
            default: score += SMALL_WEIGHT * resourceCount -
                    (stateObs.getAvatarLastPosition(playerID).equals(avatarPosition)
                            || !changed ? 1 : 0) * PENALTY + distMoved * BIG_WEIGHT + hp * BIG_WEIGHT;

        }

        return score;
    }


}


