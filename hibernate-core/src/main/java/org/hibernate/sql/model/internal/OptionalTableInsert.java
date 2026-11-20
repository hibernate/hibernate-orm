/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.model.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.MutatingTableReference;

import java.util.List;

public class OptionalTableInsert extends TableInsertStandard {

	private final @Nullable String constraintName;
	private final List<String> constraintColumnNames;

	public OptionalTableInsert(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnReference> returningColumns,
			List<ColumnValueParameter> parameters,
			@Nullable String constraintName,
			List<String> constraintColumnNames) {
		super( mutatingTable, mutationTarget, valueBindings, returningColumns, parameters );
		this.constraintName = constraintName;
		this.constraintColumnNames = constraintColumnNames;
	}

	public @Nullable String getConstraintName() {
		return constraintName;
	}

	public List<String> getConstraintColumnNames() {
		return constraintColumnNames;
	}
}
