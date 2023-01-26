package org.matsim.analysis.drt;

import com.opencsv.CSVWriter;
import org.matsim.api.core.v01.Coord;

import java.io.*;
import java.util.*;

public class DrtWalkingDistance {


    public static void main(String[] args) {
        String output_legs = "/Users/dariush/output/output-legs/busStops-2000vehicles-4seats.output_legs.csv";
        String outputFileName = "/Users/dariush/output/output-folder/busStops_walkDistance.txt";
        String outputMap = "/Users/dariush/output/output-folder/busStops_walkDistanceMap.csv";
        String outputPeople = "/Users/dariush/output/output-folder/busStops_peopleOutput.txt";
        DrtWalkingDistance drtWalkingDistance = new DrtWalkingDistance();
        List<Double> drtPersons = new ArrayList<>();
        List<List<String>> walkDistanceListList = drtWalkingDistance.readDataToMap(output_legs, drtPersons);
        List<Double> walkDistanceList = new ArrayList<>();
        for(List<String> string:walkDistanceListList){
            walkDistanceList.add(Double.valueOf(string.get(0)));
        }

        Collections.sort(walkDistanceList);
        Double maxDistance = Collections.max(walkDistanceList);
        Integer percentile95 = (int) Math.round(walkDistanceList.size()*0.95);
        OptionalDouble average = walkDistanceList.stream().mapToDouble(a->a).average();
        System.out.println(walkDistanceList.get(percentile95));
        System.out.println(maxDistance);
        System.out.println(average);
        drtWalkingDistance.writeWalkDistanceTxt(walkDistanceList,outputFileName);
        drtWalkingDistance.writeWalkDistanceCSV(walkDistanceListList, outputMap);
        drtWalkingDistance.writeWalkDistanceTxt(drtPersons, outputPeople);

    }

    List<List<String>> readDataToMap(String file, List<Double> drtPersons) {
        List<List<String>> records = new ArrayList<>();

        List<List<String>> walkDistance = new ArrayList<>();
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

        for(int i=1 ; i<records.size();i++){
            if(records.get(i).get(6).equals("drt")){
                if(!drtPersons.contains(Double.valueOf(records.get(i).get(0)))) {
                    drtPersons.add(Double.valueOf(records.get(i).get(0)));
                }
                if(records.get(i-1).get(6).equals("walk") && records.get(i+1).get(6).equals("walk")) {
                    walkDistance.add(Arrays.asList(records.get(i - 1).get(5),records.get(i - 1).get(8),records.get(i - 1).get(9)));
                    walkDistance.add(Arrays.asList(records.get(i + 1).get(5),records.get(i + 1).get(8),records.get(i + 1).get(9)));
//                    walkDistance.add(Double.valueOf(records.get(i - 1).get(5)),new Coord(Double.valueOf(records.get(i - 1).get(8)),Double.valueOf(records.get(i - 1).get(9))));
//                    walkDistance.put(Double.valueOf(records.get(i + 1).get(5)),new Coord(Double.valueOf(records.get(i + 1).get(8)),Double.valueOf(records.get(i + 1).get(9))));
//                    8 = start_x und 9 = start_y
                } else {
                    throw new IllegalArgumentException("Egress or Access to Drt is not walk \n"+ records.get(i));
                }
            }

        }

        return walkDistance;
    }

    void writeWalkDistanceTxt (List<Double> list, String outputFileName){
        try {
            BufferedWriter writer =  new BufferedWriter(new FileWriter(outputFileName));
            for(Double d:list){
                writer.write(String.valueOf(d));
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void writeWalkDistanceCSV (List<List<String>> listList , String filePath){
        File file = new File(filePath);
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            // adding header to csv
            String[] header = { "Distance", "X-Coord", "Y-Coord" };
            writer.writeNext(header);

            // add data to csv
            for(List<String> entry:listList){
                String[] data = {entry.get(0),entry.get(1),entry.get(2)};
                writer.writeNext(data);
            }

            // closing writer connection
            writer.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
