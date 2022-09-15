/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.community.dialect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
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

	@Override
	protected String getForUpdate() {
		return " with lock";
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

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		Stack<Clause> clauseStack = getClauseStack();
		clauseStack.push( Clause.SELECT );

		try {
			appendSql( "select " );
			visitSqlSelections( selectClause );
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
	protected void renderSearchClause(CteStatement cte) {
		// Firebird does not support this, but it's just a hint anyway
	}

	@Override
	protected void renderCycleClause(CteStatement cte) {
		// Firebird does not support this, but it can be emulated
	}

	@Override
	protected boolean supportsSimpleQueryGrouping() {
		// Firebird is quite strict i.e. it requires `select .. union all select * from (select ...)`
		// rather than `select .. union all (select ...)`
		return false;
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
		return " from rdb$database";
	}

	@Override
	protected String getFromDualForSelectOnly() {
		return getFromDual();
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
		this.inFunction = !( expression instanceof FunctionExpression ) || !StandardFunctions.CAST.equals( ( (FunctionExpression) expression).getFunctionName() );
		try {
			super.visitSelfRenderingExpression( expression);
		}
		finally {
			this.inFunction = inFunction;
		}
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
}
