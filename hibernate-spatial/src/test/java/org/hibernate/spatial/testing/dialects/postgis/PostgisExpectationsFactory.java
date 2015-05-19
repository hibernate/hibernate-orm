/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.postgis;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.jts.JTS;
import org.postgresql.util.PGobject;

import org.jboss.logging.Logger;

import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.NativeSQLStatement;

/**
 * This class provides the expected return values to the testsuite-suite classes in this package.
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class PostgisExpectationsFactory extends AbstractExpectationsFactory {

	private static final HSMessageLogger LOG = Logger.getMessageLogger(
			HSMessageLogger.class,
			PostgisExpectationsFactory.class.getName()
	);


	public PostgisExpectationsFactory(DataSourceUtils utils) {
		super( utils );
	}

	@Override
	protected NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_touches(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where st_touches(t.geom, ST_geomFromText(?, 4326)) = 'true' and st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_overlaps(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where st_overlaps(t.geom, ST_GeomFromText(?, 4326)) = 'true' and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		String sql = "select t.id, st_relate(t.geom, ST_GeomFromText(?, 4326), '" + matrix + "' ) from GeomTest t where st_relate(t.geom, ST_GeomFromText(?, 4326), '" + matrix + "') = 'true' and ST_SRID(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		String sql = "select t.id, st_dwithin(t.geom, ST_GeomFromText(?, 4326), " + distance + " ) from GeomTest t where st_dwithin(t.geom, ST_GeomFromText(?, 4326), " + distance + ") = 'true' and ST_SRID(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_intersects(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where st_intersects(t.geom, ST_GeomFromText(?, 4326)) = 'true' and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom && ST_GeomFromText(?, 4326) from GeomTest t where st_intersects(t.geom, ST_GeomFromText(?, 4326)) = 'true' and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_distance(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement( "select id, st_dimension(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, st_buffer(t.geom,?) from GeomTest t where ST_SRID(t.geom) = 4326",
				new Object[] { distance }
		);
	}

	@Override
	protected NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_convexhull(st_union(t.geom, ST_GeomFromText(?, 4326))) from GeomTest t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_intersection(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_difference(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_symdifference(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_union(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeTransformStatement(int epsg) {
		return createNativeSQLStatement(
				"select t.id, st_transform(t.geom," + epsg + ") from GeomTest t where ST_SRID(t.geom) = 4326"
		);
	}

	@Override
	protected NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement( "select t.id, (st_srid(t.geom) = " + srid + ") from GeomTest t where ST_SRID(t.geom) =  " + srid );
	}

	@Override
	protected NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select id, st_astext(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select id, ST_SRID(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select id, st_issimple(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement( "select id, st_isempty(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement( "select id, not st_isempty(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement( "select id, st_boundary(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select id, st_envelope(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select id, st_asbinary(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select id, st_GeometryType(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_within(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where st_within(t.geom, ST_GeomFromText(?, 4326)) = 'true' and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_equals(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where st_equals(t.geom, ST_GeomFromText(?, 4326)) = 'true' and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_crosses(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where st_crosses(t.geom, ST_GeomFromText(?, 4326)) = 'true' and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_contains(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where st_contains(t.geom, ST_GeomFromText(?, 4326)) = 'true' and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_disjoint(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where st_disjoint(t.geom, ST_GeomFromText(?, 4326)) = 'true' and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	//remove redundancy with toGeometry function in PGGeometryTypeDescriptor
	@Override
	protected Geometry decode(Object object) {
		ByteBuffer buffer = null;
		if (object instanceof PGobject ) {
			buffer = ByteBuffer.from( ( (PGobject) object ).getValue() );
		} else if ( object instanceof byte[] ) {
			byte[] bytes = (byte[]) object;
			ByteBuffer.from( bytes );
		} else {
			throw new IllegalStateException( "Received object of type " + object.getClass().getCanonicalName() );
		}
		WkbDecoder decoder = Wkb.newDecoder( Wkb.Dialect.POSTGIS_EWKB_1 );
		return JTS.to(decoder.decode( buffer));
	}

}
