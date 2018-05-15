/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * Represents a selection at the SQL/JDBC level.  Essentially made up of:
 *
 * 		{@link #getJdbcValueExtractor}:: How to read a value from JDBC (conceptually similar to a method reference)
 * 		{@link #getValuesArrayPosition}:: The position for this selection in relation to the "JDBC values array" (see {@link RowProcessingState#getJdbcValue})
 * 		{@link #getJdbcResultSetIndex()}:: The position for this selection in relation to the JDBC object (ResultSet, etc)
 *
 * @author Steve Ebersole
 */
public interface SqlSelection extends SqlSelectionGroupNode {
	/**
	 * Get the extractor that can be used to extract JDBC values for this selection
	 */
	JdbcValueExtractor getJdbcValueExtractor();

	// todo (6.0) : add proper support for "virtual" selections
	//		- things like selecting a literal or a parameter
	//		- would require the ability to define the "value
	// 			arrays" position and the ResultSet position
	//			independently on the impls side.
	//		- would require understanding whether the selection
	//			occurs in the root query or a subquery. If used
	//			in a subquery we need to render the parameter rather
	//			than just manage it locally.

	/**
	 * Get the position within the "JDBC values" array (0-based).  Negative indicates this is
	 * not a "real" selection
	 */
	int getValuesArrayPosition();

	/**
	 * Get the JDBC position (1-based)
	 */
	default int getJdbcResultSetIndex() {
		return getValuesArrayPosition() + 1;
	}

	default void prepare(
			ResultSetMappingDescriptor.JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		// By default we have nothing to do.  Here as a hook for NativeQuery mapping resolutions
	}

	@Override
	default Object hydrateStateArray(RowProcessingState currentRowState) {
		return currentRowState.getJdbcValue( this );
	}

	@Override
	default void visitSqlSelections(Consumer<SqlSelection> action) {
		action.accept( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) : remove methods below

	/**
	 * todo (6.0) : why removing this one?
	 */
	void accept(SqlAstWalker interpreter);
}
