/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;

/**
 * A SQL AST translator for HSQL.
 *
 * @author Christian Beikov
 */
public class HSQLLegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public HSQLLegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected void visitInsertStatementOnly(InsertSelectStatement statement) {
		if ( statement.getConflictClause() == null || statement.getConflictClause().isDoNothing() ) {
			// Render plain insert statement and possibly run into unique constraint violation
			super.visitInsertStatementOnly( statement );
		}
		else {
			visitInsertStatementEmulateMerge( statement );
		}
	}

	@Override
	protected void visitUpdateStatementOnly(UpdateStatement statement) {
		if ( hasNonTrivialFromClause( statement.getFromClause() ) ) {
			visitUpdateStatementEmulateMerge( statement );
		}
		else {
			super.visitUpdateStatementOnly( statement );
		}
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void renderDerivedTableReference(DerivedTableReference tableReference) {
		if ( tableReference instanceof FunctionTableReference && tableReference.isLateral() ) {
			// No need for a lateral keyword for functions
			tableReference.accept( this );
		}
		else {
			super.renderDerivedTableReference( tableReference );
		}
	}

	@Override
	protected void visitConflictClause(ConflictClause conflictClause) {
		if ( conflictClause != null ) {
			if ( conflictClause.isDoUpdate() && conflictClause.getConstraintName() != null ) {
				throw new IllegalQueryOperationException( "Insert conflict 'do update' clause with constraint name is not supported" );
			}
		}
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
	protected boolean supportsArrayConstructor() {
		return true;
	}

	@Override
	protected boolean supportsWithClauseInSubquery() {
		// Doesn't support correlations in the WITH clause
		return false;
	}

	@Override
	protected boolean supportsRecursiveClauseArrayAndRowEmulation() {
		// Even though HSQL supports the array constructor, it's illegal to use arrays in CTEs
		return false;
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
		if ( expression instanceof Literal ) {
			return ( (Literal) expression ).getJdbcMapping().getJdbcType().isStringLike();
		}
		return false;
	}

	@Override
	public boolean supportsFilterClause() {
		return true;
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			ForUpdateClause forUpdateClause,
			Boolean followOnLocking) {
		if ( getDialect().getVersion().isBefore( 2 ) ) {
			return LockStrategy.NONE;
		}
		return super.determineLockingStrategy( querySpec, forUpdateClause, followOnLocking );
	}

	@Override
	protected void renderForUpdateClause(QuerySpec querySpec, ForUpdateClause forUpdateClause) {
		if ( getDialect().getVersion().isBefore( 2 ) ) {
			return;
		}
		super.renderForUpdateClause( querySpec, forUpdateClause );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( supportsOffsetFetchClause() ) {
			assertRowsOnlyFetchClauseType( queryPart );
			renderOffsetFetchClause( queryPart, true );
		}
		else {
			renderLimitOffsetClause( queryPart );
		}
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
					render( lhs, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
					appendSql( operator.sqlText() );
					render( rhs, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
				}
				break;
			default:
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

	private boolean supportsOffsetFetchClause() {
		return getDialect().getVersion().isSameOrAfter( 2, 5 );
	}

	@Override
	protected void visitArithmeticOperand(Expression expression) {
		render( expression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
	}
}
