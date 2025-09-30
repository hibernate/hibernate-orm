/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
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
	void registerParentSessionObserver(ParentSessionObserver callbacks);

}
