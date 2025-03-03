/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Locale;

import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public interface ColumnReferenceQualifier {

	default TableReference resolveTableReference(String tableExpression) {
		return resolveTableReference( null, tableExpression );
	}

	/**
	 * Like {@link #getTableReference(NavigablePath, String, boolean)}, but will throw an exception if no
	 * table reference can be found, even after resolving possible table reference joins.
	 *
	 * @param navigablePath The path for which to look up the table reference, may be null
	 * @param tableExpression The table expression for which to look up the table reference
	 *
	 * @throws UnknownTableReferenceException to indicate that the given tableExpression could not be resolved
	 */
	default TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression) {
		assert tableExpression != null;

		final TableReference tableReference = getTableReference(
				navigablePath,
				tableExpression,
				true
		);

		if ( tableReference == null ) {
			throw new UnknownTableReferenceException(
					tableExpression,
					String.format(
							Locale.ROOT,
							"Unable to determine TableReference (`%s`) for `%s`",
							tableExpression,
							navigablePath
					)
			);
		}

		return tableReference;
	}

	default TableReference resolveTableReference(
			NavigablePath navigablePath,
			ValuedModelPart modelPart,
			String tableExpression) {
		assert modelPart != null;

		final TableReference tableReference = getTableReference(
				navigablePath,
				modelPart,
				tableExpression,
				true
		);

		if ( tableReference == null ) {
			throw new UnknownTableReferenceException(
					tableExpression,
					String.format(
							Locale.ROOT,
							"Unable to determine TableReference (`%s`) for `%s`",
							tableExpression,
							navigablePath
					)
			);
		}

		return tableReference;
	}

	default TableReference getTableReference(NavigablePath navigablePath, String tableExpression) {
		return getTableReference( navigablePath, tableExpression, false );
	}

	default TableReference getTableReference(String tableExpression) {
		return getTableReference( null, tableExpression, false );
	}

	/**
	 * Returns the table reference for the table expression, or null if not found.
	 *
	 * @param navigablePath The path for which to look up the table reference, may be null
	 * @param tableExpression The table expression for which to look up the table reference
	 * @param resolve Whether to potentially create table reference joins for this table group
	 */
	TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve);
	default TableReference getTableReference(
			NavigablePath navigablePath,
			ValuedModelPart modelPart,
			String tableExpression,
			boolean resolve) {
		return getTableReference( navigablePath, tableExpression, resolve );
	}
}
