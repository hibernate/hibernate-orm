/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.batch.spi;

import java.sql.PreparedStatement;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;

/**
 * Represents a batch of statements to be executed together.
 * <p>
 * Unlike in JDBC, here we add the ability to batch together multiple statements at a time.
 * In the underlying JDBC this correlates to multiple {@link PreparedStatement} objects,
 * one for each DML string, all maintained within the batch.
 * <p>
 * A batch is usually associated with a {@link org.hibernate.engine.jdbc.spi.JdbcCoordinator}.
 *
 * @author Steve Ebersole
 * 
 * @see org.hibernate.engine.jdbc.spi.JdbcCoordinator#getBatch(BatchKey, Integer, Supplier)
 */
@Incubating
public interface Batch {
	/**
	 * Retrieves the object being used to key (uniquely identify) this batch.
	 *
	 * @return The batch key.
	 */
	BatchKey getKey();

	/**
	 * Adds an observer to this batch.
	 *
	 * @param observer The batch observer.
	 */
	void addObserver(BatchObserver observer);

	PreparedStatementGroup getStatementGroup();

	/**
	 * Apply the value bindings to the batch JDBC statements and indicates completion
	 * of the current part of the batch.
	 */
	void addToBatch(JdbcValueBindings jdbcValueBindings, TableInclusionChecker inclusionChecker);

	/**
	 * Execute this batch.
	 */
	void execute();

	/**
	 * Used to indicate that the batch instance is no longer needed and that, therefore,
	 * it can release its resources.
	 */
	void release();
}
