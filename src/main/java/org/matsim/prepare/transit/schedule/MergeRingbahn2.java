package org.matsim.prepare.transit.schedule;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.TransitRouteStopImpl;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

public class MergeRingbahn2 {
    private final static Logger log = Logger.getLogger(MergeRingbahn2.class);
    //  private final String eventsFile;
    private final String scheduleFile;
    private SortedMap<Double, RingbahnData> departureAtStop = new TreeMap<>();
    private SortedMap<Double, RingbahnData> arrivalAtStop = new TreeMap<>();
    private Scenario scenario;
    private String newScheduleFile;
    List<TransitRoute> mergedTransitRoutes = new ArrayList<>();

    MergeRingbahn2(String scheduleFile) {
        //      this.eventsFile = eventsFile;
        this.scheduleFile = scheduleFile;
    }


    public static void main(String args[]) {

        final String scheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
        String newScheduleFile = "/Users/dariush/Desktop/TU Berlin/JAVA/scheduleFile";

        MergeRingbahn2 runner = new MergeRingbahn2(scheduleFile);
        runner.run();

    }

    void run() {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitScheduleReader scheduleReader = new TransitScheduleReader(scenario);
        scheduleReader.readFile(scheduleFile);


        for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
            if (line.getId().toString().contains("S41---10223_109")) {        // S41---10224 is ignored
                for (TransitRoute route : line.getRoutes().values()) {
                    List<OptionalTime> departureOffsets = new ArrayList<>();
                    List<OptionalTime> arrivalOffsets = new ArrayList<>();
                    for (TransitRouteStop stop : route.getStops()) {
                        departureOffsets.add(stop.getDepartureOffset());
                        arrivalOffsets.add(stop.getArrivalOffset());
                    for (Departure depart : route.getDepartures().values()) {


                            RingbahnData ringbahnData = new RingbahnData(line.getId(), route.getId(), depart, route, arrivalOffsets, departureOffsets);

                            if (stop.getStopFacility().getId().toString().equals("060058100531")) {
//                                for(OptionalTime offset : stop.getDepartureOffset().seconds()){
//
//                                }
                                double departureTime = depart.getDepartureTime() + stop.getDepartureOffset().seconds();
                                departureAtStop.put(departureTime, ringbahnData);
                            } else if (stop.getStopFacility().getId().toString().equals("060058100531.1")) {

                                double arrivalTime = depart.getDepartureTime() + stop.getArrivalOffset().seconds();
                                arrivalAtStop.put(arrivalTime, ringbahnData);
                            }

                        }
                    }

                }
            }

        }



