/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ops.genericApi;

import java.util.NoSuchElementException;
import java.util.Optional;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.annotations.GenericGenerator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = BasicGetLoadAccessTest.User.class
)
@SessionFactory
public class BasicGetLoadAccessTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session ->
						session.createQuery( "delete from User" ).executeUpdate()
		);
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create a row
		scope.inTransaction(
				session ->
						session.persist( new User( "steve" ) )
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test `get` access
		scope.inTransaction(
				session ->
						session.get( User.class, 1 )
		);


		scope.inTransaction(
				session ->
						session.get( User.class, 1, LockMode.PESSIMISTIC_WRITE )
		);

		scope.inTransaction(
				session ->
						session.get( User.class, 1, LockOptions.UPGRADE )
		);

		scope.inTransaction(
				session ->
						session.byId( User.class ).load( 1 )
		);

		scope.inTransaction(
				session ->
						session.byId( User.class ).with( LockOptions.UPGRADE ).load( 1 )
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test `load` access
		scope.inTransaction(
				session ->
						session.load( User.class, 1 )
		);

		scope.inTransaction(
				session ->
						session.load( User.class, 1, LockMode.PESSIMISTIC_WRITE )
		);

		scope.inTransaction(
				session ->
						session.load( User.class, 1, LockOptions.UPGRADE )
		);

		scope.inTransaction(
				session ->
						session.byId( User.class ).getReference( 1 )
		);

		scope.inTransaction(
				session ->
						session.byId( User.class ).with( LockOptions.UPGRADE ).getReference( 1 )
		);
	}

	@Test
	public void testNullLoadResult(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertNull( session.byId( User.class ).load( -1 ) );

					Optional<User> user = session.byId( User.class ).loadOptional( -1 );
					assertNotNull( user );
					assertFalse( user.isPresent() );
					try {
						user.get();
						fail( "Expecting call to Optional#get to throw NoSuchElementException" );
					}
					catch (NoSuchElementException expected) {
						// the expected result...
					}
				}
		);
	}

	@Test
	public void testNullQueryResult(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertNull( session.createQuery( "select u from User u where u.id = -1" ).uniqueResult() );

					Optional<User> user = session.createQuery( "select u from User u where u.id = -1" ).uniqueResultOptional();
					assertNotNull( user );
					assertFalse( user.isPresent() );
					try {
						user.get();
						fail( "Expecting call to Optional#get to throw NoSuchElementException" );
					}
					catch (NoSuchElementException expected) {
						// the expected result...
					}
				}
		);
	}

	@Entity(name = "User")
	@Table(name = "my_user")
	public static class User {
		private Integer id;
		private String name;

		public User() {
		}

		public User(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue(generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
