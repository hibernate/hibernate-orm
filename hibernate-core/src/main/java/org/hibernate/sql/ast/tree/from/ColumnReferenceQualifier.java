/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.query.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public interface ColumnReferenceQualifier {
	default TableReference resolveTableReference(NavigablePath navigablePath, String tableExpression) {
		return resolveTableReference( navigablePath, tableExpression, true );
	}

	default TableReference resolveTableReference(String tableExpression) {
		return resolveTableReference( null, tableExpression, true );
	}

	/**
	 * Like {@link #getTableReference(NavigablePath, String, boolean, boolean)}, but will throw an exception if no
	 * table reference can be found, even after resolving possible table reference joins.
	 *
	 * @param navigablePath The path for which to look up the table reference, may be null
	 * @param tableExpression The table expression for which to look up the table reference
	 * @param allowFkOptimization Whether a foreign key optimization is allowed i.e. use the FK column on the key-side
	 *
	 * @throws UnknownTableReferenceException to indicate that the given tableExpression could not be resolved
	 */
	TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization);

	default TableReference getTableReference(NavigablePath navigablePath, String tableExpression) {
		return getTableReference( navigablePath, tableExpression, true, false );
	}

	default TableReference getTableReference(String tableExpression) {
		return getTableReference( null, tableExpression, true, false );
	}

	/**
	 * Returns the table reference for the table expression, or null if not found.
	 *
	 * @param navigablePath The path for which to look up the table reference, may be null
	 * @param tableExpression The table expression for which to look up the table reference
	 * @param allowFkOptimization Whether a foreign key optimization is allowed i.e. use the FK column on the key-side
	 * @param resolve Whether to potentially create table reference joins for this table group
	 */
	TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve);
}
