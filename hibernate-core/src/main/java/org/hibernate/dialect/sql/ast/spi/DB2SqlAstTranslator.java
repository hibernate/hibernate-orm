/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
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
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public class DB2SqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {

	@SPI(SPI.Role.IMPLEMENT)
	public DB2SqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	protected TableJoinRenderingSupport getTableJoinRenderingSupport() {
		return StandardTableJoinRenderingSupport.DB2;
	}

	@Override
	protected void renderExpressionAsClauseItem(Expression expression) {
		expression.accept( this );
	}

	@Override
	protected void visitArithmeticOperand(Expression expression) {
		render( expression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
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
		return request -> request.hasFetch() && request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				? new PaginationRenderingPlan.Window( true )
				: new PaginationRenderingPlan.OffsetFetch( true );
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
	protected ReturningRenderingSupport getReturningRenderingSupport() {
		return StandardReturningRenderingSupport.DB2;
	}

	@Override
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
				// In DB2, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
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
			// In DB2, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
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

	public DatabaseVersion getDB2Version() {
		return this.getDialect().getVersion();
	}

	protected boolean supportsParameterOffsetFetchExpression() {
		return getDB2Version().isSameOrAfter( 11 );
	}

}
