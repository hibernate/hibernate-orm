package org.hibernate.spatial.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.integration.geolatte.GeomEntity;
import org.hibernate.spatial.testing.GeometryEquality;
import org.hibernate.spatial.testing.SpatialFunctionalTestCase;
import org.hibernate.spatial.testing.TestDataElement;

import org.junit.Test;

import org.geolatte.geom.codec.WktDecodeException;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

//import org.geolatte.geom.C3DM;
//import org.geolatte.geom.Geometry;
//import org.geolatte.geom.GeometryEquality;
//import org.geolatte.geom.GeometryPointEquality;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/02/2018.
 */
public abstract class AbstractTestStoreRetrieve<G, E extends GeomEntityLike<G>> extends SpatialFunctionalTestCase {


	public void prepareTest() {

	}

	protected abstract GeometryEquality<G> getGeometryEquality();

	protected abstract Class<E> getGeomEntityClass();

	protected abstract E createFrom(TestDataElement element, Dialect dialect);

	@Test
	public void testAfterStoreRetrievingEqualObject() throws WktDecodeException {
		Map<Integer, E> stored = new HashMap<>();
		//check whether we retrieve exactly what we store
		storeTestObjects( stored );
		retrieveAndCompare( stored );
	}

	@Test
	public void testStoringNullGeometries() {
		storeNullGeometry();
		retrieveNullGeometry();
	}

	private void retrieveAndCompare(Map<Integer, E> stored) {
		int id = -1;
		Transaction tx = null;
		Session session = null;
		GeometryEquality<G> geomEq = getGeometryEquality();
		try {
			session = openSession();
			tx = session.beginTransaction();
			for ( E storedEntity : stored.values() ) {
				id = storedEntity.getId();
				E retrievedEntity = (E) session.get( getGeomEntityClass(), id );
				G retrievedGeometry = retrievedEntity.getGeom();
				G storedGeometry = storedEntity.getGeom();
				String msg = createFailureMessage( storedEntity.getId(), storedGeometry, retrievedGeometry );
				assertTrue( msg, geomEq.test( storedGeometry, retrievedGeometry ) );
			}
			tx.commit();
		}
		catch (Exception e) {
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

	private String createFailureMessage(int id, G storedGeometry, G retrievedGeometry) {
		String expectedText = ( storedGeometry != null ? storedGeometry.toString() : "NULL" );
		String retrievedText = ( retrievedGeometry != null ? retrievedGeometry.toString() : "NULL" );
		return String.format(
				"Equality testsuite-suite failed for %d.%nExpected: %s%nReceived:%s",
				id,
				expectedText,
				retrievedText
		);
	}

	private void storeTestObjects(Map<Integer, E> stored) {
		Session session = null;
		Transaction tx = null;
		int id = -1;
		try {
			session = openSession();
			Dialect dialect = sessionFactory().getJdbcServices().getDialect();
			// Every testsuite-suite instance is committed seperately
			// to improve feedback in case of failure
			for ( TestDataElement element : testData ) {
				id = element.id;
				tx = session.beginTransaction();
				;
				E entity = createFrom( element, dialect );
				stored.put( entity.getId(), entity );
				session.save( entity );
				tx.commit();
			}
		}
		catch (Exception e) {
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
		catch (Exception e) {
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
		catch (Exception e) {
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
