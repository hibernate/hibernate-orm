/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteStatement;
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

	public FirebirdSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Percent fetches or ties fetches aren't supported in Firebird
		// Before 3.0 there was also no support for window functions
		// Check if current query part is already row numbering to avoid infinite recursion
		return useOffsetFetchClause( queryPart ) && getQueryPartForRowNumbering() != queryPart
				&& getDialect().getVersion() >= 300 && !isRowsOnlyFetchClauseType( queryPart );
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

	private boolean supportsOffsetFetchClause() {
		return getDialect().getVersion() >= 300;
	}
}
