/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.actionqueue;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for HHH-20863: BeforeCompletionCallbacks are not cleared on rollback.
 * When a transaction is rolled back after a flush that registered BeforeCompletionCallbacks,
 * the callbacks remain in the ActionQueue. This causes a HHH90010101 warning when the
 * session is closed.
 *
 * @author Andrea Boriero
 */
@DomainModel(annotatedClasses = {BeforeCompletionCallbackRollbackTest.Product.class})
@SessionFactory
@MessageKeyInspection(
		messageKey = "HHH90010101",
		logger = @Logger(loggerName = "org.hibernate.orm.session")
)
@JiraKey("HHH-20863")
public class BeforeCompletionCallbackRollbackTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.getSessionFactory().getSchemaManager().truncate() );
	}

	@Test
	public void testBeforeCompletionCallbackClearedOnRollback(SessionFactoryScope scope, MessageKeyWatcher watcher) {
		assertThrows( Exception.class, () ->
			scope.inTransaction( session -> {
				registerCallbacks( session );

				session.persist( new Product( 1L, "Test Product" ) );

				// Now cause a rollback by creating a duplicate key violation
				// Insert a product with the same ID
				session.persist( new Product( 1L, "Duplicate Product" ) );
			} )
		);

		// The HHH90010101 warning should NOT be logged because the callbacks should be cleared
		assertFalse(
				watcher.wasTriggered(),
				"HHH90010101 warning should not be logged after rollback: " + watcher.getTriggeredMessages()
		);
	}

	@Test
	public void testBeforeCompletionCallbackClearedOnExplicitRollback(SessionFactoryScope scope, MessageKeyWatcher watcher) {

		AtomicBoolean beforeCompletionCallbackInvoked = new AtomicBoolean( false );

		scope.inTransaction( session -> {
			registerCallbacks( session, beforeCompletionCallbackInvoked );
			session.persist( new Product( 1L, "Test Product" ) );

			// Explicitly mark the transaction for rollback
			session.getTransaction().setRollbackOnly();
		} );

		// The callback should NOT have been invoked since we rolled back
		assertFalse( beforeCompletionCallbackInvoked.get(), "Callback should not be invoked on rollback" );

		// The HHH90010101 warning should NOT be logged because the callbacks should be cleared
		assertFalse(
				watcher.wasTriggered(),
				"HHH90010101 warning should not be logged after explicit rollback: " + watcher.getTriggeredMessages()
		);

		// Verify the transaction was indeed rolled back
		scope.inTransaction( session -> {
			Long count = session.createQuery( "select count(*) from Product", Long.class )
					.uniqueResult();
			assertEquals( 0L, count, "No products should exist after rollback" );
		} );
	}

	@Test
	public void testBeforeCompletionCallbackInvokedOnCommit(SessionFactoryScope scope) {
		AtomicBoolean beforeCompletionCallbackInvoked = new AtomicBoolean( false );

		scope.inTransaction( session -> {
			registerCallbacks( session, beforeCompletionCallbackInvoked );

			session.persist( new Product( 1L, "Test Product" ) );
		} );

		// The callback SHOULD have been invoked on commit
		assertTrue( beforeCompletionCallbackInvoked.get(), "Callback should be invoked on commit" );

		// Verify the transaction was committed
		scope.inTransaction( session -> {
			Long count = session.createQuery( "select count(*) from Product", Long.class )
					.uniqueResult();
			assertEquals( 1L, count, "One product should exist after commit" );
		} );
	}

	private static void registerCallbacks(SessionImplementor session) {
		// register before completion callback that does nothing
		session.getActionQueue().registerCallback(
				s -> {
				}
		);
		// register after completion callback that does nothing
		session.getActionQueue().registerCallback(
				(success, s) -> {
				}
		);
	}

	private static void registerCallbacks(SessionImplementor session, AtomicBoolean beforeCallbackInvoked) {
		// register before completion callback that sets the AtomicBoolean to true when invoked
		session.getActionQueue().registerCallback(
				s -> {
					beforeCallbackInvoked.set( true );
				}
		);
		// register after completion callback that does nothing
		session.getActionQueue().registerCallback(
				(success, s) -> {
				}
		);
	}

	@Entity(name = "Product")
	@Table(name = "PRODUCTS")
	public static class Product {
		@Id
		private Long id;

		private String name;

		public Product() {
		}

		public Product(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
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
