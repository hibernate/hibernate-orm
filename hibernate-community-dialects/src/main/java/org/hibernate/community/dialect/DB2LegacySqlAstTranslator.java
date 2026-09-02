/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Internal;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sql.ast.spi.DB2QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardSetReturningFunctionRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardTableJoinRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.TableJoinRenderingSupport;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.SqlTypes;


/**
 * A SQL AST translator for DB2.
 *
 * @author Christian Beikov
 */
public class DB2LegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public DB2LegacySqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	@Internal
	protected TableJoinRenderingSupport getTableJoinRenderingSupport() {
		return StandardTableJoinRenderingSupport.DB2;
	}

	@Override
	protected void renderExpressionAsClauseItem(Expression expression) {
		if ( expression instanceof Predicate && getDB2Version().isBefore( 11 ) ) {
			super.renderExpressionAsClauseItem( expression );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected void visitArithmeticOperand(Expression expression) {
		render( expression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		if ( getDB2Version().isSameOrAfter( 11 ) ) {
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

	// DB2 does not allow CASE expressions where all result arms contain plain parameters.
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
		return request -> requiresWindowPagination( request )
				? new PaginationRenderingPlan.Window( true )
				: new PaginationRenderingPlan.OffsetFetch( true );
	}

	protected boolean requiresWindowPagination(PaginationRenderingRequest request) {
		return request.hasFetch() && request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				|| !supportsOffsetClause() && request.hasOffset()
				|| requiresWindowPaginationForVariableLimit( request );
	}

	protected boolean requiresWindowPaginationForVariableLimit(PaginationRenderingRequest request) {
		final QueryPart queryPart = request.queryPart();
		// According to LegacyDB2LimitHandler, variable limit isn't supported before 11.1
		return getDB2Version().isBefore( 11, 1 )
				&& ( request.usesQueryOptionsLimit()
						|| queryPart.getFetchClauseExpression() != null
								&& !( queryPart.getFetchClauseExpression() instanceof Literal ) );
	}

	protected boolean supportsOffsetClause() {
		return getDB2Version().isSameOrAfter( 11, 1 );
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.DB2;
	}

	@Override
	protected SetReturningFunctionRenderingSupport getSetReturningFunctionRenderingSupport() {
		return StandardSetReturningFunctionRenderingSupport.DB2;
	}

	@Override
	protected void renderSelectStatement(SelectStatement statement) {
		if ( getQueryPartForRowNumbering() == statement.getQueryPart()
				&& isRenderingLateralDerivedTable() ) {
			appendSql( "lateral " );
		}
		super.renderSelectStatement( statement );
	}

	@Override
	protected void emulateFetchOffsetWithWindowFunctionsVisitQueryPart(QueryPart queryPart) {
		if ( isRenderingLateralDerivedTable() ) {
			appendSql( "lateral " );
			withoutLateralDerivedTableContext(
					() -> super.emulateFetchOffsetWithWindowFunctionsVisitQueryPart( queryPart )
			);
		}
		else {
			super.emulateFetchOffsetWithWindowFunctionsVisitQueryPart( queryPart );
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
	protected void renderFetchExpression(Expression fetchExpression) {
		if ( supportsParameterOffsetFetchExpression() ) {
			super.renderFetchExpression( fetchExpression );
		}
		else {
			renderExpressionAsLiteral( fetchExpression, getJdbcParameterBindings() );
		}
	}

	@Override
	protected QueryMutationRenderingSupport getQueryMutationRenderingSupport() {
		return DB2QueryMutationRenderingSupport.INSTANCE;
	}

	@Override
	@Internal
	protected ReturningRenderingSupport getReturningRenderingSupport() {
		return StandardReturningRenderingSupport.DB2;
	}

	@Override
	@Internal
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.MERGE;
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		renderFromClauseExcludingDmlTargetReference( statement );
	}


	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		if ( getDB2Version().isSameOrAfter( 11, 1 ) ) {
			renderComparisonStandard( lhs, operator, rhs );
		}
		else {
			final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
			if ( lhsExpressionType != null && lhsExpressionType.getJdbcTypeCount() == 1
					&& lhsExpressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() == SqlTypes.SQLXML ) {
				// In SQL Server, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
				switch ( operator ) {
					case DISTINCT_FROM:
						appendSql( "decode(" );
						appendSql( "xmlserialize(" );
						lhs.accept( this );
						appendSql( " as varchar(32672))" );
						appendSql( ',' );
						appendSql( "xmlserialize(" );
						rhs.accept( this );
						appendSql( " as varchar(32672))" );
						appendSql( ",0,1)=1" );
						return;
					case NOT_DISTINCT_FROM:
						appendSql( "decode(" );
						appendSql( "xmlserialize(" );
						lhs.accept( this );
						appendSql( " as varchar(32672))" );
						appendSql( ',' );
						appendSql( "xmlserialize(" );
						rhs.accept( this );
						appendSql( " as varchar(32672))" );
						appendSql( ",0,1)=0" );
						return;
					case EQUAL:
					case NOT_EQUAL:
						appendSql( "xmlserialize(" );
						lhs.accept( this );
						appendSql( " as varchar(32672))" );
						appendSql( operator.sqlText() );
						appendSql( "xmlserialize(" );
						rhs.accept( this );
						appendSql( " as varchar(32672))" );
						return;
					default:
						// Fall through
						break;
				}
			}
			renderComparisonEmulateDecode( lhs, operator, rhs, SqlAstNodeRenderingMode.NO_UNTYPED );
		}
	}

	@Override
	protected void renderComparisonStandard(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType != null && lhsExpressionType.getJdbcTypeCount() == 1
				&& lhsExpressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() == SqlTypes.SQLXML ) {
			// In SQL Server, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
			switch ( operator ) {
				case EQUAL:
				case NOT_DISTINCT_FROM:
				case NOT_EQUAL:
				case DISTINCT_FROM:
					appendSql( "xmlserialize(" );
					lhs.accept( this );
					appendSql( " as varchar(32672))" );
					appendSql( operator.sqlText() );
					appendSql( "xmlserialize(" );
					rhs.accept( this );
					appendSql( " as varchar(32672))" );
					return;
				default:
					// Fall through
					break;
			}
		}
		super.renderComparisonStandard( lhs, operator, rhs );
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
			appendSql( "()" );
		}
		else if ( expression instanceof Summarization ) {
			Summarization summarization = (Summarization) expression;
			appendSql( summarization.getKind().sqlText() );
			appendSql( OPEN_PARENTHESIS );
			renderCommaSeparated( summarization.getGroupings() );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			expression.accept( this );
		}
	}

	public DatabaseVersion getDB2Version() {
		return this.getDialect().getVersion();
	}

	protected boolean supportsParameterOffsetFetchExpression() {
		return getDB2Version().isSameOrAfter( 11 );
	}

}
