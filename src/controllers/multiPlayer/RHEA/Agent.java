package controllers.multiPlayer.RHEA;

import controllers.multiPlayer.RHEA.sampleOLMCTS.SingleTreeNode;
import controllers.multiPlayer.heuristics.WinScoreHeuristic;
import controllers.multiPlayer.heuristics.StateHeuristicMulti;
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

    protected int POPULATION_SIZE = 1; //try 1,2,5
    public static int SIMULATION_DEPTH = 10; //try 6,8,10
    protected int INIT_TYPE = Agent.INIT_RANDOM;
    protected int BUDGET_TYPE = Agent.HALF_BUDGET;
    public static int MAX_FM_CALLS = 100;
    protected int HEURISTIC_TYPE = Agent.HEURISTIC_WINSCORE;
    protected int MACRO_ACTION_LENGTH = 1;

    private int CROSSOVER_TYPE = UNIFORM_CROSS; // 0 - 1point; 1 - uniform

    private boolean CIRCULAR = false;

    // set
    private boolean REEVALUATE = false;
    private boolean REPLACE = false;
    private int MUTATION = 1;
    private int TOURNAMENT_SIZE = 2;
    private int RESAMPLE = 2; //try 1,2,3
    private int ELITISM = 1;
    private double DISCOUNT = 1; //0.99;

    // constants
    private final long BREAK_MS = 5;
    private final int MAX_ACTIONS = 6;
    public static final double epsilon = 1e-6;

    public static final int POINT1_CROSS = 0;
    public static final int UNIFORM_CROSS = 1;
    public static final int INIT_RANDOM = 0;
    public static final int INIT_ONESTEP = 1;
    public static final int INIT_MCTS = 2;

    public static final int HEURISTIC_WINSCORE = 0;
    public static final int HEURISTIC_SIMPLESTATE = 1;

    public static final int FULL_BUDGET = 0;
    public static final int HALF_BUDGET = 1;
    public static int MCTS_BUDGET;
    public static int ONESTEP_BUDGET;

    private Individual[] population, nextPop;
    private int[] N_ACTIONS;

    private ElapsedCpuTimer timer;

    private HashMap<Integer, Types.ACTIONS>[] action_mapping;
    private HashMap<Types.ACTIONS, Integer> action_mapping_r;
    private Random randomGenerator;

    private StateHeuristicMulti heuristic;

    // number of evaluations
    private int MAX_ITERS;
    private int numEvals = 0;
    private int numCalls = 0;
    private int numPop = 0;

    private Types.ACTIONS[] currentBest;
    //    private double avgConvergence = 0;
    private double acumTimeTakenEval, avgTimeTaken;


    //MACRO ACTIONS

    private int m_actionsLeft;
    private int m_lastMacroAction;
    private boolean m_throwPop;


    // Drawing.
//    protected ArrayList<Observation> grid[][];
    protected int block_size; // itype;
    ArrayList<Vector2d> positions, newpos;
    Vector2d pos;


    int playerID, opponentID, noPlayers;

    /**
     * Public constructor with state observation and time due.
     *
     * @param stateObs     state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer, int playerID) {
        randomGenerator = new Random();
        this.playerID = playerID;
        noPlayers = stateObs.getNoPlayers();
        opponentID = (playerID+1)%noPlayers;
        heuristic = new WinScoreHeuristic(stateObs);
        this.timer = elapsedTimer;
        currentBest = new Types.ACTIONS[MAX_ITERS];

        m_actionsLeft = 0;
        m_lastMacroAction = -1;
        m_throwPop = true;

        /*
         * INITIALISE POPULATION
         */

        init_pop(stateObs, INIT_TYPE);

        /*
         * Drawing
         */

//        grid = stateObs.getObservationGrid();
        block_size = stateObs.getBlockSize();
