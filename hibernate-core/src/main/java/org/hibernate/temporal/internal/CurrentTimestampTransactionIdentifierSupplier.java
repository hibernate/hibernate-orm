/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.internal;

import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.TransactionIdentifierSupplier;

import java.time.Instant;

/**
 * Simple implementation that queries the database current timestamp.
 */
public class CurrentTimestampTransactionIdentifierSupplier implements TransactionIdentifierSupplier<Instant> {
	@Override
	public Instant generateTransactionIdentifier(SharedSessionContract session) {
		return session.createSelectionQuery( "select instant", Instant.class ).getSingleResult();
	}
}
