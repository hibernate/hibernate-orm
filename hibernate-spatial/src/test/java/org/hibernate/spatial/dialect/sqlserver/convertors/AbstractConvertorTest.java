/*
 * $Id: AbstractConvertorTest.java 278 2010-12-18 14:03:32Z maesenka $
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

package org.hibernate.spatial.dialect.sqlserver.convertors;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.hibernate.spatial.dialect.sqlserver.SQLServerExpressionTemplate;
import org.hibernate.spatial.dialect.sqlserver.SQLServerTestSupport;
import org.hibernate.spatial.test.DataSourceUtils;
import org.hibernate.spatial.test.TestData;
import org.hibernate.spatial.test.TestSupport;

import static org.junit.Assert.assertTrue;

/**
 * @author Karel Maesen, Geovise BVBA.
 *         Date: Nov 2, 2009
 */
public class AbstractConvertorTest {

	private final static DataSourceUtils dataSourceUtils = new DataSourceUtils(
			"hibernate-spatial-sqlserver-test.properties",
			new SQLServerExpressionTemplate()
	);

	private final static TestSupport support = new SQLServerTestSupport();

	Map<Integer, Geometry> decodedGeoms;
	Map<Integer, Object> rawResults;
	Map<Integer, byte[]> encodedGeoms;
	Map<Integer, Geometry> expectedGeoms;

	@BeforeClass
	public static void beforeClass() throws SQLException, IOException {
		String sql = dataSourceUtils.parseSqlIn( "create-sqlserver-test-schema.sql" );
		dataSourceUtils.executeStatement( sql );
		TestData testData = support.createTestData( null );
		dataSourceUtils.insertTestData( testData );
	}

	@AfterClass
	public static void afterClass() throws SQLException, IOException {
		String sql = dataSourceUtils.parseSqlIn( "drop-sqlserver-test-schema.sql" );
		dataSourceUtils.executeStatement( sql );
	}

	public void doDecoding(OpenGisType type) {
		rawResults = dataSourceUtils.rawDbObjects( type.toString() );
		TestData testData = support.createTestData( null );
		expectedGeoms = dataSourceUtils.expectedGeoms( type.toString(), testData );
		decodedGeoms = new HashMap<Integer, Geometry>();

		for ( Integer id : rawResults.keySet() ) {
			Geometry geometry = Decoders.decode( (byte[]) rawResults.get( id ) );
			decodedGeoms.put( id, geometry );
		}
	}

	public void doEncoding() {
		encodedGeoms = new HashMap<Integer, byte[]>();
		for ( Integer id : decodedGeoms.keySet() ) {
			Geometry geom = decodedGeoms.get( id );
			byte[] bytes = Encoders.encode( geom );
			encodedGeoms.put( id, bytes );
		}
	}

	public void test_encoding() {
		for ( Integer id : encodedGeoms.keySet() ) {
			assertTrue(
					"Wrong encoding for case " + id,
					Arrays.equals( (byte[]) rawResults.get( id ), encodedGeoms.get( id ) )
			);
		}
	}

	public void test_decoding() {
		for ( Integer id : decodedGeoms.keySet() ) {
			Geometry expected = expectedGeoms.get( id );
			Geometry received = decodedGeoms.get( id );
			assertTrue( "Wrong decoding for case " + id, expected.equalsExact( received ) );
		}
	}
}

