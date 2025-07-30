/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.JdbcParameterMetadata;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Derby.
 *
 * @author Christian Beikov
 */
public class DerbyLegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	@Deprecated(forRemoval = true, since = "7.1")
	public DerbyLegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	public DerbyLegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, @Nullable JdbcParameterMetadata parameterInfo) {
		super( sessionFactory, statement, parameterInfo );
	}

	@Override
	protected void visitDeleteStatementOnly(DeleteStatement statement) {
		if ( hasNonTrivialFromClause( statement.getFromClause() ) ) {
			appendSql( "delete from " );
			final Stack<Clause> clauseStack = getClauseStack();
			try {
				clauseStack.push( Clause.DELETE );
				super.renderDmlTargetTableExpression( statement.getTargetTable() );
				append( " dml_target_" );
			}
			finally {
				clauseStack.pop();
			}
			visitWhereClause( determineWhereClauseRestrictionWithJoinEmulation( statement, "dml_target_" ) );
			visitReturningColumns( statement.getReturningColumns() );
		}
		else {
			super.visitDeleteStatementOnly( statement );
		}
	}

	@Override
	protected void visitUpdateStatementOnly(UpdateStatement statement) {
		if ( hasNonTrivialFromClause( statement.getFromClause() ) ) {
			appendSql( "update " );
			final Stack<Clause> clauseStack = getClauseStack();
			try {
				clauseStack.push( Clause.UPDATE );
				super.renderDmlTargetTableExpression( statement.getTargetTable() );
				append( " dml_target_" );
			}
			finally {
				clauseStack.pop();
			}
			renderSetClause( statement.getAssignments() );
			visitWhereClause( determineWhereClauseRestrictionWithJoinEmulation( statement, "dml_target_" ) );
			visitReturningColumns( statement.getReturningColumns() );
		}
		else {
			super.visitUpdateStatementOnly( statement );
		}
	}

	@Override
	protected void visitSetAssignment(Assignment assignment) {
		final Statement currentStatement = getStatementStack().getCurrent();
		final UpdateStatement statement;
		if ( currentStatement instanceof UpdateStatement
				&& hasNonTrivialFromClause( ( statement = (UpdateStatement) currentStatement ).getFromClause() ) ) {
			visitSetAssignmentEmulateJoin( assignment, statement );
		}
		else {
			super.visitSetAssignment( assignment );
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

	// Derby does not allow CASE expressions where all result arms contain plain parameters.
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
			appendSql( "'0'" );
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
	protected boolean needsRowsToSkip() {
		return !supportsOffsetFetchClause();
	}

	@Override
	protected boolean needsMaxRows() {
		return !supportsOffsetFetchClause();
	}

	private boolean supportsParameterOffsetFetchExpression() {
		return getDialect().getVersion().isSameOrAfter( 10, 6 );
	}

	private boolean supportsOffsetFetchClause() {
		// Before version 10.5 Derby didn't support OFFSET and FETCH
		return getDialect().getVersion().isSameOrAfter( 10, 5 );
	}

	@Override
	protected void visitArithmeticOperand(Expression expression) {
		render( expression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
	}

}
