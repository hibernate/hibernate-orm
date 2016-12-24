/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.select;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;

/**
 * @author Steve Ebersole
 */
public class Selection {
	private final ReturnResolutionContext returnResolutionContext;
	private final Selectable selectable;
	private final Expression selectExpression;
	private final String resultVariable;

	private Return queryReturn;

	public Selection(
			ReturnResolutionContext returnResolutionContext,
			Selectable selectable,
			Expression selectExpression,
			String resultVariable) {
		this.returnResolutionContext = returnResolutionContext;
		this.selectable = selectable;
		this.selectExpression = selectExpression;
		this.resultVariable = resultVariable;
	}

	public ReturnResolutionContext getReturnResolutionContext() {
		return returnResolutionContext;
	}

	public Selectable getSelectable() {
		return selectable;
	}

	public Expression getSelectExpression() {
		return selectExpression;
	}

	public String getResultVariable() {
		return resultVariable;
	}
}
