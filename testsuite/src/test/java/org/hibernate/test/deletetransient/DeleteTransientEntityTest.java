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

	public void testCascadeAllFromClearedPersistentAssnToTransientEntity() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		Address address = new Address();
		address.setInfo( "123 Main St." );
		p.getAddresses().add( address );
		s.save( p );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Suite suite = new Suite();
		address.getSuites().add( suite );
		p.getAddresses().clear();
		s.saveOrUpdate( p );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		p = ( Person ) s.get( p.getClass(), p.getId() );
		assertEquals( "persistent collection not cleared", 0, p.getAddresses().size() );
		Long count = ( Long ) s.createQuery( "select count(*) from Address" ).list().get( 0 );
		assertEquals( 1, count.longValue() );
		count = ( Long ) s.createQuery( "select count(*) from Suite" ).list().get( 0 );
		assertEquals( 0, count.longValue() );
		s.delete( p );
		t.commit();
		s.close();
	}

	public void testCascadeAllDeleteOrphanFromClearedPersistentAssnToTransientEntity() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Address address = new Address();
		address.setInfo( "123 Main St." );
		Suite suite = new Suite();
		address.getSuites().add( suite );
		s.save( address );
		t.commit();
		s.close();


		s = openSession();
		t = s.beginTransaction();
		Note note = new Note();
		note.setDescription( "a description" );
		suite.getNotes().add( note );
		address.getSuites().clear();
		s.saveOrUpdate( address );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Long count = ( Long ) s.createQuery( "select count(*) from Suite" ).list().get( 0 );
		assertEquals( "all-delete-orphan not cascaded properly to cleared persistent collection entities", 0, count.longValue() );
		count = ( Long ) s.createQuery( "select count(*) from Note" ).list().get( 0 );
		assertEquals( 0, count.longValue() );
		s.delete( address );
		t.commit();
		s.close();
	}
}
