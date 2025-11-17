/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.SessionEventListener;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * An extension of {@link SessionCreationOptions} for cases where the
 * session to be created shares some part of the "transaction context"
 * of another session.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.SharedSessionBuilder
 *
 * @since 7.2
 */
public interface SharedSessionCreationOptions extends SessionCreationOptions {
	boolean isTransactionCoordinatorShared();
	TransactionCoordinator getTransactionCoordinator();
	JdbcCoordinator getJdbcCoordinator();
	Transaction getTransaction();
	TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacks();

	/**
	 * Registers callbacks for the child session to integrate with events of the parent session.
	 */
	void registerParentSessionObserver(ParentSessionObserver observer);

	/**
	 * Consolidated implementation of adding the parent session observer.
	 */
	default void registerParentSessionObserver(ParentSessionObserver observer, SharedSessionContractImplementor original) {
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
