/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.original;

import java.sql.SQLException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.engine.spi.SessionImplementor;

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
	public void testExtraLazy() throws HibernateException {
		inTransaction(
				s -> {
					User u = new User( "gavin" );
					u.getPermissions().add( new Permission( "obnoxiousness" ) );
					u.getPermissions().add( new Permission( "pigheadedness" ) );
					u.getSessionData().put( "foo", "foo value" );
					s.persist( u );
				}
		);


		inTransaction(
				s -> {
					User u = s.get( User.class, "gavin" );

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
				}
		);
	}

	@Test
	public void testMerge() throws HibernateException, SQLException {
		User u = new User( "gavin" );
		inTransaction(
				s -> {
					u.getPermissions().add( new Permission( "obnoxiousness" ) );
					u.getPermissions().add( new Permission( "pigheadedness" ) );
					s.persist( u );
				}
		);

		inTransaction(
				s -> {
					User u2 = findUser( s );
					u2.setPermissions( null ); //forces one shot delete
					s.merge( u );
				}
		);

		u.getPermissions().add( new Permission( "silliness" ) );

		inTransaction(
				s -> s.merge( u )
		);
		;

		inTransaction(
				s -> {
					User u2 = findUser( s );
					assertEquals( u2.getPermissions().size(), 3 );
					assertEquals( ( (Permission) u2.getPermissions().get( 0 ) ).getType(), "obnoxiousness" );
					assertEquals( ( (Permission) u2.getPermissions().get( 2 ) ).getType(), "silliness" );

				}
		);

		inTransaction(
				s -> {
					User u2 = findUser( s );
					s.delete( u2 );
					s.flush();
				}
		);
	}

	@Test
	public void testFetch() {
		inTransaction(
				s -> {
					User u = new User( "gavin" );
					u.getPermissions().add( new Permission( "obnoxiousness" ) );
					u.getPermissions().add( new Permission( "pigheadedness" ) );
					u.getEmailAddresses().add( new Email( "gavin@hibernate.org" ) );
					u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );
					s.persist( u );
				}
		);

		inTransaction(
				s -> {
					User u2 = findUser( s );
					assertTrue( Hibernate.isInitialized( u2.getEmailAddresses() ) );
					assertFalse( Hibernate.isInitialized( u2.getPermissions() ) );
					assertEquals( u2.getEmailAddresses().size(), 2 );
					s.delete( u2 );
				}
		);
	}

	@Test
	public void testUpdateOrder() {
		User u = new User( "gavin" );
		inTransaction(
				s -> {
					u.getSessionData().put( "foo", "foo value" );
					u.getSessionData().put( "bar", "bar value" );
					u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );
					u.getEmailAddresses().add( new Email( "gavin@hibernate.org" ) );
					u.getEmailAddresses().add( new Email( "gavin@illflow.com" ) );
					u.getEmailAddresses().add( new Email( "gavin@nospam.com" ) );
					s.persist( u );
				}
		);


		u.getSessionData().clear();
		u.getSessionData().put( "baz", "baz value" );
		u.getSessionData().put( "bar", "bar value" );
		u.getEmailAddresses().remove( 0 );
		u.getEmailAddresses().remove( 2 );

		inTransaction(
				s -> s.update( u )
		);

		u.getSessionData().clear();
		u.getEmailAddresses().add( 0, new Email( "gavin@nospam.com" ) );
		u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );

		inTransaction(
				s -> s.update( u )
		);

		inTransaction(
				s -> s.delete( u )
		);
	}

	@Test
	public void testValueMap() {
		inTransaction(
				s -> {
					User u = new User( "gavin" );
					u.getSessionData().put( "foo", "foo value" );
					u.getSessionData().put( "bar", null );
					u.getEmailAddresses().add( null );
					u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );
					u.getEmailAddresses().add( null );
					u.getEmailAddresses().add( null );
					s.persist( u );
				}
		);
		inTransaction(
				s -> {
					User u2 = findUser( s );
//					User u2 = (User) s.createCriteria( User.class ).uniqueResult();
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
				s -> {
					User u2 = findUser( s );
//					User u2 = (User) s.createCriteria( User.class ).uniqueResult();
					assertFalse( Hibernate.isInitialized( u2.getSessionData() ) );
					assertEquals( u2.getSessionData().size(), 1 );
					assertEquals( u2.getEmailAddresses().size(), 2 );
					assertEquals( u2.getSessionData().get( "foo" ), "new foo value" );
					assertEquals( ( (Email) u2.getEmailAddresses().get( 1 ) ).getAddress(), "gavin@hibernate.org" );
					s.delete( u2 );
				}
		);
	}

	private User findUser(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
		criteria.from( User.class );
		return s.createQuery( criteria ).uniqueResult();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-3636")
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testCollectionInheritance() {
		Zoo zoo = new Zoo();
		inTransaction(
				s -> {
					Mammal m = new Mammal();
					m.setMammalName( "name1" );
					m.setMammalName2( "name2" );
					m.setMammalName3( "name3" );
					m.setZoo( zoo );
					zoo.getAnimals().add( m );
					s.save( zoo );
				}
		);

		inTransaction(
				s -> {
					Zoo found = (Zoo) s.get( Zoo.class, zoo.getId() );
					found.getAnimals().size();
					s.delete( found );
				}
		);
	}
}
