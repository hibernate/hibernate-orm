/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class Selection {
	private final Expression selectedExpression;
	private final String resultVariable;

	private Return queryReturn;

	public Selection(
			Expression selectedExpression,
			String resultVariable) {
		assert selectedExpression != null;
		assert selectedExpression.getSelectable() != null;

		this.selectedExpression = selectedExpression;
		this.resultVariable = resultVariable;
	}

	public Expression getSelectedExpression() {
		return selectedExpression;
	}

	public String getResultVariable() {
		return resultVariable;
	}
}
