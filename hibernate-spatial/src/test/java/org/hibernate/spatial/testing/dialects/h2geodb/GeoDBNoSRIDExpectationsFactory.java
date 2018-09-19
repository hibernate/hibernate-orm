/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 *
 */
package org.hibernate.spatial.testing.dialects.h2geodb;

import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.NativeSQLStatement;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.jts.JTS;

/**
 * A Factory class that generates expected {@link org.hibernate.spatial.testing.NativeSQLStatement}s for GeoDB
 * version < 0.4. These versions don't support storage of the SRID value with
 * the geometry.
 *
 * @author Jan Boonen, Geodan IT b.v.
 * @deprecated No longer used
 */
@Deprecated
public class GeoDBNoSRIDExpectationsFactory extends AbstractExpectationsFactory {

	public GeoDBNoSRIDExpectationsFactory(GeoDBDataSourceUtils dataSourceUtils) {
		super( dataSourceUtils );
	}

	@Override
	protected NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select id, ST_AsEWKB(geom) from GEOMTEST" );
	}

	@Override
	protected NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select id, ST_AsText(geom) from GEOMTEST" );
	}

	@Override
	protected NativeSQLStatement createNativeBoundaryStatement() {
		throw new UnsupportedOperationException(
				"Method ST_Bounday() is not implemented in the current version of GeoDB."
		);
	}

	@Override
	protected NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, ST_Buffer(t.geom,?) from GEOMTEST t where ST_SRID(t.geom) = 4326",
				new Object[] { distance }
		);
	}

	@Override
	protected NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Contains(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Contains(t.geom, ST_GeomFromText(?, 4326)) = 1",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		throw new UnsupportedOperationException(
				"Method ST_ConvexHull() is not implemented in the current version of GeoDB."
		);
	}

	@Override
	protected NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Crosses(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Crosses(t.geom, ST_GeomFromText(?, 4326)) = 1",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		throw new UnsupportedOperationException(
				"Method ST_Difference() is not implemented in the current version of GeoDB."
		);
	}

	@Override
	protected NativeSQLStatement createNativeDimensionSQL() {
		throw new UnsupportedOperationException(
				"Method ST_Dimension() is not implemented in the current version of GeoDB."
		);
	}

	@Override
	protected NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Disjoint(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Disjoint(t.geom, ST_GeomFromText(?, 4326)) = 1",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeTransformStatement(int epsg) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement( "select t.id, (st_srid(t.geom) = " + srid + ") from GeomTest t where ST_SRID(t.geom) =  " + srid );
	}

	@Override
	protected NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_distance(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select id, ST_Envelope(geom) from GEOMTEST" );
	}

	@Override
	protected NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Equals(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Equals(t.geom, ST_GeomFromText(?, 4326)) = 1",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		throw new UnsupportedOperationException(
				"Method ST_MBRIntersects() is not implemented in the current version of GeoDB."
		);
	}

	@Override
	protected NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		throw new UnsupportedOperationException(
				"Method ST_GeomUnion() is not implemented in the current version of GeoDB."
		);
	}

	@Override
	protected NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select id, GeometryType(geom) from GEOMTEST" );
	}

	@Override
	protected NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		throw new UnsupportedOperationException(
				"Method ST_Intersection() is not implemented in the current version of GeoDB."
		);
	}

	@Override
	protected NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Intersects(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Intersects(t.geom, ST_GeomFromText(?, 4326)) = 1",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement( "select id, ST_IsEmpty(geom) from GEOMTEST" );
	}

	@Override
	protected NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement( "select id, not ST_IsEmpty(geom) from GEOMTEST" );
	}

	@Override
	protected NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select id, ST_IsSimple(geom) from GEOMTEST" );
	}

	@Override
	protected NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Overlaps(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Overlaps(t.geom, ST_GeomFromText(?, 4326)) = 1",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeRelateStatement(
			Geometry geom,
			String matrix) {
		throw new UnsupportedOperationException(
				"Method ST_Relate() is not implemented in the current version of GeoDB."
		);
	}

	@Override
	protected NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		String sql = "select t.id, st_dwithin(t.geom, ST_GeomFromText(?, 4326), " + distance + " ) from GeomTest t where st_dwithin(t.geom, ST_GeomFromText(?, 4326), " + distance + ") = 'true' and ST_SRID(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeSridStatement()
	 */

	@Override
	protected NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select id, ST_SRID(geom) from GEOMTEST" );
	}

	@Override
	protected NativeSQLStatement createNativeSymDifferenceStatement(
			Geometry geom) {
		throw new UnsupportedOperationException(
				"Method ST_SymDifference() is not implemented in the current version of GeoDB."
		);
	}

	@Override
	protected NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Touches(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Touches(t.geom, ST_GeomFromText(?, 4326)) = 1",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeWithinStatement(
			Geometry testPolygon) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Within(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Within(t.geom, ST_GeomFromText(?, 4326)) = 1 and ST_SRID(t.geom) = 4326",
				testPolygon.toText()
		);
	}

	@Override
	protected Geometry decode(Object o) {
		return JTS.to( Wkb.fromWkb( ByteBuffer.from( (byte[]) o ) ) );
	}
}
