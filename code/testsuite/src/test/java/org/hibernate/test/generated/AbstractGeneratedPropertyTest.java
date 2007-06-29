// $Id: AbstractGeneratedPropertyTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.generated;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Hibernate;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;

/**
 * Implementation of AbstractGeneratedPropertyTest.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractGeneratedPropertyTest extends DatabaseSpecificFunctionalTestCase {
	public AbstractGeneratedPropertyTest(String x) {
		super( x );
	}

	public final void testGeneratedProperty() {
		GeneratedPropertyEntity entity = new GeneratedPropertyEntity();
		entity.setName( "entity-1" );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.save( entity );
		s.flush();
		assertNotNull( "no timestamp retrieved", entity.getLastModified() );
		t.commit();
		s.close();

		byte[] bytes = entity.getLastModified();

		s = openSession();
		t = s.beginTransaction();
		entity = ( GeneratedPropertyEntity ) s.get( GeneratedPropertyEntity.class, entity.getId() );
		assertTrue( Hibernate.BINARY.isEqual( bytes, entity.getLastModified() ) );
		t.commit();
		s.close();

		assertTrue( Hibernate.BINARY.isEqual( bytes, entity.getLastModified() ) );

		s = openSession();
		t = s.beginTransaction();
		s.delete( entity );
		t.commit();
		s.close();
	}
}
