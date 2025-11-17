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
 * This class provides the expected return values to the test classes in this package.
 *
 * @author Karel Maesen, Geovise BVBA
 */

public class MySQLExpectationsFactory extends AbstractExpectationsFactory {

	public MySQLExpectationsFactory() {
		super();
	}

	@Override
	public NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, touches(t.geom, GeomFromText(?, 4326)) from geomtest t where touches(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, overlaps(t.geom, GeomFromText(?, 4326)) from geomtest t where overlaps(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		String sql = "select t.id, relate(t.geom, GeomFromText(?, 4326), '" + matrix + "' ) from geomtest t where relate(t.geom, GeomFromText(?, 4326), '" + matrix + "') = 1 and srid(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	public NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, intersects(t.geom, GeomFromText(?, 4326)) from geomtest t where intersects(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, MBRIntersects(t.geom, GeomFromText(?, 4326)) from geomtest t where MBRIntersects(t.geom, GeomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, distance(t.geom, GeomFromText(?, 4326)) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement( "select id, dimension(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, buffer(t.geom,?) from geomtest t where srid(t.geom) = 4326",
				new Object[] { distance }
		);
	}

	@Override
	public NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, convexhull(geomunion(t.geom, GeomFromText(?, 4326))) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, intersection(t.geom, GeomFromText(?, 4326)) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, difference(t.geom, GeomFromText(?, 4326)) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, symdifference(t.geom, GeomFromText(?, 4326)) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, geomunion(t.geom, GeomFromText(?, 4326)) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select id, astext(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select id, srid(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select id, issimple(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement( "select id, isempty(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement( "select id, not isempty(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement( "select id, boundary(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select id, envelope(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select id, asbinary(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select id, GeometryType(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, within(t.geom, GeomFromText(?, 4326)) from geomtest t where within(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, equals(t.geom, GeomFromText(?, 4326)) from geomtest t where equals(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, crosses(t.geom, GeomFromText(?, 4326)) from geomtest t where crosses(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, contains(t.geom, GeomFromText(?, 4326)) from geomtest t where contains(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, disjoint(t.geom, GeomFromText(?, 4326)) from geomtest t where disjoint(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeTransformStatement(int epsg) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement( "select t.id, (srid(t.geom) = " + srid + ") from geomtest t where SRID(t.geom) =  " + srid );
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
}
