/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

import org.jboss.logging.Logger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.h2geodb.GeoDBDialect;
import org.hibernate.spatial.dialect.oracle.OracleSpatial10gDialect;
import org.hibernate.spatial.testing.SpatialDialectMatcher;
import org.hibernate.spatial.testing.SpatialFunctionalTestCase;

import org.junit.Test;

import org.hibernate.testing.Skip;
import org.hibernate.testing.SkipForDialect;

import static java.lang.String.format;

/**
 * @author Karel Maesen, Geovise BVBA
 */
@Skip(condition = SpatialDialectMatcher.class, message = "No Spatial Dialect")
public class TestSpatialFunctions extends SpatialFunctionalTestCase {

	private static final HSMessageLogger LOG = Logger.getMessageLogger(
			HSMessageLogger.class,
			TestSpatialFunctions.class.getName()
	);

	protected HSMessageLogger getLogger() {
		return LOG;
	}

	@Test
	public void test_dimension_on_jts() throws SQLException {
		dimension( JTS );
	}

	@Test
	public void test_dimension_on_geolatte() throws SQLException {
		dimension( GEOLATTE );
	}

	public void dimension(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.dimension ) ) {
			return;
		}
		Map<Integer, Integer> dbexpected = expectationsFactory.getDimension();
		String hql = format( "SELECT id, dimension(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_astext_on_jts() throws SQLException {
		astext( JTS );
	}

	@Test
	public void test_astext_on_geolatte() throws SQLException {
		astext( GEOLATTE );
	}

	public void astext(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.astext ) ) {
			return;
		}
		Map<Integer, String> dbexpected = expectationsFactory.getAsText();
		String hql = format( "SELECT id, astext(geom) from org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_asbinary_on_jts() throws SQLException {
		asbinary( JTS );
	}

	@Test
	public void test_asbinary_on_geolatte() throws SQLException {
		asbinary( GEOLATTE );
	}


	public void asbinary(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.asbinary ) ) {
			return;
		}
		Map<Integer, byte[]> dbexpected = expectationsFactory.getAsBinary();
		String hql = format( "SELECT id, asbinary(geom) from org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_geometrytype_on_jts() throws SQLException {
		geometrytype( JTS );
	}

	@Test
	public void test_geometrytype_on_geolatte() throws SQLException {
		geometrytype( GEOLATTE );
	}

	public void geometrytype(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.geometrytype ) ) {
			return;
		}
		Map<Integer, String> dbexpected = expectationsFactory.getGeometryType();
		String hql = format(
				"SELECT id, geometrytype(geom) from org.hibernate.spatial.integration.%s.GeomEntity",
				pckg
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_srid_on_jts() throws SQLException {
		srid( JTS );
	}

	@Test
	public void test_srid_on_geolatte() throws SQLException {
		srid( GEOLATTE );
	}

	public void srid(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.srid ) ) {
			return;
		}
		Map<Integer, Integer> dbexpected = expectationsFactory.getSrid();
		String hql = format( "SELECT id, srid(geom) from org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_issimple_on_jts() throws SQLException {
		issimple( JTS );
	}

	@Test
	public void test_issimple_on_geolatte() throws SQLException {
		issimple( GEOLATTE );
	}

	public void issimple(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.issimple ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getIsSimple();
		String hql = format( "SELECT id, issimple(geom) from org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_isempty_on_jts() throws SQLException {
		isempty( JTS );
	}

	@Test
	public void test_isempty_on_geolatte() throws SQLException {
		isempty( GEOLATTE );
	}

	public void isempty(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.isempty ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getIsEmpty();
		String hql = format( "SELECT id, isEmpty(geom) from org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_boundary_on_jts() throws SQLException {
		boundary( JTS );
	}

	@Test
	public void test_boundary_on_geolatte() throws SQLException {
		boundary( GEOLATTE );
	}

	public void boundary(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.boundary ) ) {
			return;
		}
		Map<Integer, Geometry> dbexpected = expectationsFactory.getBoundary();
		String hql = format( "SELECT id, boundary(geom) from org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_envelope_on_jts() throws SQLException {
		envelope( JTS );
	}

	@Test
	public void test_envelope_on_geolatte() throws SQLException {
		envelope( GEOLATTE );
	}

	public void envelope(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.envelope ) ) {
			return;
		}
		Map<Integer, Geometry> dbexpected = expectationsFactory.getEnvelope();
		String hql = format( "SELECT id, envelope(geom) from org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_within_on_jts() throws SQLException {
		within( JTS );
	}

	@Test
	public void test_within_on_geolatte() throws SQLException {
		within( GEOLATTE );
	}

	public void within(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.within ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getWithin( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, within(geom, :filter) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where within(geom, :filter) = true and srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_equals_on_jts() throws SQLException {
		equals( JTS );
	}

	@Test
	public void test_equals_on_geolatte() throws SQLException {
		equals( GEOLATTE );
	}

	public void equals(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.equals ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getEquals( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, equals(geom, :filter) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where equals(geom, :filter) = true and srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_crosses_on_jts() throws SQLException {
		crosses( JTS );
	}

	@Test
	public void test_crosses_on_geolatte() throws SQLException {
		crosses( GEOLATTE );
	}

	public void crosses(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.crosses ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getCrosses( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, crosses(geom, :filter) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where crosses(geom, :filter) = true and srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );

	}

	@Test
	public void test_contains_on_jts() throws SQLException {
		contains( JTS );
	}

	@Test
	public void test_contains_on_geolatte() throws SQLException {
		contains( GEOLATTE );
	}

	public void contains(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.contains ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getContains( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, contains(geom, :filter) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where contains(geom, :filter) = true and srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_disjoint_on_jts() throws SQLException {
		disjoint( JTS );
	}

	@Test
	public void test_disjoint_on_geolatte() throws SQLException {
		disjoint( GEOLATTE );
	}

	public void disjoint(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.disjoint ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getDisjoint( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, disjoint(geom, :filter) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where disjoint(geom, :filter) = true and srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_intersects_on_jts() throws SQLException {
		intersects( JTS );
	}

	@Test
	public void test_intersects_on_geolatte() throws SQLException {
		intersects( GEOLATTE );
	}

	public void intersects(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.intersects ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getIntersects( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, intersects(geom, :filter) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where intersects(geom, :filter) = true and srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_overlaps_on_jts() throws SQLException {
		overlaps( JTS );
	}

	@Test
	public void test_overlaps_on_geolatte() throws SQLException {
		overlaps( GEOLATTE );
	}

	public void overlaps(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.overlaps ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getOverlaps( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, overlaps(geom, :filter) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where overlaps(geom, :filter) = true and srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_touches_on_jts() throws SQLException {
		touches( JTS );
	}

	@Test
	public void test_touches_on_geolatte() throws SQLException {
		touches( GEOLATTE );
	}

	public void touches(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.touches ) ) {
			return;
		}
		String hql = format(
				"SELECT id, touches(geom, :filter) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where touches(geom, :filter) = true and srid(geom) = 4326", pckg
		);
		Map<Integer, Boolean> dbexpected = expectationsFactory.getTouches( expectationsFactory.getTestPolygon() );
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_relate_on_jts() throws SQLException {
		relate( JTS );
	}

	@Test
	public void test_relate_on_geolatte() throws SQLException {
		relate( GEOLATTE );
	}

	public void relate(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.relate ) ) {
			return;
		}
		String matrix = "T*T***T**";
		Map<Integer, Boolean> dbexpected = expectationsFactory.getRelate(
				expectationsFactory.getTestPolygon(),
				matrix
		);
		String hql = format(
				"SELECT id, relate(geom, :filter, :matrix) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where relate(geom, :filter, :matrix) = true and srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		params.put( "matrix", matrix );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );

		matrix = "FF*FF****";
		dbexpected = expectationsFactory.getRelate( expectationsFactory.getTestPolygon(), matrix );
		params.put( "matrix", matrix );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );

	}

	@Test
	public void test_distance_on_jts() throws SQLException {
		distance( JTS );
	}

	@Test
	public void test_distance_on_geolatte() throws SQLException {
		distance( GEOLATTE );
	}

	public void distance(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.distance ) ) {
			return;
		}
		Map<Integer, Double> dbexpected = expectationsFactory.getDistance( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, distance(geom, :filter) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_buffer_on_jts() throws SQLException {
		buffer( JTS );
	}

	@Test
	public void test_buffer_on_geolatte() throws SQLException {
		buffer( GEOLATTE );
	}

	public void buffer(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.buffer ) ) {
			return;
		}
		Map<Integer, Geometry> dbexpected = expectationsFactory.getBuffer( Double.valueOf( 1.0 ) );
		String hql = format(
				"SELECT id, buffer(geom, :distance) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "distance", Double.valueOf( 1.0 ) );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );

	}

	// The convexhull tests are skipped for Oracle because of error:
	// ORA-13276: internal error [Geodetic Geometry too big for Local Transform] in coordinate transformation

	@Test
	@SkipForDialect(value = OracleSpatial10gDialect.class)
	public void test_convexhull_on_jts() throws SQLException {
		convexhull( JTS );
	}


	@Test
	@SkipForDialect(value = OracleSpatial10gDialect.class)
	public void test_convexhull_on_geolatte() throws SQLException {
		convexhull( GEOLATTE );
	}

	public void convexhull(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.convexhull ) ) {
			return;
		}
		Map<Integer, Geometry> dbexpected = expectationsFactory.getConvexHull( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, convexhull(geomunion(geom, :polygon)) from org.hibernate.spatial.integration" +
						".%s.GeomEntity where srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "polygon", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );

	}

	@Test
	@SkipForDialect(value = GeoDBDialect.class)
	public void test_intersection_on_jts() throws SQLException {
		intersection( JTS );
	}

	@Test
	@SkipForDialect(value = GeoDBDialect.class)
	public void test_intersection_on_geolatte() throws SQLException {
		intersection( GEOLATTE );
	}

	//skipped for GeoDBDialect because GeoDB throws exceptions in case the intersection is empty.
	// (Error message is "Empty Points cannot be represented in WKB")
	public void intersection(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.intersection ) ) {
			return;
		}
		Map<Integer, Geometry> dbexpected = expectationsFactory.getIntersection( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, intersection(geom, :polygon) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "polygon", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_difference_on_jts() throws SQLException {
		difference( JTS );
	}

	@Test
	public void test_difference_on_geolatte() throws SQLException {
		difference( GEOLATTE );
	}

	public void difference(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.difference ) ) {
			return;
		}
		Map<Integer, Geometry> dbexpected = expectationsFactory.getDifference( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, difference(geom, :polygon) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "polygon", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_symdifference_on_jts() throws SQLException {
		symdifference( JTS );
	}

	@Test
	public void test_symdifference_on_geolatte() throws SQLException {
		symdifference( GEOLATTE );
	}

	public void symdifference(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.symdifference ) ) {
			return;
		}
		Map<Integer, Geometry> dbexpected = expectationsFactory.getSymDifference( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, symdifference(geom, :polygon) from " +
						"org.hibernate.spatial.integration.%s.GeomEntity where srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "polygon", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_geomunion_on_jts() throws SQLException {
		geomunion( JTS );
	}

	@Test
	public void test_geomunion_on_geolatte() throws SQLException {
		geomunion( GEOLATTE );
	}

	public void geomunion(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.geomunion ) ) {
			return;
		}
		Map<Integer, Geometry> dbexpected = expectationsFactory.getGeomUnion( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, geomunion(geom, :polygon) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "polygon", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_dwithin_on_jts() throws SQLException {
		dwithin( JTS );
	}

	@Test
	public void test_dwithin_on_geolatte() throws SQLException {
		dwithin( GEOLATTE );
	}

	public void dwithin(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.dwithin ) ) {
			return;
		}
		double distance = 30.0;
		Map<Integer, Boolean> dbexpected = expectationsFactory.getDwithin(
				expectationsFactory.getTestPoint(),
				distance
		);
		String hql = format(
				"SELECT id, dwithin(geom, :filter, :distance) from " +
						"org.hibernate.spatial.integration.%s.GeomEntity where dwithin(geom, :filter, :distance) = true " +
						"and srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPoint() );
		if ( getDialect() instanceof OracleSpatial10gDialect ) {
			//because this uses the weird syntax and conventions of SDO_WITHIN_DISTANCE which returns a string (really)
			// we use a different boolean expression guaranteed to be true, and we set the third parameter to key/value string
			hql = "SELECT id, issimple(geom) from org.hibernate.spatial.integration.GeomEntity where dwithin(geom, :filter, :distance) = true and srid(geom) = 4326";
			params.put( "distance", "distance = 30" );
		}
		else {
			params.put( "distance", 30.0 );
		}
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_transform_on_jts() throws SQLException {
		transform( JTS );
	}

	@Test
	public void test_transform_on_geolatte() throws SQLException {
		transform( GEOLATTE );
	}

	public void transform(String pckg) throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.transform ) ) {
			return;
		}
		int epsg = 4324;
		Map<Integer, Geometry> dbexpected = expectationsFactory.getTransform( epsg );
		String hql = format(
				"SELECT id, transform(geom, :epsg) from org.hibernate.spatial.integration.%s.GeomEntity " +
						"where srid(geom) = 4326", pckg
		);
		Map<String, Object> params = createQueryParams( "epsg", Integer.valueOf( epsg ) );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );

	}

	public <T> void retrieveHQLResultsAndCompare(Map<Integer, T> dbexpected, String hql, String geometryType) {
		retrieveHQLResultsAndCompare( dbexpected, hql, null, geometryType );
	}

	protected <T> void retrieveHQLResultsAndCompare(
			Map<Integer, T> dbexpected,
			String hql,
			Map<String, Object> params,
			String geometryType) {
		Map<Integer, T> hsreceived = new HashMap<Integer, T>();
		doInSession( hql, hsreceived, params );
		compare( dbexpected, hsreceived, geometryType );
	}

	private Map<String, Object> createQueryParams(String filterParamName, Object value) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put( filterParamName, value );
		return params;
	}

	private <T> void doInSession(String hql, Map<Integer, T> result, Map<String, Object> params) {
		Session session = null;
		Transaction tx = null;
		try {
			session = openSession();
			tx = session.beginTransaction();
			Query query = session.createQuery( hql );
			setParameters( params, query );
			addQueryResults( result, query );
		}
		finally {
			if ( tx != null ) {
				tx.rollback();
			}
			if ( session != null ) {
				session.close();
			}
		}
	}

	private void setParameters(Map<String, Object> params, Query query) {
		if ( params == null ) {
			return;
		}
		for ( Map.Entry<String, Object> entry : params.entrySet() ) {
			query.setParameter( entry.getKey(), entry.getValue() );
		}
	}

}