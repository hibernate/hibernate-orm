/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.actionqueue;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.resource.transaction.spi.TransactionStatus;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(annotatedClasses = {CustomAfterCompletionTest.SimpleEntity.class})
@SessionFactory
public class CustomAfterCompletionTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.getSessionFactory().getSchemaManager().truncate() );
	}

	@Test
	@JiraKey(value = "HHH-13666")
	public void success(SessionFactoryScope scope) {
		scope.inSession( session -> {
			AtomicBoolean called = new AtomicBoolean( false );
			session.unwrap( EventSource.class ).getActionQueue().registerCallback(
					(success, session1) -> called.set(true) );
			Assertions.assertFalse( called.get() );
			scope.inTransaction( session, theSession -> theSession.persist(new SimpleEntity("jack")) );
			Assertions.assertTrue( called.get() );
		} );

		// Check that the transaction was committed
		scope.inTransaction( session -> {
			long count = session.createQuery( "select count(*) from SimpleEntity", Long.class ).uniqueResult();
			assertEquals( 1L, count );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13666")
	public void failure(SessionFactoryScope scope) {
		try {
			scope.inSession( session -> {
				session.unwrap( EventSource.class ).getActionQueue().registerCallback(
						(success, session1) -> {throw new RuntimeException( "My exception" );} );
				scope.inTransaction( session, theSession -> theSession.persist(new SimpleEntity("jack")) );
			} );
			Assertions.fail( "Expected exception to be thrown" );
		}
		catch (Exception e) {
			assertThat( e, instanceOf( HibernateException.class ) );
			assertThat( e.getMessage(), containsString( "Unable to perform afterTransactionCompletion callback" ) );
			Throwable cause = e.getCause();
			assertThat( cause, instanceOf( RuntimeException.class ) );
			assertThat( cause.getMessage(), containsString( "My exception" ) );

			// Make sure that the original message is appended to the wrapping exception's message, for convenience
			assertThat( e.getMessage(), containsString( "My exception" ) );
		}

		// Check that the transaction was committed
		scope.inTransaction( session -> {
			long count = session.createQuery( "select count(*) from SimpleEntity", Long.class )
					.uniqueResult();
			assertEquals( 1L, count );
		} );
	}

	@Test
	public void callbackRunsOnJdbcCommitFailure(SessionFactoryScope scope) {
		class Work implements Consumer<SessionImplementor> {
			Boolean successful;
			TransactionStatus status;
			@Override
			public void accept(SessionImplementor session) {

				// Register a custom after-completion callback before starting the transaction so we can verify
				// that JDBC commit failures still drive the ActionQueue completion callbacks.
				session.unwrap( EventSource.class ).getActionQueue()
						.registerCallback( (success, s) -> successful = success );

				var transaction = session.beginTransaction();
				transaction.runAfterCompletion( status -> this.status = status );
				// Closing the connection makes the following commit fail while still exercising the normal
				// transaction completion path.
				session.doWork( Connection::close );

				var exception = assertThrows( TransactionException.class, transaction::commit );
				assertThat( exception.getMessage(),
						containsString( "Unable to commit against JDBC Connection" ) );

				assertNotNull( successful );
				assertFalse( successful );
				assertNotNull( status );
				assertEquals( TransactionStatus.FAILED_COMMIT, status );
			}
		}
		scope.inSession( new Work() );
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
