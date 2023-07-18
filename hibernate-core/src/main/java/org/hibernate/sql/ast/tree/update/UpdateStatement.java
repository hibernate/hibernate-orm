/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.update;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.AbstractMutationStatement;
import org.hibernate.sql.ast.tree.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
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
		this( targetTable, new FromClause(), assignments, restriction );
	}

	public UpdateStatement(
			NamedTableReference targetTable,
			List<Assignment> assignments,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( targetTable, new FromClause(), assignments, restriction, returningColumns );
	}

	public UpdateStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			List<Assignment> assignments,
			Predicate restriction) {
		super( targetTable, fromClause, restriction );
		this.assignments = assignments;
	}

	public UpdateStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			List<Assignment> assignments,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		super( targetTable, fromClause, restriction, returningColumns );
		this.assignments = assignments;
	}

	public UpdateStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			FromClause fromClause,
			List<Assignment> assignments,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this(
				cteContainer.getCteStatements(),
				targetTable,
				fromClause,
				assignments,
				restriction,
				returningColumns
		);
	}

	public UpdateStatement(
			Map<String, CteStatement> cteStatements,
			NamedTableReference targetTable,
			FromClause fromClause,
			List<Assignment> assignments,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		super( cteStatements, targetTable, fromClause, restriction, returningColumns );
		this.assignments = assignments;
	}

	public List<Assignment> getAssignments() {
		return assignments;
	}

	public static class UpdateStatementBuilder {
		private final NamedTableReference targetTableRef;
		private List<Assignment> assignments;
		private Predicate restriction;

		public UpdateStatementBuilder(NamedTableReference targetTableRef) {
			this.targetTableRef = targetTableRef;
		}

		public UpdateStatementBuilder addAssignment(Assignment assignment) {
			if ( assignments == null ) {
				assignments = new ArrayList<>();
			}
			assignments.add( assignment );

			return this;
		}

		public UpdateStatementBuilder addRestriction(Predicate restriction) {
			this.restriction = SqlAstTreeHelper.combinePredicates( this.restriction, restriction );
			return this;
		}

		public UpdateStatementBuilder setRestriction(Predicate restriction) {
			this.restriction = restriction;
			return this;
		}

//		public SqlAstUpdateDescriptor createUpdateDescriptor() {
//			return new SqlAstUpdateDescriptorImpl(
//					createUpdateAst(),
//					Collections.singleton( targetTableRef.getTable().getTableExpression() )
//			);
//		}

		public UpdateStatement createUpdateAst() {
			if ( assignments == null || assignments.isEmpty() ) {
				return null;
			}

			return new UpdateStatement( targetTableRef, new FromClause(), assignments, restriction );
		}
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitUpdateStatement( this );
	}
}
