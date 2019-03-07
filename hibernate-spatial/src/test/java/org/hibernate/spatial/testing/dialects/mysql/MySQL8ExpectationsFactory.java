/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing.dialects.mysql;

import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.NativeSQLStatement;

import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 10/9/13
 */
public class MySQL8ExpectationsFactory extends AbstractExpectationsFactory {


	MySQL8ExpectationsFactory(DataSourceUtils dataSourceUtils) {
		super( dataSourceUtils );
	}

	@Override
	protected NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Touches(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Touches(t.geom, ST_geomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_overlaps(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Overlaps(t.geom, ST_geomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Intersects(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Intersects(t.geom, ST_GeomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Within(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Within(t.geom, ST_GeomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Equals(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Equals(t.geom, ST_GeomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Crosses(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Crosses(t.geom, ST_geomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Contains(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Contains(t.geom, ST_geomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Disjoint(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Disjoint(t.geom, ST_geomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}



	@Override
	protected NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		throw new UnsupportedOperationException();
	}


	@Override
	protected NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, MBRIntersects(t.geom, ST_GeomFromtext(?, 31370)) from geomtest t where MBRIntersects(t.geom, ST_GeomFromtext(?, 31370)) = 1",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_distance(t.geom, ST_GeomFromText(?, 31370)) from geomtest t ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement( "select id, ST_dimension(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, ST_buffer(t.geom,?) from geomtest t ",
				new Object[] { distance }
		);
	}

	@Override
	protected NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_convexhull(ST_Union(t.geom, ST_GeomFromText(?, 31370))) from geomtest t",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_intersection(t.geom, ST_GeomFromtext(?, 31370)) from geomtest t ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Difference(t.geom, ST_GeomFromtext(?, 31370)) from geomtest t ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Symdifference(t.geom, ST_GeomFromtext(?, 31370)) from geomtest t ",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Union(t.geom, ST_GeomFromtext(?, 31370)) from geomtest t",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select id, ST_astext(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select id, ST_SRID(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select id, ST_IsSimple(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement( "select id, ST_IsEmpty(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement( "select id, not ST_IsEmpty(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeBoundaryStatement() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select id, ST_Envelope(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select id, ST_AsBinary(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select id, ST_GeometryType(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeTransformStatement(int epsg) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement( "select t.id, (ST_Srid(t.geom) = " + srid + ") from geomtest t where ST_SRID(t.geom) =  " + srid );
	}

	@Override
	protected Geometry decode(Object bytes) {
		if ( bytes == null ) {
			return null;
		}
		ByteBuffer buffer = ByteBuffer.from( (byte[]) bytes );
		WkbDecoder decoder = Wkb.newDecoder( Wkb.Dialect.MYSQL_WKB );
		return JTS.to( decoder.decode( buffer ) );
	}

	@Override
	public int getTestSrid() {
		return 31370;
	}
}
