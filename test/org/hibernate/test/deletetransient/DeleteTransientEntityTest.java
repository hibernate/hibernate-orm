package org.hibernate.test.deletetransient;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * todo: describe DeleteTransientEntityTest
 *
 * @author Steve Ebersole
 */
public class DeleteTransientEntityTest extends FunctionalTestCase {
	public DeleteTransientEntityTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "deletetransient/Person.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( DeleteTransientEntityTest.class );
	}

	public void testTransientEntityDeletionNoCascades() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.delete( new Address() );
		t.commit();
		s.close();
	}

	public void testTransientEntityDeletionCascadingToTransientAssociation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.getAddresses().add( new Address() );
		s.delete( p );
		t.commit();
		s.close();
	}

	public void testTransientEntityDeleteCascadingToCircularity() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p1 = new Person();
		Person p2 = new Person();
		p1.getFriends().add( p2 );
		p2.getFriends().add( p1 );
		s.delete( p1 );
		t.commit();
		s.close();
	}

	public void testTransientEntityDeletionCascadingToDetachedAssociation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Address address = new Address();
		address.setInfo( "123 Main St." );
		s.save( address );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Person p = new Person();
		p.getAddresses().add( address );
		s.delete( p );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Long count = ( Long ) s.createQuery( "select count(*) from Address" ).list().get( 0 );
		assertEquals( "delete not cascaded properly across transient entity", 0, count.longValue() );
		t.commit();
		s.close();
	}

	public void testTransientEntityDeletionCascadingToPersistentAssociation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Address address = new Address();
		address.setInfo( "123 Main St." );
		s.save( address );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		address = ( Address ) s.get( Address.class, address.getId() );
		Person p = new Person();
		p.getAddresses().add( address );
		s.delete( p );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Long count = ( Long ) s.createQuery( "select count(*) from Address" ).list().get( 0 );
		assertEquals( "delete not cascaded properly across transient entity", 0, count.longValue() );
		t.commit();
		s.close();
	}
}
