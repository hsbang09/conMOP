/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.conmop;

import org.hipparchus.util.FastMath;
import org.moeaframework.algorithm.EpsilonMOEA;
import org.moeaframework.core.*;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.operator.CompoundVariation;
import org.moeaframework.core.operator.RandomInitialization;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.operator.real.SBX;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.aos.aos.AOSMOEA;
import seakers.aos.creditassignment.setimprovement.SetImprovementDominance;
import seakers.aos.history.AOSHistoryIO;
import seakers.aos.operator.AOSVariation;
import seakers.aos.operator.AOSVariationSI;
import seakers.aos.operatorselectors.AdaptivePursuit;
import seakers.aos.operatorselectors.OperatorSelector;
import seakers.conmop.operators.OrbitElementOperator;
import seakers.conmop.operators.VariableLengthOnePointCrossover;
import seakers.conmop.operators.VariablePM;
import seakers.conmop.util.Bounds;
import seakers.conmop.io.SatelliteVariableWriter;
import seakers.orekit.object.CommunicationBand;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.util.OrekitConfig;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nozomihitomi
 */
public class SearchHarris {

    private static final double DEG_TO_RAD = Math.PI / 180.;

    /**
     * @param args the command line arguments
     * @throws OrekitException
     */
    public static void main(String[] args) throws OrekitException {

        // Set path
        StringJoiner path = new StringJoiner(File.separator);
        path.add(System.getProperty("user.dir"));
        path.add("results");

        //setup logger
        Level level = Level.FINEST;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));

        OrekitConfig.init(3);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2016, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2016, 1, 8, 00, 00, 00.000, utc);

        //Enter satellite orbital parameters
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

        PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2);

        Properties problemProperty = new Properties();

        Bounds<Integer> tBounds = new Bounds<>(1, 20);
        Bounds<Double> smaBounds = new Bounds<>(a + 400000, a + 1000000);
        Bounds<Double> incBounds = new Bounds<>(30. * DEG_TO_RAD, 100. * DEG_TO_RAD);

        //properties for launch deployment
        problemProperty.setProperty("raanTimeLimit", "604800");
        problemProperty.setProperty("dvLimit", "600");

        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        CoverageDefinition cdef = new CoverageDefinition("cdef", 20.0, -30, 30, -180, 180, earthShape, CoverageDefinition.GridStyle.EQUAL_AREA);
        Set<GeodeticPoint> points = new HashSet<>();
        for (CoveragePoint pt : cdef.getPoints()) {
            points.add(pt.getPoint());
        }

        //define ground stations
        ArrayList<GndStation> gndStations = new ArrayList<>();
        TopocentricFrame wallopsTopo = new TopocentricFrame(earthShape, new GeodeticPoint(FastMath.toRadians(37.94019444), FastMath.toRadians(-75.46638889), 0.), "Wallops");
        HashSet<CommunicationBand> wallopsBands = new HashSet<>();
        wallopsBands.add(CommunicationBand.UHF);
        gndStations.add(new GndStation(wallopsTopo, new ReceiverAntenna(6., wallopsBands), new TransmitterAntenna(6., wallopsBands), FastMath.toRadians(10.)));
        TopocentricFrame moreheadTopo = new TopocentricFrame(earthShape, new GeodeticPoint(FastMath.toRadians(38.19188139), FastMath.toRadians(-83.43861111), 0.), "Mroehead");
        HashSet<CommunicationBand> moreheadBands = new HashSet<>();
        moreheadBands.add(CommunicationBand.UHF);
        gndStations.add(new GndStation(moreheadTopo, new ReceiverAntenna(47., moreheadBands), new TransmitterAntenna(47., moreheadBands), FastMath.toRadians(6.)));

        Problem problem = new ConstellationOptimizer("", startDate, endDate, pf,
                points, FastMath.toRadians(51.),
                tBounds, smaBounds, incBounds, gndStations, problemProperty);
        
        //set up the search parameters
        int populationSize = 200;
        int maxNFE = 10000;

        // Set run name
        StringJoiner runName = new StringJoiner("_");

//        String mode = path + "static";
        String mode =  "variable";
