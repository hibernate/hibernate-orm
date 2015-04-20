/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

/**
 * Defines the resource-level transaction capabilities of Hibernate, which revolves around the
 * {@link org.hibernate.resource.transaction.TransactionCoordinator} contract.  See
 * {@link org.hibernate.resource.transaction.TransactionCoordinatorBuilder} and
 * {@link org.hibernate.resource.transaction.TransactionCoordinatorBuilderFactory}
 * for information on obtaining TransactionCoordinator instances.
 *
 * <p/>
 *
 * A few terms/concepts to keep in mind here...
 *
 * <h2>Local transaction</h2>
 *
 * The local transaction is the idea of transactionality exposed to the application (as
 * {@link org.hibernate.Transaction}) as a means to control the underlying transaction.  That
 * control flows from the {@link org.hibernate.Transaction} into the TransactionCoordinator
 * through the {@link org.hibernate.resource.transaction.TransactionCoordinator.LocalInflow} it exposes.
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
 * {@link org.hibernate.resource.transaction.SynchronizationRegistry} for additional details.
 *
 */
package org.hibernate.resource.transaction;
