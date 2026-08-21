/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines the resource-level transaction capabilities of Hibernate, centered
 * around the {@link org.hibernate.resource.transaction.spi.TransactionCoordinator}
 * contract.
 * <p>
 * An instance of {@code TransactionCoordinator} may be constructed using a
 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder},
 * which is a {@link org.hibernate.service.Service} and available via the
 * {@link org.hibernate.boot.registry.StandardServiceRegistry}.
 *
 * <h3>Resource-local transaction</h3>
 *
 * A <em>resource-local</em> transaction is exposed to the application as an
 * instance of {@link org.hibernate.Transaction}, allowing full control over the
 * transaction lifecycle. That control flows from the {@code Transaction} into
 * the {@code TransactionCoordinator} via its exposed
 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver}.
 *
 * <h3>Physical transaction</h3>
 *
 * It is the underlying <em>physical transaction</em> which ultimately controls
 * the database transaction. This might be:
 * <ul>
 * <li>a JTA transaction, represented by a {@link jakarta.transaction.Transaction}
 *     or {@link jakarta.transaction.UserTransaction}, or
 * <li>a "JDBC transaction", as expressed via the JDBC {@link java.sql.Connection}.
 * </ul>
 * <p>
 * The corresponding concrete implementations of {@code TransactionCoordinator}
 * manage the necessary bridging.
 *
 * <h3>Local Synchronization</h3>
 *
 * The Hibernate transaction API allows the application itself to register JTA
 * {@link jakarta.transaction.Synchronization} objects with the
 * {@code TransactionCoordinator}. These local {@code Synchronization}s work in
 * all transaction environments.
 * <p>
 * See {@link org.hibernate.Transaction#registerSynchronization} and
 * {@link org.hibernate.resource.transaction.spi.SynchronizationRegistry} for
 * additional details.
 */
package org.hibernate.resource.transaction;
