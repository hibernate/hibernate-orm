/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Implementations of {@link org.hibernate.resource.transaction.spi.TransactionCoordinator}
 * based on JTA.
 * <p>
 * The {@link org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionAdapter}
 * abstracts over access to JTA via {@link jakarta.transaction.UserTransaction} or via
 * {@link jakarta.transaction.TransactionManager}.
 */
package org.hibernate.resource.transaction.backend.jta.internal;
