/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.update;

import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class UpdateStatement extends AbstractUpdateOrDeleteStatement {
	private final List<Assignment> assignments;

	public UpdateStatement(
			NamedTableReference targetTable,
			List<Assignment> assignments,
			Predicate restriction) {
		this( null, targetTable, new FromClause(), assignments, restriction, Collections.emptyList() );
	}

	public UpdateStatement(
			NamedTableReference targetTable,
			List<Assignment> assignments,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( null, targetTable, new FromClause(), assignments, restriction, returningColumns );
	}

	public UpdateStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			List<Assignment> assignments,
			Predicate restriction) {
		this( null, targetTable, fromClause, assignments, restriction, Collections.emptyList() );
	}

	public UpdateStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			List<Assignment> assignments,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( null, targetTable, fromClause, assignments, restriction, returningColumns );
	}

	public UpdateStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			FromClause fromClause,
			List<Assignment> assignments,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		super( cteContainer, targetTable, fromClause, restriction, returningColumns );
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
