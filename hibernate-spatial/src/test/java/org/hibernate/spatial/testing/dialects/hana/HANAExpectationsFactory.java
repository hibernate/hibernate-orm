/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.hana;

import org.hibernate.spatial.dialect.hana.HANASpatialUtils;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.NativeSQLStatement;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.sql.SQLException;
import java.util.Map;

import org.geolatte.geom.jts.JTS;

public class HANAExpectationsFactory extends AbstractExpectationsFactory {

	public HANAExpectationsFactory(DataSourceUtils utils) {
		super( utils );
	}

	@Override
	protected NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_Dimension() from GeomTest t" );
	}

	@Override
	protected NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_Buffer(?) from GeomTest t where t.geom.ST_SRID() = " + getTestSrid(),
				new Object[]{ distance } );
	}

	@Override
	protected NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Union(ST_GeomFromText(?, " + getTestSrid() + ")).ST_ConvexHull().ST_AsEWKB() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Intersection(ST_GeomFromText(?, " + getTestSrid() + ")).ST_AsEWKB() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Difference(ST_GeomFromText(?, " + getTestSrid() + ")).ST_AsEWKB() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_SymDifference(ST_GeomFromText(?, " + getTestSrid() + ")).ST_AsEWKB() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Union(ST_GeomFromText(?, " + getTestSrid() + ")).ST_AsEWKB() from GeomTest t where t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_AsText() from GeomTest t" );
	}

	@Override
	protected NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_SRID() from GeomTest t" );
	}

	@Override
	protected NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_IsSimple() from GeomTest t" );
	}

	@Override
	protected NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_IsEmpty() from GeomTest t" );
	}

	@Override
	protected NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement( "select t.id, map(t.geom.ST_IsEmpty(), 1, 0, 1) from GeomTest t" );
	}

	@Override
	protected NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_Boundary().ST_AsEWKB() from GeomTest t" );
	}

	@Override
	protected NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_Envelope().ST_AsEWKB() from GeomTest t" );
	}

	@Override
	protected NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_AsBinary() from GeomTest t" );
	}

	@Override
	protected NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_GeometryType() from GeomTest t" );
	}

	@Override
	protected Geometry decode(Object o) {
		if ( o == null ) {
			return null;
		}
		return JTS.to( HANASpatialUtils.toGeometry( o ) );
	}

	@Override
	protected NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Within(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Within(ST_GeomFromText(?, "
						+ getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Equals(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Equals(ST_GeomFromText(?, "
						+ getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Crosses(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Crosses(ST_GeomFromText(?, "
						+ getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Contains(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Contains(ST_GeomFromText(?, "
						+ getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Disjoint(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Disjoint(ST_GeomFromText(?, "
						+ getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeTransformStatement(int epsg) {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_Transform(" + epsg + ") from GeomTest t where t.geom.ST_SRID() = " + getTestSrid() );
	}

	@Override
	protected NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement( "select t.id, 1 from GeomTest t where t.geom.ST_SRID() =  " + srid );
	}

	@Override
	protected NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Intersects(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Intersects(ST_GeomFromText(?, "
						+ getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_IntersectsFilter(ST_GeomFromText(?, " + getTestSrid()
						+ ")) from GeomTest t where t.geom.ST_IntersectsFilter(ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = "
						+ getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Touches(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Touches(ST_GeomFromText(?, "
						+ getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Overlaps(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Overlaps(ST_GeomFromText(?, "
						+ getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		String sql = "select t.id, t.geom.ST_Relate(ST_GeomFromText(?, " + getTestSrid() + "), '" + matrix
				+ "' ) from GeomTest t where t.geom.ST_Relate(ST_GeomFromText(?, " + getTestSrid() + "), '" + matrix
				+ "') = 1 and t.geom.ST_SRID() = " + getTestSrid();
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_WithinDistance(ST_GeomFromText(?, " + getTestSrid() + "), "
						+ distance + ") from GeomTest t where t.geom.ST_WithinDistance(ST_GeomFromText(?, " + getTestSrid() + "), " + distance
						+ ") = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	@Override
	protected NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Distance(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
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

	/**
	 * Returns the expected alpha shapes of all testsuite-suite geometries.
	 *
	 * @return map of identifier, alpha shape
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getAlphaShape(double radius) throws SQLException {
		return retrieveExpected( createNativeAlphaShapeStatement( radius ), GEOMETRY );
	}

	private NativeSQLStatement createNativeAlphaShapeStatement(double radius) {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_AlphaShape(?).ST_AsEWKB() from GeomTest t where t.geom.ST_GeometryType() in ('ST_Point', 'ST_MultiPoint')",
				new Object[]{ radius } );
	}

	/**
	 * Returns the expected area of all testsuite-suite geometries.
	 *
	 * @return map of identifier, area
	 * @throws SQLException
	 */
	public Map<Integer, Double> getArea() throws SQLException {
		return retrieveExpected( createNativeAreaStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeAreaStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_Area() from GeomTest t where t.geom.ST_GeometryType() in ('ST_Polygon', 'ST_MultiPolygon')" );
	}

	/**
	 * Returns the expected EWKB representation of all testsuite-suite geometries.
	 *
	 * @return map of identifier, EWKB
	 * @throws SQLException
	 */
	public Map<Integer, byte[]> getAsEWKB() throws SQLException {
		return retrieveExpected( createNativeAsEWKBStatement(), OBJECT );
	}

	private NativeSQLStatement createNativeAsEWKBStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_AsEWKB() from GeomTest t" );
	}

	/**
	 * Returns the expected EWKT representation of all testsuite-suite geometries.
	 *
	 * @return map of identifier, EWKT
	 * @throws SQLException
	 */
	public Map<Integer, String> getAsEWKT() throws SQLException {
		return retrieveExpected( createNativeAsEWKTStatement(), STRING );
	}

	private NativeSQLStatement createNativeAsEWKTStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_AsEWKT() from GeomTest t" );
	}

	/**
	 * Returns the expected GeoJSON representation of all testsuite-suite geometries.
	 *
	 * @return map of identifier, GeoJSON
	 * @throws SQLException
	 */
	public Map<Integer, String> getAsGeoJSON() throws SQLException {
		return retrieveExpected( createNativeAsGeoJSONStatement(), STRING );
	}

	private NativeSQLStatement createNativeAsGeoJSONStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_AsGeoJSON() from GeomTest t" );
	}

	/**
	 * Returns the expected SVG representation of all testsuite-suite geometries.
	 *
	 * @return map of identifier, SVG
	 * @throws SQLException
	 */
	public Map<Integer, String> getAsSVG() throws SQLException {
		return retrieveExpected( createNativeAsSVGStatement(), STRING );
	}

	private NativeSQLStatement createNativeAsSVGStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_AsSVG() from GeomTest t" );
	}

	/**
	 * Returns the expected aggregated SVG representation of all testsuite-suite geometries.
	 *
	 * @return map of count, SVG
	 * @throws SQLException
	 */
	public Map<Integer, String> getAsSVGAggr() throws SQLException {
		return retrieveExpected( createNativeAsSVGAggrStatement(), STRING );
	}

	private NativeSQLStatement createNativeAsSVGAggrStatement() {
		return createNativeSQLStatement( "select cast(count(*) as int), ST_AsSVGAggr(t.geom) from GeomTest t" );
	}

	/**
	 * Returns the expected WKB representation of all testsuite-suite geometries.
	 *
	 * @return map of identifier, WKB
	 * @throws SQLException
	 */
	public Map<Integer, byte[]> getAsWKB() throws SQLException {
		return retrieveExpected( createNativeAsWKBStatement(), OBJECT );
	}

	private NativeSQLStatement createNativeAsWKBStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_AsWKB() from GeomTest t" );
	}

	/**
	 * Returns the expected WKT representation of all testsuite-suite geometries.
	 *
	 * @return map of identifier, WKT
	 * @throws SQLException
	 */
	public Map<Integer, String> getAsWKT() throws SQLException {
		return retrieveExpected( createNativeAsWKTStatement(), STRING );
	}

	private NativeSQLStatement createNativeAsWKTStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_AsWKT() from GeomTest t" );
	}

	/**
	 * Returns the expected centroid of all testsuite-suite geometries.
	 *
	 * @return map of id, centroid
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getCentroid() throws SQLException {
		return retrieveExpected( createNativeCentroidStatement(), GEOMETRY );
	}

	private NativeSQLStatement createNativeCentroidStatement() {
		return createNativeSQLStatement( "select id, t.geom.ST_Centroid() from GeomTest t where t.geom.ST_GeometryType() = 'ST_Polygon'" );
	}

	/**
	 * Returns the expected aggregated convex hull representation of all testsuite-suite geometries.
	 *
	 * @return map of count, convex hull
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getConvexHullAggr() throws SQLException {
		return retrieveExpected( createNativeConvexHullAggrStatement(), GEOMETRY );
	}

	private NativeSQLStatement createNativeConvexHullAggrStatement() {
		return createNativeSQLStatement( "select cast(count(*) as int), ST_ConvexHullAggr(t.geom) from GeomTest t" );
	}

	/**
	 * Returns the expected number of coordinate dimensions of all testsuite-suite geometries.
	 *
	 * @return map of identifier, coordinate dimension
	 * @throws SQLException
	 */
	public Map<Integer, Integer> getCoordDim() throws SQLException {
		return retrieveExpected( createNativeCoordDimStatement(), INTEGER );
	}

	private NativeSQLStatement createNativeCoordDimStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.ST_CoordDim() from GeomTest t" );
	}

	/**
	 * Returns the testsuite-suite geometries that are covered by the given geometry.
	 *
	 * @return map of identifier, whether the geometry is covered
	 * @throws SQLException
	 */
	public Map<Integer, Boolean> getCoveredBy(Geometry geom) throws SQLException {
		return retrieveExpected( createNativeCoveredByStatement( geom ), BOOLEAN );
	}

	private NativeSQLStatement createNativeCoveredByStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_CoveredBy(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_CoveredBy(ST_GeomFromText(?, "
						+ getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	/**
	 * Returns the testsuite-suite geometries that are cover the given geometry.
	 *
	 * @return map of identifier, whether the geometry covers the given geometry
	 * @throws SQLException
	 */
	public Map<Integer, Boolean> getCovers(Geometry geom) throws SQLException {
		return retrieveExpected( createNativeCoversStatement( geom ), BOOLEAN );
	}

	private NativeSQLStatement createNativeCoversStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_Covers(ST_GeomFromText(?, " + getTestSrid() + ")) from GeomTest t where t.geom.ST_Covers(ST_GeomFromText(?, "
						+ getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				geom.toText() );
	}

	/**
	 * Returns the expected endpoint of all testsuite-suite geometries.
	 *
	 * @return map of id, endpoint
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getEndPoint() throws SQLException {
		return retrieveExpected( createNativeEndPointStatement(), GEOMETRY );
	}

	private NativeSQLStatement createNativeEndPointStatement() {
		return createNativeSQLStatement( "select id, t.geom.ST_EndPoint() from GeomTest t where t.geom.ST_GeometryType() = 'ST_LineString'" );
	}

	/**
	 * Returns the expected aggregated bounding rectangle of all testsuite-suite geometries.
	 *
	 * @return map of count, bounding rectangle
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getEnvelopeAggr() throws SQLException {
		return retrieveExpected( createNativeEnvelopeAggrStatement(), GEOMETRY );
	}

	private NativeSQLStatement createNativeEnvelopeAggrStatement() {
		return createNativeSQLStatement( "select cast(count(*) as int), ST_EnvelopeAggr(t.geom) from GeomTest t" );
	}

	/**
	 * Returns the expected exterior ring of all testsuite-suite geometries.
	 *
	 * @return map of id, exterior ring
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getExteriorRing() throws SQLException {
		return retrieveExpected( createNativeExteriorRingStatement(), GEOMETRY );
	}

	private NativeSQLStatement createNativeExteriorRingStatement() {
		return createNativeSQLStatement( "select id, t.geom.ST_ExteriorRing() from GeomTest t where t.geom.ST_GeometryType() = 'ST_Polygon'" );
	}

	/**
	 * Returns the geometry from an EWKB representation.
	 *
	 * @return map of id, geometry
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getGeomFromEWKB(byte[] ewkb) throws SQLException {
		return retrieveExpected( createNativeGeomFromEWKBStatement( ewkb ), GEOMETRY );
	}

	private NativeSQLStatement createNativeGeomFromEWKBStatement(byte[] ewkb) {
		return createNativeSQLStatement( "select 1, ST_GeomFromEWKB(?) from GeomTest t", new Object[]{ ewkb } );
	}

	/**
	 * Returns the geometry from an EWKT representation.
	 *
	 * @return map of id, geometry
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getGeomFromEWKT(String ewkt) throws SQLException {
		return retrieveExpected( createNativeGeomFromEWKTStatement( ewkt ), GEOMETRY );
	}

	private NativeSQLStatement createNativeGeomFromEWKTStatement(String ewkt) {
		return createNativeSQLStatement( "select 1, ST_GeomFromEWKT(?) from GeomTest t", new Object[]{ ewkt } );
	}

	/**
	 * Returns the geometry from a text representation.
	 *
	 * @return map of id, geometry
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getGeomFromText(String text) throws SQLException {
		return retrieveExpected( createNativeGeomFromTextStatement( text ), GEOMETRY );
	}

	private NativeSQLStatement createNativeGeomFromTextStatement(String text) {
		return createNativeSQLStatement( "select 1, ST_GeomFromText(?) from GeomTest t", new Object[]{ text } );
	}

	/**
	 * Returns the geometry from a WKB representation.
	 *
	 * @return map of id, geometry
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getGeomFromWKB(byte[] wkb) throws SQLException {
		return retrieveExpected( createNativeGeomFromWKBStatement( wkb ), GEOMETRY );
	}

	private NativeSQLStatement createNativeGeomFromWKBStatement(byte[] wkb) {
		return createNativeSQLStatement( "select 1, ST_GeomFromWKB(?) from GeomTest t", new Object[]{ wkb } );
	}

	/**
	 * Returns the geometry from a WKT representation.
	 *
	 * @return map of id, geometry
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getGeomFromWKT(String wkt) throws SQLException {
		return retrieveExpected( createNativeGeomFromWKTStatement( wkt ), GEOMETRY );
	}

	private NativeSQLStatement createNativeGeomFromWKTStatement(String wkt) {
		return createNativeSQLStatement( "select 1, ST_GeomFromWKT(?) from GeomTest t", new Object[]{ wkt } );
	}

	/**
	 * Returns the expected nth geometry of all testsuite-suite geometries.
	 *
	 * @return map of id, geometry
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getGeometryN(int n) throws SQLException {
		return retrieveExpected( createNativeGeometryNStatement( n ), GEOMETRY );
	}

	private NativeSQLStatement createNativeGeometryNStatement(int n) {
		return createNativeSQLStatement( "select id, t.geom.ST_GeometryN(?) from GeomTest t where t.geom.ST_GeometryType() = 'ST_GeometryCollection'",
				new Object[]{ n } );
	}

	/**
	 * Returns the expected nth interior ring of all testsuite-suite geometries.
	 *
	 * @return map of id, interior ring
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getInteriorRingN(int n) throws SQLException {
		return retrieveExpected( createNativeInteriorRingNStatement( n ), GEOMETRY );
	}

	private NativeSQLStatement createNativeInteriorRingNStatement(int n) {
		return createNativeSQLStatement( "select id, t.geom.ST_InteriorRingN(?) from GeomTest t where t.geom.ST_GeometryType() = 'ST_Polygon'",
				new Object[]{ n } );
	}

	/**
	 * Returns the expected aggregated intersection of all testsuite-suite geometries.
	 *
	 * @return map of count, intersection
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getIntersectionAggr() throws SQLException {
		return retrieveExpected( createNativeIntersectionAggrStatement(), GEOMETRY );
	}

	private NativeSQLStatement createNativeIntersectionAggrStatement() {
		return createNativeSQLStatement( "select cast(count(*) as int), ST_IntersectionAggr(t.geom) from GeomTest t" );
	}

	/**
	 * Returns the testsuite-suite geometries that intersect the given rectangle.
	 *
	 * @return map of identifier, whether the geometry intersects the given rectangle
	 * @throws SQLException
	 */
	public Map<Integer, Boolean> getIntersectsRect(Point pmin, Point pmax) throws SQLException {
		return retrieveExpected( createNativeIntersectsRectStatement( pmin, pmax ), BOOLEAN );
	}

	private NativeSQLStatement createNativeIntersectsRectStatement(Point pmin, Point pmax) {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_IntersectsRect(ST_GeomFromText(?, " + getTestSrid() + "), ST_GeomFromText(?, " + getTestSrid()
						+ ")) from GeomTest t where t.geom.ST_IntersectsRect(ST_GeomFromText(?, "
						+ getTestSrid() + "), ST_GeomFromText(?, " + getTestSrid() + ")) = 1 and t.geom.ST_SRID() = " + getTestSrid(),
				new Object[]{ pmin.toText(), pmax.toText(), pmin.toText(), pmax.toText() } );
	}

	/**
	 * Returns the testsuite-suite geometries that are 3D geometries.
	 *
	 * @return map of identifier, whether the geometry is 3D
	 * @throws SQLException
	 */
	public Map<Integer, Boolean> getIs3D() throws SQLException {
		return retrieveExpected( createNativeIs3DStatement(), BOOLEAN );
	}

	private NativeSQLStatement createNativeIs3DStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_Is3D() from GeomTest t where t.geom.ST_Is3D() = 1 and t.geom.ST_SRID() = " + getTestSrid() );
	}

	/**
	 * Returns the testsuite-suite geometries that are closed.
	 *
	 * @return map of identifier, whether the geometry is closed
	 * @throws SQLException
	 */
	public Map<Integer, Boolean> getIsClosed() throws SQLException {
		return retrieveExpected( createNativeIsClosedStatement(), BOOLEAN );
	}

	private NativeSQLStatement createNativeIsClosedStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_IsClosed() from GeomTest t where t.geom.ST_GeometryType() in ('ST_LineString', 'ST_MultiLineString') and t.geom.ST_IsClosed() = 1 and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the testsuite-suite geometries that are measured.
	 *
	 * @return map of identifier, whether the geometry is measured
	 * @throws SQLException
	 */
	public Map<Integer, Boolean> getIsMeasured() throws SQLException {
		return retrieveExpected( createNativeIsMeasuredStatement(), BOOLEAN );
	}

	private NativeSQLStatement createNativeIsMeasuredStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_IsMeasured() from GeomTest t where t.geom.ST_IsMeasured() = 1 and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the testsuite-suite geometries that are rings.
	 *
	 * @return map of identifier, whether the geometry is a ring
	 * @throws SQLException
	 */
	public Map<Integer, Boolean> getIsRing() throws SQLException {
		return retrieveExpected( createNativeIsRingStatement(), BOOLEAN );
	}

	private NativeSQLStatement createNativeIsRingStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_IsRing() from GeomTest t where t.geom.ST_GeometryType() in ('ST_LineString') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the testsuite-suite geometries that are valid.
	 *
	 * @return map of identifier, whether the geometry is valid
	 * @throws SQLException
	 */
	public Map<Integer, Boolean> getIsValid() throws SQLException {
		return retrieveExpected( createNativeIsValidStatement(), BOOLEAN );
	}

	private NativeSQLStatement createNativeIsValidStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_IsValid() from GeomTest t where t.geom.ST_IsValid() = 1 and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the length of all testsuite-suite geometries.
	 *
	 * @return map of identifier, length
	 * @throws SQLException
	 */
	public Map<Integer, Double> getLength() throws SQLException {
		return retrieveExpected( createNativeLengthStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeLengthStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_Length() from GeomTest t where t.geom.ST_GeometryType() in ('ST_LineString', 'ST_MultiLineString') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the measure value of all testsuite-suite geometries.
	 *
	 * @return map of identifier, measure value
	 * @throws SQLException
	 */
	public Map<Integer, Double> getM() throws SQLException {
		return retrieveExpected( createNativeMStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeMStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_M() from GeomTest t where t.geom.ST_GeometryType() in ('ST_Point') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the maximum measure value of all testsuite-suite geometries.
	 *
	 * @return map of identifier, maximum measure value
	 * @throws SQLException
	 */
	public Map<Integer, Double> getMMax() throws SQLException {
		return retrieveExpected( createNativeMMaxStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeMMaxStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_MMax() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the minimum measure value of all testsuite-suite geometries.
	 *
	 * @return map of identifier, minimum measure value
	 * @throws SQLException
	 */
	public Map<Integer, Double> getMMin() throws SQLException {
		return retrieveExpected( createNativeMMinStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeMMinStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_MMin() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the number of geometries of all testsuite-suite geometries.
	 *
	 * @return map of identifier, number of geometries
	 * @throws SQLException
	 */
	public Map<Integer, Integer> getNumGeometries() throws SQLException {
		return retrieveExpected( createNativeNumGeometriesStatement(), INTEGER );
	}

	private NativeSQLStatement createNativeNumGeometriesStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_NumGeometries() from GeomTest t where (t.geom.ST_GeometryType() in ('ST_GeometryCollection')) and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the number of interior rings of all testsuite-suite geometries.
	 *
	 * @return map of identifier, number of interior rings
	 * @throws SQLException
	 */
	public Map<Integer, Integer> getNumInteriorRing() throws SQLException {
		return retrieveExpected( createNativeNumInteriorRingStatement(), INTEGER );
	}

	private NativeSQLStatement createNativeNumInteriorRingStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_NumInteriorRing() from GeomTest t where t.geom.ST_GeometryType() in ('ST_Polygon') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the number of interior rings of all testsuite-suite geometries.
	 *
	 * @return map of identifier, number of interior rings
	 * @throws SQLException
	 */
	public Map<Integer, Integer> getNumInteriorRings() throws SQLException {
		return retrieveExpected( createNativeNumInteriorRingsStatement(), INTEGER );
	}

	private NativeSQLStatement createNativeNumInteriorRingsStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_NumInteriorRings() from GeomTest t where t.geom.ST_GeometryType() in ('ST_Polygon') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the number of points of all testsuite-suite geometries.
	 *
	 * @return map of identifier, number of points
	 * @throws SQLException
	 */
	public Map<Integer, Integer> getNumPoints() throws SQLException {
		return retrieveExpected( createNativeNumPointsStatement(), INTEGER );
	}

	private NativeSQLStatement createNativeNumPointsStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_NumPoints() from GeomTest t where t.geom.ST_GeometryType() in ('ST_LineString') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the testsuite-suite geometries that are equal.
	 *
	 * @return map of identifier, whether the geometry is equal
	 * @throws SQLException
	 */
	public Map<Integer, Boolean> getOrderingEquals(Geometry geom) throws SQLException {
		return retrieveExpected( createNativeOrderingEqualsStatement( geom ), BOOLEAN );
	}

	private NativeSQLStatement createNativeOrderingEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.ST_OrderingEquals(ST_GeomFromText(?)) from GeomTest t where t.geom.ST_OrderingEquals(ST_GeomFromText(?)) = 1 and t.geom.ST_SRID() = "
						+ getTestSrid(),
				geom.toText() );
	}

	/**
	 * Returns the perimeter of all testsuite-suite geometries.
	 *
	 * @return map of identifier, perimeter
	 * @throws SQLException
	 */
	public Map<Integer, Double> getPerimeter() throws SQLException {
		return retrieveExpected( createNativePerimeterStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativePerimeterStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_Perimeter() from GeomTest t where t.geom.ST_GeometryType() in ('ST_Polygon', 'ST_MultiPolygon') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns a point on the surface of all testsuite-suite geometries.
	 *
	 * @return map of identifier, point on surface
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getPointOnSurface() throws SQLException {
		return retrieveExpected( createNativePointOnSurfaceStatement(), GEOMETRY );
	}

	private NativeSQLStatement createNativePointOnSurfaceStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_PointOnSurface() from GeomTest t where t.geom.ST_GeometryType() in ('ST_Polygon', 'ST_MultiPolygon') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the nth point of all testsuite-suite geometries.
	 *
	 * @return map of identifier, point
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getPointN(int n) throws SQLException {
		return retrieveExpected( createNativePointNStatement( n ), GEOMETRY );
	}

	private NativeSQLStatement createNativePointNStatement(int n) {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_PointN(?) from GeomTest t where t.geom.ST_GeometryType() in ('ST_LineString') and t.geom.ST_SRID() = "
						+ getTestSrid(),
				new Object[]{ n } );
	}

	/**
	 * Returns a copy of all testsuite-suite geometries with all points snapped to the grid.
	 *
	 * @return map of identifier, geometry
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getSnapToGrid() throws SQLException {
		return retrieveExpected( createNativeSnapToGridStatement(), GEOMETRY );
	}

	private NativeSQLStatement createNativeSnapToGridStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_SnapToGrid() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the expected startpoint of all testsuite-suite geometries.
	 *
	 * @return map of id, startpoint
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getStartPoint() throws SQLException {
		return retrieveExpected( createNativeStartPointStatement(), GEOMETRY );
	}

	private NativeSQLStatement createNativeStartPointStatement() {
		return createNativeSQLStatement( "select id, t.geom.ST_StartPoint() from GeomTest t where t.geom.ST_GeometryType() = 'ST_LineString'" );
	}

	/**
	 * Returns the expected aggregated union of all testsuite-suite geometries.
	 *
	 * @return map of count, union
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getUnionAggr() throws SQLException {
		return retrieveExpected( createNativeUnionAggrStatement(), GEOMETRY );
	}

	private NativeSQLStatement createNativeUnionAggrStatement() {
		return createNativeSQLStatement( "select cast(count(*) as int), ST_UnionAggr(t.geom) from GeomTest t" );
	}

	/**
	 * Returns the x coordinate of all testsuite-suite geometries.
	 *
	 * @return map of identifier, x coordinate
	 * @throws SQLException
	 */
	public Map<Integer, Double> getX() throws SQLException {
		return retrieveExpected( createNativeXStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeXStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_X() from GeomTest t where t.geom.ST_GeometryType() in ('ST_Point') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the maximum x coordinate of all testsuite-suite geometries.
	 *
	 * @return map of identifier, maximum x coordinate
	 * @throws SQLException
	 */
	public Map<Integer, Double> getXMax() throws SQLException {
		return retrieveExpected( createNativeXMaxStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeXMaxStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_XMax() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the minimum x coordinate of all testsuite-suite geometries.
	 *
	 * @return map of identifier, minumum x coordinate
	 * @throws SQLException
	 */
	public Map<Integer, Double> getXMin() throws SQLException {
		return retrieveExpected( createNativeXMinStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeXMinStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_XMin() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the y coordinate of all testsuite-suite geometries.
	 *
	 * @return map of identifier, y coordinate
	 * @throws SQLException
	 */
	public Map<Integer, Double> getY() throws SQLException {
		return retrieveExpected( createNativeYStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeYStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_Y() from GeomTest t where t.geom.ST_GeometryType() in ('ST_Point') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the maximum y coordinate of all testsuite-suite geometries.
	 *
	 * @return map of identifier, maximum y coordinate
	 * @throws SQLException
	 */
	public Map<Integer, Double> getYMax() throws SQLException {
		return retrieveExpected( createNativeYMaxStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeYMaxStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_YMax() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the minimum y coordinate of all testsuite-suite geometries.
	 *
	 * @return map of identifier, minumum y coordinate
	 * @throws SQLException
	 */
	public Map<Integer, Double> getYMin() throws SQLException {
		return retrieveExpected( createNativeYMinStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeYMinStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_YMin() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the z coordinate of all testsuite-suite geometries.
	 *
	 * @return map of identifier, z coordinate
	 * @throws SQLException
	 */
	public Map<Integer, Double> getZ() throws SQLException {
		return retrieveExpected( createNativeZStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeZStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_Z() from GeomTest t where t.geom.ST_GeometryType() in ('ST_Point') and t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the maximum z coordinate of all testsuite-suite geometries.
	 *
	 * @return map of identifier, maximum z coordinate
	 * @throws SQLException
	 */
	public Map<Integer, Double> getZMax() throws SQLException {
		return retrieveExpected( createNativeZMaxStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeZMaxStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_ZMax() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the minimum z coordinate of all testsuite-suite geometries.
	 *
	 * @return map of identifier, minumum z coordinate
	 * @throws SQLException
	 */
	public Map<Integer, Double> getZMin() throws SQLException {
		return retrieveExpected( createNativeZMinStatement(), DOUBLE );
	}

	private NativeSQLStatement createNativeZMinStatement() {
		return createNativeSQLStatement(
				"select t.id, t.geom.ST_ZMin() from GeomTest t where t.geom.ST_SRID() = "
						+ getTestSrid() );
	}

	/**
	 * Returns the result of a nested function call with a parameter inside the inner function
	 *
	 * @return map of identifier, geometry
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getNestedFunctionInner(Geometry geom) throws SQLException {
		return retrieveExpected( createNativeNestedFunctionInnerStatement( geom ), GEOMETRY );
	}

	private NativeSQLStatement createNativeNestedFunctionInnerStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom from GeomTest t where t.geom.ST_WithinDistance(ST_GeomFromText(?, " + getTestSrid()
						+ ").ST_SRID(0), 1) = 1",
				geom.toText() );
	}

	/**
	 * Returns the result of a nested function call with a parameter inside the outer function
	 *
	 * @return map of identifier, geometry
	 * @throws SQLException
	 */
	public Map<Integer, Geometry> getNestedFunctionOuter(Geometry geom) throws SQLException {
		return retrieveExpected( createNativeNestedFunctionOuterStatement( geom ), GEOMETRY );
	}

	private NativeSQLStatement createNativeNestedFunctionOuterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom from GeomTest t where ST_GeomFromText(?, " + getTestSrid() + ").ST_WithinDistance(geom.ST_SRID(0), 1) = 1",
				geom.toText() );
	}
}
