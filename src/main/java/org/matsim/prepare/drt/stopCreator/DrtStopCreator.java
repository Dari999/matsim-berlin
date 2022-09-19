package org.matsim.prepare.drt.stopCreator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.run.drt.BerlinShpUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

import java.io.IOException;
import java.security.KeyStore;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dtamleh
 */

public class DrtStopCreator extends MatsimXmlWriter{
    private static Scenario scenario;
    private Network network;
    private final String drtNetworkMode = "drt";
//    private final BerlinShpUtils shpUtils;
    List<Link> loopLinks = new ArrayList<>();
    private final CoordinateTransformation ct;
    String drtServiceAreaShapeFile;
    String networkFile;



    public static void main(String[] args) {
        String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        String drtServiceAreaShapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
        Network network = NetworkUtils.readNetwork(networkFile);
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:31468");
//        Set<Link> carLinks = network.getNodes().values().stream().filter(node -> node.getInLinks().values().stream().filter(link -> link.getAllowedModes().contains(TransportMode.car)).collect().collect(Collectors.toSet());
        DrtStopCreator dsc = new DrtStopCreator(networkFile, drtServiceAreaShapeFile, ct);
        dsc.createLoopLinks(network);
        dsc.createStopsOnLoopLinks();
        dsc.createBerlinBusStopsOnly();

    }



    public DrtStopCreator(String networkFile, String drtServiceAreaShapeFile, CoordinateTransformation ct){
        this.ct = ct;
        this.networkFile=networkFile;
        this.drtServiceAreaShapeFile=drtServiceAreaShapeFile;
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile);
        this.scenario = ScenarioUtils.createScenario(config);



//        RunDrtOpenBerlinScenario.addDRTmode(scenario, drtNetworkMode, drtServiceAreaShapeFile, 0);

    }
    public DrtStopCreator(){
        this.ct=TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:31468");
        this.networkFile= "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        this.drtServiceAreaShapeFile="https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
    }

    void createLoopLinks (Network network){
        List<Node> primaryNodes = new ArrayList<>();
        Set<Link> primarySecondaryLinks = network.getLinks().values().stream()
                .filter(link -> link.getAttributes().toString().contains("primary") || link.getAttributes().toString().contains("secondary"))
                .collect(Collectors.toSet());
        for(Link link:primarySecondaryLinks){
           primaryNodes.add(link.getToNode());
        }
//            this.network = network;
//            List<Node> loopNodes = this.network.getNodes().values().stream().filter(node -> node.getInLinks()..values().toString().contains("primary")).collect(Collectors.toList());
        for(Node node : network.getNodes().values()){
            if(!node.getInLinks().keySet().toString().contains("pt") && primaryNodes.contains(node)) {
                Set<String> allowedModes = new HashSet<>(Arrays.asList("drt", "car"));
                Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId(node.getId() + "L"), node, node,
                        50, 8.333, 10000, 2);
                link.setAllowedModes(allowedModes);
                loopLinks.add(link);
            }
        }
        NetworkWriter networkWriter = new NetworkWriter(network);
        networkWriter.write("/Users/dariush/Desktop/BA-Ordner/MATSim/input/Network/drtNetwork-loopLinksP.xml");
    }

    private void createStopsOnLoopLinks() {   // (33000)
        Scenario loopStopScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = loopStopScenario.getTransitSchedule();
        TransitScheduleFactory scheduleFactory = schedule.getFactory();
        BerlinShpUtils berlinShpUtils = new BerlinShpUtils(drtServiceAreaShapeFile);


        for(int i=0;i<loopLinks.size();i++){
            TransitStopFacility drtStop = scheduleFactory.createTransitStopFacility(Id.create("drtStop" + i, TransitStopFacility.class),
                    new Coord(loopLinks.get(i).getCoord().getX(), loopLinks.get(i).getCoord().getY()), false);
            drtStop.setLinkId(loopLinks.get(i).getId());

                if(berlinShpUtils.isCoordInDrtServiceArea(drtStop.getCoord())){
                    schedule.addStopFacility(drtStop);
                }
        }

        TransitScheduleWriter scheduleWriter = new TransitScheduleWriter(loopStopScenario.getTransitSchedule());
        scheduleWriter.writeFileV2("/Users/dariush/Desktop/BA-Ordner/MATSim/input/DrtStopFile/loopStopFile.xml");
    }

    private void createBerlinBusStopsOnly(){ // 6500 in Berlin
        Scenario busStopScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule busStopSchedule = busStopScenario.getTransitSchedule();
        TransitScheduleFactory scheduleFactory = busStopSchedule.getFactory();
        Set<TransitStopFacility> busStops = new HashSet<>();
        Scenario scenarioForBusStops = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitScheduleReader tsReader = new TransitScheduleReader(scenarioForBusStops);
        tsReader.readFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz");
        for(TransitLine transitLine : scenarioForBusStops.getTransitSchedule().getTransitLines().values()){
                for(TransitRoute transitRoute : transitLine.getRoutes().values()){
                    if(transitRoute.getTransportMode().equals("bus")){
                    for(TransitRouteStop transitRouteStop : transitRoute.getStops()){
                        try {
//                            schedule.addStopFacility(transitRouteStop.getStopFacility());
                            busStops.add(transitRouteStop.getStopFacility());
                        } catch (Exception e){

                        }
//                        if(!schedule..contains(transitRouteStop)){
//                            busStops.add(transitRouteStop.getStopFacility());
//                            schedule.addStopFacility(transitRouteStop.getStopFacility());
//                        }
//                        transitRouteStop.getStopFacility().getAttributes().putAttribute("busStop", true);
                    }
                }
            }
        }

        BerlinShpUtils berlinShpUtils = new BerlinShpUtils(drtServiceAreaShapeFile);
        for(TransitStopFacility transitStopFacility: busStops){
            if(berlinShpUtils.isCoordInDrtServiceArea(transitStopFacility.getCoord())){
                busStopSchedule.addStopFacility(transitStopFacility);
            }
        }
        Network networkWithoutPt = NetworkUtils.readNetwork(networkFile);
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


        for(TransitStopFacility transitStopFacility: busStopSchedule.getFacilities().values()){
            transitStopFacility.setLinkId(NetworkUtils.getNearestLink(networkWithoutPt,transitStopFacility.getCoord()).getId());
        }



//        for(TransitStopFacility transitStop : scenarioForBusStops.getTransitSchedule().getFacilities().values()){
//            if(!transitStop.getCustomAttributes().isEmpty() && transitStop.getAttributes().getAttribute("busStop").equals(true)){
//                    busStops.add(transitStop);
//                    schedule.addStopFacility(transitStop);
//            }
//        }
        TransitScheduleWriter scheduleWriter = new TransitScheduleWriter(busStopScenario.getTransitSchedule());
        scheduleWriter.writeFileV2("/Users/dariush/Desktop/BA-Ordner/MATSim/input/DrtStopFile/berlinBusStopsFile.xml");
    }

    private void reduceStopsTo200() {


    }

    public static Set<String> createHashSet(String... modes) {
        Set<String> set = new HashSet<String>();
        Collections.addAll(set, modes);
        return set;
    }

