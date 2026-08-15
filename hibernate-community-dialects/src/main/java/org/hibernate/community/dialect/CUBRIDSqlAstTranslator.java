/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;
import java.util.Locale;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.internal.TableGroupHelper;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for CUBRID.
 *
 * @author Christian Beikov
 */
public class CUBRIDSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public CUBRIDSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		renderCombinedLimitClause( queryPart );
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete" );
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.DELETE );
			renderTableReferenceIdentificationVariable( statement.getTargetTable() );
			if ( statement.getFromClause().getRoots().isEmpty() ) {
				appendSql( " from " );
				renderDmlTargetTableExpression( statement.getTargetTable() );
			}
			else {
				visitFromClause( statement.getFromClause() );
			}
		}
		finally {
			clauseStack.pop();
		}
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		if ( updateStatement.getFromClause().getRoots().isEmpty() ) {
			super.renderUpdateClause( updateStatement );
		}
		else {
			appendSql( "update " );
			renderFromClauseSpaces( updateStatement.getFromClause() );
		}
	}

	@Override
	protected void appendAssignmentColumn(ColumnReference column) {
		column.appendColumnForWrite(
				this,
				getAffectedTableNames().size() > 1 && !( getStatement() instanceof InsertSelectStatement )
						? determineColumnReferenceQualifier( column )
						: null
		);
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
		if ( tableReference.isLateral() ) {
			// CUBRID supports the correlated derived table but not the LATERAL keyword, so omit it
			tableReference.accept( this );
		}
		else {
			super.renderDerivedTableReference( tableReference );
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		// CUBRID has the null-safe '<=>' operator; use it for distinct-from instead of the INTERSECT emulation
		renderComparisonDistinctOperator( lhs, operator, rhs );
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

	private boolean flattening;

	@Override
	protected void renderTableGroupJoin(TableGroupJoin tableGroupJoin, List<TableGroupJoin> tableGroupJoinCollector) {
		//CUBRID's grammar has no production for a parenthesized joined table, so a real table
		//group is a syntax error; when the group is joined with an inner join we can render it
		//flat and move the join predicate to the where clause instead
		if ( canFlatten( tableGroupJoin ) ) {
			appendSql( WHITESPACE );
			appendSql( "cross join " );
			final boolean outer = flattening;
			flattening = true;
			try {
				renderJoinedTableGroup( tableGroupJoin, null, tableGroupJoinCollector );
			}
			finally {
				flattening = outer;
			}
			final Predicate predicate = tableGroupJoin.getPredicate();
			if ( predicate != null && !predicate.isEmpty() ) {
				addAdditionalWherePredicate( predicate );
			}
		}
		else {
			super.renderTableGroupJoin( tableGroupJoin, tableGroupJoinCollector );
		}
	}

	@Override
	protected boolean hasNestedTableGroupsToRender(List<TableGroupJoin> nestedTableGroupJoins) {
		return !flattening && super.hasNestedTableGroupsToRender( nestedTableGroupJoins );
	}

	private boolean canFlatten(TableGroupJoin tableGroupJoin) {
		final SqlAstJoinType joinType = tableGroupJoin.getJoinType();
		if ( joinType != SqlAstJoinType.INNER && joinType != SqlAstJoinType.CROSS ) {
			return false;
		}
		final TableGroup tableGroup = tableGroupJoin.getJoinedGroup();
		if ( !tableGroup.isRealTableGroup() ) {
			return false;
		}
		//only when the group would actually be rendered with parentheses
		if ( !super.hasNestedTableGroupsToRender( tableGroup.getNestedTableGroupJoins() )
				&& TableGroupHelper.findReferenceJoinForPredicateSwap( tableGroup, tableGroupJoin.getPredicate() )
						!= TableGroupHelper.REAL_TABLE_GROUP_REQUIRED ) {
			return false;
		}
		//hoisting the predicate to the where clause filters rows an outer join would have
		//null-extended, so decline as soon as the query contains one
		return !( getCurrentQueryPart() instanceof QuerySpec querySpec )
			|| !hasOuterJoin( querySpec.getFromClause() );
	}

	private static boolean hasOuterJoin(FromClause fromClause) {
		for ( TableGroup root : fromClause.getRoots() ) {
			if ( hasOuterJoin( root ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasOuterJoin(TableGroup tableGroup) {
		return hasOuterJoin( tableGroup.getTableGroupJoins() )
			|| hasOuterJoin( tableGroup.getNestedTableGroupJoins() );
	}

	private static boolean hasOuterJoin(List<TableGroupJoin> tableGroupJoins) {
		for ( TableGroupJoin join : tableGroupJoins ) {
			final SqlAstJoinType joinType = join.getJoinType();
			if ( joinType == SqlAstJoinType.RIGHT || joinType == SqlAstJoinType.FULL
					|| hasOuterJoin( join.getJoinedGroup() ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void renderCteColumns(CteStatement cte) {
		//hibernate-core emits the CTE column list unquoted, including the 'depth' column it
		//synthesizes for a recursive search clause, and DEPTH is a CUBRID reserved word
		final StringBuilder buffer = getSqlBuffer();
		final int start = buffer.length();
		super.renderCteColumns( cte );
		final String[] columnNames = buffer.substring( start ).split( "," );
		final StringBuilder quoted = new StringBuilder();
		for ( int i = 0; i < columnNames.length; i++ ) {
			if ( i > 0 ) {
				quoted.append( ',' );
			}
			final String columnName = columnNames[i];
			quoted.append( getDialect().getKeywords().contains( columnName.toLowerCase( Locale.ROOT ) )
					? getDialect().toQuotedIdentifier( columnName )
					: columnName );
		}
		buffer.replace( start, buffer.length(), quoted.toString() );
	}

	@Override
	protected void renderDerivedTableReferenceIdentificationVariable(DerivedTableReference tableReference) {
		//hibernate-core emits the derived-table column list unquoted, and some of those aliases
		//are CUBRID reserved words
		final String identificationVariable = tableReference.getIdentificationVariable();
		if ( identificationVariable != null ) {
			append( WHITESPACE );
			append( identificationVariable );
			final List<String> columnNames = tableReference.getColumnNames();
			append( '(' );
			for ( int i = 0; i < columnNames.size(); i++ ) {
				if ( i != 0 ) {
					append( ',' );
				}
				final String columnName = columnNames.get( i );
				append( getDialect().getKeywords().contains( columnName.toLowerCase( Locale.ROOT ) )
						? getDialect().toQuotedIdentifier( columnName )
						: columnName );
			}
			append( ')' );
		}
	}

	@Override
	public void visitInListPredicate(InListPredicate inListPredicate) {
		final List<Expression> listExpressions = inListPredicate.getListExpressions();
		//CUBRID reads 'x in ((select ...))' as a list holding one scalar subquery and fails as
		//soon as the subquery returns more than one row, so render it as a plain in-subquery
		if ( listExpressions.size() == 1 && listExpressions.get( 0 ) instanceof SelectStatement subQuery ) {
			visitInSubQueryPredicate( new InSubQueryPredicate(
					inListPredicate.getTestExpression(),
					subQuery,
					inListPredicate.isNegated(),
					inListPredicate.getExpressionType()
			) );
		}
		else {
			super.visitInListPredicate( inListPredicate );
		}
	}

}
