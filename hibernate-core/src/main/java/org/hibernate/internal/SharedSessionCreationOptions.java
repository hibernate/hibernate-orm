/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * An extension of SessionCreationOptions for cases where the Session to be created shares
 * some part of the "transaction context" of another Session.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.SharedSessionBuilder
 */
public interface SharedSessionCreationOptions extends SessionCreationOptions {
	boolean isTransactionCoordinatorShared();
	TransactionCoordinator getTransactionCoordinator();
	JdbcCoordinator getJdbcCoordinator();
	TransactionImplementor getTransaction();
	ActionQueue.TransactionCompletionProcesses getTransactionCompletionProcesses();
}
