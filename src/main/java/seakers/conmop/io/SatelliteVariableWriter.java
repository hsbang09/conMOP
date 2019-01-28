package seakers.conmop.io;

import org.moeaframework.core.Solution;
import seakers.conmop.variable.BooleanSatelliteVariable;
import seakers.conmop.variable.ConstellationVariable;
import seakers.conmop.variable.SatelliteVariable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

public class SatelliteVariableWriter {

    public void write(String filename, Iterator<Solution> populationIter){

        List<Solution> population = new ArrayList<>();
        while(populationIter.hasNext()){
            Solution sol = populationIter.next();
            population.add(sol);
        }

        write(filename, population);
    }

    public void write(String filename, List<Solution> population){

        System.out.println("Saving file: " + filename);

        StringJoiner header = new StringJoiner(",");
        StringBuilder content = new StringBuilder();

        ConstellationVariable temp = (ConstellationVariable) population.get(0).getVariable(0);
        int cnt = 0;
        for (SatelliteVariable var : temp.getSatelliteVariables()) {
            if (var instanceof BooleanSatelliteVariable) {
                if (!((BooleanSatelliteVariable) var).getManifest()) {
                    continue;
                }
            }
            cnt++;
        }

        for(int i = 0; i < cnt; i++){
            header.add("sma" + i);
        }
        for(int i = 0; i < cnt; i++){
            header.add("inc" + i);
        }
        for(int i = 0; i < cnt; i++){
            header.add("raan" + i);
        }
        for(int i = 0; i < cnt; i++){
            header.add("ta" + i);
        }

        header.add("mean_resp");
        header.add("num_sats");
        header.add("avg_sma");

        for(Solution solution: population){

            double[] objectives = solution.getObjectives();
            ConstellationVariable constel = (ConstellationVariable) solution.getVariable(0);
            List<Double> sma = new ArrayList<>();
            List<Double> inc = new ArrayList<>();
            List<Double> raan = new ArrayList<>();
            List<Double> ta = new ArrayList<>();

            for (SatelliteVariable var : constel.getSatelliteVariables()) {
                if (var instanceof BooleanSatelliteVariable) {
                    if (!((BooleanSatelliteVariable) var).getManifest()) {
                        continue;
                    }
                }
                sma.add(var.getSma());
                inc.add(var.getInc());
                raan.add(var.getRaan());
                ta.add(var.getTrueAnomaly());
            }

            StringJoiner row = new StringJoiner(",");
            for(double val: sma){
                row.add(Double.toString(val));
            }
            for(double val: inc){
                row.add(Double.toString(val));
            }
            for(double val: raan){
                row.add(Double.toString(val));
            }
            for(double val: ta){
                row.add(Double.toString(val));
            }

            // Add objectives
            for(double obj: objectives){
                row.add(Double.toString(obj));
            }

            content.append(row.toString());
            content.append("\n");
        }

        File saveFile = new File(filename);
        saveFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(saveFile)) {

            writer.append(header.toString());
            writer.append("\n");
            writer.append(content.toString());
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
