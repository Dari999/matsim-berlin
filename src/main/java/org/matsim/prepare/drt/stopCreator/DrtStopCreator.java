package org.matsim.prepare.drt.stopCreator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.run.drt.BerlinShpUtils;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;

import java.util.*;
import java.util.stream.Collectors;

public class DrtStopCreator {
    private static Scenario scenario;
    private Network network;
    private final String drtNetworkMode = "drt";
    private final BerlinShpUtils shpUtils;
    List<Link> loopLinks = new ArrayList<>();


    public static void main(String[] args) {
        String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        String drtServiceAreaShapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
        Network network = NetworkUtils.readNetwork(networkFile);
        Set<Link> carLinks = network.getLinks().values().stream().filter(link -> link.getAllowedModes().contains(TransportMode.car)).collect(Collectors.toSet());
        DrtStopCreator dsc = new DrtStopCreator(drtServiceAreaShapeFile,networkFile);
        dsc.createLoopLinks(network);
        dsc.createStopsOnLoopLinks();

    }



    public DrtStopCreator (String drtServiceAreaShapeFile,String network){
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(network);
        this.scenario = ScenarioUtils.createScenario(config);



        shpUtils = new BerlinShpUtils(drtServiceAreaShapeFile);
//        RunDrtOpenBerlinScenario.addDRTmode(scenario, drtNetworkMode, drtServiceAreaShapeFile, 0);

    }

    void createLoopLinks (Network network){
            this.network = network;
        for(Node node : this.network.getNodes().values()){
            Set<String> allowedModes = new HashSet<>(Arrays.asList("drt","ride"));
            Link link = NetworkUtils.createAndAddLink(this.network,Id.createLinkId(node.getId()+"L"),node,node,
                    50,8.333,1000,2);
            link.setAllowedModes(allowedModes);
            loopLinks.add(link);
        }
    }

    private void createStopsOnLoopLinks() {
        TransitSchedule schedule = this.scenario.getTransitSchedule();
        TransitScheduleFactory scheduleFactory = schedule.getFactory();

        for(int i=0;i<loopLinks.size();i++){
            TransitStopFacility drtStop = scheduleFactory.createTransitStopFacility(Id.create("drtStop" + i, TransitStopFacility.class),
                    new Coord(loopLinks.get(i).getCoord().getX(), loopLinks.get(i).getCoord().getY()), false);
            schedule.addStopFacility(drtStop);
        }
        TransitScheduleWriter scheduleWriter = new TransitScheduleWriter(scenario.getTransitSchedule());
        scheduleWriter.writeFileV2("/Users/dariush/Desktop/BA-Ordner/MATSim/DrtStopFile/drtStopFile.xml");
    }

}
