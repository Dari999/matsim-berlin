package org.matsim.prepare.ptRouteTrim;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.*;

import java.util.*;

/**
 * This tool creates a trims TransitRoutes, so as not to enter a user-specified ESRI shape file.
 * There are several modifier methods that can be used separately or in combination.
 *
 * @author jakobrehmann
 */

//TODO Define Hub: if no hubReach=0, is it a hub?
//TODO: modes to use?
//TODO: Check Nullpointer exceptions in tests


public class TransitRouteTrimmer {
    private static final Logger log = Logger.getLogger(TransitRouteTrimmer.class);

    // Parameters
    boolean removeEmptyLines = true; // all
    boolean includeFirstStopWithinZone = true; // 3
    boolean allowHubsWithinZone = true; // split und trim, skip stops at intermediary
    int minimumRouteLength = 2; // 3
    int allowableStopsWithinZone = 3; // split, skip
    boolean includeFirstHubInZone = false; // split und trim
    Set<String> modes2Trim = new HashSet<>();  // all


    private Vehicles vehicles;
    private TransitSchedule transitScheduleOld;
    private TransitSchedule transitScheduleNew;
    private static Set<Id<TransitStopFacility>> stopsInZone;
    private TransitScheduleFactory tsf;

    //TODO: Vehicles new


    enum modMethod {
        SplitRoute,
        SkipStopsWithinZone,
        DeleteRoutesEntirelyInsideZone,
        TrimEnds
    }


