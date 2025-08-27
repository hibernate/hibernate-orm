/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops.genericApi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;
import org.hibernate.LockMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = BasicGetLoadAccessTest.User.class
)
@SessionFactory
public class BasicGetLoadAccessTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
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
		// test `find` access
		scope.inTransaction(
				session ->
						session.find( User.class, 1 )
		);

		scope.inTransaction(
				session ->
						session.find( User.class, 1, LockMode.PESSIMISTIC_WRITE )
		);

		scope.inTransaction(
				session ->
						session.find( User.class, 1, LockModeType.PESSIMISTIC_WRITE )
		);

		scope.inTransaction(
				session ->
						session.byId( User.class ).load( 1 )
		);

		scope.inTransaction(
				session ->
						session.byId( User.class ).with( LockMode.PESSIMISTIC_WRITE ).load( 1 )
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test `getReference` access
		scope.inTransaction(
				session ->
						session.getReference( User.class, 1 )
		);

		scope.inTransaction(
				session ->
						session.find( User.class, 1, LockMode.PESSIMISTIC_WRITE )
		);

		scope.inTransaction(
				session ->
						session.byId( User.class ).getReference( 1 )
		);

		scope.inTransaction(
				session ->
						session.byId( User.class ).with( LockMode.PESSIMISTIC_WRITE ).getReference( 1 )
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
