/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.internal;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.jdbc.Expectation;

/**
 * Normal implementation of BatchKey
 *
 * @author Steve Ebersole
 */
public class BasicBatchKey implements BatchKey {
	private final String comparison;
	private final int statementCount;
	private final Expectation expectation;

	/**
	 * Constructs a BasicBatchKey
	 *
	 * @param comparison A string used to compare batch keys.
	 * @param expectation The expectation for the batch
	 */
	public BasicBatchKey(String comparison, Expectation expectation) {
		this.comparison = comparison;
		this.statementCount = 1;
		this.expectation = expectation;
	}

	@Override
	public Expectation getExpectation() {
		return expectation;
	}

	@Override
	public int getBatchedStatementCount() {
		return statementCount;
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

}
