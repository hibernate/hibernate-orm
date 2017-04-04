/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.integration.geolatte;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geolatte.geom.C3DM;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryEquality;
import org.geolatte.geom.GeometryPointEquality;
import org.geolatte.geom.codec.WktDecodeException;

import org.jboss.logging.Logger;

import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.spatial.HSMessageLogger;
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

	private static HSMessageLogger LOG = Logger.getMessageLogger(
			HSMessageLogger.class,
			TestStoreRetrieveUsingGeolatte.class.getName()
	);

	protected HSMessageLogger getLogger() {
		return LOG;
	}

	public void prepareTest() {

	}

	@Test
	public void testAfterStoreRetrievingEqualObject() throws WktDecodeException {
		Map<Integer, GeomEntity> stored = new HashMap<Integer, GeomEntity>();
		//check whether we retrieve exactly what we store
		storeTestObjects( stored );
		retrieveAndCompare( stored );
	}

	@Test
	public void testStoringNullGeometries() {
		storeNullGeometry();
		retrieveNullGeometry();
	}

	private void retrieveAndCompare(Map<Integer, GeomEntity> stored) {
		int id = -1;
		Transaction tx = null;
		Session session = null;
		GeometryEquality geomEq = new GeometryPointEquality();
		try {
			session = openSession();
			tx = session.beginTransaction();
			for ( GeomEntity storedEntity : stored.values() ) {
				id = storedEntity.getId();
				GeomEntity retrievedEntity = (GeomEntity) session.get( GeomEntity.class, id );
				Geometry<C3DM> retrievedGeometry = retrievedEntity.getGeom();
				Geometry<C3DM> storedGeometry = storedEntity.getGeom();
				String msg = createFailureMessage( storedEntity.getId(), storedGeometry, retrievedGeometry );
				assertTrue( msg, geomEq.equals( storedGeometry, retrievedGeometry ) );
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
		String expectedText = ( storedGeometry != null ? storedGeometry.toString() : "NULL" );
		String retrievedText = ( retrievedGeometry != null ? retrievedGeometry.toString() : "NULL" );
		return String.format(
				"Equality testsuite-suite failed for %d.%nExpected: %s%nReceived:%s",
				id,
				expectedText,
				retrievedText
		);
	}

	private void storeTestObjects(Map<Integer, GeomEntity> stored) {
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
				GeomEntity entity = GeomEntity.createFrom( element );
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
		GeomEntity entity = null;
		Session session = null;
		Transaction tx = null;
		try {
			session = openSession();
			tx = session.beginTransaction();
			entity = new GeomEntity();
			entity.setId( 1 );
			entity.setType( "NULL OBJECT" );
			session.save( entity );
			tx.commit();
		}
		catch ( Exception e ) {
			if ( tx != null ) {
				tx.rollback();
			}
			Integer id = entity != null ? entity.getId() : -1;
			throw new RuntimeException( "Failed storing testsuite-suite object with id:" + id, e );
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
			Criteria criteria = session.createCriteria( GeomEntity.class );
			List<GeomEntity> retrieved = criteria.list();
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
