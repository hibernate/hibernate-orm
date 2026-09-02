/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Internal;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardQueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.FunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.JdbcLiteral;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InListPredicate;
import org.hibernate.sql.ast.spi.query.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Firebird.
 *
 * @author Christian Beikov
 */
public class FirebirdSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private boolean inFunction;

	public FirebirdSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		renderIntoIntoAndTable( tableInsert );
		appendSql( "default values" );
	}

	@Override
	@Internal
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.MERGE;
	}

	@Override
	protected QueryMutationRenderingSupport getQueryMutationRenderingSupport() {
		return StandardQueryMutationRenderingSupport.MERGE;
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		if ( getDialect().getVersion().isSameOrAfter( 3 ) ) {
			final boolean isNegated = booleanExpressionPredicate.isNegated();
			if ( isNegated ) {
				appendSql( "not(" );
			}
			booleanExpressionPredicate.getExpression().accept( this );
			if ( isNegated ) {
				appendSql( CLOSE_PARENTHESIS );
			}
		}
		else {
			super.visitBooleanExpressionPredicate( booleanExpressionPredicate );
		}
	}

	// Firebird does not allow CASE expressions where all result arms contain plain parameters.
	// At least one result arm must provide some type context for inference,
	// so we cast the first result arm if we encounter this condition

	@Override
	protected void visitAnsiCaseSearchedExpression(
			CaseSearchedExpression caseSearchedExpression,
			Consumer<Expression> resultRenderer) {
		if ( getParameterRenderingMode() == SqlAstNodeRenderingMode.DEFAULT && areAllResultsParameters( caseSearchedExpression ) ) {
			final List<CaseSearchedExpression.WhenFragment> whenFragments = caseSearchedExpression.getWhenFragments();
			final Expression firstResult = whenFragments.get( 0 ).getResult();
			super.visitAnsiCaseSearchedExpression(
					caseSearchedExpression,
					e -> {
						if ( e == firstResult ) {
							renderCasted( e );
						}
						else {
							resultRenderer.accept( e );
						}
					}
			);
		}
		else {
			super.visitAnsiCaseSearchedExpression( caseSearchedExpression, resultRenderer );
		}
	}

	@Override
	protected void visitAnsiCaseSimpleExpression(
			CaseSimpleExpression caseSimpleExpression,
			Consumer<Expression> resultRenderer) {
		if ( getParameterRenderingMode() == SqlAstNodeRenderingMode.DEFAULT && areAllResultsParameters( caseSimpleExpression ) ) {
			final List<CaseSimpleExpression.WhenFragment> whenFragments = caseSimpleExpression.getWhenFragments();
			final Expression firstResult = whenFragments.get( 0 ).getResult();
			super.visitAnsiCaseSimpleExpression(
					caseSimpleExpression,
					e -> {
						if ( e == firstResult ) {
							renderCasted( e );
						}
						else {
							resultRenderer.accept( e );
						}
					}
			);
		}
		else {
			super.visitAnsiCaseSimpleExpression( caseSimpleExpression, resultRenderer );
		}
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		if ( getDialect().getVersion().isBefore( 3 ) ) {
			return request -> new PaginationRenderingPlan.FirstSkip();
		}
		return request -> request.fetchClauseType() != null
				&& request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				? new PaginationRenderingPlan.Window( true )
				: new PaginationRenderingPlan.OffsetFetch( true );
	}

	// overridden due to renderSelectItems
	@Override
	protected void renderSelectClause(SelectClause selectClause) {
		appendSql( "select " );
		if ( determinePaginationRenderingPlan( getQueryPartStack().getCurrent() )
				instanceof PaginationRenderingPlan.FirstSkip ) {
			renderFirstSkipClause( (QuerySpec) getQueryPartStack().getCurrent() );
		}
		if ( selectClause.isDistinct() ) {
			appendSql( "distinct " );
		}
		super.renderSelectItems( selectClause );
		renderVirtualSelections( selectClause );
	}

	@Override
	protected void renderSelectExpression(Expression expression) {
		renderSelectExpressionWithCastedOrInlinedPlainParameters( expression );
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

	@Override
	public void visitInListPredicate(InListPredicate inListPredicate) {
		final List<Expression> listExpressions = inListPredicate.getListExpressions();
		if ( listExpressions.isEmpty() ) {
			appendSql( "1=" + ( inListPredicate.isNegated() ? "1" : "0" ) );
			return;
		}
		final Expression testExpression = inListPredicate.getTestExpression();
		if ( isParameter( testExpression ) ) {
			renderCasted( testExpression );
			if ( inListPredicate.isNegated() ) {
				appendSql( " not" );
			}
			appendSql( " in (" );
			renderCommaSeparated( listExpressions );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			super.visitInListPredicate( inListPredicate );
		}
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.VALUES_SELECT_LIST;
	}

	@Override
	public void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
		// see comments in visitParameter
		boolean inFunction = this.inFunction;
		this.inFunction = true;
		try {
			super.visitSelfRenderingPredicate( selfRenderingPredicate );
		}
		finally {
			this.inFunction = inFunction;
		}
	}

	@Override
	public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
		// see comments in visitParameter
		boolean inFunction = this.inFunction;
		this.inFunction = isFunctionOrCastWithArithmeticExpression( expression );
		try {
			super.visitSelfRenderingExpression( expression );
		}
		finally {
			this.inFunction = inFunction;
		}
	}

	private boolean isFunctionOrCastWithArithmeticExpression(SelfRenderingExpression expression) {
		// see comments in visitParameter
		return expression instanceof FunctionExpression fe
			&& ( !"cast".equals( fe.getFunctionName() )
				// types of parameters in an arithmetic expression inside a cast are not (always?) inferred
				|| fe.getArguments().get( 0 ) instanceof BinaryArithmeticExpression );
	}

	@Override
	public void visitParameter(JdbcParameter jdbcParameter) {
		if ( inFunction ) {
			// Turn off 'inFunction' to prevent StackOverflowError while rendering the cast
			inFunction = false;
			try {
				// A lot of functions in Firebird are contextually typed and as a result,
				// Firebird cannot determine the datatype of a parameter as passed to a function,
				// resulting in a "Datatype unknown" error when the statement is compiled.
				// This adds an explicit cast so Firebird can infer the type
				renderCasted( jdbcParameter );
			}
			finally {
				inFunction = true;
			}
		}
		else {
			super.visitParameter( jdbcParameter );
		}
	}

	@Override
	public void visitJdbcLiteral(JdbcLiteral<?> jdbcLiteral) {
		visitLiteral( jdbcLiteral );
	}

	@Override
	public void visitQueryLiteral(QueryLiteral<?> queryLiteral) {
		visitLiteral( queryLiteral );
	}

	private void visitLiteral(Literal literal) {
		if ( literal.getLiteralValue() == null ) {
			appendSql( SqlAppender.NULL_KEYWORD );
		}
		else {
			// see comments in visitParameter
			renderLiteral( literal, inFunction );
		}
	}

	@Override
	public void visitRelationalPredicate(ComparisonPredicate comparisonPredicate) {
		if ( isParameterOrContainsParameter( comparisonPredicate.getLeftHandExpression() )
			&& isParameterOrContainsParameter( comparisonPredicate.getRightHandExpression() ) ) {
			// Firebird cannot compare two parameters with each other as they will be untyped, and in some cases
			// comparisons involving arithmetic expression with parameters also fails to infer the type
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER,
					() -> super.visitRelationalPredicate( comparisonPredicate )
			);
		}
		else {
			super.visitRelationalPredicate( comparisonPredicate );
		}
	}

	private static boolean isParameterOrContainsParameter( Expression expression ) {
		if ( expression instanceof BinaryArithmeticExpression arithmeticExpression ) {
			// Recursive search for parameter
			return isParameterOrContainsParameter( arithmeticExpression.getLeftHandOperand() )
				|| isParameterOrContainsParameter( arithmeticExpression.getRightHandOperand() );
		}
		return isParameter( expression );
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

}
