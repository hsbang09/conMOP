/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.conmop.operators;

import java.util.ArrayList;
import java.util.HashMap;

import org.hipparchus.util.FastMath;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import org.moeaframework.core.variable.RealVariable;
import seakers.conmop.util.Bounds;
import seakers.conmop.util.Factor;
import seakers.conmop.variable.ConstellationVariable;
import seakers.conmop.variable.SatelliteVariable;
import seakers.conmop.variable.WalkerVariable;
import seakers.orekit.constellations.Walker;

/**
 *
 * @author nhitomi
 */
public class OrbitElementOperator implements Variation {

    private final Variation operator;

    public OrbitElementOperator(Variation operator) {
        this.operator = operator;
    }

    @Override
    public int getArity() {
        return operator.getArity();
    }

    @Override
    public Solution[] evolve(Solution[] parents) {
        //copy the solutions
        Solution[] children = new Solution[parents.length];
        for (int i = 0; i < parents.length; i++) {
            children[i] = parents[i].copy();
        }

        for (int i = 0; i < children[0].getNumberOfVariables(); i++) {
            //if the solution is composed of constellation variables
            boolean constelVariables = true;
            for (int j = 0; j < parents.length; j++) {
                if (!(children[j].getVariable(i) instanceof ConstellationVariable)) {
                    constelVariables = false;
                    break;
                }
            }
            boolean satVariables = true;
            for (int j = 0; j < parents.length; j++) {
                if (!(children[j].getVariable(i) instanceof SatelliteVariable)) {
                    satVariables = false;
                    break;
                }
            }
            boolean walkerVariables = true;
            for (int j = 0; j < parents.length; j++) {
                if (!(children[j].getVariable(i) instanceof WalkerVariable)) {
                    walkerVariables = false;
                    break;
                }
            }

            if (walkerVariables) {
                WalkerVariable[] input = new WalkerVariable[children.length];
                for (int j = 0; j < children.length; j++) {
                    input[j] = (WalkerVariable) children[j].getVariable(i);
                }
                WalkerVariable[] output = evolve(input);
                for (int j = 0; j < children.length; j++) {
                    children[j].setVariable(i, output[j]);
                }

            }else if (constelVariables) {
                ConstellationVariable[] input = new ConstellationVariable[children.length];
                for (int j = 0; j < children.length; j++) {
                    input[j] = (ConstellationVariable) children[j].getVariable(i);
                }
                ConstellationVariable[] output = evolve(input);
                for (int j = 0; j < children.length; j++) {
                    children[j].setVariable(i, output[j]);
                }

            } else if (satVariables) {
                //if the solution is composed of satellite variables
                SatelliteVariable[] input = new SatelliteVariable[children.length];
                for (int j = 0; j < children.length; j++) {
                    input[j] = (SatelliteVariable) children[j].getVariable(i);
                }
                SatelliteVariable[] output = evolve(input);
                for (int j = 0; j < children.length; j++) {
                    children[j].setVariable(i, output[j]);
                }
            }
        }
        return children;
    }

    /**
     * Operates on the WalkerVariable
     *
     * @param walkerConstellations walker constellations to recombine
     * @return recombined constellation variables
     */
    private WalkerVariable[] evolve(WalkerVariable[] walkerConstellations) {

        //create the vector representation of the constellation
        Solution[] parents = new Solution[walkerConstellations.length];
        for (int i = 0; i < walkerConstellations.length; i++) {

            Solution parent = new Solution(5, 0);

            WalkerVariable wv = walkerConstellations[i];

            parent.setVariable(0,
                    new RealVariable(wv.getSma(), wv.getSmaBound().getLowerBound(), wv.getSmaBound().getUpperBound()));

            parent.setVariable(1,
                    new RealVariable(wv.getInc(), wv.getIncBound().getLowerBound(), wv.getIncBound().getUpperBound()));

            parent.setVariable(2,
                    new RealVariable(wv.getT(), (double) wv.getTBound().getLowerBound(), (double) wv.getTBound().getUpperBound()));

            parent.setVariable(3,
                    new RealVariable(wv.getP(), (double) wv.getPBound().getLowerBound(), (double) wv.getPBound().getUpperBound()));

            parent.setVariable(4,
                    new RealVariable(wv.getF(), (double) wv.getFBound().getLowerBound(), (double) wv.getFBound().getUpperBound()));

            parents[i] = parent;
        }

        Solution[] children = operator.evolve(parents);

        WalkerVariable[] out = walkerConstellations;
        for (int i = 0; i < children.length; i++) {

            Solution child = children[i];
            double sma = ((RealVariable) child.getVariable(0)).getValue();
            double inc = ((RealVariable) child.getVariable(1)).getValue();
            int t = (int) Math.round(((RealVariable) child.getVariable(2)).getValue());
            int p = (int) Math.round(((RealVariable) child.getVariable(3)).getValue());
            int f = (int) Math.round(((RealVariable) child.getVariable(4)).getValue());

            out[i].setWalker(sma, inc, t, p, f);
        }
        return out;
    }

