/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * State pertaining to the processing of a single row of a JdbcValuesSource
 *
 * @author Steve Ebersole
 */
public interface RowProcessingState {
	/**
	 * Access to the "parent state" related to the overall processing
	 * of the results.
	 */
	JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState();

	/**
	 * Retrieve the value corresponding to the given SqlSelection as part
	 * of the "current JDBC row".
	 *
	 * We read all the ResultSet values for the given row one time
	 * and store them into an array internally based on the principle that multiple
	 * accesses to this array will be significantly faster than accessing them
	 * from the ResultSet potentially multiple times.
	 */
	Object getJdbcValue(SqlSelection sqlSelection);

	void registerNonExists(EntityFetch fetch);

	void finishRowProcessing();
}
