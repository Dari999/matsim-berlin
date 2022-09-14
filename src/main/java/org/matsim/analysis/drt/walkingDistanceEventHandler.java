package org.matsim.analysis.drt;

import com.google.common.math.DoubleMath;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author dtamleh
 */

public class walkingDistanceEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

    Map<Id<Person>, Double> walkDistance = new HashMap();
    Map<Id<Person>, Double> departureTimes = new HashMap();
    Map<Id<Link>, List<Double>> distances = new HashMap();
    Map<Id<Link>, List<Double>> times = new HashMap();

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals("non_network_walk")) {
            this.departureTimes.put(event.getPersonId(), event.getTime());
        }
    }

    public void handleEvent(PersonArrivalEvent event) {
        if (this.departureTimes.containsKey(event.getPersonId()) && this.walkDistance.containsKey(event.getPersonId())) {
            double walkD = (Double)this.walkDistance.remove(event.getPersonId());
            double walkT = event.getTime() - (Double)this.departureTimes.remove(event.getPersonId());
            Id<Link> linkId = event.getLinkId();
            if (!this.distances.containsKey(linkId)) {
                this.distances.put(linkId, new ArrayList());
                this.times.put(linkId, new ArrayList());
            }

            ((List)this.distances.get(linkId)).add(walkD);
            ((List)this.times.get(linkId)).add(walkT);
        }

    }

    public void writeEgressWalkStatistics(String folder) {
        String distanceFile = folder + "/egressWalkDistances.csv";
        String timesFile = folder + "/egressWalkTimes.csv";
        this.writeStats(this.distances, distanceFile);
    }

    private void writeStats(Map<Id<Link>, List<Double>> map, String distanceFile) {
        BufferedWriter bw = IOUtils.getBufferedWriter(distanceFile);

        try {
            bw.write("LinkId;Average;Min;Max;Arrivals");
            Iterator var4 = map.entrySet().iterator();

            while(var4.hasNext()) {
                Map.Entry<Id<Link>, List<Double>> e = (Map.Entry)var4.next();
                bw.newLine();
                Object var10001 = e.getKey();
                bw.write(var10001 + ";" + DoubleMath.mean((Iterable)e.getValue()) + ";" + Collections.min((Collection)e.getValue()) + ";" + Collections.max((Collection)e.getValue()) + ";" + ((List)e.getValue()).size());
            }

            bw.flush();
            bw.close();
        } catch (IOException var6) {
            var6.printStackTrace();
        }

    }
    }
