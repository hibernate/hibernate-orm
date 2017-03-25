/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines the resource-level transaction capabilities of Hibernate, which revolves around the
 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinator} contract.
 * <p/>
 * TransactionCoordinator instances can be obtained from
 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder}, which is a Service
 * and available from the StandardServiceRegistry
 * <p/>
 * A few terms/concepts to keep in mind here...
 *
 * <h2>Local transaction</h2>
 *
 * The local transaction is the idea of transactionality exposed to the application (as
 * {@link org.hibernate.Transaction}) as a means to control the underlying transaction.  That
 * control flows from the {@link org.hibernate.Transaction} into the TransactionCoordinator
 * through the {@link org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver} it exposes.
 *
 * <h2>Physical transaction</h2>
 *
 * This is the physical underlying transaction that ultimately controls the database transaction.  This
 * can be:<ul>
 *     <li>
 *       a JTA transaction, as expressed by {@link javax.transaction.UserTransaction} or
 *       {@link javax.transaction.Transaction})
 *     </li>
 *     <li>
 *         a "JDBC transaction", as expressed through the JDBC {@link java.sql.Connection} object
 *     </li>
 * </ul>
 *
 * The corresponding concrete TransactionCoordinator implementations manage that bridging internally.
 *
 * <h2>Local Synchronization</h2>
 *
 * The Hibernate transaction api allows the application itself to register JTA Synchronization
 * objects with the TransactionCoordinator.  These local Synchronizations work in all transaction
 * environments.  See {@link org.hibernate.Transaction#registerSynchronization} and
 * {@link org.hibernate.resource.transaction.spi.SynchronizationRegistry} for additional details.
 *
 */
package org.hibernate.resource.transaction;
