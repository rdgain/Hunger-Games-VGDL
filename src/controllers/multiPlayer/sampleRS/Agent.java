package controllers.multiPlayer.sampleRS;

import controllers.multiPlayer.heuristics.StateHeuristicMulti;
import controllers.multiPlayer.heuristics.WinScoreHeuristic;
import core.game.StateObservationMulti;
import core.player.AbstractMultiPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;

public class Agent extends AbstractMultiPlayer {

    // variable
    private int SIMULATION_DEPTH = 20;
    private double DISCOUNT = 0.99;

    // constants
    private final long BREAK_MS = 10;
    public static final double epsilon = 1e-6;
    static final int POINT1_CROSS = 0;
    static final int UNIFORM_CROSS = 1;

    private ArrayList<Individual> population;
    private int NUM_INDIVIDUALS;
    private int[] N_ACTIONS;
    private HashMap<Integer, Types.ACTIONS>[] action_mapping;

    private ElapsedCpuTimer timer;
    private Random randomGenerator;

    private StateHeuristicMulti heuristic;
    private double acumTimeTakenEval = 0,avgTimeTakenEval = 0;
    private int numEvals = 0;
    private long remaining;

    Vector2d lastDiffPos;
    int ATTACK_DIST = 10;
    int RESOURCE_DIST = 3;

    int playerID, opponentID, noPlayers;

    /**
     * Public constructor with state observation and time due.
     *
     * @param stateObs     state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer, int playerID) {
        randomGenerator = new Random();
        heuristic = new WinScoreHeuristic(stateObs);
        this.timer = elapsedTimer;

        // Get multiplayer game parameters
        this.playerID = playerID;
        noPlayers = stateObs.getNoPlayers();
        opponentID = (playerID+1)%noPlayers;
        lastDiffPos = stateObs.getAvatarPosition(playerID);
        ATTACK_DIST *= stateObs.getBlockSize();
        RESOURCE_DIST *= stateObs.getBlockSize();

        // INITIALISE POPULATION
        init_pop(stateObs);
    }

    @Override
    public Types.ACTIONS act(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer) {
        this.timer = elapsedTimer;
        numEvals = 0;
        acumTimeTakenEval = 0;
        remaining = timer.remainingTimeMillis();
        NUM_INDIVIDUALS = 0;

        // INITIALISE POPULATION
        init_pop(stateObs);

        // RETURN ACTION
        Types.ACTIONS best = get_best_action(population);
        return best;
    }


    /**
     * Evaluates an individual by rolling the current state with the actions in the individual
     * and returning the value of the resulting state; random action chosen for the opponent
     * @param individual - individual to be valued
     * @param heuristic - heuristic to be used for state evaluation
     * @param state - current state, root of rollouts
     * @return - value of last state reached
     */
    private double evaluate(Individual individual, StateHeuristicMulti heuristic, StateObservationMulti state) {

        //decide state we're in
        Vector2d thispos = state.getAvatarPosition(playerID);
        boolean changed = false;
        if (!thispos.equals(lastDiffPos)) {lastDiffPos = thispos; changed = true;}

        int fsm = 0;
        if (isOpponentInRange(state)) {
            if (canFight(state)) {
                fsm = 2;
            } else fsm = 3;
        } else if (isResourceNear(state)) {
            fsm = 1;
        }

        ElapsedCpuTimer elapsedTimerIterationEval = new ElapsedCpuTimer();

        StateObservationMulti st = state.copy();
        int i;
        for (i = 0; i < SIMULATION_DEPTH; i++) {
            double acum = 0, avg;
            if (! st.isGameOver()) {
                ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

                // Multi player advance method
                Types.ACTIONS[] advanceActs = new Types.ACTIONS[noPlayers];
                for (int k = 0; k < noPlayers; k++) {
                    if (k == playerID)
                        advanceActs[k] = action_mapping[k].get(individual.actions[i]);
                    else advanceActs[k] = action_mapping[k].get(randomGenerator.nextInt(N_ACTIONS[k]));
                }
                st.advance(advanceActs);

                acum += elapsedTimerIteration.elapsedMillis();
                avg = acum / (i+1);
                remaining = timer.remainingTimeMillis();
                if (remaining < 2*avg || remaining < BREAK_MS) break;
            } else {
                break;
            }
        }

        heuristic.setDistMoved(thispos.dist(st.getAvatarPosition(playerID)));
        StateObservationMulti first = st.copy();
        double value = heuristic.evaluateState(first, playerID, changed, fsm);

        // Apply discount factor
        value *= Math.pow(DISCOUNT,i);

        individual.value = value;

        numEvals++;
        acumTimeTakenEval += (elapsedTimerIterationEval.elapsedMillis());
        avgTimeTakenEval = acumTimeTakenEval / numEvals;
        remaining = timer.remainingTimeMillis();

        return value;
    }

    boolean isOpponentInRange(StateObservationMulti state) {
        return state.getAvatarPosition(playerID).dist(state.getAvatarPosition(opponentID)) < ATTACK_DIST;
    }

    boolean canFight(StateObservationMulti state) {
        return state.getAvatarHealthPoints(playerID) > state.getAvatarHealthPoints(opponentID);
    }

    boolean isResourceNear(StateObservationMulti state) {
        try {
            return state.getResourcesPositions(state.getAvatarPosition(playerID))[0].get(0).sqDist < RESOURCE_DIST;
        } catch (Exception e) {}
        return false;
    }

    /**
     * Insert a new individual into the population at the specified position by replacing the old one.
     * @param newind - individual to be inserted into population
     * @param pop - population
     * @param idx - position where individual should be inserted
     * @param stateObs - current game state
     */
    private void add_individual(Individual newind, Individual[] pop, int idx, StateObservationMulti stateObs) {
        evaluate(newind, heuristic, stateObs);
        pop[idx] = newind.copy();
    }

    /**
     * Initialize population
     * @param stateObs - current game state
     */
    private void init_pop(StateObservationMulti stateObs) {

        double remaining = timer.remainingTimeMillis();

        N_ACTIONS = new int[noPlayers];
        action_mapping = new HashMap[noPlayers];
        for (int i = 0; i < noPlayers; i++) {
            ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions(i);
            N_ACTIONS[i] = actions.size() + 1;
            action_mapping[i] = new HashMap<>();
            int k = 0;
            for (Types.ACTIONS action : actions) {
                action_mapping[i].put(k, action);
                k++;
            }
            action_mapping[i].put(k, Types.ACTIONS.ACTION_NIL);
        }

        NUM_INDIVIDUALS = 0;

        population = new ArrayList<>();
        do {
            Individual newInd = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID], randomGenerator);
            evaluate(newInd, heuristic, stateObs);
            population.add(newInd);
            remaining = timer.remainingTimeMillis();
            NUM_INDIVIDUALS++;

        } while(remaining > avgTimeTakenEval && remaining > BREAK_MS);

        if (NUM_INDIVIDUALS > 1)
            Collections.sort(population, new Comparator<Individual>() {
                @Override
                public int compare(Individual o1, Individual o2) {
                    if (o1 == null && o2 == null) {
                        return 0;
                    }
                    if (o1 == null) {
                        return 1;
                    }
                    if (o2 == null) {
                        return -1;
                    }
                    return o1.compareTo(o2);
                }});
    }

    /**
     * @param pop - last population obtained after evolution
     * @return - first action of best individual in the population (found at index 0)
     */
    private Types.ACTIONS get_best_action(ArrayList<Individual> pop) {
        int bestAction = pop.get(0).actions[0];
        return action_mapping[playerID].get(bestAction);
    }

}