//        String mode = "kd";

        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());

        runName.add(mode);
        runName.add(timestamp);
        path.add(runName.toString());

        for (int i = 0; i < 1; i++) {

            // Set base filenames
            path.add(mode + "_" + i);
            String baseFilename = path.toString();

            long startTime = System.nanoTime();
            Initialization initialization = new RandomInitialization(problem,
                    populationSize);

            Population population = new Population();
            DominanceComparator comparator = new ParetoDominanceComparator();
//            EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(new double[]{30, 1, 100, 30});
            EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(new double[]{10, 1, 1000});

            final TournamentSelection selection = new TournamentSelection(2, comparator);

            //set up variations
            //example of operators you might use
            ArrayList<Variation> operators = new ArrayList<>();
            operators.add(new OrbitElementOperator(
                    new CompoundVariation(new SBX(1, 20), new VariablePM(20))));
            operators.add(new VariableLengthOnePointCrossover(1.0, tBounds));
//            operators.add(new DecreasePlanes());
//            operators.add(new DistributeAnomaly());
//            operators.add(new DistributePlanes());
//            operators.add(new IncreasePlanes());
//            operators.add(new CompoundVariation(
//                    new StaticOrbitElementOperator(
//                            new CompoundVariation(new SBX(1, 20),
//                                    new BinaryUniformCrossover(0.5), new VariablePM(20),
//                                    new BitFlip(1./140.))),
//                    new RepairNumberOfSatellites()
//            ));
//            operators.add(new CompoundVariation(
//                    new StaticLengthOnePointCrossover(1.0), new StaticOrbitElementOperator(
//                            new CompoundVariation(new VariablePM(20), new BitFlip(1./140.))),
//                            new RepairNumberOfSatellites()));


            //create operator selector
            OperatorSelector operatorSelector = new AdaptivePursuit(operators, 0.8, 0.8, 0.03);

            //create credit assignment
            SetImprovementDominance creditAssignment = new SetImprovementDominance(archive, 1, 0);

            //create AOS strategy
            AOSVariation aosStrategy = new AOSVariationSI(operatorSelector, creditAssignment, populationSize);

            // create EpsilonMOEA
            EpsilonMOEA emoea = new EpsilonMOEA(problem, population, archive,
                    selection, aosStrategy, initialization, comparator);

            // create AOS
            AOSMOEA aos = new AOSMOEA(emoea, aosStrategy, true);

            System.out.println(String.format("Initializing population... Size = %d", populationSize));

            int popIndex = 0;
            while (aos.getNumberOfEvaluations() < maxNFE) {

                aos.step();
                double currentTime = ((System.nanoTime() - startTime) / Math.pow(10, 9)) / 60.;
                System.out.println(
                        String.format("%d NFE out of %d NFE: Time elapsed = %10f min."
                                + " Approximate time remaining %10f min.",
                                aos.getNumberOfEvaluations(), maxNFE, currentTime,
                                currentTime / emoea.getNumberOfEvaluations() * (maxNFE - aos.getNumberOfEvaluations())));

                if(aos.getNumberOfEvaluations() % 1000 == 0){// Initialize population writer
                    SatelliteVariableWriter writer = new SatelliteVariableWriter();
                    writer.write(baseFilename + "_population_" + popIndex + ".csv", aos.getPopulation().iterator());
                    popIndex++;
                }
            }
            System.out.println(aos.getArchive().size());

            long endTime = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (endTime - startTime) / Math.pow(10, 9)));

            // Initialize population writer
            SatelliteVariableWriter writer = new SatelliteVariableWriter();

            // Save solutions in a csv file
            writer.write(baseFilename + "_archive.csv", aos.getArchive().iterator());
            writer.write(baseFilename + "_allSolutions.csv", aos.getAllSolutions().iterator());

            try {
                PopulationIO.write(new File(baseFilename + "_all.pop"), aos.getAllSolutions());
                PopulationIO.write(new File(baseFilename + ".pop"), aos.getPopulation());
                PopulationIO.writeObjectives(new File(baseFilename + "_all.obj"), aos.getAllSolutions());
                PopulationIO.writeObjectives(new File(baseFilename + ".obj"), aos.getPopulation());
            } catch (IOException ex) {
                Logger.getLogger(SearchHarris.class.getName()).log(Level.SEVERE, null, ex);
            }
            AOSHistoryIO.saveCreditHistory(aos.getCreditHistory(), new File(baseFilename + ".credit"), ",");
            AOSHistoryIO.saveSelectionHistory(aos.getSelectionHistory(), new File(baseFilename + ".select"), ",");
        }

        OrekitConfig.end();
    }
}
