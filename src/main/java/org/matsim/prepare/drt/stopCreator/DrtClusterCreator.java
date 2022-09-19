package org.matsim.prepare.drt.stopCreator;

import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.run.NetworkCleaner;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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
        this.networkFile = "/Users/dariush/Desktop/BA-Ordner/MATSim/input/Network/drtNetwork-loopLinksP.xml";
    }

    public static void main(String[] args) {
        if(args.length==0){

        }


        DrtClusterCreator creator = new DrtClusterCreator();
        String networkFile = creator.networkFile;           //args[4];
        Network network = NetworkUtils.readNetwork(networkFile);
//        System.out.println(creator.readData());

        List<List<Double>> doublePointList = creator.readDataToDoubleList(creator.file);
        List<String> clusterCenters = creator.getClusterCenters(doublePointList, creator.random, 2, 2);

//        List<String> clusterCenters = creator.getClusterCenters(doublePointList, creator.random, Integer.valueOf(args[1]), Integer.valueOf(args[2]));


        List<List<Double>> testList = new ArrayList<>();
        testList.add(Arrays.asList(1.0,3.5));
        testList.add(Arrays.asList(2.0,3.5));
        testList.add(Arrays.asList(1.5,5.0));
        testList.add(Arrays.asList(1.5,0.5));
        testList.add(Arrays.asList(0.5,1.5));
        testList.add(Arrays.asList(0.5,0.5));
        testList.add(Arrays.asList(1.5,1.5));
        testList.add(Arrays.asList(3.0,1.5));
        testList.add(Arrays.asList(5.0,2.5));
        testList.add(Arrays.asList(4.0,1.0));
        testList.add(Arrays.asList(5.0,1.0));
        testList.add(Arrays.asList(4.0,2.0));

//        List<String> testCenters = creator.getClusterCenters(testList, creator.random, 3,5);
//        System.out.println(testCenters);
        creator.stringToTransitStopFacility(clusterCenters, network);

    }

    private void stringToTransitStopFacility(List<String> testCenters, Network network) {
        Scenario kMeansScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = kMeansScenario.getTransitSchedule();
        TransitScheduleFactory scheduleFactory = schedule.getFactory();
        List<Coord> coordList = new ArrayList<>();
//        HashMap<Character, Character> replaceMap = new HashMap<>();
//        replaceMap.p
        for(String string: testCenters){
            string = string.substring(1, string.length()-1);
//            string.replaceAll(Character.toString(')'), Character.toString(' '));

            System.out.println(string);
            String[] parts = string.split(",");
            double[] doubleArray = Arrays.stream(parts).mapToDouble(Double::parseDouble).toArray();
            coordList.add(new Coord(doubleArray[0],doubleArray[1]));
        }
//        for(Link link: network.getLinks().values()){
//            if(!link.getId().toString().contains("L")){
//                network.removeLink(link.getId());
//            }
//        }

        for(int i=0;i<coordList.size();i++){
            TransitStopFacility drtStop = scheduleFactory.createTransitStopFacility(Id.create("drtStop" + i, TransitStopFacility.class),
                    coordList.get(i), false);
            drtStop.setLinkId(NetworkUtils.getNearestLink(network,coordList.get(i)).getId());
            schedule.addStopFacility(drtStop);
        }
        TransitScheduleWriter scheduleWriter = new TransitScheduleWriter(kMeansScenario.getTransitSchedule());
        scheduleWriter.writeFileV2("/Users/dariush/Desktop/BA-Ordner/MATSim/input/DrtStopFile/kMeansStopFile.xml");
    }

    protected List<List<Double>> readDataToDoubleList(String file) {
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
        List<List<Double>> doublePointList = new ArrayList<>();

        for(List<String> string:records.subList(1,records.size())){
            pointList.add(Arrays.asList(string.get(4),string.get(5)));
            pointList.add(Arrays.asList(string.get(7),string.get(8)));
        }

        for(List<String> string: pointList) {

            doublePointList.add(string.stream().map(s -> Double.valueOf(s)).collect(Collectors.toList()));
        }
        log.info("Finished reading");
        return doublePointList;
    }

    List<String> getClusterCenters(List<List<Double>> doublePointList, Random random, int k, int maxIterations){
        log.info("Start creating Cluster Centers");
        List<EuclideanDoublePoint> cDList = new ArrayList<>();
        for(List<Double> doubleList:doublePointList){
            double[] doubleArray= doubleList.stream().mapToDouble(d->d).toArray();
            EuclideanDoublePoint cD = new EuclideanDoublePoint(doubleArray);
            cDList.add(cD);
        }

        List<String> clusterCenter = new ArrayList<>();
        KMeansPlusPlusClusterer clusterer = new KMeansPlusPlusClusterer(random);
        List<Cluster> cluster = clusterer.cluster(cDList, k, maxIterations);
        for(Cluster c : cluster){
            String string = c.getCenter().toString();
//            Double d = Double.valueOf(string);
            clusterCenter.add(string);
        }
        return clusterCenter;
    }

}