            double minStopTime = 5;
            double maxStopTime = 90;
            for (Map.Entry<Double, RingbahnData> arrivalEntry : arrivalAtStop.entrySet()) {
                double arrivalTime = arrivalEntry.getKey();
                for (Map.Entry<Double, RingbahnData> departureEntry : departureAtStop.entrySet()) {
                    double departureTime = departureEntry.getKey();
                    TransitScheduleFactory transitScheduleFactory = scenario.getTransitSchedule().getFactory();
//                    for (Map.Entry<Id<Departure>, Departure> departure : arrivalEntry.getValue().transitRoute.getDepartures().entrySet()) {
//                        Departure arrivalEntryDeparture = transitScheduleFactory.createDeparture(departure.getKey(), departure.getValue().getDepartureTime());

                    // Checking time between departure and arrival
                    if (departureTime > arrivalTime + minStopTime && departureTime < arrivalTime + maxStopTime) {
                        // Create new TransitRoute
                        mergeTransitRoutes(arrivalEntry.getValue().transitRoute,
                                departureEntry.getValue().transitRoute, scenario.getTransitSchedule(),arrivalEntry.getValue());

                        addDeparture2mergedTransitRoute(arrivalEntry.getValue().transitRoute, departureEntry.getValue().transitRoute, scenario.getTransitSchedule(), arrivalEntry.getValue().departure);




//                    Departure addDepartureEntryDeparture = transitScheduleFactory.createDeparture(departureEntry.getValue().departure.getId(), departureEntry.getValue().departure.getDepartureTime());

                        //              log.info(arrivalEntry.getValue().departure.getId() + " and " + departureEntry.getValue().departure.getId() + " are merged into " + addArrivalEntryDeparture.getId());

//                    addMergedDeparture2mergedTransitRoute(arrivalEntry.getValue().transitRoute,
//                            departureEntry.getValue().transitRoute, scenario.getTransitSchedule(), addDepartureEntryDeparture, arrivalEntry.getValue());

                        if (arrivalEntry.getValue().transitLineId.equals("S41---10223_109_0")) {
                            scenario.getTransitSchedule().getTransitLines().get(arrivalEntry.getValue().transitLineId).getRoutes().get(arrivalEntry.getValue().transitRouteId).removeDeparture(arrivalEntry.getValue().departure);
                        }
                        if (departureEntry.getValue().transitLineId.equals("S41---10223_109_0")) {
                            scenario.getTransitSchedule().getTransitLines().get(departureEntry.getValue().transitLineId).getRoutes().get(departureEntry.getValue().transitRouteId).removeDeparture(departureEntry.getValue().departure);
                        }

//                    }
                    }
                }
            }
        }



    private void mergeTransitRoutes (TransitRoute transitRoute1, TransitRoute transitRoute2, TransitSchedule transitSchedule,RingbahnData ringbahnData) {
        TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
        Id<TransitRoute> mergedTransitRouteId = (Id.create(transitRoute1.getId().toString() + "_" + transitRoute2.getId().toString(), TransitRoute.class));
        TransitRoute mergedRoute = transitSchedule.getTransitLines().get(ringbahnData.transitLineId).getRoutes().get(mergedTransitRouteId);
        //       if(mergedRoute==null) {
        List<Id<Link>> mergedLinks = new ArrayList<>();
        mergedLinks.addAll(transitRoute1.getRoute().getLinkIds());
        mergedLinks.addAll(transitRoute2.getRoute().getLinkIds());
        NetworkRoute networkRoute = RouteUtils.createNetworkRoute(mergedLinks);
        List<TransitRouteStop> transitRouteStops = new ArrayList<>();
        transitRouteStops.addAll(transitRoute1.getStops());
        transitRouteStops.addAll(transitRoute2.getStops());
        mergedRoute = transitScheduleFactory.createTransitRoute(mergedTransitRouteId
                , networkRoute, transitRouteStops, transitRoute1.getDescription() + "_" + transitRoute2.getDescription());
        if(mergedTransitRoutes.isEmpty()){
            mergedTransitRoutes.add(mergedRoute);
        } else if (!mergedTransitRoutes.contains(mergedRoute)){
            mergedTransitRoutes.add(mergedRoute);
        } else{
            ;
        }

//        for(TransitRoute transitRoute: mergedTransitRoutes){
//
//            if(!mergedTransitRoutes.contains(transitRoute.getRoute())){
//                mergedRoute = transitScheduleFactory.createTransitRoute(mergedTransitRouteId
//                        , networkRoute, transitRouteStops, transitRoute1.getDescription() + "_" + transitRoute2.getDescription());
//                mergedTransitRoutes.add(mergedRoute);
//            }
//            else {
//                        ;
//            }
//        }
//        if (!mergedTransitRoutes.contains(mergedTransitRouteId)) {
//        mergedRoute = transitScheduleFactory.createTransitRoute(mergedTransitRouteId
//                , networkRoute, transitRouteStops, transitRoute1.getDescription() + "_" + transitRoute2.getDescription());
//        mergedTransitRoutes.add(mergedRoute);
//        }
    }


    private void addDeparture2mergedTransitRoute(TransitRoute transitRoute1, TransitRoute transitRoute2,
                                                 TransitSchedule transitSchedule, Departure departure) {
        TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
        // TODO:  move arrivalOffset and departureOffset of all Stops of transitRoute2 by arrivalOffset from last stop (transitRoute1) + stop time at last stop
        Id<TransitRoute> mergedTransitRouteId = (Id.create(transitRoute1.getId().toString() + "_" + transitRoute2.getId().toString(), TransitRoute.class));
//        System.out.println(mergedTransitRoutes.get(0));
        for(TransitRoute transitRoute: mergedTransitRoutes){
            if(transitRoute.getId().equals(mergedTransitRouteId)){
                transitRoute.addDeparture(departure);
            }
        }
//        mergedTransitRoutes.stream().filter(transitRoute -> mergedTransitRouteId.equals(transitRoute.getRoute())).findAny().orElse(null).addDeparture(departure);
    }

    private static class RingbahnData {
        private final Id<TransitLine> transitLineId;
        private final Id<TransitRoute> transitRouteId;
        private final Departure departure;
        private final TransitRoute transitRoute;
        private List<OptionalTime> arrivalOffset;
        private List<OptionalTime> departureOffset;


        private RingbahnData(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId,
                             Departure departure, TransitRoute transitRoute, List<OptionalTime> arrivalOffset, List<OptionalTime> departureOffset) {
            this.transitLineId = transitLineId;
            this.transitRouteId = transitRouteId;
            this.departure = departure;
            this.transitRoute = transitRoute;
            this.arrivalOffset = arrivalOffset;
            this.departureOffset = departureOffset;
        }
    }
}