//    private void writeDrtStopsXml() throws IOException, UncheckedIOException {
//        this.writeXmlHead();
//        this.writeDoctype("transitSchedule", "http://www.matsim.org/files/dtd/transitSchedule_v2.dtd");
//        this.writeStartTag("transitSchedule", (List)null);
//        this.writer.write("\n");
//        this.writeStartTag("transitStops", (List)null);
//        List<Tuple<String, String>> attributes = new ArrayList(5);
//        Iterator var2 = this.scenario.getTransitSchedule().getFacilities().values().iterator();
//        while(var2.hasNext()) {
//            TransitStopFacility stop = (TransitStopFacility)var2.next();
//            attributes.clear();
//            attributes.add(createTuple("id", stop.getId().toString()));
//            Coord coord = this.ct.transform(stop.getCoord());
//            attributes.add(createTuple("x", coord.getX()));
//            attributes.add(createTuple("y", coord.getY()));
//            if (coord.hasZ()) {
//                attributes.add(createTuple("z", coord.getZ()));
//            }
//
//            if (stop.getLinkId() != null) {
//                attributes.add(createTuple("linkRefId", stop.getLinkId().toString()));
//            }
//
//            if (stop.getName() != null) {
//                attributes.add(createTuple("name", stop.getName()));
//            }
//
//            if (stop.getStopAreaId() != null) {
//                attributes.add(createTuple("stopAreaId", stop.getStopAreaId().toString()));
//            }
//
//            attributes.add(createTuple("isBlocking", stop.getIsBlockingLane()));
////            if (AttributesUtils.isEmpty(stop.getAttributes())) {
////                this.writeStartTag("stopFacility", attributes, true);
////            } else {
////                this.writeStartTag("stopFacility", attributes, false);
////                if (!AttributesUtils.isEmpty(stop.getAttributes())) {
////                    this.writer.write("\n");
////                    this.attributesWriter.writeAttributes("\t\t\t", this.writer, stop.getAttributes());
////                }
////
////                this.writeEndTag("stopFacility");
////            }
//        }
//
//        this.writeEndTag("transitStops");
//        this.writeEndTag("transitSchedule");
//        this.close();
//    }


    }


