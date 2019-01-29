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

    public final int maxNumSat = 20;

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

        File saveFile = new File(filename);
        saveFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(saveFile)) {

            StringJoiner header = new StringJoiner(",");

            ConstellationVariable temp = (ConstellationVariable) population.get(0).getVariable(0);

            for(int i = 1; i < maxNumSat + 1; i++){
                header.add("sma" + i);
            }
            for(int i = 1; i < maxNumSat + 1; i++){
                header.add("inc" + i);
            }
            for(int i = 1; i < maxNumSat + 1; i++){
                header.add("raan" + i);
            }
            for(int i = 1; i < maxNumSat + 1; i++){
                header.add("ta" + i);
            }

            header.add("mean_resp");
            header.add("num_sats");
            header.add("avg_sma");

            writer.append(header.toString());
            writer.append("\n");
            writer.flush();

            StringBuilder content = new StringBuilder();

            int cnt = 0;
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

                for(int i = 0; i < maxNumSat; i++){
                    if(i < sma.size()){
                        row.add(Double.toString(sma.get(i)));
                    }else{
                        row.add("null");
                    }
                }

                for(int i = 0; i < maxNumSat; i++){
                    if(i < inc.size()){
                        row.add(Double.toString(inc.get(i)));
                    }else{
                        row.add("null");
                    }
                }

                for(int i = 0; i < maxNumSat; i++){
                    if(i < raan.size()){
                        row.add(Double.toString(raan.get(i)));
                    }else{
                        row.add("null");
                    }
                }

                for(int i = 0; i < maxNumSat; i++){
                    if(i < ta.size()){
                        row.add(Double.toString(ta.get(i)));
                    }else{
                        row.add("null");
                    }
                }

                // Add objectives
                for(double obj: objectives){
                    row.add(Double.toString(obj));
                }

                content.append(row.toString());
                content.append("\n");
                cnt++;

                if(cnt % 500 == 0){
                    writer.append(content.toString());
                    writer.flush();
                    content = new StringBuilder();
                    cnt = 0;
                }
            }

            if(content.length() != 0){
                writer.append(content.toString());
                writer.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
