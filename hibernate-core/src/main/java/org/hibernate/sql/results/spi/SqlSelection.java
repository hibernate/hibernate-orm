/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public interface SqlSelection {

	/**
	 * Get the reader used to read values for this selection
	 *
	 */
	SqlSelectionReader getSqlSelectionReader();

	// todo (6.0) : need this to encapsulate and expose the SqlSelectable or "thing to be selected"
	//		so that we can render the SQL select clause

	/**
	 * Get the position within the values array (0-based)
	 */
	int getValuesArrayPosition();

	/**
	 * Get the JDBC parameter position (1-based)
	 */
	default int getJdbcResultSetIndex() {
		return getValuesArrayPosition() + 1;
	}

	default void prepare(
			ResultSetMappingDescriptor.JdbcValuesMetadata jdbcResultsMetadata,
			ResultSetMappingDescriptor.ResolutionContext resolutionContext) {
		// By default we have nothing to do.  Here as a hook for NativeQuery mapping resolutions
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) : remove methods below

	/**
	 * todo (6.0) : why removing this one?
	 */
	void accept(SqlAstWalker interpreter);
}