//        itype = stateObs.getAvatarType(id);
        positions = new ArrayList<>();
        newpos = new ArrayList<>();
    }

    public Types.ACTIONS act(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer) {
//        grid = stateObs.getObservationGrid();
//        itype = stateObs.getAvatarType(id);

        positions = new ArrayList<>();

        MAX_ITERS = MAX_FM_CALLS;
        MCTS_BUDGET = MAX_FM_CALLS / 2;

        numCalls = 0;
        this.timer = elapsedTimer;
        avgTimeTaken = 0;
//        double acumTimeTaken = 0;
//        long remaining = timer.remainingTimeMillis();
        acumTimeTakenEval = 0;


        // Check if number of available actions changed, if so, reinitialise pop


        /*
         * RUN SIMULATIONS
         */

        int nextAction;

        if (MACRO_ACTION_LENGTH > 1) {
            nextAction = runMacro(stateObs, elapsedTimer);
        }
        else {
            if (stateObs.getAvailableActions(playerID).size() != N_ACTIONS[playerID] || !CIRCULAR)
                init_pop(stateObs,INIT_TYPE);
            run(stateObs, elapsedTimer);
            nextAction = get_best_action(population);

        }

        /*
         * RETURN ACTION
         */


        if (CIRCULAR) {
            // Remove first action of all individuals and add a new random one at the end
            for (int i = 0; i < POPULATION_SIZE; i++) {
                for (int j = 0; j < SIMULATION_DEPTH - 1; j++) {
                    int next = population[i].actions[j+1];
                    population[i].actions[j] = (next < N_ACTIONS[playerID]) ? next : randomGenerator.nextInt(N_ACTIONS[playerID]);
                }
                population[i].actions[SIMULATION_DEPTH - 1] = randomGenerator.nextInt(N_ACTIONS[playerID]);
            }


        }

        // check convergence
//        int found = -1;
//        int i;
//        boolean ok;
//        for (i = 0; i < MAX_ITERS; i++) {
//            if (currentBest[i] != null && currentBest[i].equals(best)) {
//                ok = true;
//                for (int j = i + 1; j < MAX_ITERS; j++) {
//                    if (!currentBest[i].equals(best)) {
//                        ok = false; break;
//                    }
//                }
//                if (ok) {
//                    found = i; break;
//                }
//            } else if (currentBest[i] == null) break;
//        }
//
//        if (found == -1) found = i;
//
//        avgConvergence = found;
//        avgTime = acumTimeTakenEval / numEvals;

//        System.out.println(found);
//        System.out.println(avgTime);
//        System.out.println(numCalls);
//        System.out.println(population[0].value);

//        if (node.children[bestAction] != null)
//            System.out.println(String.format("%.2f",node.children[bestAction].value));

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
        // if full budget to be used for evolution, reset everything after initialisation

        if (!stateObs.isGameOver()) {

            if (BUDGET_TYPE == FULL_BUDGET) {
                this.timer = elapsedTimer;
                numCalls = 0;
                avgTimeTaken = 0;
//        double acumTimeTaken = 0;
//        long remaining = timer.remainingTimeMillis();
                acumTimeTakenEval = 0;
            }


            numEvals = 0;
            numPop = 0;
            currentBest = new Types.ACTIONS[MAX_ITERS];
            boolean ok = true;
//      if (remaining > 2*avgTimeTaken && remaining > 5*BREAK_MS) {
            do {
//                ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

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

                    /*
                      Get current best action
                     */

//                currentBest[numEvals - 1] = get_best_action(nextPop);


                } else {
                    for (int i = ELITISM; i < POPULATION_SIZE; i++) {
                        if ((numCalls + SIMULATION_DEPTH) < MAX_FM_CALLS) {
                            Individual newind;
                            newind = crossover();
                            newind = newind.mutate(MUTATION, false);

                            // evaluate new individual, insert into population
                            add_individual(newind, nextPop, i, stateObs, avgTimeTaken);
//                            remaining = timer.remainingTimeMillis();

                            /*
                             * Get current best action
                             */

//                            if (numIters > MAX_ITERS) break;

                            Arrays.sort(nextPop);
//                        currentBest[numEvals - 1] = get_best_action(nextPop);

                        } else {
                            ok = false;
                            break;
                        }
                    }
                }

                population = nextPop.clone();

//                acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
//                avgTimeTaken = acumTimeTaken / numIters;

//                currentBest[numPop - 1] = get_best_action();

//            } while (remaining > 2*avgTimeTaken && remaining > BREAK_MS);
//            } while (numIters < MAX_ITERS);
            } while (ok && ((numCalls + SIMULATION_DEPTH) < MAX_FM_CALLS));
//        }
        }
    }

    public void prepareGameCopy(StateObservationMulti stateObs)
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
        StateObservationMulti last = st.copy();
        int i;
        for (i = 0; i < SIMULATION_DEPTH; i++) {
            if (! st.isGameOver()) {
//                ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
                last = st.copy();
                advanceMacro(st, individual.actions[i]);
//                st.advance(action_mapping.get(individual.actions[i]));
//                numCalls++;
//                double r = remaining - elapsedTimerIteration.remainingTimeMillis();
//                if (r < avg || r < BREAK_MS) break;
                if (numCalls + (SIMULATION_DEPTH - i) > MAX_FM_CALLS) break;
            } else {
                break;
            }
        }


        /*
         * ROLLOUTS
         */

        StateObservationMulti first = st.copy();
        double value = 0;
        value = heuristic.evaluateState(first, playerID, true, 0);


        /*
         * Apply discount factor
         */
        value *= Math.pow(DISCOUNT,i);

        individual.value = value;
