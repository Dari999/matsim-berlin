package org.matsim.prepare.drt.stopCreator;

import org.apache.commons.math.stat.clustering.Clusterable;
import org.apache.commons.math.util.MathUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

public class ClusterableDouble implements Clusterable<ClusterableDouble> {

        private final double[] point;

        public ClusterableDouble(double[] point) {
            this.point = point;
        }

        public double[] getPoint() {
            return this.point;
        }

        public double distanceFrom(ClusterableDouble p) {
            return MathUtils.distance(this.point, p.getPoint());
        }

        public ClusterableDouble centroidOf(Collection<ClusterableDouble> points) {
            double[] centroid = new double[this.getPoint().length];
            Iterator i$ = points.iterator();

            while(i$.hasNext()) {
                ClusterableDouble p = (ClusterableDouble)i$.next();

                for(int i = 0; i < centroid.length; ++i) {
                    centroid[i] += p.getPoint()[i];
                }
            }

            for(int i = 0; i < centroid.length; ++i) {
                centroid[i] /= points.size();
            }

            return new ClusterableDouble(centroid);
        }

        public boolean equals(Object other) {
            if (!(other instanceof ClusterableDouble)) {
                return false;
            } else {
                double[] otherPoint = ((ClusterableDouble)other).getPoint();
                if (this.point.length != otherPoint.length) {
                    return false;
                } else {
                    for(int i = 0; i < this.point.length; ++i) {
                        if (this.point[i] != otherPoint[i]) {
                            return false;
                        }
                    }

                    return true;
                }
            }
        }

        public int hashCode() {
            int hashCode = 0;
            double[] arr$ = this.point;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                Double i = arr$[i$];
                hashCode += i.hashCode() * 13 + 7;
            }

            return hashCode;
        }

        public String toString() {
            StringBuilder buff = new StringBuilder("(");
            double[] coordinates = this.getPoint();

            for(int i = 0; i < coordinates.length; ++i) {
                buff.append(coordinates[i]);
                if (i < coordinates.length - 1) {
                    buff.append(",");
                }
            }

            buff.append(")");
            return buff.toString();
        }



}
