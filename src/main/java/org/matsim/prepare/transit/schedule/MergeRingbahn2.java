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
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class MergeRingbahn2 {
    private final static Logger log = Logger.getLogger(MergeRingbahn2.class);
    private Scenario scenario;
    private final String scheduleFile;
    private ConcurrentSkipListMap<Double, RingbahnData> departureAtStop = new ConcurrentSkipListMap<>();
    private ConcurrentSkipListMap<Double, RingbahnData> arrivalAtStop = new ConcurrentSkipListMap<>();
//    private Scenario scenario;
    private String newScheduleFile;
    List<Id<TransitRoute>> mergedTransitRoutes = new ArrayList<>();

    MergeRingbahn2(String scheduleFile) {
        this.scheduleFile = scheduleFile;
    }


    public static void main(String args[]) {

        final String scheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
        String fileName = "/Users/dariush/Desktop/TU Berlin/JAVA/scheduleFile/newScheduleFile.xml";

        final String transitLineId = "S41---10223_109";

        MergeRingbahn2 runner = new MergeRingbahn2(scheduleFile);
        runner.run();


    }

    void run() {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitScheduleReader scheduleReader = new TransitScheduleReader(scenario);
        scheduleReader.readFile(scheduleFile);
        boolean allArrivingTrainsChecked = false;
        while (!allArrivingTrainsChecked) {
            for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
                if (line.getId().toString().contains("S41---10223_109")) {        // S41---10224 is ignored
                    for (TransitRoute route : line.getRoutes().values()) {
                        for (TransitRouteStop stop : route.getStops()) {
                            for (Departure depart : route.getDepartures().values()) {

                                if (stop.getStopFacility().getId().toString().equals("060058100531")) {
                                    RingbahnData ringbahnData = new RingbahnData(line.getId(), route.getId(), depart, route);
                                    double departureTime = depart.getDepartureTime() + stop.getDepartureOffset().seconds();
                                    departureAtStop.put(departureTime, ringbahnData);
                                } else if (stop.getStopFacility().getId().toString().equals("060058100531.1")) {
                                    RingbahnData ringbahnData = new RingbahnData(line.getId(), route.getId(), depart, route);
                                    double arrivalTime = depart.getDepartureTime() + stop.getArrivalOffset().seconds();
                                    arrivalAtStop.put(arrivalTime, ringbahnData);
                                }

                            }
                        }

                    }
                }

            }

    double minStopTime = 5;
    double maxStopTime = 60;

    Set<Map.Entry<Double, RingbahnData>> setOfArrivalEntries = arrivalAtStop.entrySet();
    Iterator<Map.Entry<Double, RingbahnData>> arrivalIterator = setOfArrivalEntries.iterator();
//            Set<Map.Entry<Double, RingbahnData>> setOfDepartureEntries = departureAtStop.entrySet();
//            Iterator<Map.Entry<Double, RingbahnData>> departureIterator = setOfDepartureEntries.iterator();
    boolean found = false;

    while (arrivalIterator.hasNext() && !found) {
        Map.Entry<Double, RingbahnData> arrivalEntry = arrivalIterator.next();
//                Map.Entry<Double, RingbahnData> departureEntry = departureIterator.next();

//            }
//            for (Map.Entry<Double, RingbahnData> arrivalEntry : arrivalAtStop.entrySet()) {
        double arrivalTime = arrivalEntry.getKey();
        for (Map.Entry<Double, RingbahnData> departureEntry : departureAtStop.entrySet()) {
            double departureTime = departureEntry.getKey();
            // Checking time between departure and arrival
            if (departureTime > arrivalTime + minStopTime && departureTime < arrivalTime + maxStopTime) {
                // Create new TransitRoute

                //
                mergeTransitRoutes(arrivalEntry.getValue(), departureEntry.getValue(), scenario.getTransitSchedule());

//                        addDeparture2mergedTransitRoute(arrivalEntry.getValue().transitRoute, departureEntry.getValue().transitRoute,
//                                scenario.getTransitSchedule(), arrivalEntry.getValue().departure);
                scenario.getTransitSchedule().getTransitLines().get(arrivalEntry.getValue().transitLineId).getRoutes()
                        .get(arrivalEntry.getValue().transitRouteId).removeDeparture(arrivalEntry.getValue().departure);
                scenario.getTransitSchedule().getTransitLines().get(departureEntry.getValue().transitLineId).getRoutes()
                        .get(departureEntry.getValue().transitRouteId).removeDeparture(departureEntry.getValue().departure);
                //              log.info(arrivalEntry.getValue().departure.getId() + " and " + departureEntry.getValue().departure.getId() + " are merged into " + addArrivalEntryDeparture.getId());
                arrivalIterator.remove();
                departureAtStop.remove(departureTime);
//                        departureIterator.remove();
                found = true;
            }
//                    else if (no more fitting) -> finish
        }

    } if(arrivalIterator.hasNext()==false) allArrivingTrainsChecked=true;
}
        TransitScheduleWriter scheduleWriter = new TransitScheduleWriter(scenario.getTransitSchedule());
        scheduleWriter.writeFileV2("/Users/dariush/Desktop/TU Berlin/JAVA/scheduleFile/newScheduleFile.xml");
        }



    private void mergeTransitRoutes (RingbahnData arrivalRingbahnData, RingbahnData departureRingbahnData, TransitSchedule transitSchedule) {
        TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
        Id<TransitRoute> mergedTransitRouteId = (Id.create(arrivalRingbahnData.transitRouteId.toString() + "+" + departureRingbahnData.transitRouteId.toString().charAt(arrivalRingbahnData.transitRouteId.toString().length()-1), TransitRoute.class));
        //       if(mergedRoute==null) {
        List<Id<Link>> mergedLinks = new ArrayList<>();
        mergedLinks.addAll(arrivalRingbahnData.transitRoute.getRoute().getLinkIds());
        mergedLinks.addAll(departureRingbahnData.transitRoute.getRoute().getLinkIds());
        NetworkRoute networkRoute = RouteUtils.createNetworkRoute(mergedLinks);
        List<TransitRouteStop> transitRouteStops1 = new ArrayList<>();
        transitRouteStops1.addAll(arrivalRingbahnData.transitRoute.getStops());
        List<TransitRouteStop> transitRouteStops2 = new ArrayList<>();
//        int index = 0;
        for(TransitRouteStop stop : departureRingbahnData.transitRoute.getStops()){
//            if(index<transitRouteStops1.size() && transitRouteStops1.get(index) != null) {
                transitRouteStops2.add(transitScheduleFactory.createTransitRouteStop(stop.getStopFacility(),
                        stop.getArrivalOffset().seconds() + transitRouteStops1.get(transitRouteStops1.size()-1).getArrivalOffset().seconds(),
                        stop.getDepartureOffset().seconds() + transitRouteStops1.get(transitRouteStops1.size()-1).getDepartureOffset().seconds()));
//                index++;
//            }
        }
//        Iterator<TransitStopFacility> stopFacilityIterator = departureRingbahnData.transitRoute.getStops();
//        Iterator<Double> arrivalOffsetIterator = departureRingbahnData.arrivalOffset.iterator();
//        Iterator<Double> departureOffsetIterator = departureRingbahnData.departureOffset.iterator();
//        while(stopFacilityIterator.hasNext()&&arrivalOffsetIterator.hasNext()&&departureOffsetIterator.hasNext()){
//            transitRouteStops2.add(transitScheduleFactory.createTransitRouteStop(stopFacilityIterator.next(),arrivalOffsetIterator.next()+,departureOffsetIterator.next()+));
//        }

        transitRouteStops1.addAll(transitRouteStops2);
        TransitRoute mergedRoute = transitScheduleFactory.createTransitRoute(mergedTransitRouteId
                , networkRoute, transitRouteStops1, "");
        RingbahnData mergedRingbahnData = new RingbahnData(arrivalRingbahnData.transitLineId, mergedTransitRouteId, arrivalRingbahnData.departure, mergedRoute);

        if (!mergedTransitRoutes.contains(mergedRoute.getId())){
            mergedTransitRoutes.add(mergedRoute.getId());
            mergedRoute.addDeparture(arrivalRingbahnData.departure);
            transitSchedule.getTransitLines().get(arrivalRingbahnData.transitLineId).addRoute(mergedRoute);
            if(transitRouteStops1.get(transitRouteStops1.size()-1).getStopFacility().getId().toString().equals("060058100531.1")) {
                arrivalAtStop.put(mergedRingbahnData.departure.getDepartureTime() + transitRouteStops1.get(transitRouteStops1.size() - 1).getArrivalOffset().seconds(), mergedRingbahnData);
            }// TODO: richtige departureTime???
            }
        else if (!transitSchedule.getTransitLines().get(arrivalRingbahnData.transitLineId).getRoutes().get(mergedTransitRouteId).getDepartures().containsValue(arrivalRingbahnData.departure)){
            transitSchedule.getTransitLines().get(arrivalRingbahnData.transitLineId).getRoutes().get(mergedTransitRouteId).addDeparture(arrivalRingbahnData.departure);
        }
//            if (transitRouteStops1.get(0).getStopFacility().getId().toString().equals("060058100531")){
//                departureAtStop.put(mergedRingbahnData.departure.getDepartureTime()+transitRouteStops1.get(0).getDepartureOffset().seconds(), mergedRingbahnData);
//            }
//        } else {
//transitRoutes = {LinkedHashMap@5596}  size = 19
//        }

//        for(TransitRoute transitRoute: mergedTransitRoutes){
//
//            if(!mergedTransitRoutes.contains(transitRoute.getRoute())){
//                mergedRoute = transitScheduleFactory.createTransitRoute(mergedTransitRouteId
//                        , networkRoute, transitRouteStops, arrivalRingbahnData.getDescription() + "_" + departureRingbahnData.getDescription());
//                mergedTransitRoutes.add(mergedRoute);
//            }
//            else {
//                        ;
//            }
//        }
//        if (!mergedTransitRoutes.contains(mergedTransitRouteId)) {
//        mergedRoute = transitScheduleFactory.createTransitRoute(mergedTransitRouteId
//                , networkRoute, transitRouteStops, arrivalRingbahnData.getDescription() + "_" + departureRingbahnData.getDescription());
//        mergedTransitRoutes.add(mergedRoute);
//        }
        TransitLine transitLine= transitSchedule.getTransitLines().get(arrivalRingbahnData.transitLineId);
    }


//    private void addDeparture2mergedTransitRoute(TransitRoute transitRoute1, TransitRoute transitRoute2,
//                                                 TransitSchedule transitSchedule, Departure departure) {
//        TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
//        Id<TransitRoute> mergedTransitRouteId = (Id.create(transitRoute1.getId().toString() + "_" + transitRoute2.getId().toString(), TransitRoute.class));
//        for(TransitRoute transitRoute: mergedTransitRoutes){
//            if(transitRoute.getId().equals(mergedTransitRouteId)){
//                transitRoute.addDeparture(departure);
//            }
//        }
////        mergedTransitRoutes.stream().filter(transitRoute -> mergedTransitRouteId.equals(transitRoute.getRoute())).findAny().orElse(null).addDeparture(departure);
//    }

    private static class RingbahnData {
        private final Id<TransitLine> transitLineId;
        private final Id<TransitRoute> transitRouteId;
        private final Departure departure;
        private final TransitRoute transitRoute;



        private RingbahnData(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId,
                             Departure departure, TransitRoute transitRoute) {
            this.transitLineId = transitLineId;
            this.transitRouteId = transitRouteId;
            this.departure = departure;
            this.transitRoute = transitRoute;

        }
    }
}