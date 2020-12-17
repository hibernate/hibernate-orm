/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import org.hibernate.FetchClauseType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for PostgreSQL.
 *
 * @author Christian Beikov
 */
public class PostgreSQLSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public PostgreSQLSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		if ( getQueryPartForRowNumbering() == queryPart || isRowsOnlyFetchClauseType( queryPart ) ) {
			return false;
		}
		final FetchClauseType fetchClauseType = queryPart.getFetchClauseType();
		switch ( fetchClauseType ) {
			case PERCENT_ONLY:
			case PERCENT_WITH_TIES:
				return !supportsOffsetFetchClausePercent();
			case ROWS_WITH_TIES:
				return !supportsOffsetFetchClauseWithTies();
		}
		return false;
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
				renderLimitOffsetClause( queryPart );
			}
		}
	}

	@Override
	protected void renderSearchClause(CteStatement cte) {
		// PostgreSQL does not support this, but it's just a hint anyway
	}

	@Override
	protected void renderCycleClause(CteStatement cte) {
		// PostgreSQL does not support this, but it can be emulated
	}

	private boolean supportsOffsetFetchClause() {
		return getDialect().getVersion() >= 840;
	}

	private boolean supportsOffsetFetchClauseWithTies() {
		return getDialect().getVersion() >= 1300;
	}

	private boolean supportsOffsetFetchClausePercent() {
		// Currently, percent always has to be emulated
		return false;
	}
}
