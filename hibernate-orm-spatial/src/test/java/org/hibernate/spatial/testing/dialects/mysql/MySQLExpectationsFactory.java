/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.mysql;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.jts.JTS;

import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.NativeSQLStatement;


/**
 * This class provides the expected return values to the test classes in this package.
 *
 * @author Karel Maesen, Geovise BVBA
 */

public class MySQLExpectationsFactory extends AbstractExpectationsFactory {

	public MySQLExpectationsFactory(DataSourceUtils dataSourceUtils) {
		super( dataSourceUtils );
	}

	@Override
	protected NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, touches(t.geom, GeomFromText(?, 4326)) from geomtest t where touches(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, overlaps(t.geom, GeomFromText(?, 4326)) from geomtest t where overlaps(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		String sql = "select t.id, relate(t.geom, GeomFromText(?, 4326), '" + matrix + "' ) from geomtest t where relate(t.geom, GeomFromText(?, 4326), '" + matrix + "') = 1 and srid(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, intersects(t.geom, GeomFromText(?, 4326)) from geomtest t where intersects(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, MBRIntersects(t.geom, GeomFromText(?, 4326)) from geomtest t where MBRIntersects(t.geom, GeomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, distance(t.geom, GeomFromText(?, 4326)) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement( "select id, dimension(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, buffer(t.geom,?) from geomtest t where srid(t.geom) = 4326",
				new Object[] { distance }
		);
	}

	@Override
	protected NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, convexhull(geomunion(t.geom, GeomFromText(?, 4326))) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, intersection(t.geom, GeomFromText(?, 4326)) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, difference(t.geom, GeomFromText(?, 4326)) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, symdifference(t.geom, GeomFromText(?, 4326)) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, geomunion(t.geom, GeomFromText(?, 4326)) from geomtest t where srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select id, astext(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select id, srid(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select id, issimple(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement( "select id, isempty(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement( "select id, not isempty(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement( "select id, boundary(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select id, envelope(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select id, asbinary(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select id, GeometryType(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, within(t.geom, GeomFromText(?, 4326)) from geomtest t where within(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, equals(t.geom, GeomFromText(?, 4326)) from geomtest t where equals(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, crosses(t.geom, GeomFromText(?, 4326)) from geomtest t where crosses(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, contains(t.geom, GeomFromText(?, 4326)) from geomtest t where contains(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, disjoint(t.geom, GeomFromText(?, 4326)) from geomtest t where disjoint(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeTransformStatement(int epsg) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement( "select t.id, (srid(t.geom) = " + srid + ") from geomtest t where SRID(t.geom) =  " + srid );
	}

	@Override
	protected Geometry decode(Object bytes) {
		if ( bytes == null ) {
			return null;
		}
		ByteBuffer buffer = ByteBuffer.from( (byte[])bytes );
		WkbDecoder decoder = Wkb.newDecoder( Wkb.Dialect.MYSQL_WKB );
		return JTS.to( decoder.decode( buffer ) );
	}
}
