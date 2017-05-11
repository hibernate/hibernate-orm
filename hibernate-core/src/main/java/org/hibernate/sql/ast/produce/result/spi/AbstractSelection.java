/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.spi;

import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.Selection;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSelection implements Selection {
	private final Expression selectedExpression;
	private final String resultVariable;

	private final QueryResultGenerator queryResultGenerator;

	public AbstractSelection(
			Expression selectedExpression,
			String resultVariable,
			QueryResultGenerator queryResultGenerator) {
		this.selectedExpression = selectedExpression;
		this.resultVariable = resultVariable;
		this.queryResultGenerator = queryResultGenerator;
	}

	@Override
	public Expression getSelectedExpression() {
		return selectedExpression;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public QueryResult createQueryResult(SqlSelectionResolver sqlSelectionResolver) {
		return queryResultGenerator.generateQueryResult( sqlSelectionResolver );
	}
}
