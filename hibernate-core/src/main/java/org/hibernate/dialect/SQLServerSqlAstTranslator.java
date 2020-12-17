/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.List;

import org.hibernate.FetchClauseType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
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

	public SQLServerSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	protected OffsetFetchClauseMode getOffsetFetchClauseMode(QueryPart queryPart) {
		final int version = getDialect().getVersion();
		final boolean hasLimit;
		final boolean hasOffset;
		if ( queryPart.isRoot() && hasLimit() ) {
			hasLimit = getLimit().getMaxRowsJpa() != Integer.MAX_VALUE;
			hasOffset = getLimit().getFirstRowJpa() != 0;
		}
		else {
			hasLimit = queryPart.getFetchClauseExpression() != null;
			hasOffset = queryPart.getOffsetClauseExpression() != null;
		}
		if ( version < 9 || !hasOffset ) {
			return hasLimit ? OffsetFetchClauseMode.TOP_ONLY : null;
		}
		else if ( version < 11 || !isRowsOnlyFetchClauseType( queryPart ) ) {
			return OffsetFetchClauseMode.EMULATED;
		}
		else {
			return OffsetFetchClauseMode.STANDARD;
		}
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
		return getDialect().getVersion() < 9;
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
			renderTopClause( querySpec, true );
		}
		else if ( offsetFetchClauseMode == OffsetFetchClauseMode.EMULATED ) {
			renderTopClause( querySpec, isRowsOnlyFetchClauseType( querySpec ) );
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
			if ( getDialect().getVersion() < 9 && !queryPart.isRoot() && queryPart.getOffsetClauseExpression() != null ) {
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

	enum OffsetFetchClauseMode {
		STANDARD,
		TOP_ONLY,
		EMULATED;
	}
}
