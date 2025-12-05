/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.Locking;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.sql.ast.Clause;

/**
 * A SQL AST translator for TimesTen.
 *
 * @author Christian Beikov
 */
public class TimesTenSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public TimesTenSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			Locking.FollowOn followOnStrategy) {
		if ( followOnStrategy == Locking.FollowOn.FORCE ) {
			return LockStrategy.FOLLOW_ON;
		}

		// TimesTen supports locks with aggregates but not with set operators
		// See https://docs.oracle.com/cd/E11882_01/timesten.112/e21642/state.htm#TTSQL329
		LockStrategy strategy = LockStrategy.CLAUSE;
		if ( getQueryPartStack().findCurrentFirst( part -> part instanceof QueryGroup ? part : null ) != null ) {
			if ( followOnStrategy == Locking.FollowOn.DISALLOW ) {
				throw new IllegalQueryOperationException( "Locking with set operators is not supported!" );
			}
			else if ( followOnStrategy != Locking.FollowOn.IGNORE ) {
				strategy = LockStrategy.NONE;
			}
			else {
				strategy = LockStrategy.FOLLOW_ON;
			}
		}
		return strategy;
	}

	@Override
	protected void visitSqlSelections(SelectClause selectClause) {
		renderRowsToClause( (QuerySpec) getQueryPartStack().getCurrent() );
		super.visitSqlSelections( selectClause );
	}

	@Override
	protected void renderFetchPlusOffsetExpression(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		renderFetchPlusOffsetExpressionAsSingleParameter( fetchClauseExpression, offsetClauseExpression, offset );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		// TimesTen uses ROWS TO clause
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
			appendSql( "'0' || '0'" );
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

	protected void renderRowsToClause(QuerySpec querySpec) {
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			renderRowsToClause( getOffsetParameter(), getLimitParameter() );
		}
		else {
			assertRowsOnlyFetchClauseType( querySpec );
			renderRowsToClause( querySpec.getOffsetClauseExpression(), querySpec.getFetchClauseExpression() );
		}
	}

	protected void renderRowsToClause(Expression offsetClauseExpression, Expression fetchClauseExpression) {
		// offsetClauseExpression -> firstRow
		// fetchClauseExpression  -> maxRows
		final Stack<Clause> clauseStack = getClauseStack();

		if ( offsetClauseExpression == null && fetchClauseExpression != null ) {
			// We only have a maxRows/limit. We use 'SELECT FIRST n' syntax
			appendSql("first ");
			clauseStack.push( Clause.FETCH );
			try {
				renderFetchExpression( fetchClauseExpression );
			}
			finally {
				clauseStack.pop();
			}
		}
		else if ( offsetClauseExpression != null ) {
			// We have an offset. We use 'SELECT ROWS m TO n' syntax
			appendSql( "rows " );

			// Render offset parameter
			clauseStack.push( Clause.OFFSET );
			try {
				renderOffsetExpression( offsetClauseExpression );
			}
			finally {
				clauseStack.pop();
			}

			appendSql( " to " );

			// Render maxRows/limit parameter
			clauseStack.push( Clause.FETCH );
			try {
				if ( fetchClauseExpression != null ) {
					// We need to substract 1 row to fit maxRows
					renderFetchPlusOffsetExpressionAsSingleParameter( fetchClauseExpression, offsetClauseExpression, -1 );
				}
				else{
					// We dont have a maxRows param, we will just use a MAX_VALUE
					appendSql( Integer.MAX_VALUE );
				}
			}
			finally {
				clauseStack.pop();
			}
		}

		appendSql( WHITESPACE );
	}
}
