/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.dialect.sqlserver.convertors;


import org.geolatte.geom.DimensionalFlag;
import org.geolatte.geom.LineString;
import org.geolatte.geom.PointCollection;
import org.geolatte.geom.PointSequenceBuilders;
import org.junit.Test;

import org.hibernate.spatial.dialect.sqlserver.SqlServer2008SpatialDialect;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.RequiresDialect;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RequiresDialect(SqlServer2008SpatialDialect.class)
public class LineStringConvertorTest extends AbstractConvertorTest {

	@BeforeClassOnce
	public void beforeClass() {
		super.beforeClass();
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

		PointCollection received = decodedGeoms.get( 5 ).getPoints();
		PointCollection expected = PointSequenceBuilders.fixedSized( 2, DimensionalFlag.XY ).add(10, 5).add(20,15).toPointSequence();
		assertPointCollectionEquality( received, expected );

		received = decodedGeoms.get( 6 ).getPoints();
		expected = PointSequenceBuilders.fixedSized( 4, DimensionalFlag.XY).add(10,5).add(20,15).add(30.3, 22.4).add(10,30).toPointSequence();
		assertPointCollectionEquality( received, expected );


		received = decodedGeoms.get( 7 ).getPoints();
		expected = PointSequenceBuilders.fixedSized( 2, DimensionalFlag.XYZ).add(10,5,0).add(20,15,3).toPointSequence();
		assertPointCollectionEquality( received, expected );

		//case 9
		received = decodedGeoms.get( 9 ).getPoints();
		expected = PointSequenceBuilders.fixedSized( 4, DimensionalFlag.XYZ).add(10,5,1).add(20,15,2).add(30.3, 22.4,5).add(10,30,2).toPointSequence();
		assertPointCollectionEquality( received, expected );

		//case 10
		received = decodedGeoms.get( 10 ).getPoints();
		expected = PointSequenceBuilders.fixedSized( 4, DimensionalFlag.XYZM).add(10,5,1,1).add(20,15,2,3).add(30.3, 22.4,5,10).add(10,30,2,12).toPointSequence();
		assertPointCollectionEquality( received, expected );

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
