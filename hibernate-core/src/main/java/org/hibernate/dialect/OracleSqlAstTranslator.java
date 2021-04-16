/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Oracle.
 *
 * @author Christian Beikov
 */
public class OracleSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public OracleSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		return getQueryPartForRowNumbering() != queryPart && !supportsOffsetFetchClause() && (
				queryPart.isRoot() && hasLimit() || queryPart.getFetchClauseExpression() != null || queryPart.getOffsetClauseExpression() != null
		);
	}

	@Override
	protected void visitValuesList(List<Values> valuesList) {
		if ( valuesList.size() < 2 ) {
			super.visitValuesList( valuesList );
		}
		else {
			// Oracle doesn't support a multi-values insert
			// So we render a select union emulation instead
			String separator = "";
			final Stack<Clause> clauseStack = getClauseStack();
			try {
				clauseStack.push( Clause.VALUES );
				for ( Values values : valuesList ) {
					appendSql( separator );
					renderExpressionsAsSubquery( values.getExpressions() );
					separator = " union all ";
				}
			}
			finally {
				clauseStack.pop();
			}
		}
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
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			if ( supportsOffsetFetchClause() ) {
				renderOffsetFetchClause( queryPart, true );
			}
			else {
				assertRowsOnlyFetchClauseType( queryPart );
			}
		}
	}

	@Override
	protected void renderRowNumber(SelectClause selectClause, QueryPart queryPart) {
		if ( supportsOffsetFetchClause() || selectClause.isDistinct() ) {
			super.renderRowNumber( selectClause, queryPart );
		}
		else {
			appendSql( "rownum" );
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonEmulateDecode( lhs, operator, rhs );
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression, boolean inSelect) {
		// Oracle did not add support for CASE until 9i
		if ( getDialect().getVersion() < 900 ) {
			visitDecodeCaseSearchedExpression( caseSearchedExpression );
		}
		else {
			visitAnsiCaseSearchedExpression( caseSearchedExpression );
		}
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "()" );
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
		return getDialect().getVersion() >= 820;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInInSubQuery() {
		return getDialect().getVersion() >= 900;
	}

	@Override
	protected String getFromDual() {
		return " from dual";
	}

	@Override
	protected String getFromDualForSelectOnly() {
		return getFromDual();
	}

	private boolean supportsOffsetFetchClause() {
		return getDialect().getVersion() >= 1200;
	}

}
