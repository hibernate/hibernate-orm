/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.spi;

import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.service.Service;

/**
 * A {@link Service} that defines how Hibernate interacts with JTA on a certain
 * platform. In particular, a {@code JtaPlatform} allows Hibernate to obtain
 * the {@link TransactionManager} and {@link UserTransaction}, and register
 * {@link Synchronization}s.
 * <p>
 * An implementation may be selected by specifying the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#JTA_PLATFORM}. Alternatively,
 * a {@link JtaPlatformProvider} or even a custom {@link JtaPlatformResolver}
 * may be used.
 *
 * @see JtaPlatformResolver
 * @see JtaPlatformProvider
 *
 * @author Steve Ebersole
 */
public interface JtaPlatform extends Service {

	/**
	 * Locate the {@link TransactionManager}.
	 *
	 * @return The {@link TransactionManager}
	 */
	@Nullable TransactionManager retrieveTransactionManager();

	/**
	 * Locate the {@link UserTransaction}.
	 * <p>
	 * If {@link org.hibernate.cfg.AvailableSettings#PREFER_USER_TRANSACTION} is enabled, Hibernate
	 * will use the {@code UserTransaction} in preference to the {@link TransactionManager} where
	 * possible.
	 *
	 * @return The {@link UserTransaction}
	 */
	@Nullable UserTransaction retrieveUserTransaction();

	/**
	 * Determine an identifier for the given transaction appropriate for use in caching/lookup usages.
	 * <p>
	 * Generally speaking the transaction itself will be returned here.  This method was added specifically
	 * for use in WebSphere and other unfriendly Java EE containers.
	 *
	 * @param transaction The transaction to be identified.
	 * @return An appropriate identifier
	 */
	@Nullable Object getTransactionIdentifier(Transaction transaction);

	/**
	 * Can we currently register a {@link Synchronization}?
	 *
	 * @return True if registering a {@link Synchronization} is currently allowed; false otherwise.
	 */
	boolean canRegisterSynchronization();

	/**
	 * Register a JTA {@link Synchronization} in the means defined by the platform.
	 *
	 * @param synchronization The synchronization to register
	 */
	void registerSynchronization(Synchronization synchronization);

	/**
	 * Obtain the current transaction status using whatever means is preferred for this platform
	 *
	 * @return The current status.
	 *
	 * @throws SystemException Indicates a problem access the underlying status
	 */
	int getCurrentStatus() throws SystemException;
}