    /**
     * Constellations can have the same number of satellites. If they do not,
     * then a number of satellites equal to the number of satellites in the
     * smallest constellations will be grouped randomly with satellites from the
     * other constellations and crossed using the given operation
     *
     * @param constellations constellations to recombine
     * @return recombined constellation variables
     */
    private ConstellationVariable[] evolve(ConstellationVariable[] constellations) {
        //find the minimum number of satellites contained in any of the constellations
        int minNSats = Integer.MAX_VALUE;
        for (int i = 0; i < constellations.length; i++) {
            minNSats = FastMath.min(minNSats, constellations[i].getNumberOfSatellites());
        }

        //create a 2-D array of all parent satellite variables involved
        SatelliteVariable[][] satsToCross = new SatelliteVariable[constellations.length][minNSats];
        int[][] satsToCrossIndex = new int[constellations.length][minNSats];
        for (int i = 0; i < constellations.length; i++) {
            ArrayList<SatelliteVariable> candidates = new ArrayList<>(constellations[i].getSatelliteVariables());
            for (int j = 0; j < minNSats; j++) {
                int index = PRNG.nextInt(candidates.size());
                satsToCross[i][j] = candidates.get(index);
                satsToCrossIndex[i][j] = index;
            }
        }

        //find which varibles should be included in search.
        //variables with lower bound == upperbound are not included
        HashMap<String, Integer> variableLocus = new HashMap<>();
        //assume that each satellite variable has the same upper and lower bounds
        SatelliteVariable repSat = satsToCross[0][0];
        int locusIndex = 0;
        if (!repSat.getSmaBound().getLowerBound().equals(repSat.getSmaBound().getUpperBound())) {
            variableLocus.put("sma", locusIndex);
            locusIndex++;
        }
        if (!repSat.getEccBound().getLowerBound().equals(repSat.getEccBound().getUpperBound())) {
            variableLocus.put("ecc", locusIndex);
            locusIndex++;
        }
        if (!repSat.getIncBound().getLowerBound().equals(repSat.getIncBound().getUpperBound())) {
            variableLocus.put("inc", locusIndex);
            locusIndex++;
        }
        if (!repSat.getArgPerBound().getLowerBound().equals(repSat.getArgPerBound().getUpperBound())) {
            variableLocus.put("ap", locusIndex);
            locusIndex++;
        }
        if (!repSat.getRaanBound().getLowerBound().equals(repSat.getRaanBound().getUpperBound())) {
            variableLocus.put("raan", locusIndex);
            locusIndex++;
        }
        if (!repSat.getAnomBound().getLowerBound().equals(repSat.getAnomBound().getUpperBound())) {
            variableLocus.put("ta", locusIndex);
        }

        //create the vector representation of the constellation
        Solution[] parents = new Solution[constellations.length];
        for (int i = 0; i < constellations.length; i++) {
            Solution parent = new Solution(variableLocus.size() * minNSats, 0);
            int satCount = 0;
            for (int j = 0; j < minNSats; j++) {
                SatelliteVariable sat = satsToCross[i][j];
                if (variableLocus.containsKey("sma")) {
                    parent.setVariable(satCount + variableLocus.get("sma"),
                            new RealVariable(sat.getSma(), sat.getSmaBound().getLowerBound(), sat.getSmaBound().getUpperBound()));
                }
                if (variableLocus.containsKey("ecc")) {
                    parent.setVariable(satCount + variableLocus.get("ecc"),
                            new RealVariable(sat.getEcc(), sat.getEccBound().getLowerBound(), sat.getEccBound().getUpperBound()));
                }
                if (variableLocus.containsKey("inc")) {
                    parent.setVariable(satCount + variableLocus.get("inc"),
                            new RealVariable(sat.getInc(), sat.getIncBound().getLowerBound(), sat.getIncBound().getUpperBound()));
                }
                if (variableLocus.containsKey("ap")) {
                    parent.setVariable(satCount + variableLocus.get("ap"),
                            new RealVariable(sat.getArgPer(), sat.getArgPerBound().getLowerBound(), sat.getArgPerBound().getUpperBound()));
                }
                if (variableLocus.containsKey("raan")) {
                    parent.setVariable(satCount + variableLocus.get("raan"),
                            new RealVariable(sat.getRaan(), sat.getRaanBound().getLowerBound(), sat.getRaanBound().getUpperBound()));
                }
                if (variableLocus.containsKey("ta")) {
                    parent.setVariable(satCount + variableLocus.get("ta"),
                            new RealVariable(sat.getTrueAnomaly(), sat.getAnomBound().getLowerBound(), sat.getAnomBound().getUpperBound()));
                }
                satCount += variableLocus.size();
            }
            parents[i] = parent;
        }

        Solution[] children = operator.evolve(parents);

        ConstellationVariable[] out = constellations;
        for (int i = 0; i < children.length; i++) {
            ArrayList<SatelliteVariable> satList = new ArrayList<>(constellations[i].getSatelliteVariables());
            int satCount = 0;
            Solution child = children[i];
            for (int j = 0; j < minNSats; j++) {
                SatelliteVariable satVar = (SatelliteVariable) satsToCross[i][j];
                if (variableLocus.containsKey("sma")) {
                    satVar.setSma(((RealVariable) child.getVariable(satCount + variableLocus.get("sma"))).getValue());
                }
                if (variableLocus.containsKey("ecc")) {
                    satVar.setEcc(((RealVariable) child.getVariable(satCount + variableLocus.get("ecc"))).getValue());
                }
                if (variableLocus.containsKey("inc")) {
                    satVar.setInc(((RealVariable) child.getVariable(satCount + variableLocus.get("inc"))).getValue());
                }
                if (variableLocus.containsKey("ap")) {
                    satVar.setArgPer(((RealVariable) child.getVariable(satCount + variableLocus.get("ap"))).getValue());
                }
                if (variableLocus.containsKey("raan")) {
                    satVar.setRaan(((RealVariable) child.getVariable(satCount + variableLocus.get("raan"))).getValue());
                }
                if (variableLocus.containsKey("ta")) {
                    satVar.setTrueAnomaly(((RealVariable) child.getVariable(satCount + variableLocus.get("ta"))).getValue());
                }

                satList.set(satsToCrossIndex[i][j], satVar);
                satCount += variableLocus.size();
            }
            out[i].setSatelliteVariables(satList);
        }
        return out;
    }

