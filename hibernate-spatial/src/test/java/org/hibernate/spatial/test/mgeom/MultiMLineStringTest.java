/**
 * $Id: MultiMLineStringTest.java 253 2010-10-02 15:14:52Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the 
 * hibernate ORM solution for geographic data. 
 *
 * Copyright © 2007 Geovise BVBA
 * Copyright © 2007 K.U. Leuven LRD, Spatial Applications Division, Belgium
 *
 * This work was partially supported by the European Commission, 
 * under the 6th Framework Programme, contract IST-2-004688-STP.
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
package org.hibernate.spatial.test.mgeom;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import junit.framework.TestCase;
import org.hibernate.spatial.mgeom.MCoordinate;
import org.hibernate.spatial.mgeom.MCoordinateSequenceFactory;
import org.hibernate.spatial.mgeom.MLineString;
import org.hibernate.spatial.mgeom.MultiMLineString;

/**
 * @author Karel Maesen
 */
public class MultiMLineStringTest extends TestCase {

    private final MCoordinateSequenceFactory mcfactory = MCoordinateSequenceFactory
            .instance();

    private final GeometryFactory geomfactory = new GeometryFactory(mcfactory);

    protected MLineString ml1;

    protected MLineString ml2;

    protected MultiMLineString mm1;

    protected MultiMLineString mmsimple;

    protected MCoordinate lastco;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MultiMLineStringTest.class);
    }

    /*
      * @see TestCase#setUp()
      */

    protected void setUp() throws Exception {
        super.setUp();

        MCoordinate mc0 = new MCoordinate(0.0, 0.0, 0.0, 0.0);
        MCoordinate mc1 = new MCoordinate(1.0, 0.0, 0.0, 0.1);
        MCoordinate mc2 = new MCoordinate(1.0, 1.0, 0.0, 0.2);
        MCoordinate mc3 = new MCoordinate(5.0, 1.0, 0.0, 0.3);
        MCoordinate mc4 = new MCoordinate(5.0, 3.0, 0.0, 0.4);
        lastco = mc4;

        MCoordinate[] m1 = {mc0, mc1, mc2};
        MCoordinate[] m2 = {mc3, mc4};

        CoordinateSequence mseq1 = mcfactory.create(m1);
        ml1 = new MLineString(mseq1, geomfactory);

        CoordinateSequence mseq2 = mcfactory.create(m2);
        ml2 = new MLineString(mseq2, geomfactory);

        mmsimple = new MultiMLineString(new MLineString[]{ml1}, 0.1,
                geomfactory);
        mm1 = new MultiMLineString(new MLineString[]{ml1, ml2}, 0.1,
                geomfactory);

    }

    /*
      * @see TestCase#tearDown()
      */

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMaxM() {
        assertEquals(0.4, mm1.getMaxM(), 0.000001);
    }

    /*
      * Class under testsuite-suite for java.lang.String getGeometryType()
      */

    public void testGetGeometryType() {
        assertTrue("wrong type reported", mm1.getGeometryType()
                .equalsIgnoreCase("multimlinestring"));
    }

    public void testGetDimension() {
        // TODO Implement getDimension().
    }

    public void testGetBoundary() {
        // TODO Implement getBoundary().
    }

    public void testGetBoundaryDimension() {
        // TODO Implement getBoundaryDimension().
    }

    /*
      * Class under testsuite-suite for boolean
      * equalsExact(com.vividsolutions.jts.geom.Geometry, double)
      */

    public void testEqualsExactGeometrydouble() {
        // TODO Implement equalsExact().
    }

    /*
      * Class under testsuite-suite for void
      * MultiLineString(com.vividsolutions.jts.geom.LineString[],
      * com.vividsolutions.jts.geom.PrecisionModel, int)
      */

    public void testMultiLineStringLineStringArrayPrecisionModelint() {
        // TODO Implement MultiLineString().
    }

    /*
      * Class under testsuite-suite for void
      * MultiLineString(com.vividsolutions.jts.geom.LineString[],
      * com.vividsolutions.jts.geom.GeometryFactory)
      */

    public void testMultiLineStringLineStringArrayGeometryFactory() {
        // TODO Implement MultiLineString().
    }

    public void testIsClosed() {
        // TODO Implement isClosed().
    }

    public void testClone() {
        // TODO implement

    }

    public void testInterpolate() {
        mm1.measureOnLength(false);
        Coordinate[] ca = mm1.getCoordinates();
        assertTrue("co 0 not OK", ((MCoordinate) ca[0]).m == 0.0);
        assertTrue("co 1 not OK",
                Math.abs(((MCoordinate) ca[1]).m - 1.0) < 0.00001);
        assertTrue("co 2 not OK",
                Math.abs(((MCoordinate) ca[2]).m - 2.0) < 0.00001);
        assertTrue("co 3 not OK", Math.abs(((MCoordinate) ca[3]).m
                - (2.0 + mm1.getMGap())) < 0.00001);
        assertTrue("co 4 not OK", Math.abs(((MCoordinate) ca[4]).m
                - (4.0 + mm1.getMGap())) < 0.00001);

        double dist = mm1.getLength();
        dist += (mm1.getNumGeometries() - 1) * mm1.getMGap();
        assertTrue("interpolation not consistent with distance", Math
                .abs(((MCoordinate) ca[4]).m - dist) < 0.00001);

    }

}
