package org.matsim.prepare.drt.stopCreator;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.Clusterable;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;
import org.apache.poi.ss.formula.functions.T;
import org.checkerframework.checker.units.qual.C;
import org.matsim.contrib.util.CSVReaders;
import org.matsim.core.gbl.MatsimRandom;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ODCoordCreator {
    public static void main(String[] args) {
        String file = "/Users/dariush/Downloads/hundekopf-rebalancing-2000vehicles-4seats.200.drt_legs_drt.csv";
        Random random = MatsimRandom.getRandom();
        ODCoordCreator creator = new ODCoordCreator();
//        System.out.println(creator.readData());
        List<List<String>> csvData = creator.readData();
        List<List<String>> pointList = new ArrayList<>();
        List<List<Double>> doublePointList = new ArrayList<>();
        List<ClusterableDouble> cDList = new ArrayList<>();

        for(List<String> string:csvData.subList(1,csvData.size())){
            pointList.add(Arrays.asList(string.get(4),string.get(5)));
            pointList.add(Arrays.asList(string.get(7),string.get(8)));
        }

        for(List<String> string: pointList) {
            doublePointList.add(string.stream().map(s -> Double.valueOf(s)).collect(Collectors.toList()));
        }



        for(List<Double> doubleList:doublePointList){
            double[] doubleArray= doubleList.stream().mapToDouble(d->d).toArray();
            ClusterableDouble cD = new ClusterableDouble(doubleArray);
            cDList.add(cD);
        }

//        List<ClusterableDouble> test = new ArrayList<>();
//        double[] d1 ={1,2};
//        double[] d2 ={1,3};
//        double[] d3 ={4,2};
//        double[] d4 ={3,2};
//        double[] d5 ={3,3};
//        test.add(new ClusterableDouble(d1));
//        test.add(new ClusterableDouble(d2));
//        test.add(new ClusterableDouble(d3));
//        test.add(new ClusterableDouble(d4));
//        test.add(new ClusterableDouble(d5));

        List<Clusterable> bidde = new ArrayList<>();

        KMeansPlusPlusClusterer clusterer = new KMeansPlusPlusClusterer(random);
        List<Cluster> cluster = clusterer.cluster(cDList, 20, 5);
        for(Cluster c : cluster){
            bidde.add(c.getCenter());
        }

        for(Clusterable i:bidde){
            System.out.println(i.toString());
        }
        System.out.println("Hi");


//        CSVReaders reader = new CSVReaders();
//        reader.readFile("/Users/dariush/Downloads/hundekopf-rebalancing-2000vehicles-4seats.200.drt_legs_drt.csv", ';');
//        reader.hasNext();
    }
    protected List<List<String>> readData() {
        String file = "/Users/dariush/Downloads/hundekopf-rebalancing-2000vehicles-4seats.200.drt_legs_drt.csv";
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
        return records;
    }
}
