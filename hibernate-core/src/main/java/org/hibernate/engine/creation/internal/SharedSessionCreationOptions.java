/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.SessionEventListener;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * An extension of {@link SessionCreationOptions} for cases
 * where the session to be created shares some part of the
 * "transaction context" of another session.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.SharedSessionBuilder
 *
 * @since 7.2
 */
public interface SharedSessionCreationOptions extends SessionCreationOptions {
	/**
	 * Is the transaction coordinator shared with another
	 * session?
	 * @return {@code true} if it is shared
	 */
	boolean isTransactionCoordinatorShared();

	/**
	 * If the transaction coordinator is shared, return it,
	 * otherwise, return {@code null}.
	 */
	@Nullable
	TransactionCoordinator getTransactionCoordinator();

	/**
	 * If the transaction coordinator is shared, return the
	 * {@link JdbcCoordinator}, otherwise, return {@code null}.
	 */
	@Nullable
	JdbcCoordinator getJdbcCoordinator();

	/**
	 * If the transaction coordinator is shared, return the
	 * {@link Transaction}, otherwise, return {@code null}.
	 */
	@Nullable
	Transaction getTransaction();

	/**
	 * If the transaction coordinator is shared, return the
	 * {@link TransactionCompletionCallbacksImplementor},
	 * otherwise, return {@code null}.
	 */
	@Nullable
	TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacks();

	/**
	 * Registers callbacks for the child session to integrate
	 * with events of the parent session.
	 */
	void registerParentSessionObserver(@Nonnull ParentSessionObserver observer);

	/**
	 * Consolidated implementation of adding the parent
	 * session observer.
	 */
	default void registerParentSessionObserver(
			@Nonnull ParentSessionObserver observer,
			@Nonnull SharedSessionContractImplementor original) {
		original.getEventListenerManager().addListener( new SessionEventListener() {
			@Override
			public void flushEnd(int numberOfEntities, int numberOfCollections) {
				observer.onParentFlush();
			}

			@Override
			public void partialFlushEnd(int numberOfEntities, int numberOfCollections) {
				observer.onParentFlush();
			}

			@Override
			public void end() {
				observer.onParentClose();
			}
		} );
	}

}
