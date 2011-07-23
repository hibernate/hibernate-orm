/*
 * $Id: GeometryEquality.java 271 2010-12-16 22:00:24Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2010 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */

package org.hibernate.spatial.test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import org.hibernate.spatial.mgeom.MCoordinate;

/**
 * This class tests for the equality between geometries.
 * <p/>
 * The notion of geometric equality can differ slightly between
 * spatial databases.
 */
public class GeometryEquality {

    public boolean test(Geometry geom1, Geometry geom2) {
        if (geom1 == null) return geom2 == null;
        if (geom1.isEmpty()) return geom2.isEmpty() && geom1.getSRID() == geom2.getSRID();
        if (geom1 instanceof GeometryCollection) {
            if (!(geom2 instanceof GeometryCollection)) return false;
            GeometryCollection expectedCollection = (GeometryCollection) geom1;
            GeometryCollection receivedCollection = (GeometryCollection) geom2;
            for (int partIndex = 0; partIndex < expectedCollection.getNumGeometries(); partIndex++) {
                Geometry partExpected = expectedCollection.getGeometryN(partIndex);
                Geometry partReceived = receivedCollection.getGeometryN(partIndex);
                if (!test(partExpected, partReceived)) return false;
            }
            return true;
        } else {
            return testSimpleGeometryEquality(geom1, geom2);
        }
    }

    /**
     * Test whether two geometries, not of type GeometryCollection are equal.
     *
     * @param geom1
     * @param geom2
     * @return
     */
    protected boolean testSimpleGeometryEquality(Geometry geom1, Geometry geom2) {
        //return geom1.equals(geom2);
        return testTypeAndVertexEquality(geom1, geom2) && geom1.getSRID() == geom2.getSRID();
    }

    protected boolean testTypeAndVertexEquality(Geometry geom1, Geometry geom2) {
        if (!geom1.getGeometryType().equals(geom2.getGeometryType())) return false;
        if (geom1.getNumGeometries() != geom2.getNumGeometries()) return false;
        if (geom1.getNumPoints() != geom2.getNumPoints()) return false;
        Coordinate[] coordinates1 = geom1.getCoordinates();
        Coordinate[] coordinates2 = geom2.getCoordinates();
        for (int i = 0; i < coordinates1.length; i++) {
            Coordinate c1 = coordinates1[i];
            Coordinate c2 = coordinates2[i];
            if (!testCoordinateEquality(c1, c2)) return false;
        }
        return true;
    }

    private boolean testCoordinateEquality(Coordinate c1, Coordinate c2) {
        if (c1 instanceof MCoordinate) {
            if (!(c2 instanceof MCoordinate)) return false;
            MCoordinate mc1 = (MCoordinate) c1;
            MCoordinate mc2 = (MCoordinate) c2;
            if (!Double.isNaN(mc1.m) && mc1.m != mc2.m) return false;
        }
        if (!Double.isNaN(c1.z) && c1.z != c2.z) return false;
        return c1.x == c2.x && c1.y == c2.y;
    }
}

