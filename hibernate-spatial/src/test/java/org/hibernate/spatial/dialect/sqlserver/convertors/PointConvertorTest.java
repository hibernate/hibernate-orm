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

import org.geolatte.geom.Point;
import org.geolatte.geom.Points;
import org.geolatte.geom.crs.CrsId;
import org.junit.Test;

import org.hibernate.spatial.dialect.sqlserver.SqlServer2008SpatialDialect;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.RequiresDialect;

import static junit.framework.Assert.assertEquals;

/**
 * @author Karel Maesen, Geovise BVBA.
 *         Date: Nov 2, 2009
 */
@RequiresDialect(SqlServer2008SpatialDialect.class)
public class PointConvertorTest extends AbstractConvertorTest {

	@BeforeClassOnce
	public void beforeClass() {
		super.beforeClass();
		doDecoding( OpenGisType.POINT );
		doEncoding();
	}

	@Test
	public void test_verify_srid() {
		assertEquals( -1, decodedGeoms.get( 1 ).getSRID() );
		assertEquals( 4326, decodedGeoms.get( 2 ).getSRID() );
		assertEquals( 31370, decodedGeoms.get( 3 ).getSRID() );
	}

	@Test
	public void test_class() {
		for ( Integer id : decodedGeoms.keySet() ) {
			assertEquals( Point.class, decodedGeoms.get( id ).getClass() );
		}
	}

	@Test
	public void test_coordinates() {
		Point expected;
		expected = Points.create( 10.0, 5.0);
		assertEquals( expected, decodedGeoms.get( 1 ).getPointN(0) );
		expected = Points.create(52.25, 2.53, CrsId.valueOf(4326));
		assertEquals( expected, decodedGeoms.get( 2 ).getPointN( 0 ) );
		expected = Points.create(150000.0, 200000.0, CrsId.valueOf(31370));
		assertEquals( expected, decodedGeoms.get( 3 ).getPointN( 0 ) );
		expected = Points.create(10.0, 2.0, 1.0, 3.0, CrsId.valueOf(4326));
		assertEquals( expected, decodedGeoms.get( 4 ).getPointN( 0 ) );
	}

	@Test
	public void test_encoding() {
		super.test_encoding();
	}

	@Test
	public void test_decoding() {
		super.test_decoding();
	}

	@Test
	public void test_test_empty_point() {
		//TODO  -- How?
	}

	@Test
	public void test_no_srid() {
		//TODO -- How?
	}


}
