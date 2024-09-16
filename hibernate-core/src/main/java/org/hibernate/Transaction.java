/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.EntityTransaction;
import jakarta.transaction.Synchronization;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Represents a resource-local transaction, where <em>resource-local</em> is interpreted
 * by Hibernate to mean any transaction under the control of Hibernate. That is to say,
 * the underlying transaction might be a JTA transaction, or it might be a JDBC transaction,
 * depending on how Hibernate is configured.
 * <p>
 * Every resource-local transaction is associated with a {@link Session} and begins with
 * an explicit call to {@link Session#beginTransaction()}, or, equivalently, with
 * {@code session.getTransaction().begin()}, and ends with a call to {@link #commit()}
 * or {@link #rollback()}.
 * <p>
 * A single session might span multiple transactions since the notion of a session
 * (a conversation between the application and the datastore) is of coarser granularity
 * than the concept of a database transaction. However, there is at most one uncommitted
 * transaction associated with a given {@link Session} at any time.
 * <p>
 * Note that this interface is never used to control container managed JTA transactions,
 * and is not usually used to control transactions that affect multiple resources.
 * <p>
 * A {@code Transaction} object is not threadsafe.
 *
 * @author Anton van Straaten
 * @author Steve Ebersole
 *
 * @see Session#beginTransaction()
 */
public interface Transaction extends EntityTransaction {
	/**
	 * Get the current {@linkplain TransactionStatus status} of this transaction.
	 */
	TransactionStatus getStatus();

	/**
	 * Register a user {@link Synchronization synchronization callback} for this transaction.
	 *
	 * @param synchronization The {@link Synchronization} callback to register.
	 *
	 * @throws HibernateException Indicates a problem registering the synchronization.
	 */
	void registerSynchronization(Synchronization synchronization);

	/**
	 * Set the transaction timeout for any transaction started by any subsequent call to
	 * {@link #begin} on this instance.
	 *
	 * @param seconds The number of seconds before a timeout.
	 */
	void setTimeout(int seconds);

	/**
	 * Retrieve the transaction timeout set for this instance. A negative integer indicates
	 * that no timeout has been set.
	 *
	 * @return The timeout, in seconds.
	 */
	@Nullable Integer getTimeout();

	/**
	 * Attempt to mark the underlying transaction for rollback only.
	 */
	default void markRollbackOnly() {
		setRollbackOnly();
	}

}
