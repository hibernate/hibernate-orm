package org.hibernate.test.jpa.removed;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.test.jpa.Item;

/**
 *
 * @author Steve Ebersole
 */
public class RemovedEntityTest extends AbstractJPATest {
	public RemovedEntityTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( RemovedEntityTest.class );
	}

	public void testRemoveThenContains() {
		Session s = openSession();
		s.beginTransaction();
		Item item = new Item();
		item.setName( "dummy" );
		s.persist( item );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( item );
		boolean contains = s.contains( item );
		s.getTransaction().commit();
		s.close();

		assertFalse( "expecting removed entity to not be contained", contains );
	}

	public void testRemoveThenGet() {
		Session s = openSession();
		s.beginTransaction();
		Item item = new Item();
		item.setName( "dummy" );
		s.persist( item );
		s.getTransaction().commit();
		s.close();

		Long id = item.getId();

		s = openSession();
		s.beginTransaction();
		s.delete( item );
		item = ( Item ) s.get( Item.class, id );
		s.getTransaction().commit();
		s.close();

		assertNull( "expecting removed entity to be returned as null from get()", item );
	}
}
