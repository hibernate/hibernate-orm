/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.original;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@DomainModel(xmlMappings = {
		"org/hibernate/orm/test/collection/original/UserPermissions.hbm.xml",
		"org/hibernate/orm/test/collection/original/Zoo.hbm.xml",
})
@SessionFactory
public class CollectionTest {

	@Test
	public void testExtraLazy(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					User u = new User( "gavin" );
					u.getPermissions().add( new Permission( "obnoxiousness" ) );
					u.getPermissions().add( new Permission( "pigheadedness" ) );
					u.getSessionData().put( "foo", "foo value" );
					s.persist( u );
				}
		);

		scope.inTransaction(
				s -> {
					User u = s.get( User.class, "gavin" );

					assertFalse( Hibernate.isInitialized( u.getPermissions() ) );
					assertEquals( 2, u.getPermissions().size() );
					assertTrue( u.getPermissions().contains( new Permission( "obnoxiousness" ) ) );
					assertFalse( u.getPermissions().contains( new Permission( "silliness" ) ) );
					assertNotNull( u.getPermissions().get( 1 ) );
					assertNull( u.getPermissions().get( 3 ) );
					assertFalse( Hibernate.isInitialized( u.getPermissions() ) );

					assertFalse( Hibernate.isInitialized( u.getSessionData() ) );
					assertEquals( 1, u.getSessionData().size() );
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
	public void testMerge(SessionFactoryScope scope) {
		User u = new User( "gavin" );
		scope.inTransaction(
				s -> {
					u.getPermissions().add( new Permission( "obnoxiousness" ) );
					u.getPermissions().add( new Permission( "pigheadedness" ) );
					s.persist( u );
				}
		);

		scope.inTransaction(
				s -> {
					User u2 = findUser( s );
					u2.setPermissions( null ); //forces one shot delete
					s.merge( u );
				}
		);

		u.getPermissions().add( new Permission( "silliness" ) );

		scope.inTransaction(
				s -> s.merge( u )
		);

		scope.inTransaction(
				s -> {
					User u2 = findUser( s );
					assertEquals( u2.getPermissions().size(), 3 );
					assertEquals( "obnoxiousness", ( (Permission) u2.getPermissions().get( 0 ) ).getType() );
					assertEquals( "silliness", ( (Permission) u2.getPermissions().get( 2 ) ).getType() );

				}
		);

		scope.inTransaction(
				s -> {
					User u2 = findUser( s );
					s.delete( u2 );
					s.flush();
				}
		);
	}

	@Test
	public void testFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					User u = new User( "gavin" );
					u.getPermissions().add( new Permission( "obnoxiousness" ) );
					u.getPermissions().add( new Permission( "pigheadedness" ) );
					u.getEmailAddresses().add( new Email( "gavin@hibernate.org" ) );
					u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );
					s.persist( u );
				}
		);

		scope.inTransaction(
				s -> {
					User u2 = findUser( s );
					assertTrue( Hibernate.isInitialized( u2.getEmailAddresses() ) );
					assertFalse( Hibernate.isInitialized( u2.getPermissions() ) );
					assertEquals( 2, u2.getEmailAddresses().size() );
					s.delete( u2 );
				}
		);
	}

	@Test
	public void testUpdateOrder(SessionFactoryScope scope) {
		User u = new User( "gavin" );
		scope.inTransaction(
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

		scope.inTransaction(
				s -> s.update( u )
		);

		u.getSessionData().clear();
		u.getEmailAddresses().add( 0, new Email( "gavin@nospam.com" ) );
		u.getEmailAddresses().add( new Email( "gavin.king@jboss.com" ) );

		scope.inTransaction(
				s -> s.update( u )
		);

		scope.inTransaction(
				s -> s.delete( u )
		);
	}

	@Test
	public void testValueMap(SessionFactoryScope scope) {
		scope.inTransaction(
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

		scope.inTransaction(
				s -> {
					User u2 = findUser( s );
//					User u2 = (User) s.createCriteria( User.class ).uniqueResult();
					assertFalse( Hibernate.isInitialized( u2.getSessionData() ) );
					assertEquals( 1, u2.getSessionData().size() );
					assertEquals( 2, u2.getEmailAddresses().size() );
					u2.getSessionData().put( "foo", "new foo value" );
					u2.getEmailAddresses().set( 1, new Email( "gavin@hibernate.org" ) );
					//u2.getEmailAddresses().remove(3);
					//u2.getEmailAddresses().remove(2);
				}
		);

		scope.inTransaction(
				s -> {
					User u2 = findUser( s );
//					User u2 = (User) s.createCriteria( User.class ).uniqueResult();
					assertFalse( Hibernate.isInitialized( u2.getSessionData() ) );
					assertEquals( 1, u2.getSessionData().size() );
					assertEquals( 2, u2.getEmailAddresses().size() );
					assertEquals( "new foo value", u2.getSessionData().get( "foo" ) );
					assertEquals( "gavin@hibernate.org", ( (Email) u2.getEmailAddresses().get( 1 ) ).getAddress() );
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
	public void testCollectionInheritance(SessionFactoryScope scope) {
		Zoo zoo = new Zoo();
		scope.inTransaction(
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

		scope.inTransaction(
				s -> {
					Zoo found = s.get( Zoo.class, zoo.getId() );
					found.getAnimals().size();
					s.delete( found );
				}
		);
	}
}
