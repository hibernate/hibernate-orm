/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.geometry;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.geolatte.geom.jts.JTSUtils;

import static org.junit.Assert.assertTrue;

public class JTSUtilsTest {

	@Test
	@JiraKey(value = "HHH-14757")
	public void testGeometryCollection() {
		Geometry elem2a = JTS.to( Wkt.fromWkt( "GEOMETRYCOLLECTION(POINT(2 3),LINESTRING(2 3,3 4))" ) );
		Geometry elem2b = JTS.to( Wkt.fromWkt( "GEOMETRYCOLLECTION(POINT(2 3),LINESTRING(2 3,3 4))" ) );
		Geometry elem1a = JTS.to( Wkt.fromWkt( "GEOMETRYCOLLECTION(POINT(2 3))" ) );
		Geometry elem1b = JTS.to( Wkt.fromWkt( "GEOMETRYCOLLECTION(POINT(2 3))" ) );

		assertTrue( JTSUtils.equalsExact3D( elem2a, elem2b ) );
		assertTrue( JTSUtils.equalsExact3D( elem1a, elem1b ) );
	}

}
