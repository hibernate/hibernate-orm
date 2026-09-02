/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardPaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardQueryMutationRenderingSupport;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorWithMerge;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InArrayPredicate;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;

/**
 * A SQL AST translator for HSQL.
 *
 * @author Christian Beikov
 */
public class HSQLSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {

	public HSQLSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.MERGE;
	}

	@Override
	protected QueryMutationRenderingSupport getQueryMutationRenderingSupport() {
		return StandardQueryMutationRenderingSupport.MERGE;
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.FUNCTION_LATERAL_IMPLICIT;
	}

	@Override
	protected void renderExpressionAsClauseItem(Expression expression) {
		expression.accept( this );
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		final boolean isNegated = booleanExpressionPredicate.isNegated();
		if ( isNegated ) {
			appendSql( "not(" );
		}
		booleanExpressionPredicate.getExpression().accept( this );
		if ( isNegated ) {
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	@Override
	public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
		// column in ( unnest(?) )
		inArrayPredicate.getTestExpression().accept( this );
		appendSql( " in (unnest(" );
		inArrayPredicate.getArrayParameter().accept( this );
		appendSql( "))" );
	}

	@Override
	protected void visitRecursivePath(Expression recursivePath, int sizeEstimate) {
		// HSQL determines the type and size of a column in a recursive CTE based on the expression of the non-recursive part
		// Due to that, we have to cast the path in the non-recursive path to a varchar of appropriate size to avoid data truncation errors
		if ( sizeEstimate == -1 ) {
			super.visitRecursivePath( recursivePath, sizeEstimate );
		}
		else {
			appendSql( "cast(" );
			recursivePath.accept( this );
			appendSql( " as varchar(" );
			appendSql( sizeEstimate );
			appendSql( "))" );
		}
	}

	// HSQL does not allow CASE expressions where all result arms contain plain parameters.
	// At least one result arm must provide some type context for inference,
	// so we cast the first result arm if we encounter this condition

	@Override
	protected void visitAnsiCaseSearchedExpression(
			CaseSearchedExpression expression,
			Consumer<Expression> resultRenderer) {
		if ( areAllResultsPlainParametersOrStringLiterals( expression ) ) {
			final List<CaseSearchedExpression.WhenFragment> whenFragments = expression.getWhenFragments();
			final Expression firstResult = whenFragments.get( 0 ).getResult();
			super.visitAnsiCaseSearchedExpression(
					expression,
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
			super.visitAnsiCaseSearchedExpression( expression, resultRenderer );
		}
	}

	@Override
	protected void visitAnsiCaseSimpleExpression(
			CaseSimpleExpression expression,
			Consumer<Expression> resultRenderer) {
		if ( areAllResultsPlainParametersOrStringLiterals( expression ) ) {
			final List<CaseSimpleExpression.WhenFragment> whenFragments = expression.getWhenFragments();
			final Expression firstResult = whenFragments.get( 0 ).getResult();
			super.visitAnsiCaseSimpleExpression(
					expression,
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
			super.visitAnsiCaseSimpleExpression( expression, resultRenderer );
		}
	}

	protected boolean areAllResultsPlainParametersOrStringLiterals(CaseSearchedExpression caseSearchedExpression) {
		final List<CaseSearchedExpression.WhenFragment> whenFragments = caseSearchedExpression.getWhenFragments();
		final Expression firstResult = whenFragments.get( 0 ).getResult();
		if ( isParameter( firstResult ) && getParameterRenderingMode() == SqlAstNodeRenderingMode.DEFAULT
				|| isStringLiteral( firstResult ) ) {
			for ( int i = 1; i < whenFragments.size(); i++ ) {
				final Expression result = whenFragments.get( i ).getResult();
				if ( isParameter( result ) ) {
					if ( getParameterRenderingMode() != SqlAstNodeRenderingMode.DEFAULT ) {
						return false;
					}
				}
				else if ( !isStringLiteral( result ) ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	protected boolean areAllResultsPlainParametersOrStringLiterals(CaseSimpleExpression caseSimpleExpression) {
		final List<CaseSimpleExpression.WhenFragment> whenFragments = caseSimpleExpression.getWhenFragments();
		final Expression firstResult = whenFragments.get( 0 ).getResult();
		if ( isParameter( firstResult ) && getParameterRenderingMode() == SqlAstNodeRenderingMode.DEFAULT
				|| isStringLiteral( firstResult ) ) {
			for ( int i = 1; i < whenFragments.size(); i++ ) {
				final Expression result = whenFragments.get( i ).getResult();
				if ( isParameter( result ) ) {
					if ( getParameterRenderingMode() != SqlAstNodeRenderingMode.DEFAULT ) {
						return false;
					}
				}
				else if ( !isStringLiteral( result ) ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private boolean isStringLiteral( Expression expression ) {
		return expression instanceof Literal literal
			&& literal.getJdbcMapping().getJdbcType().isStringLike();
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return StandardPaginationRenderingSupport.OFFSET_FETCH;
	}

	@Override
	protected void renderSelectExpression(Expression expression) {
		if ( isInSubquery() && expression instanceof Literal ) {
			renderCasted( expression );
		}
		else {
			renderSelectExpressionWithCastedOrInlinedPlainParameters( expression );
		}
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	public void visitRelationalPredicate(ComparisonPredicate comparisonPredicate) {
		if ( isParameter( comparisonPredicate.getLeftHandExpression() )
				&& isParameter( comparisonPredicate.getRightHandExpression() ) ) {
			// HSQLDB doesn't like comparing two parameters with each other
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER,
					() -> super.visitRelationalPredicate( comparisonPredicate )
			);
		}
		else {
			super.visitRelationalPredicate( comparisonPredicate );
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType == null || lhsExpressionType.getJdbcTypeCount() != 1 ) {
			renderComparisonStandard( lhs, operator, rhs );
			return;
		}
		switch ( operator ) {
			case DISTINCT_FROM:
			case NOT_DISTINCT_FROM:
				if ( lhsExpressionType.getSingleJdbcMapping().getJdbcType() instanceof ArrayJdbcType ) {
					// HSQL implements distinct from semantics for arrays
					lhs.accept( this );
					appendSql( operator == ComparisonOperator.DISTINCT_FROM ? "<>" : "=" );
					rhs.accept( this );
				}
				else {
					// HSQL does not like parameters in the distinct from predicate
					renderComparisonEmulateIntersect( lhs, operator, rhs );
				}
				break;
			default:
				// HSQL has a broken 'is distinct from' operator
				renderComparisonStandard( lhs, operator, rhs );
				break;
		}
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
			throw new UnsupportedOperationException( "Summarization is not supported by DBMS" );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected void visitArithmeticOperand(Expression expression) {
		render( expression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
	}

}
