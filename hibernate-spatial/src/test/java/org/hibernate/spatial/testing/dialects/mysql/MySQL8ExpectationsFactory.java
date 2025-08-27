/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.mysql;

import org.hibernate.spatial.testing.AbstractExpectationsFactory;
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


	public MySQL8ExpectationsFactory() {
		super();
	}

	@Override
	public NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Touches(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Touches(t.geom, ST_geomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_overlaps(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Overlaps(t.geom, ST_geomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Intersects(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Intersects(t.geom, ST_GeomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Within(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Within(t.geom, ST_GeomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Equals(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Equals(t.geom, ST_GeomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Crosses(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Crosses(t.geom, ST_geomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Contains(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Contains(t.geom, ST_geomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Disjoint(t.geom, ST_GeomFromText(?, 31370)) from geomtest t where ST_Disjoint(t.geom, ST_geomFromText(?, 31370)) = 1 ",
				geom.toText()
		);
	}


	@Override
	public NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		String sql = "select t.id, ST_Relate(t.geom, ST_GeomFromText(?, 31370), '" + matrix + "' ) from geomtest t where ST_Relate(t.geom, ST_GeomFromText(?, 31370), '" + matrix + "') = 1 ";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	public NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		throw new UnsupportedOperationException();
	}


	@Override
	public NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, MBRIntersects(t.geom, ST_GeomFromtext(?, 31370)) from geomtest t where MBRIntersects(t.geom, ST_GeomFromtext(?, 31370)) = 1",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_distance(t.geom, ST_GeomFromText(?, 31370)) from geomtest t ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement( "select id, ST_dimension(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, ST_buffer(t.geom,?) from geomtest t ",
				new Object[] { distance }
		);
	}

	@Override
	public NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_convexhull(ST_Union(t.geom, ST_GeomFromText(?, 31370))) from geomtest t",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_intersection(t.geom, ST_GeomFromtext(?, 31370)) from geomtest t ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Difference(t.geom, ST_GeomFromtext(?, 31370)) from geomtest t ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Symdifference(t.geom, ST_GeomFromtext(?, 31370)) from geomtest t ",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Union(t.geom, ST_GeomFromtext(?, 31370)) from geomtest t",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select id, ST_astext(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select id, ST_SRID(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select id, ST_IsSimple(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement( "select id, ST_IsEmpty(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement( "select id, not ST_IsEmpty(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement( "select id, st_boundary(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select id, ST_Envelope(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select id, ST_AsBinary(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select id, ST_GeometryType(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeTransformStatement(int epsg) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
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
