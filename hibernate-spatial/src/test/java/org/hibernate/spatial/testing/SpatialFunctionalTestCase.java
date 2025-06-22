/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import java.util.List;
import java.util.Map;
import jakarta.persistence.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.domain.GeomEntity;
import org.hibernate.spatial.testing.domain.JtsGeomEntity;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.locationtech.jts.geom.Geometry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Karel Maesen, Geovise BVBA
 * @deprecated This class will be removed for H6. Currently it no longer works as intended
 * TODO Remove me!
 */
@Deprecated
public abstract class SpatialFunctionalTestCase extends BaseCoreFunctionalTestCase {

	protected final static String JTS = "jts";
	protected final static String GEOLATTE = "geolatte";

	protected TestData testData;

	protected AbstractExpectationsFactory expectationsFactory;

	/**
	 * Inserts the test data via a direct route (JDBC).
	 */
	public void prepareTest() {
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
			String hql = String.format( "delete from %s", entityName( pckg ) );
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
			expectationsFactory = null;
			testData = support.createTestData( TestSupport.TestDataPurpose.StoreRetrieveData );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
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
				GeomEntity.class,
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
		Dialect dialect = getDialect();
		return true;
	}


	abstract protected HSMessageLogger getLogger();

	/**
	 * Adds the query results to a Map.
	 * <p>
	 * Each row is added as a Map entry with the first column the key,
	 * and the second the value. It is assumed that the first column is an
	 * identifier of a type assignable to Integer.
	 *
	 * @param result map of
	 * @param query the source Query
	 * @param <T> type of the second column in the query results
	 */
	protected <T> void addQueryResults(Map<Integer, T> result, Query query) {
		List<Object[]> rows = query.getResultList();
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
				assertEquals(
						"Failure on testsuite-suite for case " + id,
						expected, received
				);
			}
			else {
				if ( !( received instanceof org.geolatte.geom.Geometry ) ) {
					fail(
							"Expected a Geolatte Geometry, but received an object of type " + received.getClass()
									.getCanonicalName()
					);
				}
				assertEquals(
						"Failure on testsuite-suite for case " + id,
								expected,
								org.geolatte.geom.jts.JTS.to( (org.geolatte.geom.Geometry) received )
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
		if ( JTS.equalsIgnoreCase( pckg ) ) {
			return "org.hibernate.spatial.testing.domain.JtsGeomEntity";
		}
		else {
			return "org.hibernate.spatial.testing.domain.GeomEntity";
		}
	}

}
