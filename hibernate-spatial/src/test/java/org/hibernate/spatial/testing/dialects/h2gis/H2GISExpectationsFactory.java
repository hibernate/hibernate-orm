/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.h2gis;

import org.hibernate.spatial.dialect.h2gis.H2GISWkb;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.NativeSQLStatement;

import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;


/**
 * A Factory class that generates expected {@link NativeSQLStatement}s for
 * GeoDB.
 *
 * @Author Jan Boonen, Geodan IT b.v.
 */
public class H2GISExpectationsFactory extends AbstractExpectationsFactory {

	public H2GISExpectationsFactory() {
		super();
	}

	@Override
	public NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select id, ST_AsEWKB(geom) from GEOMTEST" );
	}

	@Override
	public NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select id, ST_AsText(geom) from GEOMTEST" );
	}

	@Override
	public NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement( "select id, ST_Boundary(geom) from GEOMTEST" );
	}

	@Override
	public NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, ST_Buffer(t.geom,?) from GEOMTEST t where ST_SRID(t.geom) = 4326",
				new Object[] { distance }
		);
	}

	@Override
	public NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Contains(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Contains(t.geom, ST_GeomFromText(?, 4326)) = 1 and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_ConvexHull(ST_Union(t.geom, ST_GeomFromText(?, 4326))) from GeomTest t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Crosses(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Crosses(t.geom, ST_GeomFromText(?, 4326)) = 1 and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		throw new UnsupportedOperationException(
				"Method ST_Difference() is not implemented in the current version of GeoDB."
		);
	}

	@Override
	public NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement( "select id, ST_Dimension(geom) from GEOMTEST" );
	}

	@Override
	public NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Disjoint(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Disjoint(t.geom, ST_GeomFromText(?, 4326)) = 1 and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeTransformStatement(int epsg) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement(
				"select t.id, (ST_SRID(t.geom) = "
						+ srid + ") from GeomTest t where ST_SRID(t.geom) =  " + srid
		);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeDistanceStatement(org.locationtech.jts.geom.Geometry)
	 */

	@Override
	public NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, st_distance(t.geom, ST_GeomFromText(?, 4326)) from GeomTest t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeEnvelopeStatement()
	 */

	@Override
	public NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select id, ST_Envelope(geom) from GEOMTEST" );
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeEqualsStatement(org.locationtech.jts.geom.Geometry)
	 */

	@Override
	public NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Equals(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Equals(t.geom, ST_GeomFromText(?, 4326)) = 1 and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeFilterStatement(org.locationtech.jts.geom.Geometry)
	 */

	@Override
	public NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		throw new UnsupportedOperationException(
				"Filter is not implemented in the current version of GeoDB."
		);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeGeomUnionStatement(org.locationtech.jts.geom.Geometry)
	 */

	@Override
	public NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Union(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeGeometryTypeStatement()
	 */

	@Override
	public NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select id, GeometryType(geom) from GEOMTEST" );
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeIntersectionStatement(org.locationtech.jts.geom.Geometry)
	 */

	@Override
	public NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Intersection(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeIntersectsStatement(org.locationtech.jts.geom.Geometry)
	 */

	@Override
	public NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Intersects(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Intersects(t.geom, ST_GeomFromText(?, 4326)) = 1 and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeIsEmptyStatement()
	 */

	@Override
	public NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement( "select id, ST_IsEmpty(geom) from GEOMTEST" );
	}

	@Override
	public NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement( "select id, not ST_IsEmpty(geom) from geomtest" );
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeIsSimpleStatement()
	 */

	@Override
	public NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select id, ST_IsSimple(geom) from GEOMTEST" );
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeOverlapsStatement(org.locationtech.jts.geom.Geometry)
	 */

	@Override
	public NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Overlaps(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Overlaps(t.geom, ST_GeomFromText(?, 4326)) = 1 and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeRelateStatement(org.locationtech.jts.geom.Geometry,
	 * java.lang.String)
	 */

	@Override
	public NativeSQLStatement createNativeRelateStatement(
			Geometry geom,
			String matrix) {
		String sql = "select t.id, ST_Relate(t.geom, ST_GeomFromText(?, 4326), '" + matrix + "' ) from GEOMTEST t where ST_Relate(t.geom, ST_GeomFromText(?, 4326), '" + matrix + "') = 'true' and ST_SRID(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	public NativeSQLStatement createNativeDwithinStatement(
			Point geom,
			double distance) {
		String sql = "select t.id, ST_DWithin(t.geom, ST_GeomFromText(?, 4326), "
				+ distance
				+ " ) from GEOMTEST t where st_dwithin(t.geom, ST_GeomFromText(?, 4326), "
				+ distance + ") = 'true' and ST_SRID(t.geom) = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeSridStatement()
	 */

	@Override
	public NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select id, ST_SRID(geom) from GEOMTEST" );
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeSymDifferenceStatement(org.locationtech.jts.geom.Geometry)
	 */

	@Override
	public NativeSQLStatement createNativeSymDifferenceStatement(
			Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_SymDifference(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeTouchesStatement(org.locationtech.jts.geom.Geometry)
	 */

	@Override
	public NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Touches(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Touches(t.geom, ST_GeomFromText(?, 4326)) = 1 and ST_SRID(t.geom) = 4326",
				geom.toText()
		);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.hibernatespatial.test.AbstractExpectationsFactory#
	 * createNativeWithinStatement(org.locationtech.jts.geom.Geometry)
	 */

	@Override
	public NativeSQLStatement createNativeWithinStatement(
			Geometry testPolygon) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, ST_Within(t.geom, ST_GeomFromText(?, 4326)) from GEOMTEST t where ST_Within(t.geom, ST_GeomFromText(?, 4326)) = 1 and ST_SRID(t.geom) = 4326",
				testPolygon.toText()
		);
	}

	@Override
	protected Geometry decode(Object o) {
		return JTS.to( H2GISWkb.from( o ) );
	}

}
