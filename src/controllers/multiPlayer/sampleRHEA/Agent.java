package controllers.multiPlayer.sampleRHEA;

import controllers.multiPlayer.RHEA.sampleOLMCTS.SingleTreeNode;
import controllers.multiPlayer.heuristics.SimpleStateHeuristic;
import controllers.multiPlayer.heuristics.StateHeuristicMulti;
import controllers.multiPlayer.heuristics.WinScoreHeuristic;
import core.game.Observation;
import core.game.StateObservationMulti;
import core.player.AbstractMultiPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;


public class Agent extends AbstractMultiPlayer {

    // variable
    private int POPULATION_SIZE = 3;
    private int SIMULATION_DEPTH = 8;
    private int CROSSOVER_TYPE = UNIFORM_CROSS;
    private double DISCOUNT = 0.99;

    // set
    private boolean REEVALUATE = false;
    private int MUTATION = 3;
    private int TOURNAMENT_SIZE = 2;
    private int ELITISM = 1;

    // constants
    private final long BREAK_MS = 5;
    public static final double epsilon = 1e-6;
    static final int POINT1_CROSS = 0;
    static final int UNIFORM_CROSS = 1;

    private Individual[] population, nextPop;
    private int NUM_INDIVIDUALS;
    private int[] N_ACTIONS;
    private HashMap<Integer, Types.ACTIONS>[] action_mapping;

    private ElapsedCpuTimer timer;
    private Random randomGenerator;

    private StateHeuristicMulti heuristic;
    private double acumTimeTakenEval = 0,avgTimeTakenEval = 0, avgTimeTaken = 0, acumTimeTaken = 0;
    private int numEvals = 0, numIters = 0;
    private boolean keepIterating = true;
    private long remaining;

    Vector2d lastDiffPos, targetPos;
    int ATTACK_DIST = 5;
    int RESOURCE_DIST = 5;
    double ATTACK_BREAK = 0.4; // chance to recklessly attack enemy, the bigger the more aggressive

    boolean changed;

    //Multiplayer game parameters
    int playerID, opponentID, noPlayers;

