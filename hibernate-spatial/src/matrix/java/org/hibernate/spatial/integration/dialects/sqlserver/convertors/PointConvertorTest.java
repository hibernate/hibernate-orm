/*
 * $Id: PointConvertorTest.java 278 2010-12-18 14:03:32Z maesenka $
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

package org.hibernate.spatial.integration.dialects.sqlserver.convertors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import org.hibernate.spatial.dialect.sqlserver.SqlServer2008SpatialDialect;
import org.hibernate.spatial.dialect.sqlserver.convertors.OpenGisType;
import org.hibernate.spatial.mgeom.MCoordinate;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Karel Maesen, Geovise BVBA.
 *         Date: Nov 2, 2009
 */
@RequiresDialect(SqlServer2008SpatialDialect.class)
public class PointConvertorTest extends AbstractConvertorTest {

	@BeforeClassOnce
	public void setup() {
		doDecoding( OpenGisType.POINT );
		doEncoding();
	}

	@Test
	public void test_verify_srid() {
		assertEquals( 0, decodedGeoms.get( 1 ).getSRID() );
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
		Coordinate expected;
		Coordinate received;
		expected = new Coordinate( 10.0, 5.0 );
		assertEquals( expected, decodedGeoms.get( 1 ).getCoordinate() );
		expected = new Coordinate( 52.25, 2.53 );
		assertEquals( expected, decodedGeoms.get( 2 ).getCoordinate() );
		expected = new Coordinate( 150000.0, 200000.0 );
		assertEquals( expected, decodedGeoms.get( 3 ).getCoordinate() );
		expected = new MCoordinate( 10.0, 2.0, 1.0, 3.0 );
		assertEquals( expected, decodedGeoms.get( 4 ).getCoordinate() );
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
