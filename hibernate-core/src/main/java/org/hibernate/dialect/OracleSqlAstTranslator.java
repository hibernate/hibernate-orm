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
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.FrameExclusion;
import org.hibernate.query.sqm.FrameKind;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.UnionTableGroup;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.SqlTypes;

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
				throw new IllegalQueryOperationException( "Locking with ORDER BY is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		// Oracle also doesn't support locks with set operators
		// See https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_10002.htm#i2066346
		if ( strategy != LockStrategy.FOLLOW_ON && isPartOfQueryGroup() ) {
			if ( followOnLockingDisabled ) {
				throw new IllegalQueryOperationException( "Locking with set operators is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( strategy != LockStrategy.FOLLOW_ON && hasSetOperations( querySpec ) ) {
			if ( followOnLockingDisabled ) {
				throw new IllegalQueryOperationException( "Locking with set operators is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( strategy != LockStrategy.FOLLOW_ON && useOffsetFetchClause( querySpec ) && !isRowsOnlyFetchClauseType( querySpec ) ) {
			if ( followOnLockingDisabled ) {
				throw new IllegalQueryOperationException( "Locking with FETCH is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( strategy != LockStrategy.FOLLOW_ON ) {
			final boolean hasOffset;
			if ( querySpec.isRoot() && hasLimit() && getLimit().getFirstRow() != null ) {
				hasOffset = true;
				// We must record that the generated SQL depends on the fact that there is an offset
				addAppliedParameterBinding( getOffsetParameter(), null );
			}
			else {
				hasOffset = querySpec.getOffsetClauseExpression() != null;
			}
			if ( hasOffset ) {
				if ( followOnLockingDisabled ) {
					throw new IllegalQueryOperationException( "Locking with OFFSET is not supported" );
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
			return !queryPart.hasSortSpecifications() && !hasOffset( queryPart )
					// Workaround an Oracle bug, segmentation fault for insert queries with a plain query group and fetch clause
					|| queryPart instanceof QueryGroup && getClauseStack().isEmpty() && getStatement() instanceof InsertStatement;
		}
		return true;
	}

	@Override
	protected FetchClauseType getFetchClauseTypeForRowNumbering(QueryPart queryPart) {
		final FetchClauseType fetchClauseType = super.getFetchClauseTypeForRowNumbering( queryPart );
		final boolean hasOffset;
		if ( queryPart.isRoot() && hasLimit() ) {
			hasOffset = getLimit().getFirstRow() != null;
		}
		else {
			hasOffset = queryPart.getOffsetClauseExpression() != null;
		}
		if ( queryPart instanceof QuerySpec && !hasOffset && fetchClauseType == FetchClauseType.ROWS_ONLY ) {
			// We return null here, because in this particular case, we render a special rownum query
			// which can be seen in #emulateFetchOffsetWithWindowFunctions
			// Note that we also build upon this in #visitOrderBy
			return null;
		}
		return fetchClauseType;
	}

	@Override
	protected void emulateFetchOffsetWithWindowFunctions(
			QueryPart queryPart,
			Expression offsetExpression,
			Expression fetchExpression,
			FetchClauseType fetchClauseType,
			boolean emulateFetchClause) {
		if ( queryPart instanceof QuerySpec && offsetExpression == null && fetchClauseType == FetchClauseType.ROWS_ONLY ) {
			// Special case for Oracle to support locking along with simple max results paging
			final QuerySpec querySpec = (QuerySpec) queryPart;
			withRowNumbering(
					querySpec,
					true, // we need select aliases to avoid ORA-00918: column ambiguously defined
					() -> {
						final QueryPart currentQueryPart = getQueryPartStack().getCurrent();
						final boolean needsParenthesis;
						final boolean needsWrapper;
						if ( currentQueryPart instanceof QueryGroup ) {
							needsParenthesis = false;
							// visitQuerySpec will add the select wrapper
							needsWrapper = !currentQueryPart.hasOffsetOrFetchClause();
						}
						else {
							needsParenthesis = !querySpec.isRoot();
							needsWrapper = true;
						}
						if ( needsWrapper ) {
							if ( needsParenthesis ) {
								appendSql( '(' );
							}
							appendSql( "select * from " );
							if ( !needsParenthesis ) {
								appendSql( '(' );
							}
						}
						super.visitQuerySpec( querySpec );
						if ( needsWrapper ) {
							if ( !needsParenthesis ) {
								appendSql( ')' );
							}
						}
						appendSql( " where rownum<=" );
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

						if ( needsWrapper ) {
							if ( needsParenthesis ) {
								appendSql( ')' );
							}
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
	protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
		// If we have a query part for row numbering, there is no need to render the order by clause
		// as that is part of the row numbering window function already, by which we then order by in the outer query
		final QueryPart queryPartForRowNumbering = getQueryPartForRowNumbering();
		if ( queryPartForRowNumbering == null ) {
			renderOrderBy( true, sortSpecifications );
		}
		else {
			// This logic is tightly coupled to #emulateFetchOffsetWithWindowFunctions and #getFetchClauseTypeForRowNumbering
			// so that this is rendered when we end up in the special case for Oracle that renders a rownum filter
			if ( getFetchClauseTypeForRowNumbering( queryPartForRowNumbering ) == null ) {
				final QuerySpec querySpec = (QuerySpec) queryPartForRowNumbering;
				if ( querySpec.getOffsetClauseExpression() == null
						&& ( !querySpec.isRoot() || getOffsetParameter() == null ) ) {
					// When rendering `rownum` for Oracle, we need to render the order by clause still
					renderOrderBy( true, sortSpecifications );
				}
			}
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
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		emulateValuesTableReferenceColumnAliasing( tableReference );
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		emulateQueryPartTableReferenceColumnAliasing( tableReference );
	}

	@Override
	public void visitFunctionTableReference(FunctionTableReference tableReference) {
		append( "table(" );
		tableReference.getFunctionExpression().accept( this );
		append( CLOSE_PARENTHESIS );
		renderTableReferenceIdentificationVariable( tableReference );
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
		if ( !queryPart.hasSortSpecifications() ) {
			// Oracle doesn't allow an empty over clause for the row_number() function
			appendSql( "rownum" );
		}
		else {
			super.renderRowNumber( selectClause, queryPart );
		}
	}

	@Override
	public void visitOver(Over<?> over) {
		final Expression expression = over.getExpression();
		if ( expression instanceof FunctionExpression && StandardFunctions.ROW_NUMBER.equals( ( (FunctionExpression) expression ).getFunctionName() ) ) {
			if ( over.getPartitions().isEmpty() && over.getOrderList().isEmpty()
					&& over.getStartKind() == FrameKind.UNBOUNDED_PRECEDING
					&& over.getEndKind() == FrameKind.CURRENT_ROW
					&& over.getExclusion() == FrameExclusion.NO_OTHERS ) {
				// Oracle doesn't allow an empty over clause for the row_number() function
				append( "rownum" );
				return;
			}
		}
		super.visitOver( over );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType == null ) {
			renderComparisonEmulateDecode( lhs, operator, rhs );
			return;
		}
		switch ( lhsExpressionType.getJdbcMappings().get( 0 ).getJdbcType().getJdbcTypeCode() ) {
			case SqlTypes.SQLXML:
				// In Oracle, XMLTYPE is not "comparable", so we have to use the xmldiff function for this purpose
				switch ( operator ) {
					case EQUAL:
					case NOT_DISTINCT_FROM:
						appendSql( "0=" );
						break;
					case NOT_EQUAL:
					case DISTINCT_FROM:
						appendSql( "1=" );
						break;
					default:
						renderComparisonEmulateDecode( lhs, operator, rhs );
						return;
				}
				appendSql( "existsnode(xmldiff(" );
				lhs.accept( this );
				appendSql( ',' );
				rhs.accept( this );
				appendSql( "),'/*[local-name()=''xdiff'']/*')" );
				break;
			case SqlTypes.BLOB:
				// In Oracle, BLOB types are not "comparable", so we have to use the dbms_lob.compare function for this purpose
				switch ( operator ) {
					case EQUAL:
						appendSql( "0=" );
						break;
					case NOT_EQUAL:
						appendSql( "-1=" );
						break;
					default:
						renderComparisonEmulateDecode( lhs, operator, rhs );
						return;
				}
				appendSql( "dbms_lob.compare(" );
				lhs.accept( this );
				appendSql( ',' );
				rhs.accept( this );
				appendSql( ')' );
				break;
			default:
				renderComparisonEmulateDecode( lhs, operator, rhs );
				break;
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
	protected void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression, boolean inSelect) {
		// Oracle did not add support for CASE until 9i
		if ( getDialect().getVersion().isBefore( 9 ) ) {
			visitDecodeCaseSearchedExpression( caseSearchedExpression );
		}
		else {
			super.visitCaseSearchedExpression( caseSearchedExpression, inSelect );
		}
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

	@Override
	protected boolean supportsDuplicateSelectItemsInQueryGroup() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInInList() {
		return getDialect().getVersion().isSameOrAfter( 8, 2 );
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInInSubQuery() {
		return getDialect().getVersion().isSameOrAfter( 9 );
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

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		final BinaryArithmeticOperator operator = arithmeticExpression.getOperator();
		if ( operator == BinaryArithmeticOperator.MODULO ) {
			append( StandardFunctions.MOD );
			appendSql( OPEN_PARENTHESIS );
			arithmeticExpression.getLeftHandOperand().accept( this );
			appendSql( ',' );
			arithmeticExpression.getRightHandOperand().accept( this );
			appendSql( CLOSE_PARENTHESIS );
			return;
		}
		else {
			super.visitBinaryArithmeticExpression( arithmeticExpression );
		}
	}

}
