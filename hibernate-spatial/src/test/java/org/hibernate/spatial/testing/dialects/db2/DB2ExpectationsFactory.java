/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.db2;

import java.sql.SQLException;
import java.util.Map;

import org.hibernate.spatial.dialect.db2.DB2GeometryType;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.NativeSQLStatement;

import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * This class provides the DB2 native spatial queries to generate the
 * results which will be compared with the HQL spatial results.
 *
 * @author David Adler, Adtech Geospatial
 * creation-date: 5/22/2014
 */
public class DB2ExpectationsFactory extends AbstractExpectationsFactory {

	private final DB2GeometryType desc = new DB2GeometryType( 4326 );

	public DB2ExpectationsFactory() {
		super();
	}

	/**
	 * Returns the expected extent of all testsuite-suite geometries.
	 *
	 * @return map of identifier, extent
	 *
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getExtent() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public NativeSQLStatement createNativeExtentStatement() {
		return createNativeSQLStatement(
				"select max(t.id), db2gse.ST_GetAggrResult(MAX(db2gse.st_BuildMBRAggr(t.geom))) from GeomTest t where db2gse.st_srid(t.geom) = 4326" );
	}

	@Override
	public NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_touches(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_touches(t.geom, DB2GSE.ST_geomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_overlaps(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_overlaps(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		String sql = "select t.id, DB2GSE.ST_relate(t.geom, DB2GSE.ST_GeomFromText(?, 4326), '" + matrix + "' ) from GeomTest t where DB2GSE.ST_relate(t.geom, DB2GSE.ST_GeomFromText(?, 4326), '" + matrix + "') = 1 and db2gse.st_srid(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	public NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		String sql = "select t.id, DB2GSE.ST_dwithin(DB2GSE.ST_GeomFromText(?, 4326), t.geom, " + distance + " , 'METER') from GeomTest t where DB2GSE.ST_dwithin(DB2GSE.ST_GeomFromText(?, 4326), t.geom,  " + distance + ", 'METER') = 1 and db2gse.st_srid(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	public NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_intersects(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_intersects(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom && ST_GeomFromText(?, 4326) from GeomTest t where DB2GSE.ST_intersects(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_distance(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_dimension(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, DB2GSE.ST_buffer(t.geom,?) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				new Object[] { distance }
		);
	}

	@Override
	public NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_convexhull(DB2GSE.ST_Union(t.geom, DB2GSE.ST_GeomFromText(?, 4326))) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_intersection(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_difference(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_symdifference(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_union(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeTransformStatement(int epsg) {
		return createNativeSQLStatement(
				"select t.id, DB2GSE.ST_transform(t.geom," + epsg + ") from GeomTest t where DB2GSE.ST_SRID(t.geom) = 4326"
		);
	}

	@Override
	public NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement(
				"select t.id, DB2GSE.st_srid(t.geom) from GeomTest t where DB2GSE.ST_SRID(t.geom) =  " + srid );
	}

	@Override
	public NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.st_astext(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_SRID(geom) from geomtest" );
	}


	@Override
	public NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_issimple(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement(
				"select id, DB2GSE.ST_isempty(geom) from geomtest where db2gse.ST_IsEmpty(geom) = 1" );
	}

	@Override
	public NativeSQLStatement createNativeIsNotEmptyStatement() { // return 'not ST_IsEmpty', 'not' is not supported by DB2
		return createNativeSQLStatement(
				"select id, case when DB2GSE.ST_isempty(geom) = 0 then 1 else 0 end from geomtest where db2gse.ST_IsEmpty(geom) = 0" );
	}

	@Override
	public NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_boundary(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_envelope(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_asbinary(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select id, DB2GSE.ST_GeometryType(geom) from geomtest" );
	}

	@Override
	public NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_within(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_within(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_equals(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_equals(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_crosses(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_crosses(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, DB2GSE.ST_contains(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) from GeomTest t where DB2GSE.ST_contains(t.geom, DB2GSE.ST_GeomFromText(?, 4326)) = 1 and db2gse.st_srid(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
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
