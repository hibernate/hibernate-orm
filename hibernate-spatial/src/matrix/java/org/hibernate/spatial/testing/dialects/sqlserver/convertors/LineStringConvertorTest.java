/*
 * $Id: LineStringConvertorTest.java 278 2010-12-18 14:03:32Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2009 Geovise BVBA
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

package org.hibernate.spatial.testing.dialects.sqlserver.convertors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.hibernate.spatial.dialect.sqlserver.SqlServer2008SpatialDialect;
import org.hibernate.spatial.dialect.sqlserver.convertors.OpenGisType;
import org.hibernate.spatial.mgeom.MCoordinate;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

@RequiresDialect(SqlServer2008SpatialDialect.class)
public class LineStringConvertorTest extends AbstractConvertorTest {

	@BeforeClassOnce
	public void setUp() {
		doDecoding( OpenGisType.LINESTRING );
		doEncoding();
	}


	@Test
	public void test_srid() {
		assertTrue( decodedGeoms.get( 5 ) instanceof LineString );
		assertTrue( decodedGeoms.get( 6 ) instanceof LineString );

		assertEquals( 4326, decodedGeoms.get( 5 ).getSRID() );
		assertEquals( 4326, decodedGeoms.get( 6 ).getSRID() );

	}

	@Test
	public void test_num_points() {
		assertEquals( 2, decodedGeoms.get( 5 ).getNumPoints() );
		assertEquals( 4, decodedGeoms.get( 6 ).getNumPoints() );
	}


	@Test
	public void test_coordinates() {

		Coordinate[] received = decodedGeoms.get( 5 ).getCoordinates();
		MCoordinate[] expected = new MCoordinate[] {
				new MCoordinate( 10.0, 5.0 ),
				new MCoordinate( 20.0, 15.0 )
		};
		assertArrayEquals( received, expected );

		received = decodedGeoms.get( 6 ).getCoordinates();
		expected = new MCoordinate[] {
				new MCoordinate( 10.0, 5.0 ),
				new MCoordinate( 20.0, 15.0 ),
				new MCoordinate( 30.3, 22.4 ),
				new MCoordinate( 10.0, 30.0 )
		};
		assertArrayEquals( expected, received );

		received = decodedGeoms.get( 7 ).getCoordinates();
		expected = new MCoordinate[] {
				new MCoordinate( 10.0, 5.0 ),
				new MCoordinate( 20.0, 15.0 )
		};
		expected[0].z = 0;
		expected[1].z = 3;
		assertArrayEquals( expected, received );

		//case 9
		received = decodedGeoms.get( 9 ).getCoordinates();
		expected = new MCoordinate[] {
				new MCoordinate( 10, 5 ),
				new MCoordinate( 20, 15 ),
				new MCoordinate( 30.3, 22.4 ),
				new MCoordinate( 10, 30 )
		};
		expected[0].z = 1;
		expected[1].z = 2;
		expected[2].z = 5;
		expected[3].z = 2;
		assertArrayEquals( expected, received );

		//case 10
		received = decodedGeoms.get( 10 ).getCoordinates();
		expected[0].m = 1;
		expected[1].m = 3;
		expected[2].m = 10;
		expected[3].m = 12;
		assertArrayEquals( expected, received );


	}

	@Test
	public void test_encoding() {
		super.test_encoding();
	}

	@Test
	public void test_decoding() {
		super.test_decoding();
	}

}
