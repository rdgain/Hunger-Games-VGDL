package controllers.multiPlayer.RHEA;

import controllers.multiPlayer.RHEA.sampleOLMCTS.SingleTreeNode;
import controllers.multiPlayer.heuristics.SimpleStateHeuristic;
import controllers.multiPlayer.heuristics.WinScoreHeuristic;
import controllers.multiPlayer.heuristics.StateHeuristicMulti;
import core.game.Observation;
import core.game.StateObservationMulti;
import core.player.AbstractMultiPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;
import tools.Vector2d;

import java.awt.*;
import java.util.*;

public class Agent extends AbstractMultiPlayer {

    // variable

    /**
     * Set in Player class instead for jar functionality
     */

    private int POPULATION_SIZE = 5;
    public static int SIMULATION_DEPTH = 8;
    private int INIT_TYPE = Agent.INIT_RANDOM;
    public static int MAX_FM_CALLS = 350;
    private int HEURISTIC_TYPE = Agent.HEURISTIC_WINSCORE;
    private int MACRO_ACTION_LENGTH = 1;

    private int CROSSOVER_TYPE = UNIFORM_CROSS; // 0 - 1point; 1 - uniform

    // set
    private boolean REEVALUATE = false;
    private boolean REPLACE = false;
    private int MUTATION = 1;
    private int TOURNAMENT_SIZE = 2;
    private int ELITISM = 1;
    private double DISCOUNT = 1; //0.99;

    // constants
    public static final double epsilon = 1e-6;

    static final int POINT1_CROSS = 0;
    static final int UNIFORM_CROSS = 1;
    private static final int INIT_RANDOM = 0;
    private static final int INIT_ONESTEP = 1;
    private static final int INIT_MCTS = 2;

    static final int HEURISTIC_WINSCORE = 0;
    public static final int HEURISTIC_SIMPLESTATE = 1;

    private static int MCTS_BUDGET;
    public static int ONESTEP_BUDGET;

    private Individual[] population, nextPop;
    private int[] N_ACTIONS;

    private ElapsedCpuTimer timer;

    private HashMap<Integer, Types.ACTIONS>[] action_mapping;
    private Random randomGenerator;

    private StateHeuristicMulti heuristic;

    // number of evaluations
    private int numEvals = 0;
    private int numCalls = 0;
    private int numPop = 0;

    private double acumTimeTakenEval, avgTimeTaken;


    //MACRO ACTIONS

    private int m_actionsLeft;
    private int m_lastMacroAction;
    private boolean m_throwPop;

    private Vector2d lastDiffPos, targetPos, thispos;
    private int ATTACK_DIST = 5;
    private int RESOURCE_DIST = 5;
    private double ATTACK_BREAK = 0.5; // chance to recklessly attack enemy, the bigger the more aggressive

    private boolean changed;
    int fsm;


    private int playerID, opponentID, noPlayers;

    /**
     * Public constructor with state observation and time due.
     *
     * @param stateObs     state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer, int playerID) {
        lastDiffPos = stateObs.getAvatarPosition(playerID);
        targetPos = lastDiffPos;
        thispos = lastDiffPos;
        fsm = 0;
        ATTACK_DIST *= stateObs.getBlockSize();
        RESOURCE_DIST *= stateObs.getBlockSize();
        changed = false;

        randomGenerator = new Random();
        this.playerID = playerID;
        noPlayers = stateObs.getNoPlayers();
        opponentID = (playerID+1)%noPlayers;
        heuristic = new SimpleStateHeuristic(stateObs);
        this.timer = elapsedTimer;

        m_actionsLeft = 0;
        m_lastMacroAction = -1;
        m_throwPop = true;

        /*
         * INITIALISE POPULATION
         */

