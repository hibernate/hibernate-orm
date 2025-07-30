/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Locking;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.JdbcParameterMetadata;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Unisys 2200.
 *
 * @author Christian Beikov
 */
public class RDMSOS2200SqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	@Deprecated(forRemoval = true, since = "7.1")
	public RDMSOS2200SqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	public RDMSOS2200SqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, @Nullable JdbcParameterMetadata parameterInfo) {
		super( sessionFactory, statement, parameterInfo );
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			Locking.FollowOn followOnLocking) {
		return LockStrategy.NONE;
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( queryPart.isRoot() ) {
			if ( hasLimit() ) {
				prepareLimitOffsetParameters();
				renderFetch( getLimitParameter(), getOffsetParameter(), FetchClauseType.ROWS_ONLY );
			}
			else if ( queryPart.getFetchClauseExpression() != null ) {
				renderFetch( queryPart.getFetchClauseExpression(), queryPart.getOffsetClauseExpression(), queryPart.getFetchClauseType() );
			}
		}
		else if ( queryPart.getOffsetClauseExpression() != null ) {
			throw new IllegalArgumentException( "Can't emulate offset clause in subquery" );
		}
	}

	@Override
	protected void renderFetchPlusOffsetExpression(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		renderFetchPlusOffsetExpressionAsSingleParameter( fetchClauseExpression, offsetClauseExpression, offset );
	}

	@Override
	protected boolean needsRowsToSkip() {
		return true;
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
	protected void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression, boolean inSelect) {
		visitDecodeCaseSearchedExpression( caseSearchedExpression );
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

}
