/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import java.sql.SQLException;

import org.geolatte.geom.ByteOrder;
import org.geolatte.geom.C2D;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.crs.CoordinateReferenceSystems;
import org.postgresql.util.PGobject;

import org.junit.Test;

import static org.geolatte.geom.builder.DSL.*;
import static org.junit.Assert.assertEquals;

/**
 * Tests the different ways Postgis seraialises Geometries
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 29/10/16.
 */
public class PostgisUnmarshalTest {

	private CoordinateReferenceSystem<G2D> crs = CoordinateReferenceSystems.WGS84;
	private Geometry<G2D> geom = linestring( crs, g( 6.123, 53.234 ), g( 6.133, 53.244 ) );
	private Geometry<C2D> geomNoSrid = linestring(
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
		Geometry<?> received = PGGeometryTypeDescriptor.toGeometry( pgo );
		assertEquals( String.format( "Failure on %s", pgValue ), expected, received );
	}


}
