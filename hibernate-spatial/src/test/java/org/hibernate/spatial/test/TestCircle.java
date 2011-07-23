/**
 * $Id: TestCircle.java 274 2010-12-18 14:02:06Z maesenka $
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
package org.hibernate.spatial.test;

import com.vividsolutions.jts.geom.Coordinate;
import org.hibernate.spatial.Circle;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test functionality of Circle class Date: Oct 15, 2007
 *
 * @author Tom Acree
 */
public class TestCircle {

    @Test
    public void testCreateCircle() {

        Coordinate center = (new Coordinate(0, 0));
        double radius = 5;

        Coordinate p1 = new Coordinate(3, 4);
        Coordinate p2 = new Coordinate(0, 5);
        Coordinate p3 = new Coordinate(-3, 4);

        Circle c0 = new Circle(center, radius);
        Circle c1 = new Circle(p1, p2, p3);
        assertEquals(c0, c1);

        assertTrue(Double.compare(c1.getRadius(), radius) == 0);
        assertTrue(c1.getCenter().equals2D(center));

        double distance = c1.distanceFromCenter(p3);
        assertTrue(Double.compare(c1.getRadius(), distance) == 0);
    }

    @Test
    public void testNormalize() {
        double actual, expected = 0;

        double angleIncr = Math.PI / 4; // increment by 45 degrees
        double twoPi = Math.PI * 2;
        int factor = 8;
        for (int i = 0; i <= factor; i++) {
            expected = i * angleIncr;
            actual = (Circle.normalizeAngle(expected));
            assertEquals(actual, expected, Math.ulp(expected));
            double degrees = Math.toDegrees(actual);
            assertTrue(actual <= twoPi);
            assertTrue(degrees <= 360);
        }

        factor = -8;
        double testAngle;
        for (int i = -1; i >= factor; i--) {
            testAngle = i * angleIncr;
            expected = twoPi + (i * angleIncr);
            actual = (Circle.normalizeAngle(testAngle));
            assertEquals(actual, expected, Math.ulp(expected));
            double degrees = Math.toDegrees(actual);
            assertTrue(actual <= Math.PI * 2);
            assertTrue(degrees <= 360);
        }

        // couple extra boundary cases
        expected = 0;
        actual = Circle.normalizeAngle(twoPi * 8);
        assertEquals(expected, actual, Math.ulp(expected));

        testAngle = angleIncr + twoPi;
        expected = angleIncr;
        actual = Circle.normalizeAngle(testAngle);
        assertEquals(expected, actual, Math.ulp(expected));

        testAngle = angleIncr - twoPi;
        expected = angleIncr;
        actual = Circle.normalizeAngle(testAngle);
        assertEquals(expected, actual, Math.ulp(expected));
    }

    @Test
    public void testAngleDifference() {
        double a1 = Math.PI / 8;
        double a2 = Math.PI / 4;

        double diff = Circle.subtractAngles(a1, a2);
        assertTrue(diff < Math.PI);

        diff = Circle.subtractAngles(a2, a1);
        assertTrue(diff > Math.PI);
    }

    @Test
    public void testMajorArc() {
        Coordinate expectedCenter = new Coordinate(3, 0);
        double expectedRadius = 5;
        Coordinate p1 = new Coordinate(0, 4);
        Coordinate p2 = new Coordinate(8, 0);
        Coordinate p3 = new Coordinate(0, -4);
        Circle c = new Circle(p1, p2, p3);

        assertTrue(c.getCenter().equals2D(expectedCenter));
        assertTrue(Double.compare(c.getRadius(), expectedRadius) == 0);
    }

    @Test
    public void testArcDirection() {
        Coordinate[] coords = new Coordinate[]{new Coordinate(0, 5),
                new Coordinate(3, 4), new Coordinate(5, 0),
                new Coordinate(3, -4), new Coordinate(0, -5),
                new Coordinate(-3, -4), new Coordinate(-5, 0),
                new Coordinate(-3, 4)};
        for (int i = 0; i < coords.length; i++) {
            Coordinate p1 = coords[i];
            Coordinate p2 = coords[(i + 1) % coords.length];
            Coordinate p3 = coords[(i + 2) % coords.length];
            Circle c = new Circle(p1, p2, p3);
            Circle.Arc a = c.createArc(p1, p2, p3);
            assertTrue("Failed Points:" + p1 + ", " + p2 + ", " + p3, a
                    .isClockwise());
        }

        for (int i = 0; i < coords.length; i++) {
            Coordinate p3 = coords[i];
            Coordinate p2 = coords[(i + 1) % coords.length];
            Coordinate p1 = coords[(i + 2) % coords.length];
            Circle c = new Circle(p1, p2, p3);
            Circle.Arc a = c.createArc(p1, p2, p3);
            assertFalse("Failed Points:" + p1 + ", " + p2 + ", " + p3, a
                    .isClockwise());
        }
    }

    @Test
    public void testLinearize() {
        Coordinate p1 = new Coordinate(5, 0);
        Coordinate p2 = new Coordinate(4, 3);
        Coordinate p3 = new Coordinate(4, -3);

        Circle c = new Circle(p1, p2, p3);

        Coordinate[] results = c.linearizeArc(p3, p2, p1, c.getRadius() * 0.01);

        assertNotNull(results);
        assertTrue(results.length > 0);
        for (Coordinate coord : results) {
            double error = c.getRadius() - c.distanceFromCenter(coord);
            assertTrue(Double.compare(error, 0.0001) < 0);
        }
    }

    @Test
    public void testLinearizeCircle() {
        Coordinate p1 = new Coordinate(5, 0);
        Coordinate p2 = new Coordinate(4, 3);
        Coordinate p3 = new Coordinate(4, -3);

        Circle c = new Circle(p1, p2, p3);
        Coordinate[] results = c.linearizeArc(p1, p2, p1,
                (c.getRadius() * 0.01));
        for (Coordinate coord : results) {
            double error = c.getRadius() - c.distanceFromCenter(coord);
            assertTrue(Double.compare(error, 0.0001) < 0);
        }

    }

}
