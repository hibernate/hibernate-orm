package org.hibernate.spatial;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import org.slf4j.Logger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.spatial.test.AbstractExpectationsFactory;
import org.hibernate.spatial.test.DataSourceUtils;
import org.hibernate.spatial.test.GeometryEquality;
import org.hibernate.spatial.test.TestData;
import org.hibernate.spatial.test.TestSupport;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Sep 30, 2010
 */
public abstract class SpatialFunctionalTestCase extends BaseCoreFunctionalTestCase {

	protected TestData testData;
	protected DataSourceUtils dataSourceUtils;
	protected GeometryEquality geometryEquality;
	protected AbstractExpectationsFactory expectationsFactory;

	public void insertTestData() {
		try {
			dataSourceUtils.insertTestData( testData );
		}
		catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
	}

	public void deleteAllTestEntities() {
		Session session = null;
		Transaction tx = null;
		try {
			session = openSession();
			tx = session.beginTransaction();
			String hql = "delete from GeomEntity";
			Query q = session.createQuery( hql );
			q.executeUpdate();
			tx.commit();
		}
		catch ( Exception e ) {
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

	public void prepareTest() {
		try {
			TestSupport tsFactory = TestSupportFactories.instance().getTestSupportFactory( getDialect() );
			Configuration cfg = configuration();
			dataSourceUtils = tsFactory.createDataSourceUtil( cfg );
			expectationsFactory = tsFactory.createExpectationsFactory( dataSourceUtils );
			testData = tsFactory.createTestData( this );
			geometryEquality = tsFactory.createGeometryEquality();
			dataSourceUtils.afterCreateSchema();
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public void cleanupTest() throws SQLException {
		dataSourceUtils.close();
	}

	public Connection getConnection() throws SQLException {
		return dataSourceUtils.getConnection();
	}

	public String getBaseForMappings() {
		return "";
	}

	public String[] getMappings() {
		return new String[] { "GeomEntity.hbm.xml" };
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

	abstract protected Logger getLogger();

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

	protected <T> void compare(Map<Integer, T> expected, Map<Integer, T> received) {
		for ( Integer id : expected.keySet() ) {
			getLogger().debug( "Case :" + id );
			getLogger().debug( "expected: " + expected.get( id ) );
			getLogger().debug( "received: " + received.get( id ) );
			compare( id, expected.get( id ), received.get( id ) );
		}
	}


	protected void compare(Integer id, Object expected, Object received) {
		assertTrue( expected != null || ( expected == null && received == null ) );
		if ( expected instanceof byte[] ) {
			assertArrayEquals( "Failure on testsuite-suite for case " + id, (byte[]) expected, (byte[]) received );

		}
		else if ( expected instanceof Geometry ) {
			if ( !( received instanceof Geometry ) ) {
				fail( "Expected a Geometry, but received an object of type " + received.getClass().getCanonicalName() );
			}
			assertTrue(
					"Failure on testsuite-suite for case " + id,
					geometryEquality.test( (Geometry) expected, (Geometry) received )
			);

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


}
