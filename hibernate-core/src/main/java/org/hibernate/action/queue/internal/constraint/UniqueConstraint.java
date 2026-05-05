/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.constraint;

import org.hibernate.metamodel.mapping.SelectableMappings;

import java.io.Serializable;

/// Describes a unique constraint (primary key, unique key, or unique foreign key)
/// in terms needed for ActionQueue graph creation and scheduling.
///
/// @param tableName The table containing the unique constraint
/// @param constraintName The constraint name ("PRIMARY" for PK, constraint name for others)
/// @param type The type of unique constraint
/// @param columns The columns forming the unique constraint
/// @param deferrability Deferrability of the constraint in the database
/// @param propertyNames Property names corresponding to the columns (for value extraction), may be null for PK
///
/// @author Steve Ebersole
public record UniqueConstraint(
		String tableName,
		String constraintName,
		ConstraintType type,
		SelectableMappings columns,
		Deferrability deferrability,
		boolean nullable,
		String[] propertyNames) implements Constraint, Serializable {

	public enum ConstraintType {
		/// Primary key constraint
		PRIMARY_KEY,

		/// Unique key constraint (not primary key)
		UNIQUE_KEY,

		/// Foreign key column with unique constraint (one-to-one)
		UNIQUE_FOREIGN_KEY
	}

	public boolean isPrimaryKey() {
		return type == ConstraintType.PRIMARY_KEY;
	}

	public boolean isUniqueKey() {
		return type == ConstraintType.UNIQUE_KEY;
	}

	public boolean isUniqueForeignKey() {
		return type == ConstraintType.UNIQUE_FOREIGN_KEY;
	}

	@Override
	public String getConstrainedTableName() {
		return tableName;
	}

	@Override
	public SelectableMappings getConstrainedColumnMappings() {
		return columns;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public Deferrability getDeferrability() {
		return deferrability;
	}

	@Override
	public boolean isDeferrable() {
		return deferrability.isDeferrable();
	}
}
