/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.update;

import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.spi.query.cte.CteContainer;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.spi.mutation.MutationTarget;

/**
 * @author Steve Ebersole
 */
public class UpdateStatement extends AbstractUpdateOrDeleteStatement {
	private final List<Assignment> assignments;

	public UpdateStatement(
			NamedTableReference targetTable,
			List<Assignment> assignments,
			Predicate restriction) {
		this( null, targetTable, null, new FromClause(), assignments, restriction, Collections.emptyList() );
	}

	public UpdateStatement(
			NamedTableReference targetTable,
			List<Assignment> assignments,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( null, targetTable, null, new FromClause(), assignments, restriction, returningColumns );
	}

	public UpdateStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			List<Assignment> assignments,
			Predicate restriction) {
		this( null, targetTable, null, fromClause, assignments, restriction, Collections.emptyList() );
	}

	public UpdateStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			List<Assignment> assignments,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( null, targetTable, null, fromClause, assignments, restriction, returningColumns );
	}

	public UpdateStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			MutationTarget mutationTarget,
			FromClause fromClause,
			List<Assignment> assignments,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		super( cteContainer, targetTable, mutationTarget, fromClause, restriction, returningColumns );
		this.assignments = assignments;
	}

	public List<Assignment> getAssignments() {
		return assignments;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitUpdateStatement( this );
	}
}
