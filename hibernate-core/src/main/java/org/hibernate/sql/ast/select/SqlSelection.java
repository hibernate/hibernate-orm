/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.select;

import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;

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

	void accept(SqlAstSelectInterpreter interpreter);
}