    /**
     * Public constructor with state observation and time due.
     *
     * @param stateObs     state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer, int playerID) {
        randomGenerator = new Random();
        heuristic = new SimpleStateHeuristic(stateObs);
        this.timer = elapsedTimer;

        // Get multiplayer game parameters
        this.playerID = playerID;
        noPlayers = stateObs.getNoPlayers();
        opponentID = (playerID+1)%noPlayers;
        lastDiffPos = stateObs.getAvatarPosition(playerID);
        targetPos = lastDiffPos;
        ATTACK_DIST *= stateObs.getBlockSize();
        RESOURCE_DIST *= stateObs.getBlockSize();
        changed = false;

        // INITIALISE POPULATION
        init_pop(stateObs);
    }

    @Override
    public Types.ACTIONS act(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer) {
        this.timer = elapsedTimer;
        avgTimeTaken = 0;
        acumTimeTaken = 0;
        numEvals = 0;
        acumTimeTakenEval = 0;
        numIters = 0;
        remaining = timer.remainingTimeMillis();
        NUM_INDIVIDUALS = 0;
        keepIterating = true;

        // INITIALISE POPULATION
        init_pop(stateObs);


        // RUN EVOLUTION
        remaining = timer.remainingTimeMillis();
        while (remaining > avgTimeTaken && remaining > BREAK_MS && keepIterating) {
            runIteration(stateObs);
            remaining = timer.remainingTimeMillis();
        }

        // RETURN ACTION
        Types.ACTIONS best = get_best_action(population);
        return best;
    }

    /**
     * Run evolutionary process for one generation
     * @param stateObs - current game state
     */
    private void runIteration(StateObservationMulti stateObs) {
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

        if (REEVALUATE) {
            for (int i = 0; i < ELITISM; i++) {
                if (remaining > 2*avgTimeTakenEval && remaining > BREAK_MS) { // if enough time to evaluate one more individual
                    evaluate(population[i], heuristic, stateObs);
                } else {keepIterating = false;}
            }
        }

        // mutate one individual randomly
        for (int i = ELITISM; i < NUM_INDIVIDUALS; i++) {
            if (remaining > 2*avgTimeTakenEval && remaining > BREAK_MS) { // if enough time to evaluate one more individual
                Individual newind;

                newind = crossover(population, population[0]);
                newind = newind.mutate(MUTATION);

                // evaluate new individual, insert into population
                add_individual(newind, nextPop, i, stateObs);

                remaining = timer.remainingTimeMillis();

            } else {keepIterating = false; break;}
        }
        if (NUM_INDIVIDUALS > 1)
            Arrays.sort(nextPop, new Comparator<Individual>() {
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

        population = nextPop.clone();

        numIters++;
        acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
        avgTimeTaken = acumTimeTaken / numIters;
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

        int fsm;
        if (isOpponentInRange(state)) {
            if (canFight(state)) {
                fsm = 2;
            } else {
                fsm = 3;
            }
        } else if  (isEnemyInRange(state)) {
            fsm = 2;
        } else if (isResourceNear(state)) {
            fsm = 1;
        } else {
            fsm = 0;
            if (!thispos.equals(lastDiffPos)) {
                lastDiffPos = thispos;
                changed = true;
            }
            ArrayList<Observation>[][] obsGrid = state.getObservationGrid();
            int blockSize = state.getBlockSize();
            int x = (int)thispos.x/blockSize;
            int y = (int)thispos.y/blockSize;
            int height = obsGrid.length;
            int width = obsGrid[0].length;
            targetPos = new Vector2d ((width-x)*blockSize,(height-y)*blockSize);
            heuristic.setTargetPos(targetPos);
        }

//        System.out.println("State: " + fsm);

        ElapsedCpuTimer elapsedTimerIterationEval = new ElapsedCpuTimer();

        StateObservationMulti st = state.copy();
        heuristic.setFirstState(state);

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

        if (fsm == 0)
            heuristic.setDistMoved(thispos.dist(st.getAvatarPosition(playerID))/state.getBlockSize());

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

    boolean isEnemyInRange(StateObservationMulti state) {
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

    boolean canFight(StateObservationMulti state) {
        return randomGenerator.nextDouble() <= ATTACK_BREAK || state.getAvatarHealthPoints(playerID) > state.getAvatarHealthPoints(opponentID);
    }

    boolean isResourceNear(StateObservationMulti state) {
        try {
            return state.getResourcesPositions(state.getAvatarPosition(playerID))[0].get(0).sqDist < RESOURCE_DIST;
        } catch (Exception e) {}
        return false;
    }

    /**
     * @param pop - the population from which a new individual should be produced
     * @return - the individual resulting from crossover applied to the specified population
     */
    private Individual crossover(Individual[] pop, Individual newind) {
        if (NUM_INDIVIDUALS > 1) {
            Individual[] tournament = new Individual[TOURNAMENT_SIZE];
            if (NUM_INDIVIDUALS > 2) {
                ArrayList<Individual> list = new ArrayList<>();
                list.addAll(Arrays.asList(pop).subList(1, NUM_INDIVIDUALS));
                Collections.shuffle(list);
                for (int i = 0; i < TOURNAMENT_SIZE; i++) {
                    tournament[i] = list.get(i);
                }
            } else {
                tournament[0] = pop[0];
                tournament[1] = pop[1];
            }
            newind.crossover(tournament, CROSSOVER_TYPE);
        }
        return newind;
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

        population = new Individual[POPULATION_SIZE];
        nextPop = new Individual[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            if (i == 0 || remaining > avgTimeTakenEval && remaining > BREAK_MS) {
                population[i] = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID], randomGenerator);
                evaluate(population[i], heuristic, stateObs);
                remaining = timer.remainingTimeMillis();
                NUM_INDIVIDUALS = i+1;
            } else {break;}
        }

        if (NUM_INDIVIDUALS > 1)
            Arrays.sort(population, new Comparator<Individual>() {
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
        for (int i = 0; i < NUM_INDIVIDUALS; i++) {
            if (population[i] != null)
                nextPop[i] = population[i].copy();
        }

    }

    /**
     * @param pop - last population obtained after evolution
     * @return - first action of best individual in the population (found at index 0)
     */
    private Types.ACTIONS get_best_action(Individual[] pop) {
        int bestAction = pop[0].actions[0];
        return action_mapping[playerID].get(bestAction);
    }

}
