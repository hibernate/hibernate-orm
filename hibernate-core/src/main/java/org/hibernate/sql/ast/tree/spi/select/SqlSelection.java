/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public interface SqlSelection {
	/**
	 * Get the selectable SQL expression.
	 */
	SqlSelectable getSqlSelectable();

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

	default void prepare(ResultSetAccess resultSetAccess, SharedSessionContractImplementor persistenceContext) {
		// By default we have nothing to do.  Here as a hook for NativeQuery mapping resolutions
	}

	void accept(SqlAstWalker interpreter);
}
