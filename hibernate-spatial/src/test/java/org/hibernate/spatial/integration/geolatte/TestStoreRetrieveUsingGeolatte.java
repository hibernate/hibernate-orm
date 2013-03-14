package org.hibernate.spatial.integration.geolatte;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import org.geolatte.geom.codec.WktDecodeException;
import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.spatial.Log;
import org.hibernate.spatial.LogFactory;
import org.hibernate.spatial.integration.jts.GeomEntity;
import org.hibernate.spatial.testing.SpatialDialectMatcher;
import org.hibernate.spatial.testing.SpatialFunctionalTestCase;
import org.hibernate.spatial.testing.TestDataElement;
import org.hibernate.testing.Skip;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This testsuite-suite class verifies whether the <code>Geometry</code>s retrieved
 * are equal to the <code>Geometry</code>s stored.
 */
@Skip(condition = SpatialDialectMatcher.class, message = "No Spatial Dialect")
public class TestStoreRetrieveUsingGeolatte extends SpatialFunctionalTestCase {

	private static Log LOG = LogFactory.make();

	protected Log getLogger() {
		return LOG;
	}

	public void prepareTest() {

	}

	@Test
	public void testAfterStoreRetrievingEqualObject() throws WktDecodeException {
		Map<Integer, org.hibernate.spatial.integration.jts.GeomEntity> stored = new HashMap<Integer, org.hibernate.spatial.integration.jts.GeomEntity>();
		//check whether we retrieve exactly what we store
		storeTestObjects( stored );
		retrieveAndCompare( stored );
	}

	@Test
	public void testStoringNullGeometries() {
		storeNullGeometry();
		retrieveNullGeometry();
	}

	private void retrieveAndCompare(Map<Integer, org.hibernate.spatial.integration.jts.GeomEntity> stored) {
		int id = -1;
		Transaction tx = null;
		Session session = null;
		try {
			session = openSession();
			tx = session.beginTransaction();
			for ( org.hibernate.spatial.integration.jts.GeomEntity storedEntity : stored.values() ) {
				id = storedEntity.getId();
				org.hibernate.spatial.integration.jts.GeomEntity retrievedEntity = (org.hibernate.spatial.integration.jts.GeomEntity) session.get( org.hibernate.spatial.integration.jts.GeomEntity.class, id );
				Geometry retrievedGeometry = retrievedEntity.getGeom();
				Geometry storedGeometry = storedEntity.getGeom();
				String msg = createFailureMessage( storedEntity.getId(), storedGeometry, retrievedGeometry );
				assertTrue( msg, geometryEquality.test( storedGeometry, retrievedGeometry ) );
			}
			tx.commit();
		}
		catch ( Exception e ) {
			if ( tx != null ) {
				tx.rollback();
			}
			throw new RuntimeException( String.format( "Failure on case: %d", id ), e );
		}
		finally {
			if ( session != null ) {
				session.close();
			}
		}
	}

	private String createFailureMessage(int id, Geometry storedGeometry, Geometry retrievedGeometry) {
		String expectedText = ( storedGeometry != null ? storedGeometry.toText() : "NULL" );
		String retrievedText = ( retrievedGeometry != null ? retrievedGeometry.toText() : "NULL" );
		return String.format(
				"Equality testsuite-suite failed for %d.\nExpected: %s\nReceived:%s",
				id,
				expectedText,
				retrievedText
		);
	}

	private void storeTestObjects(Map<Integer, org.hibernate.spatial.integration.jts.GeomEntity> stored) {
		Session session = null;
		Transaction tx = null;
		int id = -1;
		try {
			session = openSession();
			// Every testsuite-suite instance is committed seperately
			// to improve feedback in case of failure
			for ( TestDataElement element : testData ) {
				id = element.id;
				tx = session.beginTransaction();
				org.hibernate.spatial.integration.jts.GeomEntity entity = org.hibernate
						.spatial
						.integration
						.jts
						.GeomEntity
						.createFrom( element );
				stored.put( entity.getId(), entity );
				session.save( entity );
				tx.commit();
			}
		}
		catch ( Exception e ) {
			if ( tx != null ) {
				tx.rollback();
			}
			throw new RuntimeException( "Failed storing testsuite-suite object with id:" + id, e );
		}
		finally {
			if ( session != null ) {
				session.close();
			}
		}
	}

	private void storeNullGeometry() {
		org.hibernate.spatial.integration.jts.GeomEntity entity = null;
		Session session = null;
		Transaction tx = null;
		try {
			session = openSession();
			tx = session.beginTransaction();
			entity = new org.hibernate.spatial.integration.jts.GeomEntity();
			entity.setId( 1 );
			entity.setType( "NULL OBJECT" );
			session.save( entity );
			tx.commit();
		}
		catch ( Exception e ) {
			if ( tx != null ) {
				tx.rollback();
			}
			throw new RuntimeException( "Failed storing testsuite-suite object with id:" + entity.getId(), e );
		}
		finally {
			if ( session != null ) {
				session.close();
			}
		}
	}

	private void retrieveNullGeometry() {
		Transaction tx = null;
		Session session = null;
		try {
			session = openSession();
			tx = session.beginTransaction();
			Criteria criteria = session.createCriteria( org.hibernate.spatial.integration.jts.GeomEntity.class );
			List<org.hibernate.spatial.integration.jts.GeomEntity> retrieved = criteria.list();
			assertEquals( "Expected exactly one result", 1, retrieved.size() );
			GeomEntity entity = retrieved.get( 0 );
			assertNull( entity.getGeom() );
			tx.commit();
		}
		catch ( Exception e ) {
			if ( tx != null ) {
				tx.rollback();
			}
			throw new RuntimeException( e );
		}
		finally {
			if ( session != null ) {
				session.close();
			}
		}
	}
}
