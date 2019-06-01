/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression.domain;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.sqm.spi.SqmUpdateToSqlAstConverterMultiTable;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.results.spi.DomainResultCreationState;

/**
 * NavigableReference to a BasicValuedNavigable
 *
 * @author Steve Ebersole
 */
public class BasicValuedNavigableReference implements NavigableReference, AssignableNavigableReference {
	private final NavigablePath navigablePath;
	private final BasicValuedNavigable referencedNavigable;

	public BasicValuedNavigableReference(
			NavigablePath navigablePath,
			BasicValuedNavigable referencedNavigable,
			@SuppressWarnings("unused") SqlAstCreationState creationState) {
		this.navigablePath = navigablePath;
		this.referencedNavigable = referencedNavigable;
	}

	@Override
	public BasicValuedNavigable getNavigable() {
		return referencedNavigable;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void applySqlAssignments(
			Expression newValueExpression,
			SqmUpdateToSqlAstConverterMultiTable.AssignmentContext assignmentProcessingState,
			Consumer<Assignment> assignmentConsumer,
			SqlAstCreationContext creationContext) {
		final Column column = referencedNavigable.getBoundColumn();
		final TableReference tableReference = assignmentProcessingState.resolveTableReference( column.getSourceTable() );

		assignmentConsumer.accept(
				new Assignment(
						tableReference.resolveColumnReference( column ),
						newValueExpression
				)
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		creationState.getSqlExpressionResolver().resolveSqlSelection(
				creationState.getSqlExpressionResolver().resolveSqlExpression(
						creationState.getFromClauseAccess().findTableGroup( getNavigablePath().getParent() ),
						getNavigable().getBoundColumn()
				),
				getNavigable().getJavaTypeDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}
}
