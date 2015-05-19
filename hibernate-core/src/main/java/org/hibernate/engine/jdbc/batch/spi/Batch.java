/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.spi;
import java.sql.PreparedStatement;

/**
 * Conceptually models a batch.
 * <p/>
 * Unlike directly in JDBC, here we add the ability to batch together multiple statements at a time.  In the underlying
 * JDBC this correlates to multiple {@link java.sql.PreparedStatement} objects (one for each DML string) maintained within the
 * batch.
 *
 * @author Steve Ebersole
 */
public interface Batch {
	/**
	 * Retrieves the object being used to key (uniquely identify) this batch.
	 *
	 * @return The batch key.
	 */
	public BatchKey getKey();

	/**
	 * Adds an observer to this batch.
	 *
	 * @param observer The batch observer.
	 */
	public void addObserver(BatchObserver observer);

	/**
	 * Get a statement which is part of the batch, creating if necessary (and storing for next time).
	 *
	 * @param sql The SQL statement.
	 * @param callable Is the SQL statement callable?
	 *
	 * @return The prepared statement instance, representing the SQL statement.
	 */
	public PreparedStatement getBatchStatement(String sql, boolean callable);

	/**
	 * Indicates completion of the current part of the batch.
	 */
	public void addToBatch();

	/**
	 * Execute this batch.
	 */
	public void execute();

	/**
	 * Used to indicate that the batch instance is no longer needed and that, therefore, it can release its
	 * resources.
	 */
	public void release();
}

