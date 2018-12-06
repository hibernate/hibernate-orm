/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.integration.jts.JtsGeomEntity;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.locationtech.jts.geom.Geometry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Sep 30, 2010
 */
public abstract class SpatialFunctionalTestCase extends BaseCoreFunctionalTestCase {

	protected final static String JTS = "jts";
	protected final static String GEOLATTE = "geolatte";

	protected TestData testData;
	protected DataSourceUtils dataSourceUtils;
	protected JTSGeometryEquality geometryEquality;
	protected AbstractExpectationsFactory expectationsFactory;

	/**
	 * Inserts the test data via a direct route (JDBC).
	 */
	public void prepareTest() {
		try {
			dataSourceUtils.insertTestData( testData );
		}
		catch (BatchUpdateException e) {
			throw new RuntimeException( e.getNextException() );
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Removes the test data.
	 */
	public void cleanupTest() {
		cleanUpTest( "jts" );
		cleanUpTest( "geolatte" );
	}

	private void cleanUpTest(String pckg) {
		Session session = null;
		Transaction tx = null;
		try {
			session = openSession();
			tx = session.beginTransaction();
			String hql = String.format( "delete from %s", entityName(pckg) );
			Query q = session.createQuery( hql );
			q.executeUpdate();
			tx.commit();
		}
		catch (Exception e) {
			if ( tx != null ) {
				tx.rollback();
			}
		}
		finally {
			if ( session != null ) {
				session.close();
			}
		}
	}

	/**
	 * Override to also ensure that the SpatialTestSupport utility is
	 * initialised together with the Hibernate <code>Configuration</code>.
	 *
	 * @return
	 */
	protected void afterConfigurationBuilt(Configuration cfg) {
		super.afterConfigurationBuilt( cfg );
		initializeSpatialTestSupport( serviceRegistry() );
	}

	private void initializeSpatialTestSupport(ServiceRegistry serviceRegistry) {
		try {
			TestSupport support = TestSupportFactories.instance().getTestSupportFactory( getDialect() );
			dataSourceUtils = support.createDataSourceUtil( serviceRegistry );
			expectationsFactory = support.createExpectationsFactory( dataSourceUtils );
			testData = support.createTestData( this );
			geometryEquality = support.createGeometryEquality();
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Overwrites the afterSessionFactoryBuilt() method in BaseCoreFunctionalTestCase.
	 * <p/>
	 * Mostly used to register spatial metadata in databases such as Oracle Spatial.
	 */
	public void afterSessionFactoryBuilt() {
		dataSourceUtils.afterCreateSchema();
	}

	/**
	 * Cleans up the dataSourceUtils
	 *
	 * @throws SQLException
	 */
	@AfterClassOnce
	public void closeDataSourceUtils() throws SQLException {
		dataSourceUtils.close();
	}

	public String getBaseForMappings() {
		return "";
	}

	public String[] getMappings() {
		return new String[] {};
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				org.hibernate.spatial.integration.geolatte.GeomEntity.class,
				JtsGeomEntity.class
		};
	}

	/**
	 * Returns true if the spatial dialect supports the specified function
	 *
	 * @param spatialFunction
	 *
	 * @return
	 */
	public boolean isSupportedByDialect(SpatialFunction spatialFunction) {
		SpatialDialect dialect = (SpatialDialect) getDialect();
		return dialect.supports( spatialFunction );
	}

	/**
	 * Supports true if the spatial dialect supports filtering (e.g. ST_overlap, MBROverlap, SDO_FILTER)
	 *
	 * @return
	 */
	public boolean dialectSupportsFiltering() {
		SpatialDialect dialect = (SpatialDialect) getDialect();
		return dialect.supportsFiltering();
	}

	abstract protected HSMessageLogger getLogger();

	/**
	 * Adds the query results to a Map.
	 * <p/>
	 * Each row is added as a Map entry with the first column the key,
	 * and the second the value. It is assumed that the first column is an
	 * identifier of a type assignable to Integer.
	 *
	 * @param result map of
	 * @param query the source Query
	 * @param <T> type of the second column in the query results
	 */
	protected <T> void addQueryResults(Map<Integer, T> result, Query query) {
		List<Object[]> rows = (List<Object[]>) query.list();
		if ( rows.size() == 0 ) {
			getLogger().warn( "No results returned for query!!" );
		}
		for ( Object[] row : rows ) {
			Integer id = (Integer) row[0];
			T val = (T) row[1];
			result.put( id, val );
		}
	}

	protected <T> void compare(Map<Integer, T> expected, Map<Integer, T> received, String geometryType) {
		for ( Map.Entry<Integer, T> entry : expected.entrySet() ) {
			Integer id = entry.getKey();
			getLogger().debug( "Case :" + id );
			getLogger().debug( "expected: " + expected.get( id ) );
			getLogger().debug( "received: " + received.get( id ) );
			compare( id, entry.getValue(), received.get( id ), geometryType );
		}
	}

	protected void compare(Integer id, Object expected, Object received, String geometryType) {
		assertTrue( expected != null || received == null );
		if ( expected instanceof byte[] ) {
			assertArrayEquals( "Failure on testsuite-suite for case " + id, (byte[]) expected, (byte[]) received );

		}
		else if ( expected instanceof Geometry ) {
			if ( JTS.equals( geometryType ) ) {
				if ( !( received instanceof Geometry ) ) {
					fail(
							"Expected a JTS Geometry, but received an object of type " + received.getClass()
									.getCanonicalName()
					);
				}
				assertTrue(
						"Failure on testsuite-suite for case " + id,
						geometryEquality.test( (Geometry) expected, (Geometry) received )
				);
			}
			else {
				if ( !( received instanceof org.geolatte.geom.Geometry ) ) {
					fail(
							"Expected a Geolatte Geometry, but received an object of type " + received.getClass()
									.getCanonicalName()
					);
				}
				assertTrue(
						"Failure on testsuite-suite for case " + id,
						geometryEquality.test(
								(Geometry) expected,
								(Geometry) org.geolatte.geom.jts.JTS.to( (org.geolatte.geom.Geometry) received )
						)
				);
			}

		}
		else {
			if ( expected instanceof Long ) {
				assertEquals( "Failure on testsuite-suite for case " + id, ( (Long) expected ).intValue(), received );
			}
			else {
				assertEquals( "Failure on testsuite-suite for case " + id, expected, received );
			}
		}
	}

	protected String entityName(String pckg) {
		if (JTS.equalsIgnoreCase(  pckg )) {
			return "org.hibernate.spatial.integration.jts.JtsGeomEntity";
		} else {
			return "org.hibernate.spatial.integration.geolatte.GeomEntity";
		}
	}

}
