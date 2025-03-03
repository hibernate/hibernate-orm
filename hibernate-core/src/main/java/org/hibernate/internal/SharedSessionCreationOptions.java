/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * An extension of {@link SessionCreationOptions} for cases where the
 * session to be created shares some part of the "transaction context"
 * of another session.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.SharedSessionBuilder
 */
public interface SharedSessionCreationOptions extends SessionCreationOptions {
	boolean isTransactionCoordinatorShared();
	TransactionCoordinator getTransactionCoordinator();
	JdbcCoordinator getJdbcCoordinator();
	Transaction getTransaction();
	ActionQueue.TransactionCompletionProcesses getTransactionCompletionProcesses();
}