    public TransitRouteTrimmer(TransitSchedule transitSchedule, Vehicles vehicles, List<PreparedGeometry> geometries) {
        this.transitScheduleOld = transitSchedule;
        this.transitScheduleNew = (new TransitScheduleFactoryImpl()).createTransitSchedule(); // TODO: Is this okay?
        this.vehicles = vehicles;
        this.tsf = transitScheduleNew.getFactory(); //TODO: ???
        stopsInZone = new HashSet<>();
        for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
            if (ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries)) {
                this.stopsInZone.add(stop.getId());
            }
        }
    }

    public TransitRouteTrimmer(TransitSchedule transitSchedule, Vehicles vehicles, Set<Id<TransitStopFacility>> stopsInZone) {
        this.transitScheduleOld = transitSchedule;
        this.transitScheduleNew = (new TransitScheduleFactoryImpl()).createTransitSchedule(); // TODO: Is this okay?
        this.tsf = transitScheduleNew.getFactory(); //TODO: ???
        this.vehicles = vehicles;
        this.stopsInZone = stopsInZone;

    }

    public Set<Id<TransitStopFacility>> getStopsInZone() {
        return stopsInZone;
    }


    /**
     *
     * @param linesToModify
     * @param modifyMethod
     */

    public void modifyTransitLinesFromTransitSchedule(Set<Id<TransitLine>> linesToModify, modMethod modifyMethod) {

        Iterator var3 = transitScheduleOld.getFacilities().values().iterator();

        while (var3.hasNext()) {
            TransitStopFacility stop = (TransitStopFacility) var3.next();
            transitScheduleNew.addStopFacility(stop);
        }

        var3 = transitScheduleOld.getTransitLines().values().iterator();

        while (var3.hasNext()) {
            TransitLine line = (TransitLine) var3.next();
            if (!linesToModify.contains(line.getId())) {
                transitScheduleNew.addTransitLine(line);
                continue;
            }

            TransitLine lineNew = transitScheduleOld.getFactory().createTransitLine(line.getId());

            for (TransitRoute route : line.getRoutes().values()) {
                TransitRoute routeNew = null;

                // Only handles specified routes.
                if (!this.modes2Trim.isEmpty()) { //TODO
                    if (!this.modes2Trim.contains(route.getTransportMode())) {
                        lineNew.addRoute(route);
                        continue;
                    }
                }

                //                 Only handle routes that interact with zone
                if (TransitRouteTrimmerUtils.pctOfStopsInZone(route, stopsInZone) == 0.0) {
                    lineNew.addRoute(route);
                    continue;
                }

                if (modifyMethod.equals(modMethod.DeleteRoutesEntirelyInsideZone)) {
                    if (TransitRouteTrimmerUtils.pctOfStopsInZone(route, stopsInZone) == 1.0) {
                        continue;
                    }
                    routeNew = route;
                } else if (modifyMethod.equals(modMethod.TrimEnds)) {
                    routeNew = modifyRouteTrimEnds(route);
                } else if (modifyMethod.equals((modMethod.SkipStopsWithinZone))) {
                    routeNew = modifyRouteSkipStopsWithinZone(route);
                } else if (modifyMethod.equals(modMethod.SplitRoute)) {
                    ArrayList<TransitRoute> routesNew = modifyRouteSplitRoute(route);
                    for (TransitRoute rt : routesNew) {
                        lineNew.addRoute(rt);
                    }
                }

                if (routeNew != null) {
                    lineNew.addRoute(routeNew);
                }

            }

            if (lineNew.getRoutes().size() == 0 && removeEmptyLines) {
                log.info(lineNew.getId() + " does not contain routes. It will NOT be added to the schedule");
                continue;
            }

            transitScheduleNew.addTransitLine(lineNew);

        }

        log.info("Old schedule contained " + transitScheduleOld.getTransitLines().values().size() + " lines.");
        log.info("New schedule contains " + transitScheduleNew.getTransitLines().values().size() + " lines.");

        TransitRouteTrimmerUtils.countLinesInOut(transitScheduleNew, stopsInZone);
    }


    public Vehicles getVehicles() {
        return vehicles;
    }

    public TransitSchedule getTransitScheduleNew() {
        return transitScheduleNew;
    }


    // This will skip stops within zone. If beginning or end of route is within zone, it will cut those ends off.
    private TransitRoute modifyRouteSkipStopsWithinZone(TransitRoute routeOld) {
        List<TransitRouteStop> stops2Keep = new ArrayList<>();
        List<TransitRouteStop> stopsOld = new ArrayList<>(routeOld.getStops());

        for (int i = 0; i < stopsOld.size(); i++) {
            TransitRouteStop stop = stopsOld.get(i);
            // If stop is outside of zone, keep it
            if (!stopsInZone.contains(stop.getStopFacility().getId())) {
                stops2Keep.add(stop);
                continue;
            }

            // If stop is inside zone, but the stop before or after it is outside, then keep it
            if (includeFirstStopWithinZone) {
                // Checks if previous stop is outside of zone; if yes, include current stop
                if (i > 0) {
                    Id<TransitStopFacility> prevStop = stopsOld.get(i - 1).getStopFacility().getId();
                    if (!stopsInZone.contains(prevStop)) {
                        stops2Keep.add(stop); // TODO ADD ATTRIBUTE
                        continue;
                    }
                }

                // Checks if next stop is outside of zone; if yes, include current stop
                if (i < stopsOld.size() - 1) {
                    Id<TransitStopFacility> nextStop = stopsOld.get(i + 1).getStopFacility().getId();
                    if (!stopsInZone.contains(nextStop)) {
                        stops2Keep.add(stop);
                    }
                }
            }
        }


        if (stops2Keep.size() >= minimumRouteLength && stops2Keep.size() > 0) {
            return createNewRoute(routeOld, stops2Keep, 1);
        }

        return null; // TODO: Check this

    }

    private TransitRoute modifyRouteTrimEnds(TransitRoute routeOld) {

        List<TransitRouteStop> stops2Keep = new ArrayList<>();
        List<TransitRouteStop> stopsOld = new ArrayList<>(routeOld.getStops());


        // cut stops from beginning of route, if they are within zone
        Id<TransitStopFacility> startStopId = null; //TODO: ???
        for (int i = 0; i < stopsOld.size(); i++) {

            Id<TransitStopFacility> id = stopsOld.get(i).getStopFacility().getId();
            if (!stopsInZone.contains(id)) {
                if (includeFirstStopWithinZone && i > 0) {
                    startStopId = stopsOld.get(i - 1).getStopFacility().getId();
                } else {
                    startStopId = id;
                }

                break;
            }

        }

        // cut stops from end of route, if they are within zone
        Id<TransitStopFacility> lastStopId = null;//TODO: ???
        for (int i = stopsOld.size() - 1; i >= 0; i--) {

            Id<TransitStopFacility> id = stopsOld.get(i).getStopFacility().getId();
            if (!stopsInZone.contains(id)) {
                if (includeFirstStopWithinZone && i < stopsOld.size() - 1) {
                    lastStopId = stopsOld.get(i + 1).getStopFacility().getId();
                } else {
                    lastStopId = id;
                }
                break;
            }
        }

        if (startStopId == null || lastStopId == null) {
            return null;
        }

        boolean start = false;
        for (TransitRouteStop stop : stopsOld) {
            if (!start) {
                if (stop.getStopFacility().getId().equals(startStopId)) {
                    stops2Keep.add(stop);
                    start = true;
                }
                continue;
            }

            if (stop.getStopFacility().getId().equals(lastStopId)) {
                stops2Keep.add(stop);
                break;
            }
            stops2Keep.add(stop);

        }

        if (stops2Keep.size() >= minimumRouteLength && stops2Keep.size() > 0) {
            return createNewRoute(routeOld, stops2Keep, 1);
        }

        return null; // TODO: Check this

    }

    private ArrayList<TransitRoute> modifyRouteSplitRoute(TransitRoute routeOld) {
        ArrayList<TransitRoute> resultRoutes = new ArrayList<>();
        List<TransitRouteStop> stopsOld = new ArrayList<>(routeOld.getStops());

        // Get list of hubs: each hub is represented [location, reach] where location is the index along the route and
        // reach is the number of stops away the hub can be from the edge of the zone to still be included.
        List<int[]> hubLocationAndReach = getHubList(stopsOld);

        // check if stop is within or outside of zone, and store in boolean array
        boolean[] stops2keep = new boolean[stopsOld.size()];
        for (int i = 0; i < stopsOld.size(); i++) {
            stops2keep[i] = (!stopsInZone.contains(stopsOld.get(i).getStopFacility().getId()));
        }


        // Exact start and end indices from boolean array.
        List<Integer[]> routeIndices = findStartEndIndicesForAllRoutes(stops2keep);

        // Extend routes with hubs and/or first stop within zone
        for (Integer[] pair : routeIndices) {
            int leftIndex = pair[0];
            int leftIndexNew = leftIndex;

            int rightIndex = pair[1];
            int rightIndexNew = rightIndex;


            // Add hubs
            if (allowHubsWithinZone && !hubLocationAndReach.isEmpty()) {
                List<Integer> hubPositionsLeft = new ArrayList<>();
                List<Integer> hubPositionsRight = new ArrayList<>();

                for (int[] hubPosValuePair : hubLocationAndReach) {
                    int hubPos = hubPosValuePair[0];
                    int hubReach = hubPosValuePair[1];

                    // add hub before beginning of route
                    if (hubPos < leftIndex) {
                        hubPositionsLeft.add(hubPos);
                        if (hubPos < leftIndexNew && hubPos + hubReach >= leftIndex) {
                            leftIndexNew = hubPos;
                        }
                    }
                    // add hub after end of route
                    if (hubPos > rightIndex) {
                        hubPositionsRight.add(hubPos);
                        if (hubPos > rightIndexNew && hubPos - hubReach <= rightIndex) {
                            rightIndexNew = hubPos;
                        }
                    }
                }


                // if there are hubs in a particular direction, but none are included in route because their reaches
                // aren't large enough, then we can add the closest hub afterward.
                if (includeFirstHubInZone) {
                    if (leftIndex == leftIndexNew && !hubPositionsLeft.isEmpty()) {
                        leftIndexNew = Collections.max(hubPositionsLeft);
                    }

                    if (rightIndex == rightIndexNew && !hubPositionsRight.isEmpty()) {
                        rightIndexNew = Collections.min(hubPositionsRight);
                    }
                }

            }

            // add first stop within zone if hub hasn't already extended the route in the respective direction
            if (includeFirstStopWithinZone) {
                if (leftIndex == leftIndexNew) {
                    if (leftIndex > 0) {
                        leftIndexNew--;
                    }
                }

                if (rightIndex == rightIndexNew) {
                    if (rightIndex < stopsOld.size() - 1) {
                        rightIndexNew++;
                    }
                }
            }

            pair[0] = leftIndexNew;
            pair[1] = rightIndexNew;
        }


        // combine routes if they overlap
        boolean[] stops2keep2 = new boolean[stopsOld.size()];
        for (Integer[] pair : routeIndices) {
            for (int i = pair[0]; i <= pair[1]; i++) {
                stops2keep2[i] = true;
            }
        }

        // fill gaps of two or less
        int zeroCnt = 0;
        List<Integer> tmpIndex = new ArrayList<>();
        for (int i = 0; i < stops2keep2.length; i++) {
            if (!stops2keep2[i]) {
                zeroCnt++;
                tmpIndex.add(i);
            } else {
                if (zeroCnt > 0 && zeroCnt <= allowableStopsWithinZone) {
                    for (int index : tmpIndex) {
                        stops2keep2[index] = true;
                    }
                }
                zeroCnt = 0;
                tmpIndex.clear();
            }
        }

        List<Integer[]> routeIndices2 = findStartEndIndicesForAllRoutes(stops2keep2);

        // create transit routes
        int newRouteCnt = 1;
        for (Integer[] pair : routeIndices2) {
            resultRoutes.add(createNewRoute(routeOld, pair[0], pair[1], newRouteCnt));
            newRouteCnt++;
        }

        return resultRoutes;

    }

    private List<int[]> getHubList(List<TransitRouteStop> stopsOld) {
        List<int[]> hubs = new ArrayList<>();

        for (int i = 0; i < stopsOld.size(); i++) {
            TransitRouteStop stop = stopsOld.get(i);
            if (stop.getStopFacility().getAttributes().getAsMap().containsKey("hub-reach")) {
                int hubValue = (int) stop.getStopFacility().getAttributes().getAttribute("hub-reach");
                if (hubValue > 0) {
                    int[] hubPosValuePair = {i, hubValue};
                    hubs.add(hubPosValuePair);
                }
            }
        }
        return hubs;
    }

    private List<Integer[]> findStartEndIndicesForAllRoutes(boolean[] stops2Keep) {
        List<Integer[]> routeIndicies = new ArrayList<>();
        Integer startIndex = null;
        Integer endIndex = null;

        for (int i = 0; i < stops2Keep.length; i++) {
            // we only look at stops that are outside of the zone (which should be kept)
            if (stops2Keep[i]) {
                // if the route has not begun previously, then begin it at the current index
                if (startIndex == null) {
                    startIndex = i;
                }
                // If this is the last stop, end route at this stop
                if (i == stops2Keep.length - 1) {
                    endIndex = i;
                }
                // If this is not last stop, and next stop is within zone, end route at this stop
                else {
                    if (!stops2Keep[i + 1]) {

                        endIndex = i;
                    }
                }
            }
            // the route is complete when we have a start and end index --> add route indicies to routeIndicies
            if (startIndex != null && endIndex != null) {
                routeIndicies.add(new Integer[]{startIndex, endIndex});
                startIndex = null;
                endIndex = null;
            }
        }

        return routeIndicies;
    }


    private boolean checkIntersection(Integer[] pair1, Integer[] pair2) {
        Integer pair1Left = pair1[0];
        Integer pair1Right = pair1[1];
        if (pair1Left >= pair2[0] && pair1Left <= pair2[1]) {
            return true;
        } else if (pair1Right >= pair2[0] && pair1Right <= pair2[1]) {
            return true;
        } else {
            return false;
        }
    }

    private Integer[] intersectPairs(Integer[] pair1, Integer[] pair2) {
        int newLeft = Math.min(pair1[0], pair2[0]);
        int newRight = Math.max(pair1[1], pair2[1]);
        return new Integer[]{newLeft, newRight};
    }


    private TransitRoute createNewRoute(TransitRoute routeOld, Integer startIndex, Integer endIndex, int modNumber) {
        List<TransitRouteStop> stopsInNewRoute = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            stopsInNewRoute.add(routeOld.getStops().get(i));
        }

        return createNewRoute(routeOld, stopsInNewRoute, modNumber);
    }

    private TransitRoute createNewRoute(TransitRoute routeOld, List<TransitRouteStop> stopsInNewRoute,
                                        int modNumber) {

        TransitRoute routeNew;

        List<Id<Link>> linksOld = new ArrayList<>();
        linksOld.add(routeOld.getRoute().getStartLinkId());
        linksOld.addAll(routeOld.getRoute().getLinkIds());
        linksOld.add(routeOld.getRoute().getEndLinkId());

        Id<Link> startLinkNew = stopsInNewRoute.get(0).getStopFacility().getLinkId();
        Id<Link> endLinkNew = stopsInNewRoute.get(stopsInNewRoute.size() - 1).getStopFacility().getLinkId();
        ArrayList<Id<Link>> midLinksNew = new ArrayList<>();

        Iterator<TransitRouteStop> stopIt = stopsInNewRoute.iterator();

        boolean start = false;
        TransitRouteStop stop = stopIt.next();
        for (Id<Link> linkId : linksOld) {
            if (!start) {
                if (linkId.equals(startLinkNew)) {
                    start = true;
                    stop = stopIt.next();
                }
                continue;
            }

            if (!stopIt.hasNext() && linkId.equals(endLinkNew)) {

                break;
            }

            midLinksNew.add(linkId);

            if (linkId.equals(stop.getStopFacility().getLinkId())) {

                stop = stopIt.next();

            }

        }

        // Modify arrivalOffset and departureOffset for each stop
        TransitRouteStop transitRouteStop = routeOld.getStops().get(0);


        double initialDepartureOffset = transitRouteStop.getDepartureOffset().seconds();
        double departureOffset = stopsInNewRoute.get(0).getDepartureOffset().seconds() - initialDepartureOffset;

        // double initialArrivalOffset = transitRouteStop.getArrivalOffset().seconds();
        double arrivalOffset = departureOffset;
        // double arrivalOffset = stopsInNewRoute.get(0).getArrivalOffset().seconds() - initialArrivalOffset;

        List<TransitRouteStop> stopsNew = new ArrayList<>(copyStops(stopsInNewRoute, departureOffset, arrivalOffset));


        // make route.
        NetworkRoute networkRouteNew = RouteUtils.createLinkNetworkRouteImpl(startLinkNew, midLinksNew, endLinkNew);
        String routeIdOld = routeOld.getId().toString();
        Id<TransitRoute> routeIdNew = Id.create(routeIdOld + "_mod" + modNumber, TransitRoute.class);
        routeNew = tsf.createTransitRoute(routeIdNew, networkRouteNew, stopsNew, routeOld.getTransportMode());
        routeNew.setDescription(routeOld.getDescription());

        VehiclesFactory vf = this.vehicles.getFactory();

        for (Departure departure : routeOld.getDepartures().values()) {
            Id<Vehicle> vehIdOld = departure.getVehicleId();
            Id<Vehicle> vehIdNew = Id.createVehicleId(vehIdOld.toString() + "_mod" + modNumber);
            VehicleType vehType = this.vehicles.getVehicles().get(vehIdOld).getType();
            // this.vehicles.removeVehicle(departure.getVehicleId()); //TODO VEH MUST BE REMOVED LATER ON IF UNUSED
            Vehicle vehicle = vf.createVehicle(vehIdNew, vehType);
            this.vehicles.addVehicle(vehicle);

            String depIdOld = departure.getId().toString();
            Departure departureNew = tsf.createDeparture(Id.create(depIdOld + "_mod" + modNumber, Departure.class), departure.getDepartureTime() + departureOffset);
            departureNew.setVehicleId(vehIdNew);
            routeNew.addDeparture(departureNew);
        }
        return routeNew;
    }


    // TODO: how to deal with arrival and departure offsets? I don't know the conventions...
    // TODO: first stop can have no arr offset, last stop no dep offset
    private Collection<? extends TransitRouteStop> copyStops(List<TransitRouteStop> s, Double
            departureOffset, Double arrivalOffset) {
        List<TransitRouteStop> stops = new ArrayList<>();
        TransitRouteStop newStop;
        for (TransitRouteStop oldStop : s) {

            //            newStop = tsf.createTransitRouteStop(oldStop.getStopFacility(),
            //                    oldStop.getDepartureOffset().seconds() - departureOffset,
            //                    oldStop.getDepartureOffset().seconds() - departureOffset);
            if (oldStop.getDepartureOffset().isDefined() && oldStop.getArrivalOffset().isDefined()) {
                newStop = tsf.createTransitRouteStop(oldStop.getStopFacility(),
                        oldStop.getDepartureOffset().seconds() - departureOffset,
                        oldStop.getArrivalOffset().seconds() - arrivalOffset);
            } else if (oldStop.getDepartureOffset().isDefined()) {
                newStop = tsf.createTransitRouteStop(oldStop.getStopFacility(),
                        oldStop.getDepartureOffset().seconds() - departureOffset,
                        oldStop.getDepartureOffset().seconds() - departureOffset);
            } else if (oldStop.getArrivalOffset().isDefined()) {
                newStop = tsf.createTransitRouteStop(oldStop.getStopFacility(),
                        oldStop.getArrivalOffset().seconds() - arrivalOffset,
                        oldStop.getArrivalOffset().seconds() - arrivalOffset);
            } else {
                newStop = tsf.createTransitRouteStop(oldStop.getStopFacility(), 0, 0);
            }

            newStop.setAwaitDepartureTime(oldStop.isAwaitDepartureTime());
            stops.add(newStop);

        }
        return stops;
    }


}
