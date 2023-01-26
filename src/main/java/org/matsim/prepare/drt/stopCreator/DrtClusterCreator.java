package org.matsim.prepare.drt.stopCreator;

import com.opencsv.CSVWriter;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math.util.MathUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.*;
import java.util.*;

public class DrtClusterCreator {
    private String file;
    private Random random = MatsimRandom.getRandom();
    private static final Logger log = Logger.getLogger(DrtStopCreator.class);
    private String networkFile;

    public DrtClusterCreator(String file) {
        this.file=file;
    }

    public DrtClusterCreator() {
        this.file = "/Users/dariush/Downloads/hundekopf-rebalancing-2000vehicles-4seats.200.drt_legs_drt.csv";
        this.networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
    }

    public static void main(String[] args) throws IOException {
        if(args.length==0){
            log.info("No args arguments");
        }
//      arg[0] = legFile
//      arg[1] = networkFile
//      arg[2] = Anzahl Cluster
//      arg[3] = maximale Iterationen (sollten 10 bis 15 sein)
//      arg[4] = outputStopFile
//      arg[5] = outputSSE
//      args[6]= maxAnzahlClusterSSE
//      args[7]= clusterInkrementSSE
//      args[8]= clusterIterationsSSE



        DrtClusterCreator creator = new DrtClusterCreator();
//        String networkFile = creator.networkFile;           //args[1];
        Network network = NetworkUtils.readNetwork(creator.networkFile);
//        System.out.println(creator.readData());

//        List<double[]> doublePointList = creator.readDataToDoubleList(creator.file);
        List<double[]> doublePointList = creator.readDataToDoubleList(creator.file);

        Map<double[], List<double[]>> clusters = creator.getClusters(doublePointList, creator.random, 830, 15);
        List<double[]> clusterCenters = new ArrayList<>();
        for(double[] d:clusters.keySet()){
            clusterCenters.add(d);
        }
//        List<String> clusterCenters = creator.getClusterCenters(doublePointList, creator.random, Integer.valueOf(args[1]), Integer.valueOf(args[2]));


//        List<double[]> testList = new ArrayList<>();
//        testList.add(new double[]{1.0, 3.5});
//        testList.add(new double[]{2.0,3.5});
//        testList.add(new double[]{1.5,5.0});
//        testList.add(new double[]{1.5,0.5});
//        testList.add(new double[]{0.5,1.5});
//        testList.add(new double[]{0.5,0.5});
//        testList.add(new double[]{1.5,1.5});
//        testList.add(new double[]{3.0,1.5});
//        testList.add(new double[]{5.0,2.5});
//        testList.add(new double[]{4.0,1.0});
//        testList.add(new double[]{5.0,1.0});
//        testList.add(new double[]{4.0,2.0});
//
//        Map<double[], List<double[]>> testClusters = creator.getClusters(testList, creator.random, 3,5);

//        System.out.println(testClusters);
        creator.centersToTransitStopFacility(clusterCenters, network, "/Users/dariush/Desktop/BA-Ordner/MATSim/input/DrtStopFile/clusterStopFile830.xml");

//        List<Double> sumOfSquaredErrors = new ArrayList<>();
//        for (int k = 1; k <= Integer.parseInt(args[6]); k+=Integer.parseInt(args[7])) {
//            Map<double[], List<double[]>> testClusters2 = creator.getClusters(doublePointList, creator.random, k,Integer.parseInt(args[8]));
//            double sse = creator.sse(testClusters2);
//            sumOfSquaredErrors.add(sse);
//        }
////        List<String> sumOfSquaredErrorsStrings = new ArrayList<>();
////        for(Double d:sumOfSquaredErrors){
////            sumOfSquaredErrorsStrings.add(d.toString());
////        }
//
////        FileWriter fw = new FileWriter("/Users/dariush/Desktop/BA-Ordner/MATSim/output/SSE.csv");
//        FileWriter fw = new FileWriter(args[5]);
//        CSVWriter writer = new CSVWriter(fw);
//        for(Double d:sumOfSquaredErrors){
//            writer.writeNext(new String[]{Double.toString(d)});
//        }
//        writer.close();
//        fw.close();

    }


