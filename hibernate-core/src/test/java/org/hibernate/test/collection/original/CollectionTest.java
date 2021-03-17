/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.original;

import java.sql.SQLException;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
@RequiresDialectFeature(DialectChecks.SupportsNoColumnInsert.class)
public class CollectionTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "collection/original/UserPermissions.hbm.xml", "collection/original/Zoo.hbm.xml" };
	}

	@Test
	public void testExtraLazy() throws HibernateException, SQLException {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin" );
		u.getPermissions().add( new Permission( "obnoxiousness" ) );
		u.getPermissions().add( new Permission( "pigheadedness" ) );
		u.getSessionData().put( "foo", "foo value" );
		s.persist( u );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		u = ( User ) s.get( User.class, "gavin" );

		assertFalse( Hibernate.isInitialized( u.getPermissions() ) );
		assertEquals( u.getPermissions().size(), 2 );
		assertTrue( u.getPermissions().contains( new Permission( "obnoxiousness" ) ) );
		assertFalse( u.getPermissions().contains( new Permission( "silliness" ) ) );
		assertNotNull( u.getPermissions().get( 1 ) );
		assertNull( u.getPermissions().get( 3 ) );
		assertFalse( Hibernate.isInitialized( u.getPermissions() ) );

		assertFalse( Hibernate.isInitialized( u.getSessionData() ) );
		assertEquals( u.getSessionData().size(), 1 );
		assertTrue( u.getSessionData().containsKey( "foo" ) );
		assertFalse( u.getSessionData().containsKey( "bar" ) );
		assertTrue( u.getSessionData().containsValue( "foo value" ) );
		assertFalse( u.getSessionData().containsValue( "bar" ) );
		assertEquals( "foo value", u.getSessionData().get( "foo" ) );
		assertNull( u.getSessionData().get( "bar" ) );
		assertFalse( Hibernate.isInitialized( u.getSessionData() ) );

		assertFalse( Hibernate.isInitialized( u.getSessionData() ) );
		u.getSessionData().put( "bar", "bar value" );
		u.getSessionAttributeNames().add( "bar" );
		assertFalse( Hibernate.isInitialized( u.getSessionAttributeNames() ) );
		assertTrue( Hibernate.isInitialized( u.getSessionData() ) );

		s.delete( u );
		t.commit();
		s.close();
	}

	@Test
	public void testMerge() throws HibernateException, SQLException {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin" );
		u.getPermissions().add( new Permission( "obnoxiousness" ) );
		u.getPermissions().add( new Permission( "pigheadedness" ) );
		s.persist( u );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		User u2 = ( User ) s.createCriteria( User.class ).uniqueResult();
		u2.setPermissions( null ); //forces one shot delete
		s.merge( u );
		t.commit();
		s.close();

		u.getPermissions().add( new Permission( "silliness" ) );

		s = openSession();
		t = s.beginTransaction();
		s.merge( u );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		u2 = ( User ) s.createCriteria( User.class ).uniqueResult();
		assertEquals( u2.getPermissions().size(), 3 );
		assertEquals( ( ( Permission ) u2.getPermissions().get( 0 ) ).getType(), "obnoxiousness" );
		assertEquals( ( ( Permission ) u2.getPermissions().get( 2 ) ).getType(), "silliness" );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete( u2 );
		s.flush();
		t.commit();
		s.close();

	}

	@Test
	public void testFetch() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin" );
		u.getPermissions().add( new Permission( "obnoxiousness" ) );
		u.getPermissions().add( new Permission( "pigheadedness" ) );
		u.getEmailAddresses().add( new Email( "gavin@hibernate.org" ) );
		u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );
		s.persist( u );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		User u2 = ( User ) s.createCriteria( User.class ).uniqueResult();
		assertTrue( Hibernate.isInitialized( u2.getEmailAddresses() ) );
		assertFalse( Hibernate.isInitialized( u2.getPermissions() ) );
		assertEquals( u2.getEmailAddresses().size(), 2 );
		s.delete( u2 );
		t.commit();
		s.close();
	}

	@Test
	public void testUpdateOrder() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin" );
		u.getSessionData().put( "foo", "foo value" );
		u.getSessionData().put( "bar", "bar value" );
		u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );
		u.getEmailAddresses().add( new Email( "gavin@hibernate.org" ) );
		u.getEmailAddresses().add( new Email( "gavin@illflow.com" ) );
		u.getEmailAddresses().add( new Email( "gavin@nospam.com" ) );
		s.persist( u );
		t.commit();
		s.close();

		u.getSessionData().clear();
		u.getSessionData().put( "baz", "baz value" );
		u.getSessionData().put( "bar", "bar value" );
		u.getEmailAddresses().remove( 0 );
		u.getEmailAddresses().remove( 2 );

		s = openSession();
		t = s.beginTransaction();
		s.update( u );
		t.commit();
		s.close();

		u.getSessionData().clear();
		u.getEmailAddresses().add( 0, new Email( "gavin@nospam.com" ) );
		u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );

		s = openSession();
		t = s.beginTransaction();
		s.update( u );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete( u );
		t.commit();
		s.close();

	}

	@Test
	public void testValueMap() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin" );
		u.getSessionData().put( "foo", "foo value" );
		u.getSessionData().put( "bar", null );
		u.getEmailAddresses().add( null );
		u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );
		u.getEmailAddresses().add( null );
		u.getEmailAddresses().add( null );
		s.persist( u );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		User u2 = ( User ) s.createCriteria( User.class ).uniqueResult();
		assertFalse( Hibernate.isInitialized( u2.getSessionData() ) );
		assertEquals( u2.getSessionData().size(), 1 );
		assertEquals( u2.getEmailAddresses().size(), 2 );
		u2.getSessionData().put( "foo", "new foo value" );
		u2.getEmailAddresses().set( 1, new Email( "gavin@hibernate.org" ) );
		//u2.getEmailAddresses().remove(3);
		//u2.getEmailAddresses().remove(2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		u2 = ( User ) s.createCriteria( User.class ).uniqueResult();
		assertFalse( Hibernate.isInitialized( u2.getSessionData() ) );
		assertEquals( u2.getSessionData().size(), 1 );
		assertEquals( u2.getEmailAddresses().size(), 2 );
		assertEquals( u2.getSessionData().get( "foo" ), "new foo value" );
		assertEquals( ( ( Email ) u2.getEmailAddresses().get( 1 ) ).getAddress(), "gavin@hibernate.org" );
		s.delete( u2 );
		t.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3636" )
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testCollectionInheritance() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Zoo zoo = new Zoo();
		Mammal m = new Mammal();
		m.setMammalName( "name1" );
		m.setMammalName2( "name2" );
		m.setMammalName3( "name3" );
		m.setZoo( zoo );
		zoo.getAnimals().add( m );
		Long id = ( Long ) s.save( zoo );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Zoo found = ( Zoo ) s.get( Zoo.class, id );
		found.getAnimals().size();
		s.delete( found );
		t.commit();
		s.close();
	}
}
