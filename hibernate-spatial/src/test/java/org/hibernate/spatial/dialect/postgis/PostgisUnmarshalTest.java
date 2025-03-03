/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis;

import java.sql.SQLException;

import org.junit.Test;

import org.geolatte.geom.ByteOrder;
import org.geolatte.geom.C2D;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.crs.CoordinateReferenceSystems;
import org.postgresql.util.PGobject;

import static org.geolatte.geom.builder.DSL.c;
import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.linestring;
import static org.junit.Assert.assertEquals;

/**
 * Tests the different ways Postgis seraialises Geometries
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 29/10/16.
 */
public class PostgisUnmarshalTest {

	private final CoordinateReferenceSystem<G2D> crs = CoordinateReferenceSystems.WGS84;
	private final Geometry<G2D> geom = linestring( crs, g( 6.123, 53.234 ), g( 6.133, 53.244 ) );
	private final Geometry<C2D> geomNoSrid = linestring(
			CoordinateReferenceSystems.PROJECTED_2D_METER,
			c( 6.123, 53.234 ),
			c( 6.133, 53.244 )
	);


	@Test
	public void testWktWithSrid() throws SQLException {
		String ewkt = Wkt.toWkt( geom );
		testCase( ewkt, geom );
	}

	@Test
	public void testWktWithoutSrid() throws SQLException {
		String wkt = Wkt.toWkt( geom ).split( ";" )[1];
		testCase( wkt, geomNoSrid );
	}

	@Test
	public void testWkbXDR() throws SQLException {
		String wkb = Wkb.toWkb( geom, ByteOrder.XDR ).toString();
		testCase( wkb, geom );
	}

	@Test
	public void testWkbNDR() throws SQLException {
		String wkb = Wkb.toWkb( geom, ByteOrder.NDR ).toString();
		testCase( wkb, geom );
	}


	public void testCase(String pgValue, Geometry<?> expected) throws SQLException {
		PGobject pgo = new PGobject();
		pgo.setValue( pgValue );
		Geometry<?> received = PGGeometryJdbcType.INSTANCE_WKB_2.toGeometry( pgo );
		assertEquals( String.format( "Failure on %s", pgValue ), expected, received );
	}


}
