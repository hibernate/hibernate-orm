/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.Locking;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Teradata.
 *
 * @author Christian Beikov
 */
public class TeradataSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {
	private final boolean supportsLocking;

	public TeradataSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
		supportsLocking = getDialect().getVersion().isSameOrAfter( 14 );
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		super.visitQuerySpec( querySpec );

		if ( querySpec.isRoot() && supportsLocking && needsLocking( querySpec ) ) {
			final LockingClauseStrategy lockingClauseStrategy = getLockingClauseStrategy();
			if ( lockingClauseStrategy != null ) {
				// NOTE: the dialect already adds a trailing space
				lockingClauseStrategy.render( this::prependSql );
			}
		}
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			Locking.FollowOn followOnStrategy) {
		if ( !supportsLocking ) {
			return LockStrategy.NONE;
		}

		if ( followOnStrategy == Locking.FollowOn.FORCE ) {
			return LockStrategy.FOLLOW_ON;
		}

		return super.determineLockingStrategy( querySpec, followOnStrategy );
	}

	@Override
	protected void visitForUpdateClause(QuerySpec querySpec) {
		// do nothing here
	}

	@Override
	protected boolean needsRowsToSkip() {
		return true;
	}

	@Override
	protected void renderFetchPlusOffsetExpression(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		renderFetchPlusOffsetExpressionAsSingleParameter( fetchClauseExpression, offsetClauseExpression, offset );
	}

	@Override
	protected void visitSqlSelections(SelectClause selectClause) {
		renderTopClause( (QuerySpec) getQueryPartStack().getCurrent(), true, true );
		super.visitSqlSelections( selectClause );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		// Teradata only supports the TOP clause
		if ( !queryPart.isRoot() && queryPart.getOffsetClauseExpression() != null ) {
			throw new IllegalArgumentException( "Can't emulate offset clause in subquery" );
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonEmulateIntersect( lhs, operator, rhs );
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "()" );
		}
		else if ( expression instanceof Summarization ) {
			// This could theoretically be emulated by rendering all grouping variations of the query and
			// connect them via union all but that's probably pretty inefficient and would have to happen
			// on the query spec level
			throw new UnsupportedOperationException( "Summarization is not supported by DBMS!" );
		}
		else {
			expression.accept( this );
		}
	}

}
