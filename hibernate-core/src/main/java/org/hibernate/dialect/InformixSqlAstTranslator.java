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
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Informix.
 *
 * @author Christian Beikov
 */
public class InformixSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public InformixSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected void visitSqlSelections(SelectClause selectClause) {
		if ( supportsSkipFirstClause() ) {
			renderSkipFirstClause( (QuerySpec) getQueryPartStack().getCurrent() );
		}
		else {
			renderFirstClause( (QuerySpec) getQueryPartStack().getCurrent() );
		}
		super.visitSqlSelections( selectClause );
	}

	@Override
	protected boolean needsRowsToSkip() {
		return !supportsSkipFirstClause();
	}

	@Override
	protected void renderFetchPlusOffsetExpression(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		renderFetchPlusOffsetExpressionAsSingleParameter( fetchClauseExpression, offsetClauseExpression, offset );
	}

	@Override
	protected void renderFetchExpression(Expression fetchExpression) {
		if ( supportsParameterOffsetFetchExpression() ) {
			super.renderFetchExpression( fetchExpression );
		}
		else {
			renderExpressionAsLiteral( fetchExpression, getJdbcParameterBindings() );
		}
	}

	@Override
	protected void renderOffsetExpression(Expression offsetExpression) {
		if ( supportsParameterOffsetFetchExpression() ) {
			super.renderOffsetExpression( offsetExpression );
		}
		else {
			renderExpressionAsLiteral( offsetExpression, getJdbcParameterBindings() );
		}
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		// Informix only supports the SKIP clause in the top level query
		if ( !queryPart.isRoot() && queryPart.getOffsetClauseExpression() != null ) {
			throw new IllegalArgumentException( "Can't emulate offset clause in subquery" );
		}
	}

	@Override
	protected void renderSearchClause(CteStatement cte) {
		// Informix does not support this, but it's just a hint anyway
	}

	@Override
	protected void renderCycleClause(CteStatement cte) {
		// Informix does not support this, but it can be emulated
	}

	private boolean supportsParameterOffsetFetchExpression() {
		return getDialect().getVersion() >= 11;
	}

	private boolean supportsSkipFirstClause() {
		return getDialect().getVersion() >= 11;
	}
}
