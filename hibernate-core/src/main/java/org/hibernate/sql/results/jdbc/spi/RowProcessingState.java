/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.spi;

import org.hibernate.LockMode;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.spi.RowReader;

/**
 * State pertaining to the processing of a single "row" of a JdbcValuesSource
 *
 * @author Steve Ebersole
 */
public interface RowProcessingState extends ExecutionContext {
	/**
	 * Access to the state related to the overall processing of the results.
	 */
	JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState();

	LockMode determineEffectiveLockMode(String alias);

	boolean needsResolveState();

	<T extends InitializerData> T getInitializerData(int initializerId);
	void setInitializerData(int initializerId, InitializerData state);

	/**
	 * Retrieve the value corresponding to the given SqlSelection as part
	 * of the "current JDBC row".
	 *
	 * @see SqlSelection#getValuesArrayPosition()
	 * @see #getJdbcValue(int)
	 */
	default Object getJdbcValue(SqlSelection sqlSelection) {
		return getJdbcValue( sqlSelection.getValuesArrayPosition() );
	}

	/**
	 * todo (6.0) : do we want this here?  Depends how we handle caching assembler / result memento
	 */
	RowReader<?> getRowReader();

	/**
	 * Retrieve the value corresponding to the given index as part
	 * of the "current JDBC row".
	 *
	 * We read all the ResultSet values for the given row one time
	 * and store them into an array internally based on the principle that multiple
	 * accesses to this array will be significantly faster than accessing them
	 * from the ResultSet potentially multiple times.
	 */
	Object getJdbcValue(int position);

	void registerNonExists(EntityFetch fetch);

	boolean isQueryCacheHit();

	/**
	 * Callback at the end of processing the current "row"
	 */
	void finishRowProcessing(boolean wasAdded);

	/**
	 * If this is a row processing state for aggregate components,
	 * this will return the underlying row processing state.
	 */
	default RowProcessingState unwrap() {
		return this;
	}

}
