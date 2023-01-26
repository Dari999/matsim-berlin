package org.matsim.analysis.drt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class populationCheck {


//    private Scenario scenarioDoor2Door ;
//    private Scenario scenarioLoopStops ;


    public static void main(String[] args) {

        String d2dPopulationFile = "/Users/dariush/output/output-plans/hundekopf-rebalancing-2000vehicles-4seats.output_plans.xml.gz";
        String loopStopsPopulationFile = "/Users/dariush/output/output-plans/loopStopsPShortIntersection-2000vehicles-4seats.output_plans.xml.gz";
        String outputFileName = "/Users/dariush/output/output-plans/personWithoutDrt.txt";
        Config config = ConfigUtils.createConfig();
        Scenario scenarioDoor2Door = ScenarioUtils.loadScenario(config);


        PopulationReader popReader = new PopulationReader(scenarioDoor2Door);
        popReader.readFile(d2dPopulationFile);

        List<Id<Person>> personsD2D = new ArrayList<>();
//        return personsD2D
//                = scenarioDoor2Door.getPopulation().getPersons().values().stream().forEach(person -> ((Leg) person.getSelectedPlan().getPlanElements()).getMode().equals(TransportMode.drt))
//                map(person -> person((Leg) person.getSelectedPlan().getPlanElements()).getMode().equals(TransportMode.drt))
//                .
//                .collect(Collectors.toList());
//                filter(planElement -> planElement instanceof Leg).
//                filter(leg -> leg.getMode().equals(TransportMode.drt)).collect(Collectors.toList());


//                map(person -> person.getSelectedPlan()).
//                map(plan -> plan.getPlanElements()).
//                flatMap(planElements -> planElements.stream()).
//                filter(planElement -> planElement instanceof Leg).
//                map(planElement -> (Leg) planElement).
//                filter(leg -> leg.getMode().equals(TransportMode.drt)).collect(Collectors.toList());

        for (Person person : scenarioDoor2Door.getPopulation().getPersons().values()) {
            if (((Leg) person.getSelectedPlan().getPlanElements()).getMode().equals(TransportMode.drt)) {
                personsD2D.add(person.getId());
            }
        }

        Scenario scenarioLoopStops = ScenarioUtils.loadScenario(config);
        PopulationReader popReader2 = new PopulationReader(scenarioLoopStops);
        popReader2.readFile(loopStopsPopulationFile);

        for (Person person : scenarioLoopStops.getPopulation().getPersons().values()) {
            if (((Leg) person.getSelectedPlan().getPlanElements()).getMode().equals(TransportMode.drt)) {
                if (personsD2D.contains(person.getId())) {
                    personsD2D.remove(person.getId());
                }
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
            for (Id<Person> d : personsD2D) {
                writer.write(String.valueOf(d));
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    void writePeopleTxt (List < Id < Person >> list, String outputFileName){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
            for (Id<Person> d : list) {
                writer.write(String.valueOf(d));
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


