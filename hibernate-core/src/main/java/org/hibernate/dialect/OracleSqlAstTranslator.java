/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.FetchClauseType;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.UnionTableGroup;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;
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

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			ForUpdateClause forUpdateClause,
			Boolean followOnLocking) {
		LockStrategy strategy = super.determineLockingStrategy( querySpec, forUpdateClause, followOnLocking );
		final boolean followOnLockingDisabled = Boolean.FALSE.equals( followOnLocking );
		if ( strategy != LockStrategy.FOLLOW_ON && querySpec.hasSortSpecifications() ) {
			if ( followOnLockingDisabled ) {
				throw new IllegalQueryOperationException( "Locking with ORDER BY is not supported!" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		// Oracle also doesn't support locks with set operators
		// See https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_10002.htm#i2066346
		if ( strategy != LockStrategy.FOLLOW_ON && isPartOfQueryGroup() ) {
			if ( followOnLockingDisabled ) {
				throw new IllegalQueryOperationException( "Locking with set operators is not supported!" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( strategy != LockStrategy.FOLLOW_ON && hasSetOperations( querySpec ) ) {
			if ( followOnLockingDisabled ) {
				throw new IllegalQueryOperationException( "Locking with set operators is not supported!" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( strategy != LockStrategy.FOLLOW_ON && useOffsetFetchClause( querySpec ) && !isRowsOnlyFetchClauseType( querySpec ) ) {
			if ( followOnLockingDisabled ) {
				throw new IllegalQueryOperationException( "Locking with FETCH is not supported!" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( strategy != LockStrategy.FOLLOW_ON ) {
			final boolean hasOffset;
			if ( querySpec.isRoot() && hasLimit() && getLimit().getFirstRowJpa() != 0 ) {
				hasOffset = true;
				// We must record that the generated SQL depends on the fact that there is an offset
				addAppliedParameterBinding( getOffsetParameter(), null );
			}
			else {
				hasOffset = querySpec.getOffsetClauseExpression() != null;
			}
			if ( hasOffset ) {
				if ( followOnLockingDisabled ) {
					throw new IllegalQueryOperationException( "Locking with OFFSET is not supported!" );
				}
				strategy = LockStrategy.FOLLOW_ON;
			}
		}
		return strategy;
	}

	private boolean hasSetOperations(QuerySpec querySpec) {
		return querySpec.getFromClause().queryTableGroups( group -> group instanceof UnionTableGroup ? group : null ) != null;
	}

	private boolean isPartOfQueryGroup() {
		return getQueryPartStack().findCurrentFirst( part -> part instanceof QueryGroup ? part : null ) != null;
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		if (getQueryPartForRowNumbering() == queryPart) {
			return false;
		}
		final boolean hasLimit = queryPart.isRoot() && hasLimit() || queryPart.getFetchClauseExpression() != null
				|| queryPart.getOffsetClauseExpression() != null;
		if ( !hasLimit ) {
			return false;
		}
		// Even if Oracle supports the OFFSET/FETCH clause, there are conditions where we still want to use the ROWNUM pagination
		if ( supportsOffsetFetchClause() ) {
			// When the query has no sort specifications and offset, we want to use the ROWNUM pagination as that is a special locking case
			return !queryPart.hasSortSpecifications() && !hasOffset( queryPart );
		}
		return true;
	}

	@Override
	protected void emulateFetchOffsetWithWindowFunctions(
			QueryPart queryPart,
			Expression offsetExpression,
			Expression fetchExpression,
			FetchClauseType fetchClauseType,
			boolean emulateFetchClause) {
		if ( queryPart instanceof QuerySpec && !queryPart.hasSortSpecifications() && offsetExpression == null && fetchClauseType == FetchClauseType.ROWS_ONLY ) {
			// Special case for Oracle to support locking along with simple max results paging
			final QuerySpec querySpec = (QuerySpec) queryPart;
			withRowNumbering(
					querySpec,
					() -> {
						appendSql( "select * from (" );
						super.visitQuerySpec( querySpec );
						appendSql( ") where rownum <= " );
						final Stack<Clause> clauseStack = getClauseStack();
						clauseStack.push( Clause.WHERE );
						try {
							fetchExpression.accept( this );

							// We render the FOR UPDATE clause in the outer query
							clauseStack.pop();
							clauseStack.push( Clause.FOR_UPDATE );
							visitForUpdateClause( querySpec );
						}
						finally {
							clauseStack.pop();
						}
					}
			);
		}
		else {
			super.emulateFetchOffsetWithWindowFunctions(
					queryPart,
					offsetExpression,
					fetchExpression,
					fetchClauseType,
					emulateFetchClause
			);
		}
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
			final List<SortSpecification> sortSpecifications = getSortSpecificationsRowNumbering( selectClause, queryPart );
			if ( selectClause.isDistinct() ) {
				appendSql( "dense_rank()" );
			}
			else {
				if ( sortSpecifications.isEmpty() ) {
					appendSql( "rownum" );
					return;
				}
				appendSql( "row_number()" );
			}
			visitOverClause( Collections.emptyList(), sortSpecifications );
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
		return getDialect().supportsFetchClause( FetchClauseType.ROWS_ONLY );
	}

}
