/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.select;

import org.hibernate.sql.sqm.ast.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class Selection {
	private final Expression selectExpression;
	private final String resultVariable;

	public Selection(Expression selectExpression, String resultVariable) {
		this.selectExpression = selectExpression;
		this.resultVariable = resultVariable;
	}

	public Expression getSelectExpression() {
		return selectExpression;
	}

	public String getResultVariable() {
		return resultVariable;
	}
}
