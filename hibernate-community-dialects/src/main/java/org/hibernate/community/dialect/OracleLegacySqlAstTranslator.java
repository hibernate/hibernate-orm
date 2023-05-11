/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.FrameExclusion;
import org.hibernate.query.sqm.FrameKind;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.AggregateColumnWriteExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.UnionTableGroup;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.SqlTypes;

/**
 * A SQL AST translator for Oracle.
 *
 * @author Christian Beikov
 */
public class OracleLegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public OracleLegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected boolean needsRecursiveKeywordInWithClause() {
		return false;
	}

	@Override
	protected boolean supportsWithClauseInSubquery() {
		// Oracle has some limitations, see ORA-32034, so we just report false here for simplicity
		return false;
	}

	@Override
	protected boolean supportsRecursiveSearchClause() {
		return true;
	}

	@Override
	protected boolean supportsRecursiveCycleClause() {
		return true;
	}

	@Override
	public void visitSqlSelection(SqlSelection sqlSelection) {
		if ( getCurrentCteStatement() != null ) {
			if ( getCurrentCteStatement().getMaterialization() == CteMaterialization.MATERIALIZED ) {
				appendSql( "/*+ materialize */ " );
			}
		}
		super.visitSqlSelection( sqlSelection );
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

	@Override
	protected boolean shouldEmulateLateralWithIntersect(QueryPart queryPart) {
		// On Oracle 11 where there is no lateral support,
		// make sure we don't use intersect if the query has an offset/fetch clause
		return !queryPart.hasOffsetOrFetchClause();
	}

	@Override
	protected boolean supportsNestedSubqueryCorrelation() {
		// It seems it doesn't support it, at least on version 11
		return false;
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
					|| queryPart instanceof QueryGroup && getClauseStack().isEmpty() && getStatement() instanceof InsertSelectStatement;
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
						appendSql( "select * from " );
						emulateFetchOffsetWithWindowFunctionsVisitQueryPart( querySpec );
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
				if ( getQueryPartStack().depth() > 1 && queryPart.hasSortSpecifications()
						&& getQueryPartStack().peek( 1 ) instanceof QueryGroup
						&& ( queryPart.isRoot() && !hasLimit() || !queryPart.hasOffsetOrFetchClause() ) ) {
					// If the current query part has a query group parent, no offset/fetch clause, but an order by clause,
					// then we must render "offset 0 rows" as that is needed for the SQL to be valid
					appendSql( " offset 0 rows" );
				}
				else {
					renderOffsetFetchClause( queryPart, true );
				}
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
			// For regular window function usage, we render a constant order by,
			// but since this is used for emulating limit/offset anyway, this is fine
			appendSql( "rownum" );
		}
		else {
			super.renderRowNumber( selectClause, queryPart );
		}
	}

	@Override
	public void visitOver(Over<?> over) {
		final Expression expression = over.getExpression();
		if ( expression instanceof FunctionExpression && "row_number".equals( ( (FunctionExpression) expression ).getFunctionName() ) ) {
			if ( over.getPartitions().isEmpty() && over.getOrderList().isEmpty()
					&& over.getStartKind() == FrameKind.UNBOUNDED_PRECEDING
					&& over.getEndKind() == FrameKind.CURRENT_ROW
					&& over.getExclusion() == FrameExclusion.NO_OTHERS ) {
				// Oracle doesn't allow an empty over clause for the row_number() function,
				// so we order by a constant
				append( "row_number() over(order by 1)" );
				return;
			}
		}
		super.visitOver( over );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType == null || lhsExpressionType.getJdbcTypeCount() != 1 ) {
			renderComparisonEmulateDecode( lhs, operator, rhs );
			return;
		}
		switch ( lhsExpressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() ) {
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
			case SqlTypes.CLOB:
			case SqlTypes.NCLOB:
			case SqlTypes.BLOB:
				// In Oracle, BLOB, CLOB and NCLOB types are not "comparable",
				// so we have to use the dbms_lob.compare function for this purpose
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
			case SqlTypes.ARRAY:
				switch ( operator ) {
					case DISTINCT_FROM:
						appendSql( "decode(" );
						arrayToString( lhs );
						appendSql( ',' );
						arrayToString( rhs );
						appendSql( ",0,1)=1" );
						break;
					case NOT_DISTINCT_FROM:
						appendSql( "decode(" );
						arrayToString( lhs );
						appendSql( ',' );
						arrayToString( rhs );
						appendSql( ",0,1)=0" );
						break;
					default:
						arrayToString( lhs );
						appendSql( operator.sqlText() );
						arrayToString( rhs );
				}
				break;
			default:
				renderComparisonEmulateDecode( lhs, operator, rhs );
				break;
		}
	}

	private void arrayToString(Expression expression) {
		appendSql("case when ");
		expression.accept( this );
		appendSql(" is not null then (select listagg(column_value||',')");
		if ( !getDialect().getVersion().isSameOrAfter( 18 ) ) {
			// The within group clause became optional in 18
			appendSql(" within group(order by rownum)");
		}
		appendSql("||';' from table(");
		expression.accept( this );
		appendSql(")) else null end");
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
	protected void visitSetAssignment(Assignment assignment) {
		final List<ColumnReference> columnReferences = assignment.getAssignable().getColumnReferences();
		if ( columnReferences.size() == 1 ) {
			columnReferences.get( 0 ).appendColumnForWrite( this );
			appendSql( '=' );
			final Expression assignedValue = assignment.getAssignedValue();
			final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( assignedValue );
			if ( sqlTuple != null ) {
				assert sqlTuple.getExpressions().size() == 1;
				sqlTuple.getExpressions().get( 0 ).accept( this );
			}
			else {
				assignedValue.accept( this );
			}
		}
		else {
			char separator = OPEN_PARENTHESIS;
			for ( ColumnReference columnReference : columnReferences ) {
				appendSql( separator );
				columnReference.appendColumnForWrite( this );
				separator = COMA_SEPARATOR_CHAR;
			}
			appendSql( ")=" );
			assignment.getAssignedValue().accept( this );
		}
	}
}