        init_pop(stateObs, INIT_TYPE);
    }

    public Types.ACTIONS act(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer) {

        MCTS_BUDGET = MAX_FM_CALLS / 2;

        //decide state we're in
        thispos = stateObs.getAvatarPosition(playerID);

        SIMULATION_DEPTH = 8;
        if (isOpponentInRange(stateObs)) {
            SIMULATION_DEPTH = 3;
            if (canFight(stateObs)) {
                fsm = 2;
            } else {
                fsm = 3;
            }
        } else if  (isEnemyInRange(stateObs)) {
            fsm = 2;
            SIMULATION_DEPTH = 3;
        } else if (isResourceNear(stateObs)) {
            fsm = 1;
        } else {
            fsm = 0;
            if (!thispos.equals(lastDiffPos)) {
                lastDiffPos = thispos;
                changed = true;
            }
            ArrayList<Observation>[][] obsGrid = stateObs.getObservationGrid();
            int blockSize = stateObs.getBlockSize();
            int x = (int)thispos.x/blockSize;
            int y = (int)thispos.y/blockSize;
            int height = obsGrid.length;
            int width = obsGrid[0].length;
            targetPos = new Vector2d ((width-x)*blockSize,(height-y)*blockSize);
            heuristic.setTargetPos(targetPos);
        }

        numCalls = 0;
        this.timer = elapsedTimer;
        avgTimeTaken = 0;
        acumTimeTakenEval = 0;

        /*
         * RUN SIMULATIONS
         */

        int nextAction;

        if (MACRO_ACTION_LENGTH > 1) {
            nextAction = runMacro(stateObs, elapsedTimer);
        }
        else {
            init_pop(stateObs,INIT_TYPE);
            run(stateObs, elapsedTimer);
            nextAction = get_best_action(population);
        }

        /*
         * RETURN ACTION
         */

        return action_mapping[playerID].get(nextAction);
    }

    private int runMacro(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer) {
        int nextAction;
        if (stateObs.getGameTick() == 0) {
            if (m_throwPop)
                init_pop(stateObs,INIT_TYPE);

            run(stateObs, elapsedTimer);

            //Game just started, determine a macro-action.
            int best = get_best_action(population);

            m_lastMacroAction = best;
            m_throwPop = true;
            nextAction = best;
            m_actionsLeft = MACRO_ACTION_LENGTH-1;

        } else {

            if(m_actionsLeft > 0) { //In the middle of the macro action.

                if (stateObs.getAvailableActions(playerID).size() != N_ACTIONS[playerID] || m_throwPop)
                    init_pop(stateObs,INIT_TYPE);
                prepareGameCopy(stateObs);

                run(stateObs, elapsedTimer);

                nextAction = m_lastMacroAction;
                m_actionsLeft--;
                m_throwPop = false;

            } else if(m_actionsLeft == 0) { //Finishing a macro-action
                prepareGameCopy(stateObs);

                run(stateObs, elapsedTimer);
                int best = get_best_action(population);
                nextAction = m_lastMacroAction;
                m_lastMacroAction = best;
                m_actionsLeft = MACRO_ACTION_LENGTH-1;
                m_throwPop = true;

            } else{
                throw new RuntimeException("This should not be happening: " + m_actionsLeft);
            }
        }
        return nextAction;
    }


    private void run(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer) {

        if (!stateObs.isGameOver()) {

            numEvals = 0;
            numPop = 0;
            boolean ok = true;
            do {

                numPop++;

                if (REEVALUATE) {
                    for (int i = 0; i < ELITISM; i++) {
                        evaluate(population[i], heuristic, stateObs, avgTimeTaken, timer.remainingTimeMillis());
                    }
                }

                // mutate one individual randomly
                if (POPULATION_SIZE < 2) {
                    Individual newind = population[0].mutate(MUTATION, false); //only 1 individual in population, mutate it
                    double value = evaluate(newind, heuristic, stateObs, avgTimeTaken, timer.remainingTimeMillis());

                    if (population[0].value < value) {
                        nextPop[0] = newind.copy();
                    }

                } else {
                    for (int i = ELITISM; i < POPULATION_SIZE; i++) {
                        if ((numCalls + SIMULATION_DEPTH) < MAX_FM_CALLS) {
                            Individual newind;
                            newind = crossover();
                            newind = newind.mutate(MUTATION, false);

                            // evaluate new individual, insert into population
                            add_individual(newind, nextPop, i, stateObs, avgTimeTaken);

                            Arrays.sort(nextPop);

                        } else {
                            ok = false;
                            break;
                        }
                    }
                }

                population = nextPop.clone();

            } while (ok && ((numCalls + SIMULATION_DEPTH) < MAX_FM_CALLS));
//        }
        }
    }

    private void prepareGameCopy(StateObservationMulti stateObs)
    {
        if(m_lastMacroAction != -1)
        {
            int first = MACRO_ACTION_LENGTH - m_actionsLeft - 1;
            for(int i = first; i < MACRO_ACTION_LENGTH; ++i)
            {
                if (!stateObs.isGameOver()) {
                    Types.ACTIONS[] acts = new Types.ACTIONS[noPlayers];
                    acts[playerID] = action_mapping[playerID].get(m_lastMacroAction);
                    acts[opponentID] = opponentModel();
                    stateObs.advance(acts);
                    numCalls++;
                } else break;
            }
        }
    }


    /**
     * Evaluates an individual by rolling the current state with the actions in the individual
     * and returning the value of the resulting state; random action chosen for the opponent
     * @param individual - individual to be valued
     * @param heuristic - heuristic to be used for state evaluation
     * @param state - current state, root of rollouts
     * @return - value of last state reached
     */
    private double evaluate(Individual individual, StateHeuristicMulti heuristic, StateObservationMulti state, double avg, long remaining) {

        ElapsedCpuTimer elapsedTimerIterationEval = new ElapsedCpuTimer();

        numEvals++;

        StateObservationMulti st = state.copy();
        heuristic.setFirstState(state);
        int i;
        for (i = 0; i < SIMULATION_DEPTH; i++) {
            if (! st.isGameOver()) {
                advanceMacro(st, individual.actions[i]);
                if (numCalls + (SIMULATION_DEPTH - i) > MAX_FM_CALLS) break;
            } else {
                break;
            }
        }

        if (fsm == 0)
            heuristic.setDistMoved(thispos.dist(st.getAvatarPosition(playerID))/state.getBlockSize());

        StateObservationMulti first = st.copy();
        double value = heuristic.evaluateState(first, playerID, changed, fsm);

        /*
         * Apply discount factor
         */
        value *= Math.pow(DISCOUNT,i);
        individual.value = value;
        acumTimeTakenEval += (elapsedTimerIterationEval.elapsedMillis());

        return value;
    }


    private boolean isOpponentInRange(StateObservationMulti state) {
        return state.getAvatarPosition(playerID).dist(state.getAvatarPosition(opponentID)) < ATTACK_DIST;
    }

    private boolean isEnemyInRange(StateObservationMulti state) {
        double minDistanceNPC = Double.POSITIVE_INFINITY;
        ArrayList<Observation>[] npcPositions = state.getNPCPositions(state.getAvatarPosition(playerID));
        if (npcPositions != null) {
            for (ArrayList<Observation> npcs : npcPositions) {
                if(npcs.size() > 0)
                {
                    for (Observation o : npcs) {
                        if (o.itype == 54)
                            minDistanceNPC = o.sqDist;
                    }
                }
            }
        }
        return minDistanceNPC < ATTACK_DIST;
    }

    private boolean canFight(StateObservationMulti state) {
        return randomGenerator.nextDouble() <= ATTACK_BREAK || state.getAvatarHealthPoints(playerID) > state.getAvatarHealthPoints(opponentID);
    }

    private boolean isResourceNear(StateObservationMulti state) {
        try {
            return state.getResourcesPositions(state.getAvatarPosition(playerID))[0].get(0).sqDist < RESOURCE_DIST;
        } catch (Exception ignored) {}
        return false;
    }

    private Individual crossover() {
        Individual[] tournament = new Individual[TOURNAMENT_SIZE];
        if (POPULATION_SIZE > 2) {
            ArrayList<Individual> list = new ArrayList<>();
            list.addAll(Arrays.asList(population).subList(1, POPULATION_SIZE));
            Collections.shuffle(list);
            for (int i = 0; i < TOURNAMENT_SIZE; i++) {
                tournament[i] = list.get(i);
            }
        } else {
            tournament[0] = population[0];
            tournament[1] = population[1];
        }
        Individual newind = new Individual(SIMULATION_DEPTH,N_ACTIONS[playerID],randomGenerator);
        newind.crossover(tournament, CROSSOVER_TYPE);
        return newind;
    }

    private void add_individual(Individual newind, Individual[] pop, int idx, StateObservationMulti stateObs, double avgTimeTaken) {
        evaluate(newind, heuristic, stateObs, avgTimeTaken, timer.remainingTimeMillis());

        if (REPLACE) {
            if (REEVALUATE) {
                evaluate(population[idx], heuristic, stateObs, avgTimeTaken, timer.remainingTimeMillis());
            }
            if (population[idx].value < newind.value) {
                pop[idx] = newind.copy();
            } else if (population[idx].value == newind.value && population[idx].lastValue < newind.lastValue) {
                pop[idx] = newind.copy();
            } else {
                pop[idx] = population[idx].copy();
            }
        } else {
            pop[idx] = newind.copy();
        }
    }

    private void init_pop(StateObservationMulti stateObs, int init_type) {

        N_ACTIONS = new int[noPlayers];
        N_ACTIONS[playerID] = stateObs.getAvailableActions(playerID).size() + 1;
        N_ACTIONS[opponentID] = stateObs.getAvailableActions(opponentID).size() + 1;
        action_mapping = new HashMap[noPlayers];
        HashMap<Types.ACTIONS, Integer> action_mapping_r = new HashMap<>();
        int k = 0;
        for (int i = 0; i < noPlayers; i++) {
            action_mapping[i] = new HashMap<>();
            k = 0;
            for (Types.ACTIONS action : stateObs.getAvailableActions(i)) {
                action_mapping[i].put(k, action);
                action_mapping_r.put(action, k);
                k++;
            }

            action_mapping[i].put(k, Types.ACTIONS.ACTION_NIL);
        }

        action_mapping_r.put(Types.ACTIONS.ACTION_NIL, k);

        population = new Individual[POPULATION_SIZE];
        nextPop = new Individual[POPULATION_SIZE];

        for (int i = 0; i < POPULATION_SIZE; i++) {

            if (i == 0 || (numCalls + SIMULATION_DEPTH) < MAX_FM_CALLS) {
                population[i] = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID], randomGenerator);
                if (init_type != INIT_RANDOM) {
                    if (i > 0) {
                        population[i] = population[0].mutate(MUTATION, false);
                    } else {
                        if (init_type == INIT_ONESTEP) {
                            population[i].one_step_init(stateObs, action_mapping_r, HEURISTIC_TYPE, playerID);
                        }

                        if (init_type == INIT_MCTS) {
                            Types.ACTIONS[] actions = new Types.ACTIONS[N_ACTIONS[playerID]];
                            for (int j = 0; j < N_ACTIONS[playerID]; j++)
                                actions[j] = action_mapping[playerID].get(j);
                            SingleTreeNode m_root = new SingleTreeNode(randomGenerator, N_ACTIONS[playerID], actions);
                            m_root.rootState = stateObs;//Do the search within the available time.
                            m_root.mctsSearchCalls(MCTS_BUDGET, 10);

                            // Seed only first gene
//                            population[i].actions[0] = m_root.mostVisitedAction();

                            // Seed N relevant genes
                            ArrayList<Integer> ind = m_root.mostVisitedActions(m_root);
                            int limit = ind.size() < SIMULATION_DEPTH ? ind.size() : SIMULATION_DEPTH;
                            for (int j = 0; j < limit; j++) {
                                population[i].actions[j] = ind.get(j);
                            }
                            numCalls += MCTS_BUDGET;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < POPULATION_SIZE; i++) {
            evaluate(population[i], heuristic, stateObs, 0, timer.remainingTimeMillis());
        }
        if (POPULATION_SIZE > 1)
            Arrays.sort(population);
        for (int i = 0; i < POPULATION_SIZE; i++) {
            nextPop[i] = population[i].copy();
        }
    }

    private int get_best_action(Individual[] pop) {
        int bestAction = -1;
        bestAction = pop[0].actions[0];  //first action of the best individual in population

        return bestAction;
    }


    private void advanceMacro(StateObservationMulti state, int action)
    {
        int i = 0;
        boolean end = false;
        Types.ACTIONS act = action_mapping[playerID].get(action);
        if (act == null) act = Types.ACTIONS.ACTION_NIL;

        while(!end)
        {
            Types.ACTIONS[] acts = new Types.ACTIONS[noPlayers];
            acts[playerID] = act;
            acts[opponentID] = opponentModel();

            state.advance(acts);
            numCalls++;
            end = (++i >= MACRO_ACTION_LENGTH) || state.isGameOver();
        }

    }

    private Types.ACTIONS opponentModel() {
        return action_mapping[opponentID].get(randomGenerator.nextInt(N_ACTIONS[opponentID]));
    }
}