    private void centersToTransitStopFacility(List<double[]> clusterCenters, Network network, String outputFile) {
        Scenario kMeansScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = kMeansScenario.getTransitSchedule();
        TransitScheduleFactory scheduleFactory = schedule.getFactory();
        List<Coord> coordList = new ArrayList<>();
        List<Link> loopLinks = new ArrayList<>(); // 33000
//        HashMap<Character, Character> replaceMap = new HashMap<>();
//        replaceMap.p
        for(double[] doubleArray: clusterCenters){
//            string = string.substring(1, string.length()-1);
////            string.replaceAll(Character.toString(')'), Character.toString(' '));
//
//            System.out.println(string);
//            String[] parts = string.split(",");
//            double[] doubleArray = Arrays.stream(parts).mapToDouble(Double::parseDouble).toArray();
            coordList.add(new Coord(doubleArray[0],doubleArray[1]));
        }
        for(Link link: network.getLinks().values()){
            if(link.getId().toString().contains("L")){
                loopLinks.add(link);
            }
        }

        Network networkWithoutPt = network;
        for(Link link:networkWithoutPt.getLinks().values()){
            if(link.getId().toString().contains("pt")){
                networkWithoutPt.removeLink(link.getId());
            }
        }
        for(Node node:networkWithoutPt.getNodes().values()){
            if(node.getId().toString().contains("pt")){
                networkWithoutPt.removeNode(node.getId());
            }
        }

        for(int i=0;i<coordList.size();i++){

            TransitStopFacility drtStop = scheduleFactory.createTransitStopFacility(Id.create("drtStop" + i, TransitStopFacility.class),
                    coordList.get(i), false);

//            Link nearestLoopLink = getNearestLoopLink(loopLinks,coordList.get(i));
//            drtStop.setLinkId(nearestLoopLink.getId());
//            drtStop.setCoord(nearestLoopLink.getCoord());

            Link nearestLink = NetworkUtils.getNearestLink(networkWithoutPt,coordList.get(i));
            drtStop.setLinkId(nearestLink.getId());
            drtStop.setCoord(nearestLink.getCoord());

            schedule.addStopFacility(drtStop);
        }
        TransitScheduleWriter scheduleWriter = new TransitScheduleWriter(kMeansScenario.getTransitSchedule());
//        scheduleWriter.writeFileV2("/Users/dariush/Desktop/BA-Ordner/MATSim/input/DrtStopFile/kMeansLoopLinksStopFile.xml");
        scheduleWriter.writeFileV2(outputFile);

    }


    protected List<double[]> readDataToDoubleList(String file) {
        log.info("Start reading File "+file);
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;
            while ((line = br.readLine()) != null) {
                List<String> stringList = Arrays.asList(line.split(";"));
                records.add(stringList);
            }

        } catch (FileNotFoundException e) {
            System.out.println("Reading CSV Error!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<List<String>> pointList = new ArrayList<>();
//        List<List<Double>> doublePointList = new ArrayList<>();
        List<double[]> doublePointList = new ArrayList<>();


        for(List<String> string:records.subList(1,records.size())){
            pointList.add(Arrays.asList(string.get(4),string.get(5)));
            pointList.add(Arrays.asList(string.get(7),string.get(8)));
        }

        for(List<String> string: pointList) {
//            doublePointList.add(string.stream().map(s -> Double.valueOf(s)).collect(Collectors.toList()));
            doublePointList.add(string.stream().mapToDouble(d-> Double.parseDouble(d)).toArray());

        }
        log.info("Finished reading");
        return doublePointList;
    }

    Map<double[], List<double[]>> getClusters(List<double[]> doubleArrayList, Random random, int k, int maxIterations){
        log.info("Start creating Cluster Centers");
        List<EuclideanDoublePoint> cDList = new ArrayList<>();
        for(double[] d:doubleArrayList){
//            double[] doubleArray= doubleList.stream().mapToDouble(d->d).toArray();
            EuclideanDoublePoint cD = new EuclideanDoublePoint(d);
            cDList.add(cD);
        }

//        List<String> clusterCenter = new ArrayList<>();
//        List<String> clusterPoints = new ArrayList<>();
        Map<double[], List<double[]>> clusters = new HashMap<>();


        KMeansPlusPlusClusterer clusterer = new KMeansPlusPlusClusterer(random);
        List<Cluster> cluster = clusterer.cluster(cDList, k, maxIterations);
        for(Cluster c : cluster){
            EuclideanDoublePoint eDP = (EuclideanDoublePoint) c.getCenter();
            List<EuclideanDoublePoint> points = c.getPoints();
            List<double[]> doublePoints = new ArrayList<>();
            for(EuclideanDoublePoint p:points){
                doublePoints.add(p.getPoint());
            }
//            Double d = Double.valueOf(string);
            clusters.put(eDP.getPoint(), doublePoints);
//            clusterCenter.add(string);
        }
        return clusters;
    }

    private static Link getNearestLoopLink(List<Link> loopLinks, Coord coord) {
        Link nearestLink = null;
        double shortestDistance = 1.7976931348623157E308D;

        Iterator var6 = loopLinks.iterator();

        while(var6.hasNext()) {
            Link link = (Link)var6.next();
            double dist = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);
            if (dist < shortestDistance) {
                shortestDistance = dist;
                nearestLink = link;
            }
        }

        if (nearestLink == null) {
            log.warn("[nearestLink not found]");
        }

        return nearestLink;
    }

    public static double sse(Map<double[], List<double[]>> clustered) {
        double sum = 0;
        for (Map.Entry<double[], List<double[]>> entry : clustered.entrySet()) {
            double[] centroid = entry.getKey();
            for (double[] record : entry.getValue()) {
                double d = MathUtils.distance(centroid, record);
                sum += Math.pow(d, 2);
            }
        }
        return sum;
    }


}
