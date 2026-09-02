/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.spi;

import org.hibernate.LockMode;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.spi.RowReader;

/**
 * State pertaining to the processing of a single "row" of a JdbcValuesSource
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface RowProcessingState extends ExecutionContext {
	/// Advance to the next row.
	/// @since 8.0
	boolean next();

	/// Move to the previous row.
	/// @since 8.0
	boolean previous();

	/// Move by the given relative number of rows.
	/// @since 8.0
	boolean scroll(int numberOfRows);

	/// Move to the given absolute position.
	/// @since 8.0
	boolean position(int position);

	/// Obtain the current row position.
	/// @since 8.0
	int getPosition();

	/// Move before the first row.
	/// @since 8.0
	void beforeFirst();

	/// Move to the first row.
	/// @since 8.0
	boolean first();

	/// Move to the last row.
	/// @since 8.0
	boolean last();

	/// Move after the last row.
	/// @since 8.0
	void afterLast();

	/// Whether the cursor is on the first row.
	/// @since 8.0
	boolean isFirst();

	/// Whether the cursor is on the last row.
	/// @since 8.0
	boolean isLast();

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
