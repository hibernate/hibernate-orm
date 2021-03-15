/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.NullnessLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Derby.
 *
 * @author Christian Beikov
 */
public class DerbySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public DerbySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	public void visitCteContainer(CteContainer cteContainer) {
		if ( cteContainer.isWithRecursive() ) {
			throw new IllegalArgumentException( "Recursive CTEs can't be emulated" );
		}
		super.visitCteContainer( cteContainer );
	}

	@Override
	protected void renderSearchClause(CteStatement cte) {
		// Derby does not support this, but it's just a hint anyway
	}

	@Override
	protected void renderCycleClause(CteStatement cte) {
		// Derby does not support this, but it can be emulated
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		// Derby only supports the OFFSET and FETCH clause with ROWS
		assertRowsOnlyFetchClauseType( queryPart );
		if ( supportsOffsetFetchClause() ) {
			renderOffsetFetchClause( queryPart, true );
		}
		else if ( !getClauseStack().isEmpty() ) {
			throw new IllegalArgumentException( "Can't render offset and fetch clause for subquery" );
		}
	}

	@Override
	protected void renderFetchExpression(Expression fetchExpression) {
		if ( supportsParameterOffsetFetchExpression() ) {
			super.renderFetchExpression( fetchExpression );
		}
		else {
			renderExpressionAsLiteral( fetchExpression, getJdbcParameterBindings() );
		}
	}

	@Override
	protected void renderOffsetExpression(Expression offsetExpression) {
		if ( supportsParameterOffsetFetchExpression() ) {
			super.renderOffsetExpression( offsetExpression );
		}
		else {
			renderExpressionAsLiteral( offsetExpression, getJdbcParameterBindings() );
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonEmulateIntersect( lhs, operator, rhs );
	}

	@Override
	protected void renderSelectExpression(Expression expression) {
		// Null literals have to be casted in the select clause
		if ( expression instanceof Literal ) {
			final Literal literal = (Literal) expression;
			if ( literal.getLiteralValue() == null ) {
				renderCasted( literal );
			}
			else {
				renderLiteral( literal, true );
			}
		}
		else if ( expression instanceof NullnessLiteral || expression instanceof JdbcParameter || expression instanceof SqmParameterInterpretation ) {
			renderCasted( expression );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "'0'" );
		}
		else if ( expression instanceof Summarization ) {
			Summarization summarization = (Summarization) expression;
			appendSql( summarization.getKind().name().toLowerCase() );
			appendSql( OPEN_PARENTHESIS );
			renderCommaSeparated( summarization.getGroupings() );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	protected String getFromDual() {
		return " from (values 0) as dual";
	}

	@Override
	protected String getFromDualForSelectOnly() {
		return getFromDual();
	}

	@Override
	protected boolean needsRowsToSkip() {
		return !supportsOffsetFetchClause();
	}

	@Override
	protected boolean needsMaxRows() {
		return !supportsOffsetFetchClause();
	}

	private boolean supportsParameterOffsetFetchExpression() {
		return getDialect().getVersion() >= 1060;
	}

	private boolean supportsOffsetFetchClause() {
		// Before version 10.5 Derby didn't support OFFSET and FETCH
		return getDialect().getVersion() >= 1050;
	}

}
