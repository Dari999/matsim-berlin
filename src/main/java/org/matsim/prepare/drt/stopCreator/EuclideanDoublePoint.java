package org.matsim.prepare.drt.stopCreator;

import org.apache.commons.math.stat.clustering.Clusterable;
import org.apache.commons.math.util.MathUtils;

import java.util.Collection;
import java.util.Iterator;

public class EuclideanDoublePoint implements Clusterable<EuclideanDoublePoint> {

        private final double[] point;

        public EuclideanDoublePoint(double[] point) {
            this.point = point;
        }

        public double[] getPoint() {
            return this.point;
        }

        public double distanceFrom(EuclideanDoublePoint p) {
            return MathUtils.distance(this.point, p.getPoint());
        }

        public EuclideanDoublePoint centroidOf(Collection<EuclideanDoublePoint> points) {
            double[] centroid = new double[this.getPoint().length];
            Iterator i$ = points.iterator();

            while(i$.hasNext()) {
                EuclideanDoublePoint p = (EuclideanDoublePoint)i$.next();

                for(int i = 0; i < centroid.length; ++i) {
                    centroid[i] += p.getPoint()[i];
                }
            }

            for(int i = 0; i < centroid.length; ++i) {
                centroid[i] /= points.size();
            }

            return new EuclideanDoublePoint(centroid);
        }

        public boolean equals(Object other) {
            if (!(other instanceof EuclideanDoublePoint)) {
                return false;
            } else {
                double[] otherPoint = ((EuclideanDoublePoint)other).getPoint();
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
