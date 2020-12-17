/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for H2.
 *
 * @author Christian Beikov
 */
public class H2SqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public H2SqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( isRowsOnlyFetchClauseType( queryPart ) ) {
			if ( supportsOffsetFetchClause() ) {
				renderOffsetFetchClause( queryPart, true );
			}
			else {
				renderLimitOffsetClause( queryPart );
			}
		}
		else {
			if ( supportsOffsetFetchClausePercentWithTies() ) {
				renderOffsetFetchClause( queryPart, true );
			}
			else {
				// FETCH PERCENT and WITH TIES were introduced along with window functions
				throw new IllegalArgumentException( "Can't emulate fetch clause type: " + queryPart.getFetchClauseType() );
			}
		}
	}

	@Override
	protected void renderSearchClause(CteStatement cte) {
		// H2 does not support this, but it's just a hint anyway
	}

	@Override
	protected void renderCycleClause(CteStatement cte) {
		// H2 does not support this, but it can be emulated
	}

	private boolean supportsOffsetFetchClause() {
		return getDialect().getVersion() >= 104195;
	}

	private boolean supportsOffsetFetchClausePercentWithTies() {
		// Introduction of TIES clause https://github.com/h2database/h2database/commit/876e9fbe7baf11d01675bfe871aac2cf1b6104ce
		// Introduction of PERCENT support https://github.com/h2database/h2database/commit/f45913302e5f6ad149155a73763c0c59d8205849
		return getDialect().getVersion() >= 104198;
	}
}
