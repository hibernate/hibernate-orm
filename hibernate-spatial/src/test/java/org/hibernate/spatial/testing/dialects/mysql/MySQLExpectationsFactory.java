/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.testing.dialects.mysql;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.hibernate.spatial.dialect.mysql.MySQLGeometryValueExtractor;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.NativeSQLStatement;


/**
 * This class provides the expected return values to the test classes in this package.
 *
 * @author Karel Maesen, Geovise BVBA
 */

public class MySQLExpectationsFactory extends AbstractExpectationsFactory {

	private final MySQLGeometryValueExtractor decoder = new MySQLGeometryValueExtractor();

	public MySQLExpectationsFactory(DataSourceUtils dataSourceUtils) {
		super(dataSourceUtils);
	}

	@Override
	protected NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, touches(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where touches(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, overlaps(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where overlaps(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		String sql = "select t.id, relate(t.geom, GeomFromText(?, 4326), '" + matrix + "' ) from GEOMTEST t where relate(t.geom, GeomFromText(?, 4326), '" + matrix + "') = 1 and srid(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams(sql, geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, intersects(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where intersects(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, MBRIntersects(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where MBRIntersects(t.geom, GeomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, distance(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement("select id, dimension(geom) from GEOMTEST");
	}

	@Override
	protected NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement("select t.id, buffer(t.geom,?) from GEOMTEST t where srid(t.geom) = 4326", new Object[]{distance});
	}

	@Override
	protected NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, convexhull(geomunion(t.geom, GeomFromText(?, 4326))) from GEOMTEST t where srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, intersection(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, difference(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, symdifference(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, geomunion(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement("select id, astext(geom) from GEOMTEST");
	}

	@Override
	protected NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement("select id, srid(geom) from GEOMTEST");
	}

	@Override
	protected NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement("select id, issimple(geom) from GEOMTEST");
	}

	@Override
	protected NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement("select id, isempty(geom) from GEOMTEST");
	}

	@Override
	protected NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement("select id, not isempty(geom) from GEOMTEST");
	}

	@Override
	protected NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement("select id, boundary(geom) from GEOMTEST");
	}

	@Override
	protected NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement("select id, envelope(geom) from GEOMTEST");
	}

	@Override
	protected NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement("select id, asbinary(geom) from GEOMTEST");
	}

	@Override
	protected NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement("select id, GeometryType(geom) from GEOMTEST");
	}

	@Override
	protected NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, within(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where within(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, equals(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where equals(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, crosses(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where crosses(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, contains(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where contains(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, disjoint(t.geom, GeomFromText(?, 4326)) from GEOMTEST t where disjoint(t.geom, geomFromText(?, 4326)) = 1 and srid(t.geom) = 4326",
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeTransformStatement(int epsg) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement("select t.id, (srid(t.geom) = " + srid + ") from GEOMTEST t where SRID(t.geom) =  " + srid);
	}

	@Override
	protected Geometry decode(Object o) {
		return decoder.toJTS(o);
	}
}
