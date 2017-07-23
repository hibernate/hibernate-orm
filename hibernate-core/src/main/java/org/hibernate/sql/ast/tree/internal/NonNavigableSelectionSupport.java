/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal;

import org.hibernate.sql.ast.tree.spi.select.QueryResult;
import org.hibernate.sql.ast.tree.spi.select.QueryResultCreationContext;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.Selection;

/**
 * @author Steve Ebersole
 */
public abstract class NonNavigableSelectionSupport implements Selection {
	private final Expression selectedExpression;
	private final String resultVariable;

	public NonNavigableSelectionSupport(
			Expression selectedExpression,
			String resultVariable) {
		this.selectedExpression = selectedExpression;
		this.resultVariable = resultVariable;
	}

	@Override
	public Expression getSelectedExpression() {
		return selectedExpression;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	protected abstract QueryResultGenerator getQueryResultGenerator();

	@Override
	public QueryResult createQueryResult(
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return getQueryResultGenerator().generateQueryResult( sqlSelectionResolver, creationContext );
	}

	/**
	 * Only used as part of org.hibernate.sql.ast.tree.internal.NonNavigableSelectionSupport
	 *
	 * @author Steve Ebersole
	 */
	public interface QueryResultGenerator {
		QueryResult generateQueryResult(
				SqlSelectionResolver sqlSelectionResolver,
				QueryResultCreationContext creationContext);
	}
}
