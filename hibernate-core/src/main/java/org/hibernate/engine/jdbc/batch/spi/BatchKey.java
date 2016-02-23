/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.spi;

import org.hibernate.jdbc.Expectation;

/**
 * Unique key for batch identification.
 *
 * @author Steve Ebersole
 */
public interface BatchKey {
	/**
	 * How many statements will be in this batch?
	 * <p/>
	 * Note that this is distinctly different than the size of the batch.
	 *
	 * @return The number of statements.
	 */
	int getBatchedStatementCount();

	/**
	 * Get the expectation pertaining to the outcome of the {@link Batch} associated with this key.
	 *
	 * @return The expectations
	 */
	Expectation getExpectation();
}
