/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.geometry;

import org.hibernate.testing.TestForIssue;
import org.junit.Ignore;
import org.junit.Test;

import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.geolatte.geom.jts.JTSUtils;

import static org.junit.Assert.assertTrue;

public class JTSUtilsTest {

	@Test
	@TestForIssue(jiraKey = "HHH-14757")
	public void testGeometryCollection() {
		Geometry elem2a = JTS.to( Wkt.fromWkt( "GEOMETRYCOLLECTION(POINT(2 3),LINESTRING(2 3,3 4))" ) );
		Geometry elem2b = JTS.to( Wkt.fromWkt( "GEOMETRYCOLLECTION(POINT(2 3),LINESTRING(2 3,3 4))" ) );
		Geometry elem1a = JTS.to( Wkt.fromWkt( "GEOMETRYCOLLECTION(POINT(2 3))" ) );
		Geometry elem1b = JTS.to( Wkt.fromWkt( "GEOMETRYCOLLECTION(POINT(2 3))" ) );

		assertTrue( JTSUtils.equalsExact3D( elem2a, elem2b ) );
		assertTrue( JTSUtils.equalsExact3D( elem1a, elem1b ) );
	}

}
