/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.insert;

import java.util.List;

import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.update.Assignment;

import jakarta.annotation.Nullable;

/**
 * @since 6.5
 */
public class ConflictClause {
	private final @Nullable String constraintName;
	private final List<String> constraintColumnNames;
	private final List<Assignment> assignments;
	private final @Nullable Predicate predicate;

	public ConflictClause(
			@Nullable String constraintName,
			List<String> constraintColumnNames,
			List<Assignment> assignments,
			@Nullable Predicate predicate) {
		this.constraintName = constraintName;
		this.constraintColumnNames = constraintColumnNames;
		this.assignments = assignments;
		this.predicate = predicate;
	}

	public @Nullable String getConstraintName() {
		return constraintName;
	}

	public List<String> getConstraintColumnNames() {
		return constraintColumnNames;
	}

	public List<Assignment> getAssignments() {
		return assignments;
	}

	public boolean isDoNothing() {
		return assignments.isEmpty();
	}

	public boolean isDoUpdate() {
		return !assignments.isEmpty();
	}

	public @Nullable Predicate getPredicate() {
		return predicate;
	}
}
