/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.conmop.operators.knowledge;

import aos.operator.AbstractCheckParent;
import java.util.ArrayList;
import java.util.Collections;
import org.hipparchus.util.FastMath;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import seakers.conmop.comparators.SatelliteComparator;
import seakers.conmop.deployment.DeploymentStrategy;
import seakers.conmop.deployment.Installment;
import seakers.conmop.util.OrbitalElementEnum;
import seakers.conmop.variable.ConstellationVariable;
import seakers.conmop.variable.SatelliteVariable;

/**
 * Operator that tries to distribute the mean anomalies of satellites within the
 * same plane
 *
 * @author nozomihitomi
 */
public class DistributeAnomaly extends AbstractCheckParent  {

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    public Solution[] evolveParents(Solution[] parents) {
        Solution child = parents[0].copy();
        for (int i = 0; i < child.getNumberOfVariables(); i++) {
            if (child.getVariable(i) instanceof ConstellationVariable) {
                child.setVariable(i, evolve((ConstellationVariable) child.getVariable(i)));
            }
        }
        return new Solution[]{child};
    }

    /**
     * The operation to distribute the anomalies within a plane. The order by
     * anomaly of the satellites in that plane remains the same. The anomalies
     * are modified to be regularly distributed
     *
     * @param constelVariable
     * @return The modified instance of a constellation variable
     */
    private ConstellationVariable evolve(ConstellationVariable constelVariable) {
        DeploymentStrategy deploymentStrategy = constelVariable.getDeploymentStrategy();

        ArrayList<SatelliteVariable> allSats = new ArrayList<>();

        //randomly select a installment to operate on
        ArrayList<Installment> installmentCandidates = new ArrayList<>();
        for (Installment installment : deploymentStrategy.getInstallments()) {
            if (installment.getSatellites().size() > 1) {
                installmentCandidates.add(installment);
            }
            allSats.addAll(installment.getSatellites());
        }

        if (installmentCandidates.isEmpty()) {
            return constelVariable;
        }

        Collections.shuffle(installmentCandidates);
        Installment installmentCandidate = installmentCandidates.get(0);
        ArrayList<SatelliteVariable> sats = new ArrayList<>(installmentCandidate.getSatellites());
        int nsats = sats.size();
        double separation = 2. * FastMath.PI / nsats;

        //select random satellite within plane whose anomaly will remain constant
        Collections.sort(sats, new SatelliteComparator(OrbitalElementEnum.TA));
        int n = PRNG.nextInt(nsats);
        SatelliteVariable anchor = sats.get(n);
        for (int i = 0; i < n; i++) {
            double newAnomaly = anchor.getTrueAnomaly() - separation * (n - i);
            if (newAnomaly < 0) {
                newAnomaly += 2. * FastMath.PI;
            }
            sats.get(i).setTrueAnomaly(newAnomaly);
        }
        for (int i = n + 1; i < sats.size(); i++) {
            double newAnomaly = anchor.getTrueAnomaly() + separation * (i - n);
            if (newAnomaly > 2. * FastMath.PI) {
                newAnomaly -= 2. * FastMath.PI;
            }
            sats.get(i).setTrueAnomaly(newAnomaly);
        }

        constelVariable.setSatelliteVariables(allSats);
        return constelVariable;
    }

    @Override
    public boolean check(Solution[] parents) {
        for (Solution parent : parents) {
            for (int i = 0; i < parent.getNumberOfVariables(); i++) {
                if (parent.getVariable(i) instanceof ConstellationVariable) {
                    ConstellationVariable constelVariable
                            = (ConstellationVariable) parent.getVariable(i);
                    for (Installment installment : constelVariable.getDeploymentStrategy().getInstallments()) {
                        if (installment.getSatellites().size() > 1) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
