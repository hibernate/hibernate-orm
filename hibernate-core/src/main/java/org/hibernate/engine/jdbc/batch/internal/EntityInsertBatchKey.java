/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.internal;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;

/**
 * Batch key used for entity insert statements.
 * @author Gavin King
 */
public record EntityInsertBatchKey(String comparison) implements BatchKey {
	@Override
	public String toLoggableString() {
		return comparison;
	}

	@Override
	public String toString() {
		return "EntityInsertBatchKey(" + comparison + ")";
	}
}
