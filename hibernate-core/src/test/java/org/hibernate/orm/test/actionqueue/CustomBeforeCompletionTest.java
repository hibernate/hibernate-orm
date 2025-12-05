/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.actionqueue;

import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.HibernateException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {CustomBeforeCompletionTest.SimpleEntity.class})
@SessionFactory
public class CustomBeforeCompletionTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.getSessionFactory().getSchemaManager().truncate() );
	}

	@Test
	@JiraKey(value = "HHH-13666")
	public void success(SessionFactoryScope scope) {
		scope.inSession( session -> {
			AtomicBoolean called = new AtomicBoolean( false );
			session.getActionQueue().registerCallback( s -> called.set(true) );
			Assertions.assertFalse( called.get() );
			scope.inTransaction( session, theSession -> theSession.persist(new SimpleEntity("jack")) );
			Assertions.assertTrue( called.get() );
		} );

		// Check that the transaction was committed
		scope.inTransaction( session -> {
			long count = session.createQuery( "select count(*) from SimpleEntity", Long.class )
					.uniqueResult();
			assertEquals( 1L, count );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13666")
	public void failure(SessionFactoryScope scope) {
		try {
			scope.inSession( session -> {
				session.getActionQueue().registerCallback( session1 -> {
					throw new RuntimeException( "My exception" );
				} );
				scope.inTransaction( session, theSession -> theSession.persist(new SimpleEntity("jack")) );
			} );
			Assertions.fail( "Expected exception to be thrown" );
		}
		catch (Exception e) {
			assertThat( e, instanceOf( HibernateException.class ) );
			assertThat( e.getMessage(), containsString( "Unable to perform beforeTransactionCompletion callback" ) );
			Throwable cause = e.getCause();
			assertThat( cause, instanceOf( RuntimeException.class ) );
			assertThat( cause.getMessage(), containsString( "My exception" ) );

			// Make sure that the original message is appended to the wrapping exception's message, for convenience
			assertThat( e.getMessage(), containsString( "My exception" ) );
		}

		// Check that the transaction was rolled back
		scope.inTransaction( session -> {
			long count = session.createQuery( "select count(*) from SimpleEntity", Long.class )
					.uniqueResult();
			assertEquals( 0L, count );
		} );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		SimpleEntity() {
		}

		SimpleEntity(String name) {
			this.name = name;
		}

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
