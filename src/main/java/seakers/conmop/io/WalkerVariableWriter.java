package seakers.conmop.io;

import org.moeaframework.core.Solution;
import seakers.conmop.variable.BooleanSatelliteVariable;
import seakers.conmop.variable.ConstellationVariable;
import seakers.conmop.variable.SatelliteVariable;
import seakers.conmop.variable.WalkerVariable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

public class WalkerVariableWriter {

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

            WalkerVariable walkerVar = (WalkerVariable) population.get(0).getVariable(0);

            header.add("sma");
            header.add("inc");
            header.add("t");
            header.add("p");
            header.add("f");

            header.add("mean_resp");
            header.add("num_sats");

            writer.append(header.toString());
            writer.append("\n");

            StringBuilder content = new StringBuilder();

            int cnt = 0;
            for(Solution solution: population){

                WalkerVariable constel = (WalkerVariable) solution.getVariable(0);

                StringJoiner row = new StringJoiner(",");

                row.add(Double.toString(constel.getSma()));
                row.add(Double.toString(constel.getInc()));
                row.add(Integer.toString(constel.getT()));
                row.add(Integer.toString(constel.getP()));
                row.add(Integer.toString(constel.getF()));

                // Add objectives
                double[] objectives = solution.getObjectives();
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
