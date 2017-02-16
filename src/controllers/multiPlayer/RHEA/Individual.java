package controllers.multiPlayer.RHEA;

import controllers.multiPlayer.heuristics.StateHeuristicMulti;
import controllers.multiPlayer.heuristics.SimpleStateHeuristic;
import controllers.multiPlayer.heuristics.WinScoreHeuristic;
import core.game.StateObservation;
import core.game.StateObservationMulti;
import ontology.Types;
import tools.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class Individual implements Comparable{

    protected int[] actions; // actions in individual. length of individual = actions.length
    protected int n; // number of legal actions
    protected double value;
    protected double lastValue;
    private Random gen;
    boolean canMut;
//    StateObservation lastState;

    private boolean MUT_BIAS = false;

    public Individual(int L, int n, Random gen) {
        actions = new int[L];
        canMut = true;
        for (int i = 0; i < L; i++) {
            if (n <= 1)  {n = 1; canMut = false;}
            actions[i] = gen.nextInt(n);
        }
        this.n = n;
        this.gen = gen;
//        lastState = null;
    }

    public void setActions (int[] a) {
        System.arraycopy(a, 0, actions, 0, a.length);
    }

    /**
     * Returns new individual
     * @param MUT
     * @param BANDIT_SELECTION
     * @return
     */
    public Individual mutate (int MUT, boolean INIT) {

        Individual b = this.copy();
        b.setActions(actions);

        if (b.canMut) {

            // mutate N = MUT actions of the individual
            int count = 0;
            while (count < MUT) {

                int a; // index of action to mutate

                if (MUT_BIAS || INIT) {
                    // bias mutations towards the beginning of the array of individuals

                    // do it Raluca's way
//                    int L = b.actions.length;
//                    ArrayList<Integer> list = new ArrayList<>();
//                    for (int i = 0; i < L; i++)
//                        for (int j = 0; j < L - i; j++)
//                            list.add(i);
//                    Collections.shuffle(list);
//                    a = list.get(0);

                    // do it Softmax way
                    int L = b.actions.length;
                    double[] p = new double[L];
                    double sum = 0, psum = 0;
                    for (int i = 0; i < L; i++) {
                        sum += Math.pow(Math.E, -(i + 1));
                    }
                    double prob = Math.random();
                    a = 0;
                    for (int i = 0; i < L; i++) {
                        p[i] = Math.pow(Math.E, -(i + 1)) / sum;
                        psum += p[i];
                        if (psum > prob) {
                            a = i;
                            break;
                        }
                    }


                } else {
                    // random mutation of one action
                    a = gen.nextInt(b.actions.length);
                }

                int s;
                do {
                    s = gen.nextInt(n); // find new action, different than the previous one
                } while (s == b.actions[a]);
                b.actions[a] = s;

                count++;

            }
        }

        return b;
    }

    /**
     * Modifies individual
     * @param cross
     * @param CROSSOVER_TYPE
     */
    public void crossover (Individual[] cross, int CROSSOVER_TYPE) {
        if (CROSSOVER_TYPE == Agent.POINT1_CROSS) {
            // 1-point
            int p = gen.nextInt(actions.length - 3) + 1;
            for ( int i = 0; i < actions.length; i++) {
                if (i < p)
                    actions[i] = cross[0].actions[i];
                else
                    actions[i] = cross[1].actions[i];
            }

        } else if (CROSSOVER_TYPE == Agent.UNIFORM_CROSS) {
            // uniform
            for (int i = 0; i < actions.length; i++) {
                actions[i] = cross[gen.nextInt(cross.length)].actions[i];
            }
        }
    }

    /**
     * Modifies individual
     */
    public void one_step_init(StateObservationMulti stateObs, HashMap<Types.ACTIONS,Integer> action_mapping, int hType, int playerID) {
        StateObservationMulti so = stateObs.copy();
        StateHeuristicMulti heuristic;
        Types.ACTIONS bestAction;
        double maxQ;

        for (int i = 0; i < actions.length; i++) {
            bestAction = null;
            maxQ = Double.NEGATIVE_INFINITY;

            if (hType == Agent.HEURISTIC_WINSCORE)
                heuristic =  new WinScoreHeuristic(so);
            else
                heuristic = new SimpleStateHeuristic(so);

            if (!so.isGameOver()) {
                for (Types.ACTIONS action : so.getAvailableActions()) {

                    StateObservationMulti stCopy = so.copy();
                    stCopy.advance(action);
                    double Q = heuristic.evaluateState(stCopy, playerID);
                    Q = Utils.noise(Q, Agent.epsilon, gen.nextDouble());

                    //System.out.println("Action:" + action + " score:" + Q);
                    if (Q > maxQ) {
                        maxQ = Q;
                        bestAction = action;
                    }
                }

                actions[i] = action_mapping.get(bestAction);
                so.advance(bestAction);
            }
        }
    }

    @Override
    public int compareTo(Object o) {
        Individual a = this;
        Individual b = (Individual)o;
        if (a.value < b.value) return 1;
        else if (a.value > b.value) return -1;
        else return 0;
    }

    @Override
    public boolean equals(Object o) {
        Individual a = this;
        Individual b = (Individual)o;

        for (int i = 0; i < actions.length; i++) {
            if (a.actions[i] != b.actions[i]) return false;
        }

        return true;
    }

    public Individual copy () {
        Individual a = new Individual(this.actions.length, this.n, this.gen);
        a.value = this.value;
        a.lastValue = this.lastValue;
//        if (this.lastState != null)
//            a.lastState = this.lastState.copy();
        a.setActions(this.actions);
        a.canMut = this.canMut;

        return a;
    }

    @Override
    public String toString() {
        String s = "" + value + ": ";
        for (int i = 0; i < actions.length; i++)
            s += actions[i] + " ";
        return s;
    }
}
