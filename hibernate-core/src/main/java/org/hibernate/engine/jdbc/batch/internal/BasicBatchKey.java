/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
