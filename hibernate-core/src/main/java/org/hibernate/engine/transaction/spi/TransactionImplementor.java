/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.spi;

import org.hibernate.Transaction;

/**
 * Defines the "internal contract" for an implementation of {@link Transaction}.
 *
 * @author Steve Ebersole
 *
 * @deprecated This is no longer needed
 */
@Deprecated(since = "7.0", forRemoval = true)
public interface TransactionImplementor extends Transaction {
}
