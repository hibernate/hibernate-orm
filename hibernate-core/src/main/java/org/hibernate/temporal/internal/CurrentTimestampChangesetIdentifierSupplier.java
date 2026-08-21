/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.internal;

import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;

import java.time.Instant;

/**
 * Simple implementation that queries the database current timestamp.
 */
public class CurrentTimestampChangesetIdentifierSupplier implements ChangesetIdentifierSupplier<Instant> {
	@Override
	public Instant generateIdentifier(SharedSessionContract session) {
		return session.createSelectionQuery( "select instant", Instant.class ).getSingleResult();
	}
}
