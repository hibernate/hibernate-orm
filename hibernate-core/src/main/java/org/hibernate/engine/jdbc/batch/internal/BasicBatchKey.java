/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.internal;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.jdbc.Expectations;

/**
 * Normal implementation of BatchKey
 *
 * @author Steve Ebersole
 */
public class BasicBatchKey implements BatchKey {
	private final String comparison;

	/**
	 * Constructs a BasicBatchKey with {@link Expectations#NONE}
	 */
	public BasicBatchKey(String comparison) {
		this.comparison = comparison;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final BasicBatchKey that = (BasicBatchKey) o;
		return comparison.equals( that.comparison );
	}

	@Override
	public int hashCode() {
		return comparison.hashCode();
	}

	@Override
	public String toLoggableString() {
		return comparison;
	}

	@Override
	public String toString() {
		return "BasicBatchKey(" + comparison + ")";
	}
}