    /**
     * Operates on the real-valued orbital elements with the given operator
     *
     * @param satellites The satellite variables to operate on
     * @return the modified satellite variables. They are new instances
     */
    private SatelliteVariable[] evolve(SatelliteVariable[] satellites) {
        //find which varibles should be included in search.
        //variables with lower bound == upperbound are not included
        HashMap<String, Integer> variableLocus = new HashMap<>();
        //assume that each satellite variable has the same upper and lower bounds
        SatelliteVariable repSat = satellites[0];
        int locusIndex = 0;
        if (!repSat.getSmaBound().getLowerBound().equals(repSat.getSmaBound().getUpperBound())) {
            variableLocus.put("sma", locusIndex);
            locusIndex++;
        }
        if (!repSat.getEccBound().getLowerBound().equals(repSat.getEccBound().getUpperBound())) {
            variableLocus.put("ecc", locusIndex);
            locusIndex++;
        }
        if (!repSat.getIncBound().getLowerBound().equals(repSat.getIncBound().getUpperBound())) {
            variableLocus.put("inc", locusIndex);
            locusIndex++;
        }
        if (!repSat.getArgPerBound().getLowerBound().equals(repSat.getArgPerBound().getUpperBound())) {
            variableLocus.put("ap", locusIndex);
            locusIndex++;
        }
        if (!repSat.getRaanBound().getLowerBound().equals(repSat.getRaanBound().getUpperBound())) {
            variableLocus.put("raan", locusIndex);
            locusIndex++;
        }
        if (!repSat.getAnomBound().getLowerBound().equals(repSat.getAnomBound().getUpperBound())) {
            variableLocus.put("ta", locusIndex);
        }

        Solution[] parents = new Solution[satellites.length];
        for (int i = 0; i < satellites.length; i++) {
            Solution parent = new Solution(variableLocus.size(), 0);
            SatelliteVariable sat = satellites[i];
            if (variableLocus.containsKey("sma")) {
                parent.setVariable(variableLocus.get("sma"),
                        new RealVariable(sat.getSma(), sat.getSmaBound().getLowerBound(), sat.getSmaBound().getUpperBound()));
            }
            if (variableLocus.containsKey("ecc")) {
                parent.setVariable(variableLocus.get("ecc"),
                        new RealVariable(sat.getEcc(), sat.getEccBound().getLowerBound(), sat.getEccBound().getUpperBound()));
            }
            if (variableLocus.containsKey("inc")) {
                parent.setVariable(variableLocus.get("inc"),
                        new RealVariable(sat.getInc(), sat.getIncBound().getLowerBound(), sat.getIncBound().getUpperBound()));
            }
            if (variableLocus.containsKey("ap")) {
                parent.setVariable(variableLocus.get("ap"),
                        new RealVariable(sat.getArgPer(), sat.getArgPerBound().getLowerBound(), sat.getArgPerBound().getUpperBound()));
            }
            if (variableLocus.containsKey("raan")) {
                parent.setVariable(variableLocus.get("raan"),
                        new RealVariable(sat.getRaan(), sat.getRaanBound().getLowerBound(), sat.getRaanBound().getUpperBound()));
            }
            if (variableLocus.containsKey("ta")) {
                parent.setVariable(variableLocus.get("ta"),
                        new RealVariable(sat.getTrueAnomaly(), sat.getAnomBound().getLowerBound(), sat.getAnomBound().getUpperBound()));
            }
            parents[i] = parent;
        }

        Solution[] offspring = operator.evolve(parents);

        SatelliteVariable[] out = new SatelliteVariable[satellites.length];
        for (int i = 0; i < satellites.length; i++) {
            Solution child = offspring[i];
            SatelliteVariable satVar = satellites[i];
            if (variableLocus.containsKey("sma")) {
                satVar.setSma(((RealVariable) child.getVariable(variableLocus.get("sma"))).getValue());
            }
            if (variableLocus.containsKey("ecc")) {
                satVar.setEcc(((RealVariable) child.getVariable(variableLocus.get("ecc"))).getValue());
            }
            if (variableLocus.containsKey("inc")) {
                satVar.setInc(((RealVariable) child.getVariable(variableLocus.get("inc"))).getValue());
            }
            if (variableLocus.containsKey("ap")) {
                satVar.setArgPer(((RealVariable) child.getVariable(variableLocus.get("ap"))).getValue());
            }
            if (variableLocus.containsKey("raan")) {
                satVar.setRaan(((RealVariable) child.getVariable(variableLocus.get("raan"))).getValue());
            }
            if (variableLocus.containsKey("ta")) {
                satVar.setTrueAnomaly(((RealVariable) child.getVariable(variableLocus.get("ta"))).getValue());
            }

            out[i] = satVar;
        }
        return out;
    }
}
