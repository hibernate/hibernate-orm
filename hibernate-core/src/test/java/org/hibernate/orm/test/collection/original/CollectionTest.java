/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.original;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@Disabled("Inheritance support not yet implemented")
@RequiresDialectFeature(DialectChecks.SupportsNoColumnInsert.class)
public class CollectionTest extends SessionFactoryBasedFunctionalTest {
	@Override
	public String[] getHmbMappingFiles() {
		return new String[] { "collection/original/UserPermissions.hbm.xml", "collection/original/Zoo.hbm.xml" };
	}

	@Test
	public void testExtraLazy() throws HibernateException {
		final User user = new User( "gavin" );
		user.getPermissions().add( new Permission( "obnoxiousness" ) );
		user.getPermissions().add( new Permission( "pigheadedness" ) );
		user.getSessionData().put( "foo", "foo value" );
		inTransaction(
				session -> {
					session.persist( user );
				}
		);
		inTransaction(
				session -> {
					User u = session.get( User.class, "gavin" );

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

					session.delete( u );
				}
		);
	}

	@Test
	public void testMerge() throws HibernateException {
		User user = new User( "gavin" );
		user.getPermissions().add( new Permission( "obnoxiousness" ) );
		user.getPermissions().add( new Permission( "pigheadedness" ) );
		inTransaction(
				session -> {
					session.persist( user );
				}
		);


		inTransaction(
				session -> {
					//todo (6.0) when criteria will ne implemented use criteria commented as in the original test
//					User u2 = (User) session.createCriteria(User.class).uniqueResult()
					User u2 = (User) session.createQuery( "from User" ).uniqueResult();
					u2.setPermissions( null ); //forces one shot delete
					session.merge( user );

				}
		);

		user.getPermissions().add( new Permission( "silliness" ) );

		inTransaction(
				session -> {
					session.merge( user );
				}
		);

		User u = inTransaction(
				session -> {
					//todo (6.0) when criteria will ne implemented use criteria commented as in the original test
//					User u2 = (User) session.createCriteria( User.class ).uniqueResult();
					User u2 = (User) session.createQuery( "from User" ).uniqueResult();

					assertEquals( u2.getPermissions().size(), 3 );
					assertEquals( ( (Permission) u2.getPermissions().get( 0 ) ).getType(), "obnoxiousness" );
					assertEquals( ( (Permission) u2.getPermissions().get( 2 ) ).getType(), "silliness" );
					return u2;
				}
		);

		inTransaction(
				session -> {
					session.delete( u );
					session.flush();
				}
		);
	}

	@Test
	public void testFetch() {
		User u = new User( "gavin" );
		u.getPermissions().add( new Permission( "obnoxiousness" ) );
		u.getPermissions().add( new Permission( "pigheadedness" ) );
		u.getEmailAddresses().add( new Email( "gavin@hibernate.org" ) );
		u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );

		inTransaction(
				session -> {
					session.persist( u );

				}
		);

		inTransaction(
				session -> {
					//todo (6.0) when criteria will ne implemented use criteria commented as in the original test
//					User u2 = (User) session.createCriteria( User.class ).uniqueResult();
					User u2 = (User) session.createQuery( "from User" ).uniqueResult();

					assertTrue( Hibernate.isInitialized( u2.getEmailAddresses() ) );
					assertFalse( Hibernate.isInitialized( u2.getPermissions() ) );
					assertEquals( u2.getEmailAddresses().size(), 2 );
					session.delete( u2 );
				}
		);
	}

	@Test
	public void testUpdateOrder() {
		User user = new User( "gavin" );
		user.getSessionData().put( "foo", "foo value" );
		user.getSessionData().put( "bar", "bar value" );
		user.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );
		user.getEmailAddresses().add( new Email( "gavin@hibernate.org" ) );
		user.getEmailAddresses().add( new Email( "gavin@illflow.com" ) );
		user.getEmailAddresses().add( new Email( "gavin@nospam.com" ) );

		inTransaction(
				session -> {
					session.persist( user );

				}
		);

		user.getSessionData().clear();
		user.getSessionData().put( "baz", "baz value" );
		user.getSessionData().put( "bar", "bar value" );
		user.getEmailAddresses().remove( 0 );
		user.getEmailAddresses().remove( 2 );

		inTransaction(
				session -> {
					session.update( user );
				}
		);

		user.getSessionData().clear();
		user.getEmailAddresses().add( 0, new Email( "gavin@nospam.com" ) );
		user.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );

		inTransaction(
				session -> {
					session.update( user );
				}
		);

		inTransaction(
				session -> {
					session.delete( user );

				}
		);
	}

	@Test
	public void testValueMap() {
		User u = new User( "gavin" );
		u.getSessionData().put( "foo", "foo value" );
		u.getSessionData().put( "bar", null );
		u.getEmailAddresses().add( null );
		u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );
		u.getEmailAddresses().add( null );
		u.getEmailAddresses().add( null );
		inTransaction(
				session -> {
					session.persist( u );
				}
		);

		inTransaction(
				session -> {
					//todo (6.0) when criteria will ne implemented use criteria commented as in the original test
//					User u2 = (User) session.createCriteria( User.class ).uniqueResult();
					User u2 = (User) session.createQuery( "from User" ).uniqueResult();
					assertFalse( Hibernate.isInitialized( u2.getSessionData() ) );
					assertEquals( u2.getSessionData().size(), 1 );
					assertEquals( u2.getEmailAddresses().size(), 2 );
					u2.getSessionData().put( "foo", "new foo value" );
					u2.getEmailAddresses().set( 1, new Email( "gavin@hibernate.org" ) );
					//u2.getEmailAddresses().remove(3);
					//u2.getEmailAddresses().remove(2);
				}
		);

		inTransaction(
				session -> {
					//todo (6.0) when criteria will ne implemented use criteria commented as in the original test
//					User u2 = (User) session.createCriteria( User.class ).uniqueResult();
					User u2 = (User) session.createQuery( "from User" ).uniqueResult();
					assertFalse( Hibernate.isInitialized( u2.getSessionData() ) );
					assertEquals( u2.getSessionData().size(), 1 );
					assertEquals( u2.getEmailAddresses().size(), 2 );
					assertEquals( u2.getSessionData().get( "foo" ), "new foo value" );
					assertEquals( ( (Email) u2.getEmailAddresses().get( 1 ) ).getAddress(), "gavin@hibernate.org" );
					session.delete( u2 );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-3636")
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testCollectionInheritance() {
		Zoo zoo = new Zoo();
		Mammal m = new Mammal();
		m.setMammalName( "name1" );
		m.setMammalName2( "name2" );
		m.setMammalName3( "name3" );
		m.setZoo( zoo );
		zoo.getAnimals().add( m );
		Long id = inTransaction(
				session -> {
					return (Long) session.save( zoo );
				}
		);

		inTransaction(
				session -> {
					Zoo found = session.get( Zoo.class, id );
					found.getAnimals().size();
					session.delete( found );
				}
		);
	}
}
