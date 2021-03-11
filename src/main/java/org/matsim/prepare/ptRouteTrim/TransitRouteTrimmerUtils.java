package org.matsim.prepare.ptRouteTrim;

import org.apache.log4j.Logger;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class TransitRouteTrimmerUtils {
    private static final Logger log = Logger.getLogger(TransitRouteTrimmer.class);

    public static Vehicles removeUnusedVehicles(Vehicles vehicles, TransitSchedule tS) {

        Scenario scenarioNew = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Vehicles vehiclesNew = scenarioNew.getVehicles();
        Set<Id<Vehicle>> vehIds = tS.getTransitLines().values().stream().
                flatMap(line -> line.getRoutes().values().stream().
                        flatMap(route -> route.getDepartures().values().stream()
                                .map(Departure::getVehicleId)))
                                .collect(Collectors.toSet());


        // Add all Vehicle Types
        for ( VehicleType vehicleType : vehicles.getVehicleTypes().values()) {
            vehiclesNew.addVehicleType(vehicleType);
        }

        // Add only vehicles that are in use
        int vehicleDeleteCount = 0;
        for (Map.Entry<Id<Vehicle>, Vehicle> veh : vehicles.getVehicles().entrySet()) {
            if (vehIds.contains(veh.getKey())) {
                vehiclesNew.addVehicle(veh.getValue());
            } else {
                vehicleDeleteCount++;
            }
        }

        log.info(vehicleDeleteCount + " vehicles were removed");
        return vehicles;

    }

    public static void removeLinksAndRoutes(Plan plan) {
        for (PlanElement pe : plan.getPlanElements()) {
            if (pe instanceof Activity) {
                ((Activity) pe).setLinkId(null); // Remove link
            }
            if (pe instanceof Leg) {
                ((Leg) pe).setRoute(null); // Remove route
            }
        }
    }

    public static void removeLinksAndRoutes(Population pop) {
        pop.getPersons().values().
                forEach(person -> {
                    person.getPlans().
                            forEach(TransitRouteTrimmerUtils::removeLinksAndRoutes);
                });
    }

    // This tool creates a LineString for each route in a TransitSchedule, based on the coordinates of the StopFacilities.
    // The collection of LineStrings is then exported to a ESRI shape file.
    public static void transitSchedule2ShapeFile(TransitSchedule tS, String outputFilename, String epsgCode ) throws SchemaException, IOException {

        File newFile = new File(outputFilename);

        final SimpleFeatureType TYPE =
                DataUtilities.createType(
                        "Link",
                        "the_geom:LineString:srid=" + epsgCode + ","
                                + // <- the geometry attribute: Point type
                                "name:String,"
//                                + // <- a String attribute
//                                "number:Integer" // a number attribute
                );
        System.out.println("TYPE:" + TYPE);

        List<SimpleFeature> features = new ArrayList<>();

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

        for(TransitLine line : tS.getTransitLines().values()){
            for (TransitRoute route : line.getRoutes().values()) {

                List<TransitRouteStop> stops = route.getStops();
                Coordinate[] coordinates = new Coordinate[stops.size()] ;
                for (int i = 0; i < stops.size(); i++) {
                    TransitRouteStop stop = stops.get(i);
                    Coord coord = stop.getStopFacility().getCoord();
                    Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
                    coordinates[i]=coordinate;
                }
                if (coordinates.length == 1) {
                    continue;
                }
                LineString routeString = geometryFactory.createLineString(coordinates);
                String routeName = route.getId().toString();
                featureBuilder.add(routeString);
                featureBuilder.add(routeName);
                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
            }
        }


        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore =
                (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);


        newDataStore.createSchema(TYPE);

        /*
         * Write the features to the shapefile
         */
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();

        System.out.println("SHAPE:" + SHAPE_TYPE);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(collection);
                transaction.commit();
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
        }
    }

    public static Set<Id<TransitLine>> filterTransitLinesForMode(Collection<TransitLine> allLines, Set<String> modes2Trim) {
        Set<Id<TransitLine>> lines2Modify = new HashSet<>();

        for (TransitLine line : allLines) {
            if (allRoutesInList(line, modes2Trim)) {
                lines2Modify.add(line.getId());
            }
        }

        return lines2Modify;
    }

    private static boolean allRoutesInList(TransitLine line, Set<String> modes2Trim) {
        for (TransitRoute route : line.getRoutes().values()) {
            if (!modes2Trim.contains(route.getTransportMode())) {
                return false;
            }
        }
        return true;
    }

    static double pctOfStopsInZone(TransitRoute route, Set<Id<TransitStopFacility>> stopsInZone) {
        double inAreaCount = 0.;
        for (TransitRouteStop stop : route.getStops()) {
            if (stopsInZone.contains(stop.getStopFacility().getId())) {
                inAreaCount++;
            }
        }
        return inAreaCount / route.getStops().size();
    }

    static void countLinesInOut(TransitSchedule tS, Set<Id<TransitStopFacility>> stopsInZone) {
        int inCount = 0;
        int outCount = 0;
        int wrongCount = 0;
        int halfCount = 0;
        int totalCount = 0;

        for (TransitLine line : tS.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
                totalCount++;
                ArrayList<Boolean> inOutList = new ArrayList<>();
                for (TransitRouteStop stop : route.getStops()) {
                    Id<TransitStopFacility> id = stop.getStopFacility().getId();
                    inOutList.add(stopsInZone.contains(id));
                }
                if (inOutList.contains(true) && inOutList.contains(false)) {
                    halfCount++;
                } else if (inOutList.contains(true)) {
                    inCount++;
                } else if (inOutList.contains(false)) {
                    outCount++;
                } else {
                    wrongCount++;
                }
            }
        }

        System.out.printf("in: %d, out: %d, half: %d, wrong: %d, total: %d %n", inCount, outCount, halfCount, wrongCount, totalCount);

    }

    static Set<Id<TransitStopFacility>> getStopsInZone(TransitSchedule transitSchedule, String zoneShpFile) {
        List<PreparedGeometry> geometries = null;
        try {
            geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(zoneShpFile));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Wrong Filename!");
        }
        Set<Id<TransitStopFacility>> stopsInZone = new HashSet<>();
        for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
            if (ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries)) {
                stopsInZone.add(stop.getId());
            }
        }

        return stopsInZone;
    }

    static Set<Id<TransitStopFacility>> getStopsInZone(TransitSchedule transitSchedule, URL zoneShpFileUrl) {
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(zoneShpFileUrl);
        Set<Id<TransitStopFacility>> stopsInZone = new HashSet<>();
        for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
            if (ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries)) {
                stopsInZone.add(stop.getId());
            }
        }

        return stopsInZone;
    }

}
