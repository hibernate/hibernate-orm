/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.internal;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;

/**
 * Normal implementation of {@link BatchKey}
 *
 * @author Steve Ebersole
 */
public record BasicBatchKey(String comparison) implements BatchKey {
	@Override
	public String toLoggableString() {
		return comparison;
	}

	@Override
	public String toString() {
		return "BasicBatchKey(" + comparison + ")";
	}
}
