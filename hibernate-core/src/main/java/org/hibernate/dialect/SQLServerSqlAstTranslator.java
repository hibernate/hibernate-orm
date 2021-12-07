/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.query.FetchClauseType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for SQL Server.
 *
 * @author Christian Beikov
 */
public class SQLServerSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private static final String UNION_ALL = " union all ";

	public SQLServerSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected boolean renderTableReference(TableReference tableReference, LockMode lockMode) {
		final String tableExpression = tableReference.getTableExpression();
		if ( tableReference instanceof UnionTableReference && lockMode != LockMode.NONE && tableExpression.charAt( 0 ) == '(' ) {
			// SQL Server requires to push down the lock hint to the actual table names
			int searchIndex = 0;
			int unionIndex;
			while ( ( unionIndex = tableExpression.indexOf( UNION_ALL, searchIndex ) ) != -1 ) {
				append( tableExpression, searchIndex, unionIndex );
				renderLockHint( lockMode );
				appendSql( UNION_ALL );
				searchIndex = unionIndex + UNION_ALL.length();
			}
			append( tableExpression, searchIndex, tableExpression.length() - 2 );
			renderLockHint( lockMode );
			appendSql( " )" );

			registerAffectedTable( tableReference );
			final Clause currentClause = getClauseStack().getCurrent();
			if ( rendersTableReferenceAlias( currentClause ) ) {
				final String identificationVariable = tableReference.getIdentificationVariable();
				if ( identificationVariable != null ) {
					appendSql( ' ' );
					appendSql( identificationVariable );
				}
			}
		}
		else {
			super.renderTableReference( tableReference, lockMode );
			renderLockHint( lockMode );
		}
		// Just always return true because SQL Server doesn't support the FOR UPDATE clause
		return true;
	}

	private void renderLockHint(LockMode lockMode) {
		if ( getDialect().getVersion().isSince( 9 ) ) {
			final int effectiveLockTimeout = getEffectiveLockTimeout( lockMode );
			switch ( lockMode ) {
				//noinspection deprecation
				case UPGRADE:
				case PESSIMISTIC_WRITE:
				case WRITE:
					switch ( effectiveLockTimeout ) {
						case LockOptions.SKIP_LOCKED:
							appendSql( " with (updlock,rowlock,readpast)" );
							break;
						case LockOptions.NO_WAIT:
							appendSql( " with (updlock,holdlock,rowlock,nowait)" );
							break;
						default:
							appendSql( " with (updlock,holdlock,rowlock)" );
							break;
					}
					break;
				case PESSIMISTIC_READ:
					switch ( effectiveLockTimeout ) {
						case LockOptions.SKIP_LOCKED:
							appendSql( " with (updlock,rowlock,readpast)" );
							break;
						case LockOptions.NO_WAIT:
							appendSql( " with (holdlock,rowlock,nowait)");
							break;
						default:
							appendSql( " with (holdlock,rowlock)");
							break;
					}
					break;
				case UPGRADE_SKIPLOCKED:
					if ( effectiveLockTimeout == LockOptions.NO_WAIT ) {
						appendSql( " with (updlock,rowlock,readpast,nowait)" );
					}
					else {
						appendSql( " with (updlock,rowlock,readpast)" );
					}
					break;
				case UPGRADE_NOWAIT:
					appendSql( " with (updlock,holdlock,rowlock,nowait)" );
					break;
			}
		}
		else {
			switch ( lockMode ) {
				//noinspection deprecation
				case UPGRADE:
				case UPGRADE_NOWAIT:
				case PESSIMISTIC_WRITE:
				case WRITE:
					appendSql( " with (updlock,rowlock)" );
					break;
				case PESSIMISTIC_READ:
					appendSql(" with (holdlock,rowlock)" );
					break;
				case UPGRADE_SKIPLOCKED:
					appendSql( " with (updlock,rowlock,readpast)" );
					break;
			}
		}
	}

	@Override
	protected void renderForUpdateClause(QuerySpec querySpec, ForUpdateClause forUpdateClause) {
		// SQL Server does not support the FOR UPDATE clause
	}

	protected OffsetFetchClauseMode getOffsetFetchClauseMode(QueryPart queryPart) {
		final DatabaseVersion version = getDialect().getVersion();
		final boolean hasLimit;
		final boolean hasOffset;
		if ( queryPart.isRoot() && hasLimit() ) {
			hasLimit = getLimit().getMaxRows() != null;
			hasOffset = getLimit().getFirstRow() != null;
		}
		else {
			hasLimit = queryPart.getFetchClauseExpression() != null;
			hasOffset = queryPart.getOffsetClauseExpression() != null;
		}
		if ( queryPart instanceof QueryGroup ) {
			// We can't use TOP for set operations
			if ( hasOffset || hasLimit ) {
				if ( version.isBefore( 11 ) || !isRowsOnlyFetchClauseType( queryPart ) ) {
					return OffsetFetchClauseMode.EMULATED;
				}
				else {
					return OffsetFetchClauseMode.STANDARD;
				}
			}

			return null;
		}
		else {
			if ( version.isBefore( 9 ) || !hasOffset ) {
				return hasLimit ? OffsetFetchClauseMode.TOP_ONLY : null;
			}
			else if ( version.isBefore( 11 ) || !isRowsOnlyFetchClauseType( queryPart ) ) {
				return OffsetFetchClauseMode.EMULATED;
			}
			else {
				return OffsetFetchClauseMode.STANDARD;
			}
		}
	}

	@Override
	protected boolean supportsSimpleQueryGrouping() {
		// SQL Server is quite strict i.e. it requires `select .. union all select * from (select ...)`
		// rather than `select .. union all (select ...)` because parenthesis followed by select
		// is always treated as a subquery, which is not supported in a set operation
		return false;
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		return getQueryPartForRowNumbering() != queryPart && getOffsetFetchClauseMode( queryPart ) == OffsetFetchClauseMode.EMULATED;
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		if ( shouldEmulateFetchClause( queryGroup ) ) {
			emulateFetchOffsetWithWindowFunctions( queryGroup, !isRowsOnlyFetchClauseType( queryGroup ) );
		}
		else {
			super.visitQueryGroup( queryGroup );
		}
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		if ( shouldEmulateFetchClause( querySpec ) ) {
			emulateFetchOffsetWithWindowFunctions( querySpec, !isRowsOnlyFetchClauseType( querySpec ) );
		}
		else {
			super.visitQuerySpec( querySpec );
		}
	}

	@Override
	protected boolean needsRowsToSkip() {
		return getDialect().getVersion().isBefore( 9 );
	}

	@Override
	protected void renderFetchPlusOffsetExpression(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		renderFetchPlusOffsetExpressionAsSingleParameter( fetchClauseExpression, offsetClauseExpression, offset );
	}

	@Override
	protected void visitSqlSelections(SelectClause selectClause) {
		final QuerySpec querySpec = (QuerySpec) getQueryPartStack().getCurrent();
		final OffsetFetchClauseMode offsetFetchClauseMode = getOffsetFetchClauseMode( querySpec );
		if ( offsetFetchClauseMode == OffsetFetchClauseMode.TOP_ONLY ) {
			renderTopClause( querySpec, true, true );
		}
		else if ( offsetFetchClauseMode == OffsetFetchClauseMode.EMULATED ) {
			renderTopClause( querySpec, isRowsOnlyFetchClauseType( querySpec ), true );
		}
		super.visitSqlSelections( selectClause );
	}

	@Override
	protected void renderOrderBy(boolean addWhitespace, List<SortSpecification> sortSpecifications) {
		if ( sortSpecifications != null && !sortSpecifications.isEmpty() ) {
			super.renderOrderBy( addWhitespace, sortSpecifications );
		}
		else if ( getClauseStack().getCurrent() == Clause.OVER ) {
			if ( addWhitespace ) {
				appendSql( ' ' );
			}
			renderEmptyOrderBy();
		}
	}

	protected void renderEmptyOrderBy() {
		// Always need an order by clause: https://blog.jooq.org/2014/05/13/sql-server-trick-circumvent-missing-order-by-clause/
		appendSql( "order by @@version" );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			if ( getDialect().getVersion().isBefore( 9 ) && !queryPart.isRoot() && queryPart.getOffsetClauseExpression() != null ) {
				throw new IllegalArgumentException( "Can't emulate offset clause in subquery" );
			}
			// Note that SQL Server is very strict i.e. it requires an order by clause for TOP or OFFSET
			final OffsetFetchClauseMode offsetFetchClauseMode = getOffsetFetchClauseMode( queryPart );
			if ( offsetFetchClauseMode == OffsetFetchClauseMode.STANDARD ) {
				if ( !queryPart.hasSortSpecifications() ) {
					appendSql( ' ' );
					renderEmptyOrderBy();
				}
				final Expression offsetExpression;
				final Expression fetchExpression;
				final FetchClauseType fetchClauseType;
				if ( queryPart.isRoot() && hasLimit() ) {
					prepareLimitOffsetParameters();
					offsetExpression = getOffsetParameter();
					fetchExpression = getLimitParameter();
					fetchClauseType = FetchClauseType.ROWS_ONLY;
				}
				else {
					offsetExpression = queryPart.getOffsetClauseExpression();
					fetchExpression = queryPart.getFetchClauseExpression();
					fetchClauseType = queryPart.getFetchClauseType();
				}
				if ( offsetExpression == null ) {
					appendSql( " offset 0 rows" );
				}
				else {
					renderOffset( offsetExpression, true );
				}

				if ( fetchExpression != null ) {
					renderFetch( fetchExpression, null, fetchClauseType );
				}
			}
			else if ( offsetFetchClauseMode == OffsetFetchClauseMode.TOP_ONLY && !queryPart.hasSortSpecifications() ) {
				appendSql( ' ' );
				renderEmptyOrderBy();
			}
		}
	}

	@Override
	protected void renderSearchClause(CteStatement cte) {
		// SQL Server does not support this, but it's just a hint anyway
	}

	@Override
	protected void renderCycleClause(CteStatement cte) {
		// SQL Server does not support this, but it can be emulated
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonEmulateIntersect( lhs, operator, rhs );
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "()" );
		}
		else if ( expression instanceof Summarization ) {
			Summarization summarization = (Summarization) expression;
			renderCommaSeparated( summarization.getGroupings() );
			appendSql( " with " );
			appendSql( summarization.getKind().sqlText() );
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

	enum OffsetFetchClauseMode {
		STANDARD,
		TOP_ONLY,
		EMULATED;
	}
}
