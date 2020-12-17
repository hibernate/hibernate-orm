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
 * A SQL AST translator for Ingres.
 *
 * @author Christian Beikov
 */
public class IngresSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public IngresSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
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
		if ( !supportsOffsetFetchClause() ) {
			renderFirstClause( (QuerySpec) getQueryPartStack().getCurrent() );
		}
		super.visitSqlSelections( selectClause );
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
		if ( supportsOffsetFetchClause() ) {
			renderOffsetFetchClause( queryPart, false );
		}
		else if ( !queryPart.isRoot() && queryPart.getOffsetClauseExpression() != null ) {
			throw new IllegalArgumentException( "Can't emulate offset clause in subquery" );
		}
	}

	@Override
	protected void renderSearchClause(CteStatement cte) {
		// Ingres does not support this, but it's just a hint anyway
	}

	@Override
	protected void renderCycleClause(CteStatement cte) {
		// Ingres does not support this, but it can be emulated
	}

	@Override
	protected boolean needsRowsToSkip() {
		return !supportsOffsetFetchClause();
	}

	private boolean supportsParameterOffsetFetchExpression() {
		return getDialect().getVersion() >= 930;
	}

	private boolean supportsOffsetFetchClause() {
		return getDialect().getVersion() >= 930;
	}

}
