/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.deletetransient;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class DeleteTransientEntityTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "deletetransient/Person.hbm.xml" };
	}
	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testTransientEntityDeletionNoCascades() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.delete( new Address() );
		t.commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testTransientEntityDeletionCascadingToTransientAssociation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.getAddresses().add( new Address() );
		s.delete( p );
		t.commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
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

	@Test
	@SuppressWarnings( {"unchecked"})
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

	@Test
	@SuppressWarnings( {"unchecked"})
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

	@Test
	@SuppressWarnings( {"unchecked"})
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

	@Test
	@SuppressWarnings( {"unchecked"})
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
