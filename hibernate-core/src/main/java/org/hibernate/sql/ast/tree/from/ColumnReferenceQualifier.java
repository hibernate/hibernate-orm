/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.query.NavigablePath;

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

	TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve);
}
