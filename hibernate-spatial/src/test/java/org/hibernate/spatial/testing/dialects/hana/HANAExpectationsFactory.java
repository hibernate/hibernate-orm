/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.hana;

import org.geolatte.geom.jts.JTS;
import org.hibernate.spatial.dialect.hana.HANASpatialUtils;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.NativeSQLStatement;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class HANAExpectationsFactory extends AbstractExpectationsFactory {

	public HANAExpectationsFactory(DataSourceUtils utils) {
		super(utils);
	}

	@Override
	protected NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement("select t.id, t.geom.ST_Dimension() from GeomTest t");
	}

	@Override
	protected NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_Buffer(?) from GeomTest t where t.geom.ST_SRID() = " + getTestSrid(),
				new Object[] { distance });
	}

	@Override
	protected NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Union(ST_GeomFromText(?, " + getTestSrid() + ")).ST_ConvexHull().ST_AsEWKB() from GeomTest t where t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Intersection(ST_GeomFromText(?, " + getTestSrid() + ")).ST_AsEWKB() from GeomTest t where t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Difference(ST_GeomFromText(?, " + getTestSrid() + ")).ST_AsEWKB() from GeomTest t where t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_SymDifference(ST_GeomFromText(?, " + getTestSrid() + ")).ST_AsEWKB() from GeomTest t where t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Union(ST_GeomFromText(?, " + getTestSrid() + ")).ST_AsEWKB() from GeomTest t where t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement("select t.id, t.geom.ST_AsText() from GeomTest t");
	}

	@Override
	protected NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement("select t.id, t.geom.ST_SRID() from GeomTest t");
	}

	@Override
	protected NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement("select t.id, t.geom.ST_IsSimple() from GeomTest t");
	}

	@Override
	protected NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement("select t.id, t.geom.ST_IsEmpty() from GeomTest t");
	}

	@Override
	protected NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement("select t.id, map(t.geom.ST_IsEmpty(), 1, 0, 1) from GeomTest t");
	}

	@Override
	protected NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement("select t.id, t.geom.ST_Boundary().ST_AsEWKB() from GeomTest t");
	}

	@Override
	protected NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement("select t.id, t.geom.ST_Envelope().ST_AsEWKB() from GeomTest t");
	}

	@Override
	protected NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement("select t.id, t.geom.ST_AsBinary() from GeomTest t");
	}

	@Override
	protected NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement("select t.id, t.geom.ST_GeometryType() from GeomTest t");
	}

	@Override
	protected Geometry decode(Object o) {
		if (o == null) {
			return null;
		}
		return JTS.to(HANASpatialUtils.toGeometry(o));
	}

	@Override
	protected NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Within(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Within(ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Equals(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Equals(ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Crosses(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Crosses(ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Contains(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Contains(ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Disjoint(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Disjoint(ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeTransformStatement(int epsg) {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_Transform(" + epsg + ") from GeomTest t where t.geom.ST_SRID() = " + getTestSrid());
	}

	@Override
	protected NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement("select t.id, 1 from GeomTest t where t.geom.ST_SRID() =  " + srid);
	}

	@Override
	protected NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Intersects(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Intersects(ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_IntersectsFilter(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_IntersectsFilter(ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Touches(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Touches(ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Overlaps(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Overlaps(ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		String sql = "select t.id, t.geom.ST_Relate(ST_GeomFromText(?, " + getTestSrid() + "), '" + matrix
				+ "' ) from GeomTest t where t.geom.ST_Relate(ST_GeomFromText(?, " + getTestSrid() + "), '" + matrix
				+ "') = 1 and t.geom.ST_SRID() = " + getTestSrid();
		return createNativeSQLStatementAllWKTParams(sql, geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		return createNativeSQLStatementAllWKTParams("select t.id, t.geom.ST_WithinDistance(ST_GeomFromText(?, " + getTestSrid() + "), "
				+ distance + ") from GeomTest t where t.geom.ST_WithinDistance(ST_GeomFromText(?, " + getTestSrid() + "), " + distance
				+ ") = 1 and t.geom.ST_SRID() = " + getTestSrid(), geom.toText());
	}

	@Override
	protected NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Distance(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText());
	}
	
	@Override
	public Polygon getTestPolygon() {
		WKTReader reader = new WKTReader();
		try {
			Polygon polygon = (Polygon) reader.read( "POLYGON((0 0, 50 0, 90 90, 100 0, 0 0))" );
			polygon.setSRID( getTestSrid() );
			return polygon;
		}
		catch (ParseException e) {
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public int getTestSrid() {
		return 0;
	}
	
}
