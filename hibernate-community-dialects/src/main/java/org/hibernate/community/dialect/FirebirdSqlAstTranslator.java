/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Firebird.
 *
 * @author Christian Beikov
 */
public class FirebirdSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private boolean inFunction;

	public FirebirdSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
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

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Percent fetches or ties fetches aren't supported in Firebird
		// Before 3.0 there was also no support for window functions
		// Check if current query part is already row numbering to avoid infinite recursion
		return useOffsetFetchClause( queryPart ) && getQueryPartForRowNumbering() != queryPart
				&& getDialect().getVersion().isSameOrAfter( 3 ) && !isRowsOnlyFetchClauseType( queryPart );
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		if ( shouldEmulateFetchClause( queryGroup ) ) {
			emulateFetchOffsetWithWindowFunctions( queryGroup, true );
		}
		else {
			super.visitQueryGroup( queryGroup );
		}
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		if ( shouldEmulateFetchClause( querySpec ) ) {
			emulateFetchOffsetWithWindowFunctions( querySpec, true );
		}
		else {
			super.visitQuerySpec( querySpec );
		}
	}

	// overridden due to visitSqlSelections
	@Override
	public void visitSelectClause(SelectClause selectClause) {
		Stack<Clause> clauseStack = getClauseStack();
		clauseStack.push( Clause.SELECT );

		try {
			appendSql( "select " );
			visitSqlSelections( selectClause );
			renderVirtualSelections( selectClause );
		}
		finally {
			clauseStack.pop();
		}
	}

	@Override
	protected void visitSqlSelections(SelectClause selectClause) {
		if ( !supportsOffsetFetchClause() ) {
			renderFirstSkipClause( (QuerySpec) getQueryPartStack().getCurrent() );
		}
		if ( selectClause.isDistinct() ) {
			appendSql( "distinct " );
		}
		super.visitSqlSelections( selectClause );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( supportsOffsetFetchClause() ) {
			// Firebird only supports a FIRST and SKIP clause before 3.0 which is handled in visitSqlSelections
			if ( !isRowNumberingCurrentQueryPart() ) {
				renderOffsetFetchClause( queryPart, true );
			}
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
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		emulateValuesTableReferenceColumnAliasing( tableReference );
	}

	private boolean supportsOffsetFetchClause() {
		return getDialect().getVersion().isSameOrAfter( 3 );
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
