/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2014 Adtech Geospatial
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

package org.hibernate.spatial.testing.dialects.db2;

import java.sql.SQLException;
import java.util.Map;

import org.hibernate.spatial.dialect.db2.DB2GeometryTypeDescriptor;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.NativeSQLStatement;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.geolatte.geom.jts.JTS;

/**
 * This class provides the DB2 native spatial queries to generate the
 * results which will be compared with the HQL spatial results.
 *
 * @author David Adler, Adtech Geospatial
 * creation-date: 5/22/2014
 */
public class DB2ExpectationsFactory extends AbstractExpectationsFactory {

	private final DB2GeometryTypeDescriptor desc = new DB2GeometryTypeDescriptor( 4326 );

	public DB2ExpectationsFactory(DataSourceUtils utils) {
		super( utils );
	}

	/**
	 * Returns the expected extent of all testsuite-suite geometries.
	 *
	 * @return map of identifier, extent
	 *
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getExtent() throws SQLException {
		return retrieveExpected( createNativeExtentStatement(), GEOMETRY );
	}

	protected NativeSQLStatement createNativeExtentStatement() {
		return createNativeSQLStatement(
				"select max(t.id), db2gse.ST_GetAggrResult(MAX(db2gse.st_BuildMBRAggr(t.geom))) from GeomTest t where db2gse.st_srid(t.geom) = 4326" );
	}

	@Override
	protected NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_touches(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_touches(t.geom, DB2GSE.ST_geomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_overlaps(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_overlaps(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		String sql = "select t.id, DB2GSE.ST_relate(t.geom, DB2GSE.ST_GeomFromText(?, 4326), '" + matrix + "' ) from GeomTest t where DB2GSE.ST_relate(t.geom, DB2GSE.ST_GeomFromText(?, 4326), '" + matrix + "') = 1 and db2gse.st_srid(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		String sql = "select t.id, DB2GSE.ST_dwithin(DB2GSE.ST_GeomFromText(?, 4326), t.geom, " + distance + " , 'METER') from GeomTest t where DB2GSE.ST_dwithin(DB2GSE.ST_GeomFromText(?, 4326), t.geom,  " + distance + ", 'METER') = 1 and db2gse.st_srid(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_intersects(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_intersects(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom && ST_GeomFromText(?, 4326) from GeomTest t where DB2GSE.ST_intersects(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_distance(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_dimension(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, DB2GSE.ST_buffer(t.geom,?) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				new Object[] { distance }
		);
	}

	@Override
	protected NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_convexhull(DB2GSE.ST_Union(t.geom, DB2GSE.ST_GeomFromText(?, 4326))) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_intersection(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_difference(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_symdifference(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_union(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeTransformStatement(int epsg) {
		return createNativeSQLStatement(
				"select t.id, DB2GSE.ST_transform(t.geom," + epsg + ") from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326"
		);
	}

	@Override
	protected NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement(
				"select t.id, DB2GSE.st_srid(t.geom) from GeomTest t where DB2GSE.ST_SRID(t.geom) =  " + srid );
	}

	@Override
	protected NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.st_astext(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_SRID(geom) from geomtest" );
	}


	@Override
	protected NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_issimple(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement(
				"select id, DB2GSE.ST_isempty(geom) from geomtest where db2gse.ST_IsEmpty(geom) = 1" );
	}

	@Override
	protected NativeSQLStatement createNativeIsNotEmptyStatement() { // return 'not ST_IsEmpty', 'not' is not supported by DB2
		return createNativeSQLStatement(
				"select id, case when DB2GSE.ST_isempty(geom) = 0 then 1 else 0 end from geomtest where db2gse.ST_IsEmpty(geom) = 0" );
	}

	@Override
	protected NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_boundary(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_envelope(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_asbinary(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_GeometryType(geom) from geomtest" );
	}

	@Override
	protected NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_within(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_within(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_equals(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_equals(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_crosses(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_crosses(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_contains(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_contains(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_disjoint(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_disjoint(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	protected Geometry decode(Object o) {
		org.geolatte.geom.Geometry<?> geometry = desc.toGeometry( o );
		return geometry == null ? null : JTS.to( geometry );
	}

}
