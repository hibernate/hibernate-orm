/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.spatial.HSMessageLogger;

import org.jboss.logging.Logger;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * An <code>AbstractExpectationsFactory</code> provides the expected
 * values to be used in the integration tests of the spatial functions
 * provided by specific providers.
 * <p>
 * The expected values are returned as a map of (identifier, expected value) pairs.
 *
 * @author Karel Maesen, Geovise BVBA
 * @deprecated Will be removed once we have all the Native SQL templates collected in the NativeSQLTemplates
 */
@Deprecated
public abstract class AbstractExpectationsFactory {

	public final static String TEST_POLYGON_WKT = "POLYGON((0 0, 50 0, 100 100, 0 100, 0 0))";
	public final static String TEST_POINT_WKT = "POINT(0 0)";
	public final static int INTEGER = 1;
	public final static int DOUBLE = 2;
	public final static int GEOMETRY = 3;
	public final static int STRING = 4;
	public final static int BOOLEAN = 5;
	public final static int OBJECT = -1;
	private static final HSMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			HSMessageLogger.class,
			AbstractExpectationsFactory.class.getName()
	);
	private final static int TEST_SRID = 4326;
	private static final int MAX_BYTE_LEN = 1024;


	public AbstractExpectationsFactory() {
	}

	/**
	 * Returns the SRID in which all tests are conducted. This is for now 4326;
	 *
	 * @return
	 */
	public int getTestSrid() {
		return TEST_SRID;
	}


	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, touches(geom, :filter) from GeomEntity where touches(geom, :filter) = true and srid(geom) = 4326"
	 *
	 * @param geom the geometry corresponding to the ':filter' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeTouchesStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, overlaps(geom, :filter) from GeomEntity where overlaps(geom, :filter) = true and srid(geom) = 4326"
	 *
	 * @param geom the geometry corresponding to the ':filter' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeOverlapsStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, relate(geom, :filter, :matrix) from GeomEntity where relate(geom, :filter, :matrix) = true and srid(geom) = 4326"
	 *
	 * @param geom the geometry corresponding to the ':filter' query parameter
	 * @param matrix the string corresponding to the ':matrix' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, dwithin(geom, :filter, :distance) from GeomEntity where dwithin(geom, :filter, :distance) = true and srid(geom) = 4326"
	 *
	 * @param geom the geometry corresponding to the ':filter' query parameter
	 * @param distance the string corresponding to the ':distance' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeDwithinStatement(Point geom, double distance);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, intersects(geom, :filter) from GeomEntity where intersects(geom, :filter) = true and srid(geom) = 4326"
	 *
	 * @param geom the geometry corresponding to the ':filter' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeIntersectsStatement(Geometry geom);

	/**
	 * Returns the statement corresponding to the SpatialRestrictions.filter() method.
	 *
	 * @param geom filter geometry
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeFilterStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, distance(geom, :filter) from GeomEntity where srid(geom) = 4326"
	 *
	 * @param geom
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeDistanceStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "select id, dimension(geom) from GeomEntity".
	 *
	 * @return the SQL String
	 */
	public abstract NativeSQLStatement createNativeDimensionSQL();

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, buffer(geom, :distance) from GeomEntity where srid(geom) = 4326"
	 *
	 * @param distance parameter corresponding to the ':distance' query parameter
	 *
	 * @return the native SQL Statement
	 */
	public abstract NativeSQLStatement createNativeBufferStatement(Double distance);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, convexhull(geomunion(geom, :polygon)) from GeomEntity where srid(geom) = 4326"
	 *
	 * @param geom parameter corresponding to the ':polygon' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeConvexHullStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, intersection(geom, :polygon) from GeomEntity where srid(geom) = 4326"
	 *
	 * @param geom parameter corresponding to the ':polygon' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeIntersectionStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, difference(geom, :polygon) from GeomEntity where srid(geom) = 4326"26"
	 *
	 * @param geom parameter corresponding to the ':polygon' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeDifferenceStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, symdifference(geom, :polygon) from GeomEntity where srid(geom) = 4326"26"
	 *
	 * @param geom parameter corresponding to the ':polygon' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, geomunion(geom, :polygon) from GeomEntity where srid(geom) = 4326"26"
	 *
	 * @param geom parameter corresponding to the ':polygon' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeGeomUnionStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "select id, astext(geom) from GeomEntity".
	 *
	 * @return the native SQL Statement
	 */
	public abstract NativeSQLStatement createNativeAsTextStatement();


	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "select id, srid(geom) from GeomEntity".
	 *
	 * @return the native SQL Statement
	 */
	public abstract NativeSQLStatement createNativeSridStatement();

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "select id, issimple(geom) from GeomEntity".
	 *
	 * @return the native SQL Statement
	 */
	public abstract NativeSQLStatement createNativeIsSimpleStatement();

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "select id, isempty(geom) from GeomEntity".
	 *
	 * @return the native SQL Statement
	 */
	public abstract NativeSQLStatement createNativeIsEmptyStatement();


	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "select id,not isempty(geom) from GeomEntity".
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeIsNotEmptyStatement();

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "select id, boundary(geom) from GeomEntity".
	 *
	 * @return the native SQL Statement
	 */
	public abstract NativeSQLStatement createNativeBoundaryStatement();

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "select id, envelope(geom) from GeomEntity".
	 *
	 * @return the native SQL Statement
	 */
	public abstract NativeSQLStatement createNativeEnvelopeStatement();

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "select id, asbinary(geom) from GeomEntity".
	 *
	 * @return the native SQL Statement
	 */
	public abstract NativeSQLStatement createNativeAsBinaryStatement();

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "select id, geometrytype(geom) from GeomEntity".
	 *
	 * @return the SQL String
	 */
	public abstract NativeSQLStatement createNativeGeometryTypeStatement();

	/**
	 * Returns a statement corresponding to the HQL statement
	 * "SELECT id, within(geom, :filter) from GeomEntity where within(geom, :filter) = true and srid(geom) = 4326"
	 *
	 * @param testPolygon the geometry corresponding to the ':filter' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeWithinStatement(Geometry testPolygon);

	/**
	 * Returns a statement corresponding to the HQL statement
	 * "SELECT id, equals(geom, :filter) from GeomEntity where equals(geom, :filter) = true and srid(geom) = 4326"
	 *
	 * @param geom the geometry corresponding to the ':filter' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeEqualsStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement
	 * "SELECT id, crosses(geom, :filter) from GeomEntity where crosses(geom, :filter) = true and srid(geom) = 4326"
	 *
	 * @param geom the geometry corresponding to the ':filter' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeCrossesStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement:
	 * "SELECT id, contains(geom, :filter) from GeomEntity where contains(geom, :geom) = true and srid(geom) = 4326";
	 *
	 * @param geom
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeContainsStatement(Geometry geom);

	/**
	 * Returns a statement corresponding to the HQL statement
	 * "SELECT id, disjoint(geom, :filter) from GeomEntity where disjoint(geom, :filter) = true and srid(geom) = 4326"
	 *
	 * @param geom the geometry corresponding to the ':filter' query parameter
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeDisjointStatement(Geometry geom);


	/**
	 * Returns a statement corresponding to the HQL statement
	 * "SELECT id, transform(geom, :epsg) from GeomEntity where srid(geom) = 4326"
	 *
	 * @param epsg - the EPSG code of the target projection system.
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeTransformStatement(int epsg);

	/**
	 * Returns the statement corresponding to the HQL statement
	 * "select id, (srid(geom) = :epsg) from GeomEntity where srid(geom) = :epsg ";
	 *
	 * @param srid
	 *
	 * @return
	 */
	public abstract NativeSQLStatement createNativeHavingSRIDStatement(int srid);

	/**
	 * Decodes a native database object to a JTS <code>Geometry</code> instance
	 *
	 * @param o native database object
	 *
	 * @return decoded geometry
	 */
	protected abstract Geometry decode(Object o);

	/**
	 * Return a testsuite-suite polygon (filter, ...)
	 *
	 * @return a testsuite-suite polygon
	 */
	public Polygon getTestPolygon() {
		WKTReader reader = new WKTReader();
		try {
			Polygon polygon = (Polygon) reader.read( TEST_POLYGON_WKT );
			polygon.setSRID( getTestSrid() );
			return polygon;
		}
		catch (ParseException e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Return a testsuite-suite point (filter, ...)
	 *
	 * @return a testsuite-suite point
	 */
	public Point getTestPoint() {
		WKTReader reader = new WKTReader();
		try {
			Point point = (Point) reader.read( TEST_POINT_WKT );
			point.setSRID( getTestSrid() );
			return point;
		}
		catch (ParseException e) {
			throw new RuntimeException( e );
		}
	}


	public NativeSQLStatement createNativeSQSStatement(final NativeSQLTemplate template, final String table) {
		return createNativeSQLStatement( template.mkNativeSQLString( table ) );
	}

	public NativeSQLStatement createNativeSQLStatement(final String sql) {
		return new NativeSQLStatement() {
			public PreparedStatement prepare(Connection connection) throws SQLException {
				return connection.prepareStatement( sql );
			}

			public String toString() {
				return sql;
			}
		};
	}

	public NativeSQLStatement createNativeSQLStatementAllWKTParams(final String sql, final String wkt) {
		return new NativeSQLStatement() {
			public PreparedStatement prepare(Connection connection) throws SQLException {
				PreparedStatement pstmt = connection.prepareStatement( sql );
				for ( int i = 1; i <= numPlaceHoldersInSQL( sql ); i++ ) {
					pstmt.setString( i, wkt );
				}
				return pstmt;
			}

			public String toString() {
				return String.format( "sql; %s, wkt: %s", sql, wkt );
			}
		};
	}

	public NativeSQLStatement createNativeSQLStatement(final String sql, final Object[] params) {
		return new NativeSQLStatement() {
			public PreparedStatement prepare(Connection connection) throws SQLException {
				PreparedStatement pstmt = connection.prepareStatement( sql );
				int i = 1;
				for ( Object param : params ) {
					pstmt.setObject( i++, param );
				}
				return pstmt;
			}

			public String toString() {
				return sql;
			}
		};
	}


	protected int numPlaceHoldersInSQL(String sql) {
		return sql.replaceAll( "[^?]", "" ).length();
	}


}