//        individual.lastValue = heuristic.evaluateState(last, playerID) * Math.pow(DISCOUNT, i-1);


        acumTimeTakenEval += (elapsedTimerIterationEval.elapsedMillis());


        return value;
    }

    private Individual crossover() {
        Individual[] tournament = new Individual[TOURNAMENT_SIZE];
        if (POPULATION_SIZE > 2) {
//            tournament[0] = population[randomGenerator.nextInt(POPULATION_SIZE)];
//            for (int i = 1; i < TOURNAMENT_SIZE; i++) {
//                do {
//                    tournament[i] = population[randomGenerator.nextInt(POPULATION_SIZE - 1) + 1]; //don't include the first one in tournament
//                    System.out.println("here");
//                } while (tournament[i].equals(tournament[i - 1]));
//            }
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
        action_mapping_r = new HashMap<>();
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


    public void advanceMacro(StateObservationMulti state, int action)
    {
        int i = 0;
        boolean end = false;
        Types.ACTIONS act = action_mapping[playerID].get(action);
        if (act == null) act = Types.ACTIONS.ACTION_NIL;

        while(!end)
        {
//            if (viewer != null) {
//                pos = state.getAvatarPosition();
//                positions.add(pos);
//            }

            Types.ACTIONS[] acts = new Types.ACTIONS[noPlayers];
            acts[playerID] = act;
            acts[opponentID] = opponentModel();

            state.advance(acts);
            numCalls++;
            end = (++i >= MACRO_ACTION_LENGTH) || state.isGameOver();
        }

    }

    public Types.ACTIONS opponentModel() {
        return action_mapping[opponentID].get(randomGenerator.nextInt(N_ACTIONS[opponentID]));
    }


    public void draw(Graphics2D g) {

        /**
         * Draw exploration
         */

//        g.setColor(new Color(0,0,0,5));
//
        //g.fillRect(0, 0, grid.length * block_size, grid[0].length * block_size);
//
//        for(int j = 0; j < grid[0].length; ++j) {
//            for(int i = 0; i < grid.length; ++i) {
//                for (Observation o : grid[i][j]) {
//                    if (o.itype == itype) {
//                        positions.add(o.position);
//                    }
//                }
//            }
//        }
//
//        for (Vector2d p : positions) {
//            g.fillRect((int)p.x,(int)p.y, block_size, block_size);
//        }

        /**
         * Draw thinking
         */

//        g.setColor(new Color(255,255,255,25));
//        newpos.clear();
//        newpos.addAll(positions);
//        if (!newpos.isEmpty()) {
//            for (Vector2d pos : newpos) {
//                g.fillOval((int) pos.x + block_size / 2, (int) pos.y + block_size / 2, block_size / 2, block_size / 2);
//            }
//        }

    }



    public void result(StateObservationMulti stateObs, ElapsedCpuTimer elapsedCpuTimer)
    {
//        System.out.println(avgConvergence / stateObs.getGameTick());
    }
}
