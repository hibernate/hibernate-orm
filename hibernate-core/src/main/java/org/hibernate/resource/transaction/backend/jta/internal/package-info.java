/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
